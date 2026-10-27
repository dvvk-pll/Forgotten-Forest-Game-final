
package com.dsarpg;

public class DSAQuestion {

    private final String topic;
    private final String text;
    private final String[] options;
    private final int correctAnswer;
    private final int difficulty;
    private final String hint;
    private final String explanation;

    public DSAQuestion(String topic, String text, String[] options,
                       int correctAnswer, int difficulty,
                       String hint, String explanation) {

        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic cannot be empty.");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty.");
        }

        if (options == null || options.length != 4) {
            throw new IllegalArgumentException(
                    "Every question must have exactly four options.");
        }

        if (correctAnswer < 0 || correctAnswer >= options.length) {
            throw new IllegalArgumentException("Invalid correct answer.");
        }

        if (difficulty < 1 || difficulty > 3) {
            throw new IllegalArgumentException(
                    "Difficulty must be 1, 2 or 3.");
        }

        this.topic = topic;
        this.text = text;
        this.options = options.clone();
        this.correctAnswer = correctAnswer;
        this.difficulty = difficulty;
        this.hint = hint;
        this.explanation = explanation;
    }

    /** Stable identifier used by the adaptive engine and usability logs. */
    public String getId() {
        return topic + "|" + text;
    }

    public String getTopic() {
        return topic;
    }

    public String getText() {
        return text;
    }

    public String[] getOptions() {
        return options.clone();
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getHint() {
        return hint;
    }

    public String getExplanation() {
        return explanation;
    }
}
