package com.dsarpg.ai;

/**
 * Gamification adaptations the AI Coach can apply to a single question.
 * Each one is visible to the player and explained in the battle screen.
 */
public enum Intervention {

    NONE("Standard", ""),

    FIFTY_FIFTY("50/50 help",
            "Two wrong answers were removed to help you through a tough patch."),

    SHIELD("Second-chance shield",
            "Shield active: a wrong answer will not cost health this time."),

    HINT_BOOST("Hint opened",
            "The AI Coach opened the hint for you."),

    BONUS_XP("Challenge bonus",
            "Challenge bonus: a correct answer earns double XP!");

    private final String label;
    private final String message;

    Intervention(String label, String message) {
        this.label = label;
        this.message = message;
    }

    public String getLabel() {
        return label;
    }

    public String getMessage() {
        return message;
    }
}
