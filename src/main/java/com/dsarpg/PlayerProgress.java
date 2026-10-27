package com.dsarpg;

public class PlayerProgress {

    private final int playerId;
    private final String username;

    private int xp;
    private int level;
    private int health;
    private int enemiesDefeated;
    private int healingPotions;

    private boolean questAccepted;
    private boolean questCompleted;
    private boolean questRewardGranted;

    private int questStartingDefeatedCount;

    private double playerX;
    private double playerY;

    private String defeatedEnemyIds = "";
    private String openedChestIds = "";

    private boolean bossSpawned;
    private boolean bossDefeated;

    private int unlockedLevel = 1;
    private String completedLevels = "";
    private int currentLevel = 1;

    public PlayerProgress(
            int playerId,
            String username,
            int xp,
            int level,
            int health,
            int enemiesDefeated) {

        this(
                playerId, username, xp, level, health,
                enemiesDefeated, 0, false, false, false, 0
        );
    }

    public PlayerProgress(
            int playerId,
            String username,
            int xp,
            int level,
            int health,
            int enemiesDefeated,
            int healingPotions,
            boolean questAccepted,
            boolean questCompleted,
            boolean questRewardGranted) {

        this(
                playerId, username, xp, level, health,
                enemiesDefeated, healingPotions,
                questAccepted, questCompleted,
                questRewardGranted, 0
        );
    }

    public PlayerProgress(
            int playerId,
            String username,
            int xp,
            int level,
            int health,
            int enemiesDefeated,
            int healingPotions,
            boolean questAccepted,
            boolean questCompleted,
            boolean questRewardGranted,
            int questStartingDefeatedCount) {

        this.playerId = playerId;
        this.username = username;

        setXp(xp);
        setLevel(level);
        setHealth(health);
        setEnemiesDefeated(enemiesDefeated);
        setHealingPotions(healingPotions);

        this.questAccepted = questAccepted;
        this.questCompleted = questCompleted;
        this.questRewardGranted = questRewardGranted;

        setQuestStartingDefeatedCount(questStartingDefeatedCount);
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int value) {
        xp = Math.max(0, value);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int value) {
        level = Math.max(1, value);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int value) {
        health = Math.max(0, Math.min(5, value));
    }

    public int getEnemiesDefeated() {
        return enemiesDefeated;
    }

    public void setEnemiesDefeated(int value) {
        enemiesDefeated = Math.max(0, value);
    }

    public int getHealingPotions() {
        return healingPotions;
    }

    public void setHealingPotions(int value) {
        healingPotions = Math.max(0, value);
    }

    public boolean isQuestAccepted() {
        return questAccepted;
    }

    public void setQuestAccepted(boolean value) {
        questAccepted = value;
    }

    public boolean isQuestCompleted() {
        return questCompleted;
    }

    public void setQuestCompleted(boolean value) {
        questCompleted = value;
    }

    public boolean isQuestRewardGranted() {
        return questRewardGranted;
    }

    public void setQuestRewardGranted(boolean value) {
        questRewardGranted = value;
    }

    public int getQuestStartingDefeatedCount() {
        return questStartingDefeatedCount;
    }

    public void setQuestStartingDefeatedCount(int value) {
        questStartingDefeatedCount = Math.max(0, value);
    }

    public double getPlayerX() {
        return playerX;
    }

    public void setPlayerX(double value) {
        playerX = Math.max(0, value);
    }

    public double getPlayerY() {
        return playerY;
    }

    public void setPlayerY(double value) {
        playerY = Math.max(0, value);
    }

    public String getDefeatedEnemyIds() {
        return defeatedEnemyIds;
    }

    public void setDefeatedEnemyIds(String value) {
        defeatedEnemyIds = value == null ? "" : value;
    }

    public String getOpenedChestIds() {
        return openedChestIds;
    }

    public void setOpenedChestIds(String value) {
        openedChestIds = value == null ? "" : value;
    }

    public boolean isBossSpawned() {
        return bossSpawned;
    }

    public void setBossSpawned(boolean value) {
        bossSpawned = value;
    }

    public boolean isBossDefeated() {
        return bossDefeated;
    }

    public void setBossDefeated(boolean value) {
        bossDefeated = value;
    }

    public int getUnlockedLevel() {
        return unlockedLevel;
    }

    public void setUnlockedLevel(int value) {
        unlockedLevel = Math.max(1, Math.min(5, value));
    }

    public String getCompletedLevels() {
        return completedLevels;
    }

    public void setCompletedLevels(String value) {
        completedLevels = value == null ? "" : value;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int value) {
        currentLevel = Math.max(1, Math.min(5, value));
    }
}