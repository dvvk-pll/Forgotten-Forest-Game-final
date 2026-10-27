
package com.dsarpg;

public class QuestManager {

    private boolean accepted = false;
    private boolean completed = false;
    private int startingDefeatedCount = 0;

    private final String npcName;
    private final String region;

    private static final int REQUIRED_DEFEATS = 3;
    private static final int REWARD_XP = 50;

    public QuestManager() {
        this("Forest Guardian", "forest");
    }

    public QuestManager(String npcName, String region) {
        this.npcName = npcName;
        this.region = region;
    }

    public String interact(int totalDefeated) {

        if (completed) {
            return npcName + ": You have restored some of the "
                    + region + "'s lost knowledge. Well done, hero!";
        }

        if (!accepted) {
            accepted = true;
            startingDefeatedCount = totalDefeated;

            return npcName + ":\n"
                    + "The " + region + "'s ancient knowledge has been corrupted!\n"
                    + "Defeat three enemies by answering their DSA questions.\n"
                    + "Return to me when you have completed the task.";
        }

        int defeated = getQuestDefeats(totalDefeated);

        if (defeated >= REQUIRED_DEFEATS) {
            completed = true;

            return "QUEST COMPLETED!\n"
                    + "You defeated three enemies and proved your knowledge.\n"
                    + "Reward: " + REWARD_XP + " bonus XP!";
        }

        return npcName + ":\n"
                + "You have defeated " + defeated + " of "
                + REQUIRED_DEFEATS + " enemies.\n"
                + "Keep exploring and answering DSA questions!";
    }

    public int getQuestDefeats(int totalDefeated) {
        return Math.max(0, totalDefeated - startingDefeatedCount);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getRequiredDefeats() {
        return REQUIRED_DEFEATS;
    }

    public int getRewardXp() {
        return REWARD_XP;
    }

    public int getStartingDefeatedCount() {
        return startingDefeatedCount;
    }

    // Restore quest state from saved progress.
    public void loadProgress(
            boolean accepted,
            boolean completed,
            int startingDefeatedCount) {

        this.accepted = accepted;
        this.completed = completed;
        this.startingDefeatedCount =
                Math.max(0, startingDefeatedCount);
    }

    // Reset the quest when starting a completely new game.
    public void reset() {
        accepted = false;
        completed = false;
        startingDefeatedCount = 0;
    }

    public String getStatus(int totalDefeated) {

        if (completed) {
            return "Quest: The Forgotten Knowledge - COMPLETED";
        }

        if (!accepted) {
            return "Quest: Find the " + npcName + " and press E";
        }

        return "Quest: Defeat enemies - "
                + getQuestDefeats(totalDefeated)
                + "/" + REQUIRED_DEFEATS;
    }
}
