package com.dsarpg;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AccountService {

    public PlayerProgress register(String username, char[] password)
            throws SQLException, GeneralSecurityException {

        validateCredentials(username, password);

        String sql = """
                INSERT INTO players
                    (username, password_hash, xp, player_level, health,
                     enemies_defeated, healing_potions, quest_accepted,
                     quest_completed, quest_reward_granted,
                     quest_starting_defeated_count, player_x, player_y,
                     defeated_enemy_ids, opened_chest_ids, boss_spawned,
                     boss_defeated, unlocked_level, completed_levels,
                     current_level)
                VALUES (?, ?, 0, 1, 5, 0, 0, FALSE, FALSE, FALSE, 0,
                        69, 69, '', '', FALSE, FALSE, 1, '', 1)
                """;

        String hash = PasswordUtil.hash(password);

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username.trim());
            ps.setString(2, hash);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new PlayerProgress(
                            keys.getInt(1),
                            username.trim(),
                            0, 1, 5, 0,
                            0, false, false, false, 0
                    );
                }
            }
        }

        throw new SQLException("Could not retrieve the new player ID.");
    }

    public PlayerProgress login(String username, char[] password)
            throws SQLException, GeneralSecurityException {

        if (username == null || username.isBlank()
                || password == null || password.length == 0) {
            return null;
        }

        String sql = """
                SELECT id, username, password_hash, xp, player_level, health,
                       enemies_defeated, healing_potions, quest_accepted,
                       quest_completed, quest_reward_granted,
                       quest_starting_defeated_count, player_x, player_y,
                       defeated_enemy_ids, opened_chest_ids, boss_spawned,
                       boss_defeated, unlocked_level, completed_levels,
                       current_level
                FROM players
                WHERE username = ?
                """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                if (!PasswordUtil.verify(
                        password, rs.getString("password_hash"))) {
                    return null;
                }

                return readPlayerProgress(rs);
            }
        }
    }

    public void saveProgress(PlayerProgress player)
            throws SQLException {

        String sql = """
                UPDATE players SET
                    xp = ?,
                    player_level = ?,
                    health = ?,
                    enemies_defeated = ?,
                    healing_potions = ?,
                    quest_accepted = ?,
                    quest_completed = ?,
                    quest_reward_granted = ?,
                    quest_starting_defeated_count = ?,
                    player_x = ?,
                    player_y = ?,
                    defeated_enemy_ids = ?,
                    opened_chest_ids = ?,
                    boss_spawned = ?,
                    boss_defeated = ?,
                    unlocked_level = ?,
                    completed_levels = ?,
                    current_level = ?
                WHERE id = ?
                """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, player.getXp());
            ps.setInt(2, player.getLevel());
            ps.setInt(3, player.getHealth());
            ps.setInt(4, player.getEnemiesDefeated());
            ps.setInt(5, player.getHealingPotions());

            ps.setBoolean(6, player.isQuestAccepted());
            ps.setBoolean(7, player.isQuestCompleted());
            ps.setBoolean(8, player.isQuestRewardGranted());

            ps.setInt(9, player.getQuestStartingDefeatedCount());

            ps.setDouble(10, player.getPlayerX());
            ps.setDouble(11, player.getPlayerY());

            ps.setString(12, player.getDefeatedEnemyIds());
            ps.setString(13, player.getOpenedChestIds());

            ps.setBoolean(14, player.isBossSpawned());
            ps.setBoolean(15, player.isBossDefeated());

            ps.setInt(16, player.getUnlockedLevel());
            ps.setString(17, player.getCompletedLevels());
            ps.setInt(18, player.getCurrentLevel());

            // The player ID belongs in parameter 19.
            ps.setInt(19, player.getPlayerId());

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Progress was not saved. Please log in again.");
            }
        }
    }

    public PlayerProgress loadProgress(int playerId)
            throws SQLException {

        String sql = """
                SELECT id, username, xp, player_level, health,
                       enemies_defeated, healing_potions,
                       quest_accepted, quest_completed,
                       quest_reward_granted,
                       quest_starting_defeated_count,
                       player_x, player_y, defeated_enemy_ids,
                       opened_chest_ids, boss_spawned, boss_defeated,
                       unlocked_level, completed_levels, current_level
                FROM players
                WHERE id = ?
                """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, playerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return readPlayerProgress(rs);
            }
        }
    }

    private PlayerProgress readPlayerProgress(ResultSet rs)
            throws SQLException {

        PlayerProgress player = new PlayerProgress(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getInt("xp"),
                rs.getInt("player_level"),
                rs.getInt("health"),
                rs.getInt("enemies_defeated"),
                rs.getInt("healing_potions"),
                rs.getBoolean("quest_accepted"),
                rs.getBoolean("quest_completed"),
                rs.getBoolean("quest_reward_granted"),
                rs.getInt("quest_starting_defeated_count")
        );

        player.setPlayerX(rs.getDouble("player_x"));
        player.setPlayerY(rs.getDouble("player_y"));

        player.setDefeatedEnemyIds(
                rs.getString("defeated_enemy_ids"));
        player.setOpenedChestIds(
                rs.getString("opened_chest_ids"));

        player.setBossSpawned(rs.getBoolean("boss_spawned"));
        player.setBossDefeated(rs.getBoolean("boss_defeated"));

        player.setUnlockedLevel(rs.getInt("unlocked_level"));
        player.setCompletedLevels(rs.getString("completed_levels"));
        player.setCurrentLevel(rs.getInt("current_level"));

        return player;
    }

    private void validateCredentials(String username, char[] password) {

        if (username == null
                || !username.matches("[A-Za-z0-9_]{3,50}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-50 characters: "
                    + "letters, numbers or underscore.");
        }

        if (password == null
                || password.length < 8
                || password.length > 128) {
            throw new IllegalArgumentException(
                    "Password must contain between 8 and 128 characters.");
        }
    }
}