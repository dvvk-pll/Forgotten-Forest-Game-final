package com.dsarpg.usability;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Accessibility and comfort settings, remembered between sessions in
 * ~/.dsarpg/settings.properties.
 *
 * Every setting has a safe default and the game works even when the file
 * cannot be read or written.
 */
public final class UserSettings {

    /** Text size choices (scale factor applied to every piece of text). */
    public enum TextSize {
        SMALL("Small", 0.9),
        MEDIUM("Medium", 1.0),
        LARGE("Large", 1.2),
        EXTRA_LARGE("Extra large", 1.4);

        private final String label;
        private final double scale;

        TextSize(String label, double scale) {
            this.label = label;
            this.scale = scale;
        }

        public String getLabel() {
            return label;
        }

        public double getScale() {
            return scale;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static UserSettings instance;

    private final File file;

    private TextSize textSize = TextSize.MEDIUM;
    private boolean highContrast = false;
    private boolean colorblindSafe = false;
    private boolean reduceMotion = false;
    private boolean aiCoach = true;
    private boolean showMinimap = true;
    private boolean tutorialSeen = false;
    private String lastUsername = "";

    public UserSettings(File file) {
        this.file = file;
        load();
    }

    /** The shared settings object used by the whole game. */
    public static synchronized UserSettings get() {
        if (instance == null) {
            instance = new UserSettings(defaultFile());
        }

        return instance;
    }

    public static File defaultFile() {
        return new File(System.getProperty("user.home"),
                ".dsarpg/settings.properties");
    }

    private void load() {
        if (file == null || !file.isFile()) {
            return;
        }

        Properties p = new Properties();

        try (InputStream in = Files.newInputStream(file.toPath())) {
            p.load(in);
        } catch (IOException | IllegalArgumentException ex) {
            return;
        }

        try {
            textSize = TextSize.valueOf(p.getProperty("textSize", "MEDIUM"));
        } catch (IllegalArgumentException ex) {
            textSize = TextSize.MEDIUM;
        }

        highContrast = Boolean.parseBoolean(p.getProperty("highContrast", "false"));
        colorblindSafe = Boolean.parseBoolean(p.getProperty("colorblindSafe", "false"));
        reduceMotion = Boolean.parseBoolean(p.getProperty("reduceMotion", "false"));
        aiCoach = Boolean.parseBoolean(p.getProperty("aiCoach", "true"));
        showMinimap = Boolean.parseBoolean(p.getProperty("showMinimap", "true"));
        tutorialSeen = Boolean.parseBoolean(p.getProperty("tutorialSeen", "false"));
        lastUsername = p.getProperty("lastUsername", "");
    }

    public void save() {
        if (file == null) {
            return;
        }

        Properties p = new Properties();
        p.setProperty("textSize", textSize.name());
        p.setProperty("highContrast", Boolean.toString(highContrast));
        p.setProperty("colorblindSafe", Boolean.toString(colorblindSafe));
        p.setProperty("reduceMotion", Boolean.toString(reduceMotion));
        p.setProperty("aiCoach", Boolean.toString(aiCoach));
        p.setProperty("showMinimap", Boolean.toString(showMinimap));
        p.setProperty("tutorialSeen", Boolean.toString(tutorialSeen));
        p.setProperty("lastUsername", lastUsername == null ? "" : lastUsername);

        try {
            File dir = file.getParentFile();

            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
                return;
            }

            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                p.store(out, "DSA RPG settings");
            }
        } catch (IOException ex) {
            System.err.println("Could not save settings: " + ex.getMessage());
        }
    }

    public TextSize getTextSize() { return textSize; }
    public void setTextSize(TextSize value) { this.textSize = value == null ? TextSize.MEDIUM : value; }
    public double getTextScale() { return textSize.getScale(); }

    public boolean isHighContrast() { return highContrast; }
    public void setHighContrast(boolean value) { this.highContrast = value; }

    public boolean isColorblindSafe() { return colorblindSafe; }
    public void setColorblindSafe(boolean value) { this.colorblindSafe = value; }

    public boolean isReduceMotion() { return reduceMotion; }
    public void setReduceMotion(boolean value) { this.reduceMotion = value; }

    public boolean isAiCoach() { return aiCoach; }
    public void setAiCoach(boolean value) { this.aiCoach = value; }

    public boolean isShowMinimap() { return showMinimap; }
    public void setShowMinimap(boolean value) { this.showMinimap = value; }

    public boolean isTutorialSeen() { return tutorialSeen; }
    public void setTutorialSeen(boolean value) { this.tutorialSeen = value; }

    public String getLastUsername() { return lastUsername == null ? "" : lastUsername; }
    public void setLastUsername(String value) { this.lastUsername = value == null ? "" : value; }
}
