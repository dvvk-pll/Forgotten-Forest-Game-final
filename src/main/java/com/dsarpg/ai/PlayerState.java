package com.dsarpg.ai;

/**
 * Engagement / emotional state estimated from recent play.
 * The labels are shown to the player (with an icon and text, never colour
 * alone) so the system's view of them is transparent.
 */
public enum PlayerState {

    WARMING_UP("Warming up",
            "The AI Coach is still learning how you play."),

    FRUSTRATED("Needs support",
            "Several mistakes in a row. Questions get gentler and help is offered."),

    FLOW("In the zone",
            "Challenge and skill are well matched. Keep going!"),

    BORED("Ready for more",
            "You are answering quickly and correctly. Harder questions are coming.");

    private final String label;
    private final String description;

    PlayerState(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
