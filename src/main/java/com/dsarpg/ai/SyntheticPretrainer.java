package com.dsarpg.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Cold-start training for {@link CorrectnessPredictor}.
 *
 * A brand-new player has no history, so the logistic model is first trained
 * on simulated learners. Each simulated learner has a latent ability per
 * topic that improves with practice, answers questions with an
 * item-response-theory style probability (with a 25% guess floor), and
 * produces realistic response times and hint use. The simulated answers are
 * pushed through the real BKT and feature code, so the pre-trained weights
 * match what the model sees in the game.
 *
 * Training takes a few milliseconds and the result is only a prior: the model
 * keeps learning from the real player afterwards.
 */
public final class SyntheticPretrainer {

    private static final String[] TOPICS = {
            "Arrays", "Searching", "Stacks", "Queues"
    };

    private SyntheticPretrainer() {
    }

    /** One simulated training example. */
    public static final class Sample {
        public final double[] x;
        public final boolean correct;

        Sample(double[] x, boolean correct) {
            this.x = x;
            this.correct = correct;
        }
    }

    public static List<Sample> generate(Random rng, int learners, int steps) {
        List<Sample> samples = new ArrayList<>();

        for (int s = 0; s < learners; s++) {
            BayesianKnowledgeTracer bkt = new BayesianKnowledgeTracer();
            CorrectnessPredictor context = new CorrectnessPredictor();

            double general = rng.nextGaussian();
            double[] ability = new double[TOPICS.length];

            for (int t = 0; t < TOPICS.length; t++) {
                ability[t] = general + 0.5 * rng.nextGaussian();
            }

            for (int step = 0; step < steps; step++) {
                int t = rng.nextInt(TOPICS.length);
                int difficulty = 1 + rng.nextInt(3);

                double[] x = context.features(TOPICS[t], difficulty, bkt);

                double difficultyOffset = (difficulty - 2) * 1.1;
                double p = 0.25 + 0.75 * OnlineLogisticModel.sigmoid(
                        1.5 * (ability[t] - difficultyOffset));

                boolean correct = rng.nextDouble() < p;

                // Realistic behaviour around the outcome.
                double advantage = ability[t] - difficultyOffset;
                double seconds = Math.exp(
                        Math.log(11.0) - 0.25 * advantage
                        + 0.35 * rng.nextGaussian());
                boolean hint = rng.nextDouble()
                        < OnlineLogisticModel.sigmoid(-1.4 * advantage - 1.5);

                samples.add(new Sample(x, correct));

                bkt.update(TOPICS[t], correct, hint);
                context.updateContext(TOPICS[t], correct, seconds, hint);

                // The simulated learner improves with practice.
                ability[t] += 0.04;
            }
        }

        return samples;
    }

    /** Trains the predictor's model on simulated learners. */
    public static void pretrain(CorrectnessPredictor predictor, Random rng) {
        List<Sample> samples = generate(rng, 300, 40);

        OnlineLogisticModel model = predictor.getModel();

        for (int epoch = 0; epoch < 4; epoch++) {
            Collections.shuffle(samples, rng);

            for (Sample sample : samples) {
                model.update(sample.x, sample.correct);
            }
        }
    }
}
