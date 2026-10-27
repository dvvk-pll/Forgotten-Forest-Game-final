package com.dsarpg.ai;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Random;

/**
 * Loads and saves each player's AI profile (knowledge estimates, learned
 * model weights and bandit beliefs) as a small properties file in
 * ~/.dsarpg/profiles. The game's database schema is not touched, and a
 * failure to read or write the file never stops the game.
 */
public final class AiProfileStore {

    private AiProfileStore() {
    }

    public static File profileDirectory() {
        return new File(System.getProperty("user.home"), ".dsarpg/profiles");
    }

    public static File fileFor(String username) {
        String safe = username == null ? "player" : username;
        String clean = safe.replaceAll("[^A-Za-z0-9_-]", "_");

        return new File(profileDirectory(),
                clean + "-" + Integer.toHexString(safe.hashCode()) + ".properties");
    }

    public static AdaptiveCoach load(String username) {
        return load(username, fileFor(username));
    }

    public static AdaptiveCoach load(String username, File file) {
        Properties saved = null;

        if (file != null && file.isFile()) {
            try (InputStream in = Files.newInputStream(file.toPath())) {
                saved = new Properties();
                saved.load(in);
            } catch (IOException | IllegalArgumentException ex) {
                System.err.println("Could not read AI profile: "
                        + ex.getMessage());
                saved = null;
            }
        }

        AdaptiveCoach coach = new AdaptiveCoach(username, saved, new Random());
        coach.startSession();
        return coach;
    }

    public static void save(AdaptiveCoach coach) {
        if (coach != null) {
            save(coach, fileFor(coach.getUsername()));
        }
    }

    public static void save(AdaptiveCoach coach, File file) {
        if (coach == null || file == null) {
            return;
        }

        try {
            File dir = file.getParentFile();

            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
                return;
            }

            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                coach.toProperties().store(out,
                        "DSA RPG AI Coach profile for " + coach.getUsername());
            }
        } catch (IOException ex) {
            System.err.println("Could not save AI profile: " + ex.getMessage());
        }
    }
}
