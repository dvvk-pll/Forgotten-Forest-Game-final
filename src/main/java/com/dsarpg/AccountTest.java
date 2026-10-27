
package com.dsarpg;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Scanner;

public class AccountTest {

    public static void main(String[] args) {
        AccountService service = new AccountService();

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("=== DSARPG Account Test ===");
            System.out.println("1. Register new player");
            System.out.println("2. Login existing player");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine().trim();

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            char[] password = scanner.nextLine().toCharArray();

            try {
                PlayerProgress player;

                if ("1".equals(choice)) {
                    player = service.register(username, password);
                    System.out.println("Registration successful!");

                    // Test saving some sample progress.
                    player.setXp(35);
                    player.setLevel(1);
                    player.setHealth(4);
                    player.setEnemiesDefeated(1);

                    service.saveProgress(player);
                    System.out.println("Sample progress saved.");

                } else if ("2".equals(choice)) {
                    player = service.login(username, password);

                    if (player == null) {
                        System.out.println(
                                "Login failed. Check your username and password.");
                        return;
                    }

                    System.out.println("Login successful!");

                } else {
                    System.out.println("Invalid option.");
                    return;
                }

                // Read the saved values back from MySQL.
                PlayerProgress loaded =
                        service.loadProgress(player.getPlayerId());

                if (loaded != null) {
                    System.out.println("\n--- Loaded Player Progress ---");
                    System.out.println("Username: " + loaded.getUsername());
                    System.out.println("XP: " + loaded.getXp());
                    System.out.println("Level: " + loaded.getLevel());
                    System.out.println("Health: " + loaded.getHealth());
                    System.out.println(
                            "Enemies defeated: " + loaded.getEnemiesDefeated());
                    System.out.println("Save/load test successful!");
                }

            } finally {
                java.util.Arrays.fill(password, '\0');
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Input error: " + e.getMessage());

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());

        } catch (GeneralSecurityException e) {
            System.err.println("Password security error: " + e.getMessage());
        }
    }
}
