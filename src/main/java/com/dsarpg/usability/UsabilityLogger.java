package com.dsarpg.usability;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Collects the usability metrics named in the project brief for one play
 * session, shows them in a readable report and appends them to CSV files
 * for later analysis (for example in a spreadsheet).
 *
 *  - Task completion rate : battles won / battles started
 *  - Error rate           : wrong answers / answers
 *  - Engagement           : session length and answers per minute
 *  - Efficiency           : average response time, hint reliance
 *  - Satisfaction         : 1-5 rating given by the player
 *  - Input modality       : mouse versus keyboard answers
 *
 * Files are written to ~/.dsarpg/usability/ :
 *   sessions.csv - one row per session
 *   events.csv   - one row per answered question
 */
public final class UsabilityLogger {

    private static final class Event {
        final long timestamp;
        final String topic;
        final int difficulty;
        final boolean correct;
        final long responseMillis;
        final boolean hintUsed;
        final String intervention;
        final String state;
        final double predicted;
        final boolean mouse;

        Event(long timestamp, String topic, int difficulty, boolean correct,
              long responseMillis, boolean hintUsed, String intervention,
              String state, double predicted, boolean mouse) {
            this.timestamp = timestamp;
            this.topic = topic;
            this.difficulty = difficulty;
            this.correct = correct;
            this.responseMillis = responseMillis;
            this.hintUsed = hintUsed;
            this.intervention = intervention;
            this.state = state;
            this.predicted = predicted;
            this.mouse = mouse;
        }
    }

    private final String username;
    private final boolean aiMode;
    private final long startMillis;
    private final File directory;
    private final List<Event> events = new ArrayList<>();

    private int battlesStarted;
    private int battlesWon;
    private int gameOvers;
    private int hintsUsed;
    private int mouseAnswers;
    private int helpOpens;
    private int insightsOpens;
    private int settingsOpens;
    private int satisfaction;       // 0 = not rated
    private int correctAnswers;
    private long totalResponseMillis;
    private boolean flushed;

    public UsabilityLogger(String username, boolean aiMode) {
        this(username, aiMode, new File(System.getProperty("user.home"),
                ".dsarpg/usability"));
    }

    public UsabilityLogger(String username, boolean aiMode, File directory) {
        this.username = username == null ? "player" : username;
        this.aiMode = aiMode;
        this.directory = directory;
        this.startMillis = System.currentTimeMillis();
    }

    // --------------------------------------------------
    // EVENTS
    // --------------------------------------------------

    public void battleStarted() { battlesStarted++; }
    public void battleWon() { battlesWon++; }
    public void gameOver() { gameOvers++; }
    public void helpOpened() { helpOpens++; }
    public void insightsOpened() { insightsOpens++; }
    public void settingsOpened() { settingsOpens++; }

    public void hintUsed() { hintsUsed++; }

    public void setSatisfaction(int rating) {
        satisfaction = Math.max(0, Math.min(5, rating));
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    public void answer(String topic, int difficulty, boolean correct,
                       long responseMillis, boolean hintUsed,
                       String intervention, String state,
                       double predictedSuccess, boolean viaMouse) {

        events.add(new Event(System.currentTimeMillis(), topic, difficulty,
                correct, responseMillis, hintUsed, intervention, state,
                predictedSuccess, viaMouse));

        if (correct) {
            correctAnswers++;
        }

        if (viaMouse) {
            mouseAnswers++;
        }

        totalResponseMillis += responseMillis;
    }

    // --------------------------------------------------
    // DERIVED METRICS
    // --------------------------------------------------

    public int getAnswers() { return events.size(); }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getBattlesStarted() { return battlesStarted; }
    public int getBattlesWon() { return battlesWon; }
    public int getGameOvers() { return gameOvers; }
    public int getHintsUsed() { return hintsUsed; }

    public double durationSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000.0;
    }

    public double taskCompletionRate() {
        return battlesStarted == 0 ? 0 : (double) battlesWon / battlesStarted;
    }

    public double errorRate() {
        return events.isEmpty() ? 0
                : 1.0 - (double) correctAnswers / events.size();
    }

    public double averageResponseSeconds() {
        return events.isEmpty() ? 0
                : totalResponseMillis / 1000.0 / events.size();
    }

    public double hintRate() {
        return events.isEmpty() ? 0 : (double) hintsUsed / events.size();
    }

    public double mouseShare() {
        return events.isEmpty() ? 0 : (double) mouseAnswers / events.size();
    }

    public double answersPerMinute() {
        double minutes = Math.max(durationSeconds() / 60.0, 1.0 / 60.0);
        return events.size() / minutes;
    }

    // --------------------------------------------------
    // REPORT
    // --------------------------------------------------

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "Mode: %s%n", aiMode ? "AI Coach (adaptive)" : "Standard (rule-based)"));
        sb.append(String.format(Locale.ROOT,
                "Session length: %s%n", formatDuration(durationSeconds())));
        sb.append(String.format(Locale.ROOT,
                "Questions answered: %d (%d correct)%n", events.size(), correctAnswers));
        sb.append(String.format(Locale.ROOT,
                "Task completion rate: %.0f%% (%d of %d battles won)%n",
                taskCompletionRate() * 100, battlesWon, battlesStarted));
        sb.append(String.format(Locale.ROOT,
                "Error rate: %.0f%%%n", errorRate() * 100));
        sb.append(String.format(Locale.ROOT,
                "Average response time: %.1f s%n", averageResponseSeconds()));
        sb.append(String.format(Locale.ROOT,
                "Hints used: %d%n", hintsUsed));
        sb.append(String.format(Locale.ROOT,
                "Answers by mouse: %.0f%%%n", mouseShare() * 100));
        sb.append(String.format(Locale.ROOT,
                "Engagement: %.1f answers per minute%n", answersPerMinute()));
        sb.append(satisfaction == 0 ? "Satisfaction: not rated"
                : "Satisfaction: " + satisfaction + " / 5");
        return sb.toString();
    }

    private static String formatDuration(double seconds) {
        int total = (int) Math.round(seconds);
        return String.format(Locale.ROOT, "%d min %02d s", total / 60, total % 60);
    }

    // --------------------------------------------------
    // CSV EXPORT
    // --------------------------------------------------

    /** Appends this session to the CSV files. Safe to call more than once. */
    public synchronized void flush() {
        if (flushed || directory == null) {
            return;
        }

        if (events.isEmpty() && battlesStarted == 0 && satisfaction == 0) {
            return;
        }

        try {
            if (!directory.isDirectory() && !directory.mkdirs()) {
                return;
            }

            File sessions = new File(directory, "sessions.csv");
            boolean newSessions = !sessions.exists();

            try (PrintWriter out = writer(sessions)) {
                if (newSessions) {
                    out.println("timestamp,username,mode,duration_sec,battles_started,"
                            + "battles_won,task_completion_rate,questions,correct,"
                            + "error_rate,avg_response_sec,hints_used,game_overs,"
                            + "mouse_answer_share,answers_per_min,help_opens,"
                            + "insight_opens,settings_opens,satisfaction");
                }

                out.println(String.join(",",
                        csv(Instant.ofEpochMilli(startMillis).toString()),
                        csv(username),
                        aiMode ? "ai" : "standard",
                        fmt(durationSeconds()),
                        Integer.toString(battlesStarted),
                        Integer.toString(battlesWon),
                        fmt(taskCompletionRate()),
                        Integer.toString(events.size()),
                        Integer.toString(correctAnswers),
                        fmt(errorRate()),
                        fmt(averageResponseSeconds()),
                        Integer.toString(hintsUsed),
                        Integer.toString(gameOvers),
                        fmt(mouseShare()),
                        fmt(answersPerMinute()),
                        Integer.toString(helpOpens),
                        Integer.toString(insightsOpens),
                        Integer.toString(settingsOpens),
                        satisfaction == 0 ? "" : Integer.toString(satisfaction)));
            }

            File eventFile = new File(directory, "events.csv");
            boolean newEvents = !eventFile.exists();

            try (PrintWriter out = writer(eventFile)) {
                if (newEvents) {
                    out.println("timestamp,username,mode,topic,difficulty,correct,"
                            + "response_ms,hint_used,intervention,engagement_state,"
                            + "predicted_success,input");
                }

                for (Event e : events) {
                    out.println(String.join(",",
                            csv(Instant.ofEpochMilli(e.timestamp).toString()),
                            csv(username),
                            aiMode ? "ai" : "standard",
                            csv(e.topic),
                            Integer.toString(e.difficulty),
                            e.correct ? "1" : "0",
                            Long.toString(e.responseMillis),
                            e.hintUsed ? "1" : "0",
                            csv(e.intervention),
                            csv(e.state),
                            fmt(e.predicted),
                            e.mouse ? "mouse" : "keyboard"));
                }
            }

            flushed = true;

        } catch (IOException ex) {
            System.err.println("Could not write usability log: " + ex.getMessage());
        }
    }

    private static PrintWriter writer(File file) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(file.toPath(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND));
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    static String csv(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
