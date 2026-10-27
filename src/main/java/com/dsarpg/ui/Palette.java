package com.dsarpg.ui;

import javafx.scene.paint.Color;

import com.dsarpg.usability.UserSettings;

/**
 * Colour set used by every canvas panel. It follows the player's
 * accessibility settings:
 *  - high contrast: black panels, white text and borders, yellow accents
 *  - colour-blind safe: blue (correct) and orange (wrong) instead of
 *    green and red (Okabe-Ito palette)
 * Colour is never the only signal; icons and words are always shown too.
 */
public final class Palette {

    public final Color panel;
    public final Color panelSoft;
    public final Color border;
    public final Color text;
    public final Color textDim;
    public final Color accent;
    public final Color good;
    public final Color bad;
    public final Color info;
    public final Color barBackground;
    public final Color optionFill;
    public final Color optionHover;
    public final Color buttonFill;
    public final Color buttonHover;
    public final Color buttonText;
    public final Color disabledFill;
    public final Color disabledText;
    public final Color dim;
    public final boolean highContrast;

    private static Palette cached;
    private static boolean cachedContrast;
    private static boolean cachedBlind;

    private Palette(boolean hc, boolean cb) {
        highContrast = hc;

        if (hc) {
            panel = Color.color(0, 0, 0, 0.97);
            panelSoft = Color.color(0.08, 0.08, 0.08, 0.98);
            border = Color.WHITE;
            text = Color.WHITE;
            textDim = Color.web("#E6E6E6");
            accent = Color.web("#FFFF33");
            good = Color.web(cb ? "#56B4E9" : "#33FF99");
            bad = Color.web(cb ? "#FFB000" : "#FF6666");
            info = Color.web("#66E0FF");
            barBackground = Color.web("#303030");
            optionFill = Color.web("#101010");
            optionHover = Color.web("#2A2A2A");
            buttonFill = Color.web("#FFFF33");
            buttonHover = Color.WHITE;
            buttonText = Color.BLACK;
            disabledFill = Color.web("#444444");
            disabledText = Color.web("#DDDDDD");
            dim = Color.color(0, 0, 0, 0.82);
        } else {
            panel = Color.color(0.07, 0.11, 0.09, 0.94);
            panelSoft = Color.color(0.13, 0.20, 0.15, 0.96);
            border = Color.web("#D7B56D");
            text = Color.web("#F6EFDD");
            textDim = Color.web("#BCC8B6");
            accent = Color.web("#F3D27A");
            good = Color.web(cb ? "#56B4E9" : "#6CCB78");
            bad = Color.web(cb ? "#E69F00" : "#F07A6A");
            info = Color.web("#7CC8E8");
            barBackground = Color.web("#33423A");
            optionFill = Color.web("#233A2C");
            optionHover = Color.web("#315240");
            buttonFill = Color.web("#D7B56D");
            buttonHover = Color.web("#F3D27A");
            buttonText = Color.web("#243B2A");
            disabledFill = Color.web("#56645A");
            disabledText = Color.web("#C9D0C8");
            dim = Color.color(0, 0, 0, 0.62);
        }
    }

    /** The palette for the current settings (rebuilt only when they change). */
    public static synchronized Palette get() {
        UserSettings s = UserSettings.get();

        if (cached == null
                || cachedContrast != s.isHighContrast()
                || cachedBlind != s.isColorblindSafe()) {
            cached = new Palette(s.isHighContrast(), s.isColorblindSafe());
            cachedContrast = s.isHighContrast();
            cachedBlind = s.isColorblindSafe();
        }

        return cached;
    }
}
