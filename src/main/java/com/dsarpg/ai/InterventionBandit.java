package com.dsarpg.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * Contextual multi-armed bandit (Thompson sampling) that learns which
 * gamification intervention works best for this player in each
 * engagement state.
 *
 * Every (state, intervention) pair keeps a Beta(alpha, beta) belief about
 * its average reward. To choose, one value is sampled from each belief and
 * the highest sample wins, so the coach mostly uses what has worked but
 * still explores alternatives.
 *
 * Guard-rails: only interventions that make sense for a state are legal
 * (for example, a double-XP challenge is never offered to a frustrated
 * player and the 50/50 helper is never offered to a bored one).
 */
public final class InterventionBandit {

    private final double[][] alpha;
    private final double[][] beta;

    public InterventionBandit() {
        int states = PlayerState.values().length;
        int actions = Intervention.values().length;

        alpha = new double[states][actions];
        beta = new double[states][actions];

        for (int s = 0; s < states; s++) {
            for (int a = 0; a < actions; a++) {
                // Mild prior in favour of doing nothing.
                boolean none = Intervention.values()[a] == Intervention.NONE;
                alpha[s][a] = none ? 2 : 1;
                beta[s][a] = none ? 2 : 1;
            }
        }
    }

    public static List<Intervention> legalActions(PlayerState state) {
        List<Intervention> list = new ArrayList<>();
        list.add(Intervention.NONE);

        switch (state) {
            case FRUSTRATED:
                list.add(Intervention.FIFTY_FIFTY);
                list.add(Intervention.SHIELD);
                list.add(Intervention.HINT_BOOST);
                break;

            case WARMING_UP:
                list.add(Intervention.HINT_BOOST);
                break;

            case FLOW:
            case BORED:
                list.add(Intervention.BONUS_XP);
                break;

            default:
                break;
        }

        return list;
    }

    public Intervention choose(PlayerState state, Random rng) {
        Intervention best = Intervention.NONE;
        double bestSample = -1;

        for (Intervention action : legalActions(state)) {
            double sample = sampleBeta(
                    alpha[state.ordinal()][action.ordinal()],
                    beta[state.ordinal()][action.ordinal()],
                    rng);

            if (sample > bestSample) {
                bestSample = sample;
                best = action;
            }
        }

        return best;
    }

    /** Reward must be in the range 0-1. */
    public void update(PlayerState state, Intervention action, double reward) {
        double r = Math.max(0, Math.min(1, reward));

        alpha[state.ordinal()][action.ordinal()] += r;
        beta[state.ordinal()][action.ordinal()] += 1 - r;
    }

    /** Mean of the belief (for insights and tests). */
    public double expectedReward(PlayerState state, Intervention action) {
        double a = alpha[state.ordinal()][action.ordinal()];
        double b = beta[state.ordinal()][action.ordinal()];
        return a / (a + b);
    }

    public double pulls(PlayerState state, Intervention action) {
        // Prior mass is 2 for NONE and 1 for every other action.
        double prior = action == Intervention.NONE ? 4 : 2;
        return alpha[state.ordinal()][action.ordinal()]
                + beta[state.ordinal()][action.ordinal()] - prior;
    }

    // --------------------------------------------------
    // BETA SAMPLING (via two Gamma samples)
    // --------------------------------------------------

    static double sampleBeta(double a, double b, Random rng) {
        double x = sampleGamma(a, rng);
        double y = sampleGamma(b, rng);
        return x / (x + y);
    }

    /** Marsaglia and Tsang's method. */
    static double sampleGamma(double shape, Random rng) {
        if (shape < 1) {
            double u = rng.nextDouble();
            return sampleGamma(shape + 1, rng) * Math.pow(u, 1.0 / shape);
        }

        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);

        while (true) {
            double x;
            double v;

            do {
                x = rng.nextGaussian();
                v = 1.0 + c * x;
            } while (v <= 0);

            v = v * v * v;
            double u = rng.nextDouble();

            if (u < 1 - 0.0331 * x * x * x * x) {
                return d * v;
            }

            if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) {
                return d * v;
            }
        }
    }

    // --------------------------------------------------
    // PERSISTENCE
    // --------------------------------------------------

    public void save(Properties p) {
        for (PlayerState s : PlayerState.values()) {
            for (Intervention a : Intervention.values()) {
                p.setProperty("bandit." + s.name() + "." + a.name(),
                        alpha[s.ordinal()][a.ordinal()] + ","
                        + beta[s.ordinal()][a.ordinal()]);
            }
        }
    }

    public void load(Properties p) {
        for (PlayerState s : PlayerState.values()) {
            for (Intervention a : Intervention.values()) {
                String v = p.getProperty(
                        "bandit." + s.name() + "." + a.name());

                if (v == null) {
                    continue;
                }

                try {
                    String[] parts = v.split(",");
                    double al = Double.parseDouble(parts[0]);
                    double be = Double.parseDouble(parts[1]);

                    if (al > 0 && be > 0) {
                        alpha[s.ordinal()][a.ordinal()] = al;
                        beta[s.ordinal()][a.ordinal()] = be;
                    }
                } catch (RuntimeException ex) {
                    // Ignore a damaged entry and keep the prior.
                }
            }
        }
    }
}
