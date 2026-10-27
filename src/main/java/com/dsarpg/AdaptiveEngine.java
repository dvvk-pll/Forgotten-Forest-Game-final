
package com.dsarpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Rule-based adaptive engine (the original algorithm).
 *
 * It tracks accuracy and streaks per topic and picks the next question's
 * topic (least practised / weakest) and difficulty (easier after mistakes,
 * harder after streaks).
 *
 * The engine is still used in two ways:
 *  1. As the baseline mode ("AI Coach" switched off), where
 *     {@link #nextQuestion()} behaves exactly as before.
 *  2. As one of the signals combined with the AI/ML models inside
 *     {@link com.dsarpg.ai.AdaptiveCoach} (hybrid adaptation). For that, the
 *     hooks {@link #freshCandidates()}, {@link #ruleBasedTopic()},
 *     {@link #ruleBasedDifficulty(String)} and {@link #select(DSAQuestion)}
 *     expose the rule-based decisions without changing them.
 */
public class AdaptiveEngine {

    private static class TopicStats {
        int attempts = 0;
        int correct = 0;
        int streak = 0;

        double accuracy() {
            if (attempts == 0) {
                return 0;
            }

            return (double) correct / attempts;
        }
    }

    private final List<DSAQuestion> questions = new ArrayList<>();
    private final Map<String, TopicStats> mastery = new LinkedHashMap<>();
    private final Random random = new Random();
    private final List<String> recentQuestionKeys = new ArrayList<>();
    private DSAQuestion currentQuestion;
    private int currentLevel = 1;

    public AdaptiveEngine() {
        loadQuestions();
    }

    public AdaptiveEngine(int level) {
        loadQuestions();
        setLevel(level);
    }

    public void setLevel(int level) {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException(
                    "Level must be between 1 and 5.");
        }

        currentLevel = level;
        currentQuestion = null;
    }

    public int getLevel() {
        return currentLevel;
    }

    /** Topics taught in the given level (1-5). */
    public static List<String> topicsForLevel(int level) {
        switch (level) {
            case 1:
                return Arrays.asList("Arrays", "Searching");

            case 2:
                return Arrays.asList(
                        "Linked Lists", "Stacks", "Queues");

            case 3:
                return Arrays.asList("Sorting", "Recursion");

            case 4:
                return Arrays.asList("Trees", "Graphs");

            case 5:
                return Arrays.asList(
                        "Greedy Algorithms",
                        "Dynamic Programming");

            default:
                return Arrays.asList("Arrays", "Searching");
        }
    }

    private List<String> topicsForLevel() {
        return topicsForLevel(currentLevel);
    }

    /** Topics for the level this engine is currently set to. */
    public List<String> getLevelTopics() {
        return topicsForLevel();
    }

    private void loadQuestions() {
        for (DSAQuestion question : QuestionBank.all()) {
            questions.add(question);

            // Create mastery statistics for each topic.
            mastery.putIfAbsent(question.getTopic(), new TopicStats());
        }
    }

    // --------------------------------------------------
    // ORIGINAL RULE-BASED SELECTION (baseline mode)
    // --------------------------------------------------

    public DSAQuestion nextQuestion() {
        List<String> allowedTopics = topicsForLevel();
        String chosenTopic = chooseTopic(allowedTopics);
        int targetDifficulty = desiredDifficulty(chosenTopic);

        List<DSAQuestion> candidates = new ArrayList<>();

        // First, collect questions at the desired adaptive difficulty.
        for (DSAQuestion question : questions) {
            if (question.getTopic().equals(chosenTopic)
                    && question.getDifficulty() == targetDifficulty) {
                candidates.add(question);
            }
        }

        // Prefer questions that have not appeared recently.
        candidates.removeIf(question ->
            recentQuestionKeys.contains(question.getId())
        );

        // If needed, try other topics and difficulties in this level.
        if (candidates.isEmpty()) {
            for (DSAQuestion question : questions) {
                if (allowedTopics.contains(question.getTopic())
                        && !recentQuestionKeys.contains(question.getId())) {
                    candidates.add(question);
                }
            }
        }

        // If every question has been used recently, start a fresh cycle.
        if (candidates.isEmpty()) {
            recentQuestionKeys.clear();

            for (DSAQuestion question : questions) {
                if (allowedTopics.contains(question.getTopic())) {
                    candidates.add(question);
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                "No questions available for Level " + currentLevel
            );
        }

        DSAQuestion chosen = candidates.get(
            random.nextInt(candidates.size())
        );

        select(chosen);
        return chosen;
    }

    // --------------------------------------------------
    // HOOKS FOR THE HYBRID AI COACH
    // --------------------------------------------------

    /**
     * All questions of this level that have not been asked recently.
     * Starts a fresh cycle when everything has been used.
     */
    public List<DSAQuestion> freshCandidates() {
        List<String> allowedTopics = topicsForLevel();
        List<DSAQuestion> fresh = new ArrayList<>();

        for (DSAQuestion question : questions) {
            if (allowedTopics.contains(question.getTopic())
                    && !recentQuestionKeys.contains(question.getId())) {
                fresh.add(question);
            }
        }

        if (fresh.isEmpty()) {
            recentQuestionKeys.clear();

            for (DSAQuestion question : questions) {
                if (allowedTopics.contains(question.getTopic())) {
                    fresh.add(question);
                }
            }
        }

        return fresh;
    }

    /**
     * Questions of this level that were not among the last {@code window}
     * questions shown. Unlike {@link #freshCandidates()}, questions may
     * come back after a short gap, which gives the adaptive models real
     * freedom to choose the best-fitting difficulty.
     */
    public List<DSAQuestion> candidatesAvoidingRecent(int window) {
        List<String> allowedTopics = topicsForLevel();
        int from = Math.max(0, recentQuestionKeys.size() - Math.max(0, window));
        List<String> avoid = recentQuestionKeys.subList(
                from, recentQuestionKeys.size());

        List<DSAQuestion> result = new ArrayList<>();

        for (DSAQuestion question : questions) {
            if (allowedTopics.contains(question.getTopic())
                    && !avoid.contains(question.getId())) {
                result.add(question);
            }
        }

        if (result.isEmpty()) {
            for (DSAQuestion question : questions) {
                if (allowedTopics.contains(question.getTopic())) {
                    result.add(question);
                }
            }
        }

        return result;
    }

    /** The topic the rule-based algorithm would practise next. */
    public String ruleBasedTopic() {
        return chooseTopic(topicsForLevel());
    }

    /** The difficulty (1-3) the rule-based algorithm wants for a topic. */
    public int ruleBasedDifficulty(String topic) {
        return desiredDifficulty(topic);
    }

    /** Records that a question was shown (used for freshness filtering). */
    public void select(DSAQuestion question) {
        currentQuestion = question;

        if (question != null) {
            recentQuestionKeys.add(question.getId());
        }
    }

    private String chooseTopic(List<String> allowedTopics) {
        String chosen = allowedTopics.get(0);

        int fewestAttempts = Integer.MAX_VALUE;
        double lowestAccuracy = Double.MAX_VALUE;

        for (String topic : allowedTopics) {
            TopicStats stats = mastery.get(topic);

            if (stats == null) {
                continue;
            }

            if (stats.attempts < fewestAttempts
                    || (stats.attempts == fewestAttempts
                    && stats.accuracy() < lowestAccuracy)) {

                chosen = topic;
                fewestAttempts = stats.attempts;
                lowestAccuracy = stats.accuracy();
            }
        }

        return chosen;
    }

    private int desiredDifficulty(String topic) {
        TopicStats stats = mastery.get(topic);

        if (stats == null || stats.attempts < 2) {
            return 1;
        }

        if (stats.accuracy() < 0.50 || stats.streak <= -2) {
            return 1;
        }

        if (stats.accuracy() >= 0.80 && stats.streak >= 3) {
            return 3;
        }

        return 2;
    }

    public void recordAnswer(DSAQuestion question, boolean correct) {
        if (question == null) {
            throw new IllegalArgumentException("Question cannot be null.");
        }

        TopicStats stats = mastery.get(question.getTopic());

        if (stats == null) {
            throw new IllegalArgumentException("Unknown topic.");
        }

        stats.attempts++;

        if (correct) {
            stats.correct++;
            stats.streak = stats.streak >= 0 ? stats.streak + 1 : 1;
        } else {
            stats.streak = stats.streak <= 0 ? stats.streak - 1 : -1;
        }
    }

    // --------------------------------------------------
    // READ-ONLY STATISTICS
    // --------------------------------------------------

    public int getAttempts(String topic) {
        TopicStats stats = mastery.get(topic);
        return stats == null ? 0 : stats.attempts;
    }

    public int getCorrect(String topic) {
        TopicStats stats = mastery.get(topic);
        return stats == null ? 0 : stats.correct;
    }

    public double getAccuracy(String topic) {
        TopicStats stats = mastery.get(topic);
        return stats == null ? 0 : stats.accuracy();
    }

    public String getMasterySummary() {
        StringBuilder summary = new StringBuilder();

        for (String topic : mastery.keySet()) {
            TopicStats stats = mastery.get(topic);

            int percent = (int) Math.round(stats.accuracy() * 100);

            summary.append(topic)
                   .append(": ")
                   .append(stats.attempts == 0 ? "Not attempted"
                           : percent + "% (" + stats.correct + "/"
                             + stats.attempts + ")")
                   .append("\n");
        }

        return summary.toString();
    }

    public int getTopicCount() {
        return mastery.size();
    }

    public DSAQuestion getCurrentQuestion() {
        return currentQuestion;
    }
}
