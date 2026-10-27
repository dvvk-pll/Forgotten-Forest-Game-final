package com.dsarpg.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.dsarpg.DSAQuestion;

/**
 * Predicts the probability that the player answers a given question
 * correctly. It combines:
 *   - the Bayesian mastery estimate for the topic,
 *   - recent behaviour (accuracy, hint use, response time, streak, fatigue),
 *   - the question's difficulty,
 * and feeds them to an online logistic regression.
 *
 * The same feature code is used for pre-training on simulated learners and
 * for live personalisation, so the model sees consistent inputs.
 */
public final class CorrectnessPredictor {

    public static final int FEATURES = 10;

    private final OnlineLogisticModel model =
            new OnlineLogisticModel(FEATURES, 0.03, 0.0005);

    // Running behavioural context (exponential moving averages).
    private double recentAccuracy = 0.60;
    private double hintRate = 0.10;
    private double responseLog = 0.0;   // ln(responseSeconds / 10), clipped
    private int streak = 0;             // + correct run, - wrong run
    private int sessionAnswers = 0;
    private final Map<String, Integer> topicAttempts = new HashMap<>();

    public double[] features(DSAQuestion question, BayesianKnowledgeTracer bkt) {
        return features(question.getTopic(), question.getDifficulty(), bkt);
    }

    public double[] features(String topic, int difficulty,
                             BayesianKnowledgeTracer bkt) {

        double mastery = bkt.mastery(topic);
        double d = difficulty - 2;

        double[] x = new double[FEATURES];
        x[0] = 1.0;
        x[1] = mastery;
        x[2] = recentAccuracy;
        x[3] = d;
        x[4] = mastery * d;
        x[5] = Math.tanh(streak / 3.0);
        x[6] = hintRate;
        x[7] = responseLog / 1.5;
        x[8] = Math.min(1.0, sessionAnswers / 30.0);
        x[9] = Math.min(1.0, topicAttempts.getOrDefault(topic, 0) / 6.0);
        return x;
    }

    public double predict(double[] x) {
        return model.predict(x);
    }

    /** Learns from the observed outcome and then updates the context. */
    public void observe(String topic, double[] x, boolean correct,
                        double responseSeconds, boolean hintUsed) {

        model.update(x, correct);
        updateContext(topic, correct, responseSeconds, hintUsed);
    }

    /** Context-only update (no weight change), used by the simulator. */
    public void updateContext(String topic, boolean correct,
                              double responseSeconds, boolean hintUsed) {

        recentAccuracy += 0.20 * ((correct ? 1.0 : 0.0) - recentAccuracy);
        hintRate += 0.20 * ((hintUsed ? 1.0 : 0.0) - hintRate);

        double rt = Math.max(0.5, responseSeconds);
        double logValue = Math.log(rt / 10.0);
        logValue = Math.max(-1.5, Math.min(1.5, logValue));
        responseLog += 0.30 * (logValue - responseLog);

        if (correct) {
            streak = streak >= 0 ? streak + 1 : 1;
        } else {
            streak = streak <= 0 ? streak - 1 : -1;
        }

        sessionAnswers++;
        topicAttempts.merge(topic, 1, Integer::sum);
    }

    /** Starts a new play session (keeps learned weights, resets fatigue). */
    public void startSession() {
        sessionAnswers = 0;
        streak = 0;
    }

    public OnlineLogisticModel getModel() {
        return model;
    }

    public int getTopicAttempts(String topic) {
        return topicAttempts.getOrDefault(topic, 0);
    }

    // --------------------------------------------------
    // PERSISTENCE
    // --------------------------------------------------

    public void save(Properties p) {
        double[] w = model.getWeights();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < w.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(w[i]);
        }

        p.setProperty("lr.weights", sb.toString());
        p.setProperty("lr.recentAccuracy", Double.toString(recentAccuracy));
        p.setProperty("lr.hintRate", Double.toString(hintRate));
        p.setProperty("lr.responseLog", Double.toString(responseLog));

        for (Map.Entry<String, Integer> e : topicAttempts.entrySet()) {
            p.setProperty("topicAttempts." + e.getKey(),
                    Integer.toString(e.getValue()));
        }
    }

    /** Returns true when stored weights were found and applied. */
    public boolean load(Properties p) {
        String stored = p.getProperty("lr.weights");

        if (stored == null) {
            return false;
        }

        try {
            String[] parts = stored.split(",");

            if (parts.length != FEATURES) {
                return false;
            }

            double[] w = new double[FEATURES];

            for (int i = 0; i < FEATURES; i++) {
                w[i] = Double.parseDouble(parts[i]);
            }

            model.setWeights(w);

            recentAccuracy = Double.parseDouble(
                    p.getProperty("lr.recentAccuracy", "0.6"));
            hintRate = Double.parseDouble(
                    p.getProperty("lr.hintRate", "0.1"));
            responseLog = Double.parseDouble(
                    p.getProperty("lr.responseLog", "0"));

            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("topicAttempts.")) {
                    topicAttempts.put(
                            key.substring("topicAttempts.".length()),
                            Integer.parseInt(p.getProperty(key)));
                }
            }

            return true;

        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
