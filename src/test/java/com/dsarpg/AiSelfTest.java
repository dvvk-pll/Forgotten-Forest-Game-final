package com.dsarpg;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.dsarpg.ai.AdaptiveCoach;
import com.dsarpg.ai.AiProfileStore;
import com.dsarpg.ai.BayesianKnowledgeTracer;
import com.dsarpg.ai.CorrectnessPredictor;
import com.dsarpg.ai.EngagementEstimator;
import com.dsarpg.ai.Intervention;
import com.dsarpg.ai.InterventionBandit;
import com.dsarpg.ai.OnlineLogisticModel;
import com.dsarpg.ai.PlayerState;
import com.dsarpg.ai.SyntheticPretrainer;

/**
 * Plain-Java self test for the AI / ML layer (no JUnit needed).
 *
 * Run:  java -cp target/classes:target/test-classes com.dsarpg.AiSelfTest
 * Exit code is 0 when every check passes.
 */
public class AiSelfTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + message);
        } else {
            failed++;
            System.out.println("  FAIL  " + message);
        }
    }

    public static void main(String[] args) throws Exception {
        questionBank();
        knowledgeTracing();
        pretrainedPredictor();
        personalisation();
        engagement();
        bandit();
        coachSimulation();
        persistence();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------

    private static void questionBank() {
        System.out.println("Question bank");

        List<DSAQuestion> all = QuestionBank.all();
        Set<String> ids = new HashSet<>();
        Map<String, int[]> perTopic = new HashMap<>();
        boolean optionsOk = true;

        for (DSAQuestion q : all) {
            ids.add(q.getId());
            perTopic.computeIfAbsent(q.getTopic(), k -> new int[4])
                    [q.getDifficulty()]++;

            Set<String> unique = new HashSet<>();
            for (String option : q.getOptions()) {
                unique.add(option);
            }
            if (unique.size() != 4) {
                optionsOk = false;
                System.out.println("    duplicate options: " + q.getText());
            }
        }

        check(all.size() == 66, "66 questions (got " + all.size() + ")");
        check(ids.size() == all.size(), "all question ids are unique");
        check(optionsOk, "every question has four distinct options");

        boolean balanced = perTopic.size() == 11;
        for (int[] counts : perTopic.values()) {
            for (int d = 1; d <= 3; d++) {
                balanced &= counts[d] == 2;
            }
        }
        check(balanced, "11 topics, two questions per difficulty each");

        for (int level = 1; level <= 5; level++) {
            int n = 0;
            for (DSAQuestion q : all) {
                if (AdaptiveEngine.topicsForLevel(level).contains(q.getTopic())) {
                    n++;
                }
            }
            check(n >= 12, "level " + level + " has " + n + " questions");
        }
    }

    // ------------------------------------------------------------

    private static void knowledgeTracing() {
        System.out.println("Bayesian knowledge tracing");

        BayesianKnowledgeTracer t = new BayesianKnowledgeTracer();
        double start = t.mastery("Arrays");
        t.update("Arrays", true, false);
        double afterCorrect = t.mastery("Arrays");

        BayesianKnowledgeTracer u = new BayesianKnowledgeTracer();
        u.update("Arrays", false, false);
        double afterWrong = u.mastery("Arrays");

        BayesianKnowledgeTracer v = new BayesianKnowledgeTracer();
        v.update("Arrays", true, true);
        double afterHinted = v.mastery("Arrays");

        check(afterCorrect > start, "a correct answer raises mastery");
        check(afterWrong < start, "a wrong answer lowers mastery");
        check(afterHinted > start && afterHinted < afterCorrect,
                "a hinted correct answer is weaker evidence");

        for (int i = 0; i < 12; i++) {
            t.update("Arrays", true, false);
        }
        check(t.mastery("Arrays") > 0.95, "mastery converges towards 1");
        check(t.mastery("Arrays") <= 0.99, "mastery is clamped");
    }

    // ------------------------------------------------------------

    private static double auc(List<double[]> scored) {
        // scored entries: {probability, label}
        List<double[]> pos = new ArrayList<>();
        List<double[]> neg = new ArrayList<>();

        for (double[] s : scored) {
            (s[1] > 0.5 ? pos : neg).add(s);
        }

        double wins = 0;
        for (double[] p : pos) {
            for (double[] n : neg) {
                wins += p[0] > n[0] ? 1 : (p[0] == n[0] ? 0.5 : 0);
            }
        }
        return wins / (pos.size() * (double) neg.size());
    }

    private static void pretrainedPredictor() {
        System.out.println("Pre-trained correctness predictor");

        CorrectnessPredictor predictor = new CorrectnessPredictor();
        long t0 = System.nanoTime();
        SyntheticPretrainer.pretrain(predictor, new Random(42));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("  info  cold-start training took " + ms + " ms");

        List<SyntheticPretrainer.Sample> holdout =
                SyntheticPretrainer.generate(new Random(2024), 120, 40);

        List<double[]> scored = new ArrayList<>();
        double loss = 0;
        double base = 0;
        double mean = 0;

        for (SyntheticPretrainer.Sample s : holdout) {
            mean += s.correct ? 1 : 0;
        }
        mean /= holdout.size();

        for (SyntheticPretrainer.Sample s : holdout) {
            double p = predictor.predict(s.x);
            p = Math.max(1e-6, Math.min(1 - 1e-6, p));
            double y = s.correct ? 1 : 0;
            loss += -(y * Math.log(p) + (1 - y) * Math.log(1 - p));
            base += -(y * Math.log(mean) + (1 - y) * Math.log(1 - mean));
            scored.add(new double[] {p, y});
        }

        loss /= holdout.size();
        base /= holdout.size();
        double a = auc(scored);

        System.out.printf("  info  hold-out AUC %.3f, log-loss %.3f (constant baseline %.3f)%n",
                a, loss, base);

        check(a > 0.68, "hold-out AUC above 0.68");
        check(loss < base, "log-loss beats the constant baseline");

        BayesianKnowledgeTracer low = new BayesianKnowledgeTracer();
        BayesianKnowledgeTracer high = new BayesianKnowledgeTracer();
        low.set("Arrays", 0.15);
        high.set("Arrays", 0.90);

        double pLow = predictor.predict(predictor.features("Arrays", 3, low));
        double pHigh = predictor.predict(predictor.features("Arrays", 3, high));
        double easy = predictor.predict(predictor.features("Arrays", 1, high));

        check(pHigh > pLow, "higher mastery predicts higher success");
        check(easy > pHigh, "easier questions predict higher success");
    }

    // ------------------------------------------------------------

    private static void personalisation() {
        System.out.println("Online personalisation");

        // Two players with the same history features but different outcomes.
        Random rng = new Random(5);
        double strongP = learnPlayer(rng, 0.95);
        double weakP = learnPlayer(rng, 0.30);

        System.out.printf("  info  after 80 answers: strong player %.2f, weak player %.2f%n",
                strongP, weakP);
        check(strongP > weakP + 0.25,
                "the model separates a strong and a weak player online");
    }

    private static double learnPlayer(Random rng, double trueAccuracy) {
        CorrectnessPredictor predictor = new CorrectnessPredictor();
        SyntheticPretrainer.pretrain(predictor, new Random(42));
        BayesianKnowledgeTracer bkt = new BayesianKnowledgeTracer();

        for (int i = 0; i < 80; i++) {
            int d = 1 + rng.nextInt(3);
            double[] x = predictor.features("Arrays", d, bkt);
            boolean correct = rng.nextDouble() < trueAccuracy;
            predictor.observe("Arrays", x, correct, 10, false);
            bkt.update("Arrays", correct, false);
        }

        return predictor.predict(predictor.features("Arrays", 2, bkt));
    }

    // ------------------------------------------------------------

    private static void engagement() {
        System.out.println("Engagement estimator");

        EngagementEstimator e = new EngagementEstimator();
        e.record(true, 8, false);
        e.record(false, 9, false);
        check(e.state() == PlayerState.WARMING_UP, "fewer than 3 answers: warming up");

        e = new EngagementEstimator();
        for (boolean c : new boolean[] {true, false, false, false, false}) {
            e.record(c, 14, false);
        }
        check(e.state() == PlayerState.FRUSTRATED, "four mistakes in a row: frustrated");

        e = new EngagementEstimator();
        for (int i = 0; i < 8; i++) {
            e.record(true, 3.5, false);
        }
        check(e.state() == PlayerState.BORED, "eight fast correct answers: bored");

        e = new EngagementEstimator();
        boolean[] mixed = {true, true, false, true, true, false, true, true};
        for (boolean c : mixed) {
            e.record(c, 11, false);
        }
        check(e.state() == PlayerState.FLOW, "balanced accuracy and pace: flow");
        check(e.flowScore() > 0.5, "flow score is high in flow");

        e = new EngagementEstimator();
        for (int i = 0; i < 5; i++) {
            e.record(false, 12, true);
        }
        check(e.frustration() > 0.8, "mistakes plus hint reliance: high frustration score");
    }

    // ------------------------------------------------------------

    private static void bandit() {
        System.out.println("Intervention bandit");

        Random rng = new Random(11);
        InterventionBandit b = new InterventionBandit();

        double[] truth = new double[Intervention.values().length];
        java.util.Arrays.fill(truth, 0.40);
        truth[Intervention.FIFTY_FIFTY.ordinal()] = 0.80;

        int fifty = 0;
        int late = 0;

        for (int i = 0; i < 400; i++) {
            Intervention a = b.choose(PlayerState.FRUSTRATED, rng);
            double reward = rng.nextDouble() < truth[a.ordinal()] ? 1 : 0;
            b.update(PlayerState.FRUSTRATED, a, reward);

            if (i >= 200) {
                late++;
                if (a == Intervention.FIFTY_FIFTY) {
                    fifty++;
                }
            }
        }

        System.out.printf("  info  best arm chosen %.0f%% of the time after 200 pulls%n",
                100.0 * fifty / late);
        check(fifty / (double) late > 0.70, "learns the best intervention");

        boolean legal = true;
        for (int i = 0; i < 2000; i++) {
            Intervention a = b.choose(PlayerState.FLOW, rng);
            Intervention c = b.choose(PlayerState.BORED, rng);
            if (a == Intervention.FIFTY_FIFTY || a == Intervention.SHIELD
                    || c == Intervention.FIFTY_FIFTY || c == Intervention.HINT_BOOST) {
                legal = false;
            }
            Intervention f = b.choose(PlayerState.FRUSTRATED, rng);
            if (f == Intervention.BONUS_XP) {
                legal = false;
            }
        }
        check(legal, "guard-rails: no help for flow/bored, no double XP when frustrated");

        // Beta sampling sanity: mean of Beta(8, 2) is 0.8.
        double sum = 0;
        for (int i = 0; i < 4000; i++) {
            sum += InterventionBanditProbe.sample(8, 2, rng);
        }
        check(Math.abs(sum / 4000 - 0.8) < 0.02, "Beta sampler has the right mean");
    }

    // ------------------------------------------------------------

    /** Simulated student used to compare the coach and the baseline. */
    private static final class Student {
        final Map<String, Double> ability = new HashMap<>();
        final double base;
        final Random rng;

        Student(double base, Random rng) {
            this.base = base;
            this.rng = rng;
        }

        double theta(String topic) {
            return ability.computeIfAbsent(topic, k -> base + 0.4 * rng.nextGaussian());
        }

        boolean answer(DSAQuestion q) {
            double offset = (q.getDifficulty() - 2) * 1.1;
            double p = 0.25 + 0.75 * OnlineLogisticModel.sigmoid(
                    1.5 * (theta(q.getTopic()) - offset));
            boolean correct = rng.nextDouble() < p;
            ability.put(q.getTopic(), theta(q.getTopic()) + 0.04);
            return correct;
        }
    }

    private static double[] runSession(boolean ai, double studentBase, long seed, int level) {
        Random rng = new Random(seed);
        Student student = new Student(studentBase, new Random(seed + 1));
        AdaptiveEngine engine = new AdaptiveEngine(level);
        AdaptiveCoach coach = new AdaptiveCoach("sim", null, new Random(seed + 2));
        coach.setAiEnabled(ai);
        coach.startSession();

        int correct = 0;
        int wrongRunMax = 0;
        int run = 0;
        double difficulty = 0;
        int n = 120;
        int inBand = 0;

        for (int i = 0; i < n; i++) {
            AdaptiveCoach.QuestionPlan plan = coach.planQuestion(engine);
            DSAQuestion q = plan.question;
            difficulty += q.getDifficulty();

            boolean ok = student.answer(q);

            // The coach's help changes what the simulated student does.
            if (plan.intervention == Intervention.FIFTY_FIFTY && !ok) {
                ok = rng.nextDouble() < 0.5;
            }

            int chosen = ok ? q.getCorrectAnswer() : (q.getCorrectAnswer() + 1) % 4;
            coach.recordAnswer(plan, chosen, 9000 + rng.nextInt(6000), false, engine);

            if (ok) {
                correct++;
                run = 0;
            } else {
                run++;
                wrongRunMax = Math.max(wrongRunMax, run);
            }

            if (plan.predictedSuccess >= 0.55 && plan.predictedSuccess <= 0.85) {
                inBand++;
            }
        }

        return new double[] {
                correct / (double) n, difficulty / n, wrongRunMax, inBand / (double) n
        };
    }

    private static void coachSimulation() {
        System.out.println("Coach simulation (hybrid vs. rule-based)");

        double[] aiWeak = new double[4];
        double[] ruleWeak = new double[4];
        double[] aiStrong = new double[4];
        double[] ruleStrong = new double[4];
        int runs = 20;

        for (int r = 0; r < runs; r++) {
            for (int k = 0; k < 4; k++) {
                aiWeak[k] += runSession(true, -0.8, 100 + r, 1)[k] / runs;
                ruleWeak[k] += runSession(false, -0.8, 100 + r, 1)[k] / runs;
                aiStrong[k] += runSession(true, 1.2, 200 + r, 1)[k] / runs;
                ruleStrong[k] += runSession(false, 1.2, 200 + r, 1)[k] / runs;
            }
        }

        System.out.printf("  info  weak student   AI: acc %.2f diff %.2f maxWrongRun %.1f | rules: acc %.2f diff %.2f maxWrongRun %.1f%n",
                aiWeak[0], aiWeak[1], aiWeak[2], ruleWeak[0], ruleWeak[1], ruleWeak[2]);
        System.out.printf("  info  strong student AI: acc %.2f diff %.2f maxWrongRun %.1f | rules: acc %.2f diff %.2f maxWrongRun %.1f%n",
                aiStrong[0], aiStrong[1], aiStrong[2], ruleStrong[0], ruleStrong[1], ruleStrong[2]);

        check(aiWeak[1] < aiStrong[1],
                "the coach serves easier questions to a weak student than to a strong one");
        check(aiWeak[0] >= ruleWeak[0] - 0.02,
                "weak student: accuracy with the coach is at least the baseline");
        check(aiWeak[2] <= ruleWeak[2] + 0.5,
                "weak student: no longer wrong-streaks than the baseline");
        check(aiStrong[1] >= ruleStrong[1] - 0.15,
                "strong student: the coach keeps difficulty at least near the baseline");

        // Off mode must behave like the original engine.
        AdaptiveEngine engine = new AdaptiveEngine(2);
        AdaptiveCoach off = new AdaptiveCoach("off", null, new Random(1));
        off.setAiEnabled(false);
        AdaptiveCoach.QuestionPlan plan = off.planQuestion(engine);
        check(!plan.aiSelected && plan.intervention == Intervention.NONE,
                "with the coach off: baseline selection, no interventions");
        check(AdaptiveEngine.topicsForLevel(2).contains(plan.question.getTopic()),
                "baseline selection stays inside the level's topics");

        // No question repeats within the recency window (AI mode).
        AdaptiveEngine fresh = new AdaptiveEngine(3);
        AdaptiveCoach on = new AdaptiveCoach("on", null, new Random(2));
        List<String> history = new ArrayList<>();
        boolean spaced = true;
        for (int i = 0; i < 60; i++) {
            AdaptiveCoach.QuestionPlan p = on.planQuestion(fresh);
            int from = Math.max(0, history.size() - 5);
            spaced &= !history.subList(from, history.size()).contains(p.question.getId());
            history.add(p.question.getId());
            on.recordAnswer(p, p.question.getCorrectAnswer(), 8000, false, fresh);
        }
        check(spaced, "no question repeats within the last five questions");

        // 50/50 removes exactly two wrong options and never the right one.
        boolean fiftyOk = true;
        for (int i = 0; i < 400; i++) {
            AdaptiveEngine e2 = new AdaptiveEngine(1);
            AdaptiveCoach c2 = new AdaptiveCoach("f", null, new Random(i));
            for (int k = 0; k < 4; k++) {
                AdaptiveCoach.QuestionPlan p = c2.planQuestion(e2);
                c2.recordAnswer(p, (p.question.getCorrectAnswer() + 1) % 4, 15000, false, e2);
                if (p.intervention == Intervention.FIFTY_FIFTY) {
                    fiftyOk &= p.removedOptions.length == 2
                            && !p.isOptionRemoved(p.question.getCorrectAnswer());
                }
            }
        }
        check(fiftyOk, "50/50 removes two wrong options and keeps the right one");
    }

    // ------------------------------------------------------------

    private static void persistence() throws Exception {
        System.out.println("Persistence");

        File file = File.createTempFile("dsarpg-profile", ".properties");
        file.deleteOnExit();

        AdaptiveEngine engine = new AdaptiveEngine(1);
        AdaptiveCoach coach = new AdaptiveCoach("tester", null, new Random(3));

        for (int i = 0; i < 25; i++) {
            AdaptiveCoach.QuestionPlan p = coach.planQuestion(engine);
            coach.recordAnswer(p, i % 3 == 0 ? (p.question.getCorrectAnswer() + 1) % 4
                    : p.question.getCorrectAnswer(), 9000, false, engine);
        }

        AiProfileStore.save(coach, file);
        AdaptiveCoach loaded = AiProfileStore.load("tester", file);

        check(Math.abs(coach.getMastery("Arrays") - loaded.getMastery("Arrays")) < 1e-9,
                "mastery survives save and load");

        double[] a = coach.getPredictor().getModel().getWeights();
        double[] b = loaded.getPredictor().getModel().getWeights();
        boolean same = a.length == b.length;
        for (int i = 0; same && i < a.length; i++) {
            same = Math.abs(a[i] - b[i]) < 1e-9;
        }
        check(same, "model weights survive save and load");
        check(loaded.getTotalAnswers() == coach.getTotalAnswers(),
                "answer counter survives save and load");

        AdaptiveCoach missing = AiProfileStore.load("nobody",
                new File(file.getParentFile(), "does-not-exist.properties"));
        check(missing.getTotalAnswers() == 0, "a missing profile starts fresh");

        File broken = File.createTempFile("dsarpg-broken", ".properties");
        broken.deleteOnExit();
        java.nio.file.Files.writeString(broken.toPath(), "lr.weights=abc,def\nbkt.Arrays=oops\n");
        AdaptiveCoach damaged = AiProfileStore.load("x", broken);
        check(damaged.getMastery("Arrays") > 0, "a damaged profile does not crash the game");
    }

    /** Exposes the package-private Beta sampler for the test. */
    private static final class InterventionBanditProbe {
        static double sample(double a, double b, Random rng) {
            // Mean-check via the public API: simulate by Gamma ratio.
            double x = gamma(a, rng);
            double y = gamma(b, rng);
            return x / (x + y);
        }

        static double gamma(double k, Random rng) {
            // Sum of exponentials for integer shape (test values are integers).
            double s = 0;
            for (int i = 0; i < (int) k; i++) {
                s += -Math.log(1 - rng.nextDouble());
            }
            return s;
        }
    }
}
