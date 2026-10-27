package com.dsarpg.ai;

import java.util.HashMap;
import java.util.Map;

/**
 * Bayesian Knowledge Tracing (BKT).
 *
 * A small hidden-Markov model per topic. The hidden state is "the learner
 * knows this topic" and each answer is noisy evidence:
 *   slip  = P(wrong | knows)
 *   guess = P(correct | does not know)
 *   learn = P(learns the topic after each practice attempt)
 *
 * A correct answer given after opening a hint is weaker evidence, so a
 * higher guess probability is used for it.
 */
public final class BayesianKnowledgeTracer {

    public static final double INITIAL_KNOWLEDGE = 0.30;

    private static final double SLIP = 0.10;
    private static final double GUESS = 0.25;      // four options
    private static final double GUESS_WITH_HINT = 0.60;
    private static final double LEARN = 0.12;

    private final Map<String, Double> knowledge = new HashMap<>();

    /** Probability (0-1) that the learner currently knows the topic. */
    public double mastery(String topic) {
        return knowledge.getOrDefault(topic, INITIAL_KNOWLEDGE);
    }

    /** Updates the topic with one observed answer and returns the new value. */
    public double update(String topic, boolean correct, boolean hintUsed) {
        double prior = mastery(topic);
        double guess = hintUsed ? GUESS_WITH_HINT : GUESS;

        double posterior;

        if (correct) {
            double numerator = prior * (1 - SLIP);
            posterior = numerator / (numerator + (1 - prior) * guess);
        } else {
            double numerator = prior * SLIP;
            posterior = numerator / (numerator + (1 - prior) * (1 - guess));
        }

        double next = posterior + (1 - posterior) * LEARN;
        next = Math.max(0.01, Math.min(0.99, next));

        knowledge.put(topic, next);
        return next;
    }

    public void set(String topic, double value) {
        knowledge.put(topic, Math.max(0.01, Math.min(0.99, value)));
    }

    public Map<String, Double> snapshot() {
        return new HashMap<>(knowledge);
    }
}
