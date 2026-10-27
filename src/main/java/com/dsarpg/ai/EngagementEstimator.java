package com.dsarpg.ai;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Estimates the player's engagement state from a sliding window of recent
 * answers (accuracy, response speed, hint use and streaks).
 *
 * Two smooth scores are computed with logistic functions:
 *   frustration - many recent mistakes, long wrong streaks, leaning on hints
 *   boredom     - near-perfect accuracy answered very quickly
 * The larger one above a threshold decides the state; otherwise the player
 * is considered to be in flow. The scores are interpretable on purpose, so
 * the game can explain them to the player.
 */
public final class EngagementEstimator {

    private static final int WINDOW = 8;
    private static final int MIN_EVENTS = 3;
    private static final double THRESHOLD = 0.60;

    private static final class Event {
        final boolean correct;
        final double seconds;
        final boolean hint;

        Event(boolean correct, double seconds, boolean hint) {
            this.correct = correct;
            this.seconds = seconds;
            this.hint = hint;
        }
    }

    private final Deque<Event> events = new ArrayDeque<>();

    public void record(boolean correct, double seconds, boolean hintUsed) {
        events.addLast(new Event(correct, Math.max(0.5, seconds), hintUsed));

        while (events.size() > WINDOW) {
            events.removeFirst();
        }
    }

    public void reset() {
        events.clear();
    }

    public int size() {
        return events.size();
    }

    public double accuracy() {
        if (events.isEmpty()) {
            return 0.6;
        }

        int correct = 0;

        for (Event e : events) {
            if (e.correct) {
                correct++;
            }
        }

        return (double) correct / events.size();
    }

    private double hintRate() {
        if (events.isEmpty()) {
            return 0;
        }

        int hints = 0;

        for (Event e : events) {
            if (e.hint) {
                hints++;
            }
        }

        return (double) hints / events.size();
    }

    private double averageSeconds() {
        if (events.isEmpty()) {
            return 10;
        }

        double sum = 0;

        for (Event e : events) {
            sum += e.seconds;
        }

        return sum / events.size();
    }

    private int tail(boolean wanted) {
        int count = 0;
        Event[] array = events.toArray(new Event[0]);

        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i].correct == wanted) {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    /** 0-1 score: how likely the player is frustrated. */
    public double frustration() {
        if (events.size() < MIN_EVENTS) {
            return 0;
        }

        double z = 5.0 * (0.45 - accuracy())
                + 0.8 * (tail(false) - 1)
                + 1.5 * (hintRate() - 0.40);

        return OnlineLogisticModel.sigmoid(z);
    }

    /** 0-1 score: how likely the player is under-challenged. */
    public double boredom() {
        if (events.size() < MIN_EVENTS) {
            return 0;
        }

        double fast = Math.max(-1, Math.min(1, (7.0 - averageSeconds()) / 7.0));

        double z = 6.0 * (accuracy() - 0.85)
                + 2.0 * fast
                + 0.3 * (tail(true) - 4);

        return OnlineLogisticModel.sigmoid(z);
    }

    public PlayerState state() {
        if (events.size() < MIN_EVENTS) {
            return PlayerState.WARMING_UP;
        }

        double f = frustration();
        double b = boredom();

        if (f >= THRESHOLD && f >= b) {
            return PlayerState.FRUSTRATED;
        }

        if (b >= THRESHOLD) {
            return PlayerState.BORED;
        }

        return PlayerState.FLOW;
    }

    /** 0-1 "focus" value for the on-screen meter (1 = well balanced). */
    public double flowScore() {
        if (events.size() < MIN_EVENTS) {
            return 0.5;
        }

        return Math.max(0, Math.min(1, 1.0 - Math.max(frustration(), boredom())));
    }
}
