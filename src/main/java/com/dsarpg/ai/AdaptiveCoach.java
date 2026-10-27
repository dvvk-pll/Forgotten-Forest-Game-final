package com.dsarpg.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.dsarpg.AdaptiveEngine;
import com.dsarpg.DSAQuestion;

/**
 * The AI Coach: a hybrid adaptive system that sits on top of the original
 * rule-based {@link AdaptiveEngine}.
 *
 * Components
 *  - Bayesian Knowledge Tracing  : per-topic mastery estimate (AI)
 *  - Online logistic regression  : predicts P(correct) for each candidate
 *                                  question, pre-trained on simulated
 *                                  learners and personalised live (ML)
 *  - Engagement estimator        : frustrated / in flow / bored (AI)
 *  - Thompson-sampling bandit    : learns which supportive intervention
 *                                  works best for this player (ML/RL)
 *  - Original rule-based engine  : topic and difficulty rules, still used
 *                                  as one signal in the question score
 *
 * Question choice (hybrid score for every fresh candidate):
 *   score = 0.50 * fit      how close the predicted success is to the target
 *         + 0.20 * rules    agreement with the original rule-based engine
 *         + 0.25 * need     1 - mastery, so weak topics are practised more
 *         + 0.05 * variety  small bonus for switching topic
 * The target success rate depends on the engagement state: easier when the
 * player is frustrated, harder when bored, around 70% for flow (the "zone of
 * proximal development").
 *
 * When the coach is switched off, the original engine runs unchanged, which
 * allows a fair A/B comparison for usability studies.
 */
public final class AdaptiveCoach {

    // ------------------------------------------------------------------
    // RESULT TYPES
    // ------------------------------------------------------------------

    /** Everything the battle screen needs to know about the next question. */
    public static final class QuestionPlan {
        public final DSAQuestion question;
        public final double predictedSuccess;
        public final double targetSuccess;
        public final PlayerState state;
        public final Intervention intervention;
        public final int[] removedOptions;
        public final boolean aiSelected;
        public final String reason;
        public final String coachMessage;
        final double[] features;

        QuestionPlan(DSAQuestion question, double predictedSuccess,
                     double targetSuccess, PlayerState state,
                     Intervention intervention, int[] removedOptions,
                     boolean aiSelected, String reason,
                     String coachMessage, double[] features) {
            this.question = question;
            this.predictedSuccess = predictedSuccess;
            this.targetSuccess = targetSuccess;
            this.state = state;
            this.intervention = intervention;
            this.removedOptions = removedOptions;
            this.aiSelected = aiSelected;
            this.reason = reason;
            this.coachMessage = coachMessage;
            this.features = features;
        }

        public boolean isOptionRemoved(int index) {
            for (int removed : removedOptions) {
                if (removed == index) {
                    return true;
                }
            }

            return false;
        }

        public boolean hintOpenedByCoach() {
            return intervention == Intervention.HINT_BOOST;
        }

        public int xpMultiplier() {
            return intervention == Intervention.BONUS_XP ? 2 : 1;
        }
    }

    /** What happened when the answer was recorded. */
    public static final class AnswerResult {
        public final boolean correct;
        public final double masteryBefore;
        public final double masteryAfter;
        public final int xpMultiplier;
        public final boolean shieldTriggered;

        AnswerResult(boolean correct, double masteryBefore,
                     double masteryAfter, int xpMultiplier,
                     boolean shieldTriggered) {
            this.correct = correct;
            this.masteryBefore = masteryBefore;
            this.masteryAfter = masteryAfter;
            this.xpMultiplier = xpMultiplier;
            this.shieldTriggered = shieldTriggered;
        }
    }

    /** One row of the learning-insights screen. */
    public static final class TopicInsight {
        public final String topic;
        public final double mastery;
        public final int attempts;
        public final int correct;

        TopicInsight(String topic, double mastery, int attempts, int correct) {
            this.topic = topic;
            this.mastery = mastery;
            this.attempts = attempts;
            this.correct = correct;
        }
    }

    // ------------------------------------------------------------------
    // STATE
    // ------------------------------------------------------------------

    private static final double EXPLORE_RATE = 0.10;
    private static final int RECENT_WINDOW = 5;

    private final String username;
    private final Random rng;
    private final BayesianKnowledgeTracer bkt = new BayesianKnowledgeTracer();
    private final CorrectnessPredictor predictor = new CorrectnessPredictor();
    private final EngagementEstimator estimator = new EngagementEstimator();
    private final InterventionBandit bandit = new InterventionBandit();

    private boolean aiEnabled = true;
    private int consecutiveHelped = 0;
    private int totalAnswers = 0;
    private int sessionAnswers = 0;
    private int sessionCorrect = 0;
    private String lastTopic = "";
    private QuestionPlan lastPlan;

    /** Creates a coach, restoring saved state when available. */
    public AdaptiveCoach(String username, Properties saved, Random rng) {
        this.username = username == null ? "player" : username;
        this.rng = rng == null ? new Random() : rng;

        boolean restored = false;

        if (saved != null) {
            restored = predictor.load(saved);

            for (String key : saved.stringPropertyNames()) {
                if (key.startsWith("bkt.")) {
                    try {
                        bkt.set(key.substring(4),
                                Double.parseDouble(saved.getProperty(key)));
                    } catch (NumberFormatException ex) {
                        // Ignore a damaged value.
                    }
                }
            }

            bandit.load(saved);

            try {
                totalAnswers = Integer.parseInt(
                        saved.getProperty("answers.total", "0"));
            } catch (NumberFormatException ex) {
                totalAnswers = 0;
            }
        }

        if (!restored) {
            // Cold start: learn a prior from simulated learners.
            SyntheticPretrainer.pretrain(predictor, new Random(42));
        }
    }

    public AdaptiveCoach(String username) {
        this(username, null, null);
    }

    public String getUsername() {
        return username;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean enabled) {
        this.aiEnabled = enabled;
    }

    /** Call when a new play session starts (login). */
    public void startSession() {
        predictor.startSession();
        estimator.reset();
        consecutiveHelped = 0;
        sessionAnswers = 0;
        sessionCorrect = 0;
        lastTopic = "";
        lastPlan = null;
    }

    // ------------------------------------------------------------------
    // PLANNING THE NEXT QUESTION
    // ------------------------------------------------------------------

    public static double targetFor(PlayerState state) {
        switch (state) {
            case FRUSTRATED:
                return 0.85;
            case BORED:
                return 0.55;
            case WARMING_UP:
                return 0.78;
            case FLOW:
            default:
                return 0.70;
        }
    }

    public QuestionPlan planQuestion(AdaptiveEngine engine) {
        PlayerState state = estimator.state();
        double target = targetFor(state);

        // Baseline mode: the original algorithm, untouched.
        if (!aiEnabled) {
            DSAQuestion q = engine.nextQuestion();
            double[] x = predictor.features(q, bkt);

            lastPlan = new QuestionPlan(q, predictor.predict(x), target,
                    state, Intervention.NONE, new int[0], false,
                    "AI Coach is off: original rule-based selection.",
                    "", x);
            lastTopic = q.getTopic();
            return lastPlan;
        }

        // Avoid the last few questions, but allow a repeat after a short gap
        // so the models can choose the best-fitting difficulty.
        List<DSAQuestion> pool = engine.candidatesAvoidingRecent(RECENT_WINDOW);
        String ruleTopic = engine.ruleBasedTopic();

        List<Scored> scored = new ArrayList<>();

        for (DSAQuestion q : pool) {
            double[] x = predictor.features(q, bkt);
            double p = predictor.predict(x);

            double fit = 1.0 - Math.min(1.0, Math.abs(p - target) / 0.5);

            double rules = 0;

            if (q.getTopic().equals(ruleTopic)) {
                rules += 0.5;
            }

            int gap = Math.abs(
                    q.getDifficulty() - engine.ruleBasedDifficulty(q.getTopic()));

            rules += gap == 0 ? 0.5 : (gap == 1 ? 0.2 : 0);

            double need = 1.0 - bkt.mastery(q.getTopic());
            double variety = q.getTopic().equals(lastTopic) ? 0 : 1;

            double score = 0.50 * fit + 0.20 * rules
                    + 0.25 * need + 0.05 * variety
                    + 0.01 * rng.nextDouble();

            scored.add(new Scored(q, x, p, score));
        }

        Collections.sort(scored, new Comparator<Scored>() {
            @Override
            public int compare(Scored a, Scored b) {
                return Double.compare(b.score, a.score);
            }
        });

        // Small chance of exploring one of the top three alternatives.
        int index = 0;

        if (scored.size() > 1 && rng.nextDouble() < EXPLORE_RATE) {
            index = rng.nextInt(Math.min(3, scored.size()));
        }

        Scored pick = scored.get(index);
        engine.select(pick.question);

        // Choose a supportive (or challenging) intervention.
        Intervention intervention = bandit.choose(state, rng);

        if (consecutiveHelped >= 2
                && intervention != Intervention.NONE
                && intervention != Intervention.BONUS_XP) {
            intervention = Intervention.NONE;
        }

        boolean support = intervention == Intervention.FIFTY_FIFTY
                || intervention == Intervention.SHIELD
                || intervention == Intervention.HINT_BOOST;

        // No need to help on a question the player will very likely get.
        if (support && pick.probability >= 0.85) {
            intervention = Intervention.NONE;
            support = false;
        }

        consecutiveHelped = support ? consecutiveHelped + 1 : 0;

        int[] removed = new int[0];

        if (intervention == Intervention.FIFTY_FIFTY) {
            removed = pickWrongOptions(pick.question, 2);
        }

        String reason = String.format(
                "Predicted success %d%% (target %d%%), %s mastery %d%%, "
                + "state: %s.",
                Math.round(pick.probability * 100),
                Math.round(target * 100),
                pick.question.getTopic(),
                Math.round(bkt.mastery(pick.question.getTopic()) * 100),
                state.getLabel());

        String message = intervention == Intervention.NONE
                ? defaultMessage(state)
                : intervention.getMessage();

        lastTopic = pick.question.getTopic();
        lastPlan = new QuestionPlan(pick.question, pick.probability, target,
                state, intervention, removed, true, reason, message,
                pick.features);

        return lastPlan;
    }

    private int[] pickWrongOptions(DSAQuestion q, int count) {
        List<Integer> wrong = new ArrayList<>();

        for (int i = 0; i < q.getOptions().length; i++) {
            if (i != q.getCorrectAnswer()) {
                wrong.add(i);
            }
        }

        Collections.shuffle(wrong, rng);

        int n = Math.min(count, wrong.size());
        int[] result = new int[n];

        for (int i = 0; i < n; i++) {
            result[i] = wrong.get(i);
        }

        return result;
    }

    private static String defaultMessage(PlayerState state) {
        switch (state) {
            case FRUSTRATED:
                return "Take your time. A hint is one click away.";
            case BORED:
                return "Nice pace! Expect tougher questions from here.";
            case FLOW:
                return "You are in the zone. Keep it up!";
            case WARMING_UP:
            default:
                return "Warm-up: your answers shape your learning path.";
        }
    }

    private static final class Scored {
        final DSAQuestion question;
        final double[] features;
        final double probability;
        final double score;

        Scored(DSAQuestion question, double[] features,
               double probability, double score) {
            this.question = question;
            this.features = features;
            this.probability = probability;
            this.score = score;
        }
    }

    // ------------------------------------------------------------------
    // RECORDING AN ANSWER
    // ------------------------------------------------------------------

    public AnswerResult recordAnswer(QuestionPlan plan, int chosenIndex,
                                     long responseMillis, boolean hintUsed,
                                     AdaptiveEngine engine) {

        DSAQuestion q = plan.question;
        boolean correct = chosenIndex == q.getCorrectAnswer();
        double seconds = responseMillis / 1000.0;
        boolean hinted = hintUsed || plan.hintOpenedByCoach();

        double before = bkt.mastery(q.getTopic());
        double after = bkt.update(q.getTopic(), correct, hinted);

        predictor.observe(q.getTopic(), plan.features, correct, seconds, hinted);
        estimator.record(correct, seconds, hinted);

        if (engine != null) {
            engine.recordAnswer(q, correct);
        }

        boolean shieldTriggered = !correct
                && plan.intervention == Intervention.SHIELD;

        if (aiEnabled && plan.aiSelected) {
            double reward = 0.5 + 0.5 * ((correct ? 1.0 : 0.0)
                    - plan.predictedSuccess);

            if (shieldTriggered) {
                reward += 0.2;
            }

            bandit.update(plan.state, plan.intervention, reward);
        }

        totalAnswers++;
        sessionAnswers++;

        if (correct) {
            sessionCorrect++;
        }

        return new AnswerResult(correct, before, after,
                plan.xpMultiplier(), shieldTriggered);
    }

    // ------------------------------------------------------------------
    // INSIGHTS FOR THE UI
    // ------------------------------------------------------------------

    public PlayerState getState() {
        return estimator.state();
    }

    public double getFlowScore() {
        return estimator.flowScore();
    }

    public double getFrustration() {
        return estimator.frustration();
    }

    public double getBoredom() {
        return estimator.boredom();
    }

    public double getMastery(String topic) {
        return bkt.mastery(topic);
    }

    /** Average mastery over a set of topics (0-1). */
    public double readiness(List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return 0;
        }

        double sum = 0;

        for (String topic : topics) {
            sum += bkt.mastery(topic);
        }

        return sum / topics.size();
    }

    public List<TopicInsight> insights(List<String> topics,
                                       AdaptiveEngine engine) {
        List<TopicInsight> list = new ArrayList<>();

        for (String topic : topics) {
            list.add(new TopicInsight(
                    topic,
                    bkt.mastery(topic),
                    engine == null ? predictor.getTopicAttempts(topic)
                            : engine.getAttempts(topic),
                    engine == null ? 0 : engine.getCorrect(topic)));
        }

        return list;
    }

    public QuestionPlan getLastPlan() {
        return lastPlan;
    }

    public int getTotalAnswers() {
        return totalAnswers;
    }

    public int getSessionAnswers() {
        return sessionAnswers;
    }

    public int getSessionCorrect() {
        return sessionCorrect;
    }

    /**
     * Plain-language description of what the bandit has learned for the
     * current state, or null when it has not seen enough data yet.
     */
    public String learnedPreference(PlayerState state) {
        Intervention best = null;
        double bestValue = -1;

        for (Intervention action : InterventionBandit.legalActions(state)) {
            if (action == Intervention.NONE
                    || bandit.pulls(state, action) < 2) {
                continue;
            }

            double value = bandit.expectedReward(state, action);

            if (value > bestValue) {
                bestValue = value;
                best = action;
            }
        }

        if (best == null) {
            return null;
        }

        return best.getLabel() + " (" + Math.round(bestValue * 100)
                + "% reward)";
    }

    public InterventionBandit getBandit() {
        return bandit;
    }

    public CorrectnessPredictor getPredictor() {
        return predictor;
    }

    public BayesianKnowledgeTracer getKnowledgeTracer() {
        return bkt;
    }

    // ------------------------------------------------------------------
    // PERSISTENCE
    // ------------------------------------------------------------------

    public Properties toProperties() {
        Properties p = new Properties();

        for (java.util.Map.Entry<String, Double> e
                : bkt.snapshot().entrySet()) {
            p.setProperty("bkt." + e.getKey(), Double.toString(e.getValue()));
        }

        predictor.save(p);
        bandit.save(p);
        p.setProperty("answers.total", Integer.toString(totalAnswers));
        return p;
    }
}
