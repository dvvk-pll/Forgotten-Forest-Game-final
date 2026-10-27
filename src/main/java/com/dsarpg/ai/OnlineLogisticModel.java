package com.dsarpg.ai;

/**
 * Logistic regression trained one example at a time with stochastic
 * gradient descent (online learning).
 *
 * Used to predict P(correct answer) from learner and question features.
 * The model is first pre-trained on simulated learners (cold start) and then
 * keeps adapting to the real player after every answer.
 */
public final class OnlineLogisticModel {

    private final double[] weights;
    private final double learningRate;
    private final double l2;
    private int updates;

    public OnlineLogisticModel(int features, double learningRate, double l2) {
        this.weights = new double[features];
        this.learningRate = learningRate;
        this.l2 = l2;
    }

    public static double sigmoid(double z) {
        if (z >= 0) {
            double e = Math.exp(-z);
            return 1.0 / (1.0 + e);
        }

        double e = Math.exp(z);
        return e / (1.0 + e);
    }

    public double predict(double[] x) {
        double z = 0;

        for (int i = 0; i < weights.length; i++) {
            z += weights[i] * x[i];
        }

        return sigmoid(z);
    }

    /** One SGD step on the log-loss with a little L2 regularisation. */
    public void update(double[] x, boolean positive) {
        double error = (positive ? 1.0 : 0.0) - predict(x);

        for (int i = 0; i < weights.length; i++) {
            weights[i] += learningRate * (error * x[i] - l2 * weights[i]);
        }

        updates++;
    }

    public int getUpdates() {
        return updates;
    }

    public double[] getWeights() {
        return weights.clone();
    }

    public void setWeights(double[] values) {
        if (values == null || values.length != weights.length) {
            return;
        }

        System.arraycopy(values, 0, weights, 0, weights.length);
    }

    public int size() {
        return weights.length;
    }
}
