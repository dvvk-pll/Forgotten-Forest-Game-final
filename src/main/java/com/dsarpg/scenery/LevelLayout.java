package com.dsarpg.scenery;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything is placed in each region: the player's start, enemies
 * (with their patrol range), treasure chests, the guardian NPC and the boss.
 * The {@link MapGenerator} builds terrain around these points so that
 * everything is always reachable and never blocked by scenery.
 */
public final class LevelLayout {

    /** A regular enemy that patrols horizontally around its start. */
    public static final class EnemySpawn {
        public final String name;
        public final double x;
        public final double y;
        public final int health;
        public final double patrolMin;
        public final double patrolMax;

        EnemySpawn(String name, double x, double y, int health) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.health = health;
            this.patrolMin = Math.max(45, x - 150);
            this.patrolMax = Math.min(1920 - 45, x + 150 + 36);
        }
    }

    public static final class ChestSpawn {
        public final double x;
        public final double y;
        public final int rewardXp;

        ChestSpawn(double x, double y, int rewardXp) {
            this.x = x;
            this.y = y;
            this.rewardXp = rewardXp;
        }
    }

    private final int level;
    private final double playerX;
    private final double playerY;
    private final double guardianX;
    private final double guardianY;
    private final double bossX;
    private final double bossY;
    private final int bossHealth;
    private final List<EnemySpawn> enemies = new ArrayList<>();
    private final List<ChestSpawn> chests = new ArrayList<>();

    private LevelLayout(int level, double playerX, double playerY,
                        double guardianX, double guardianY,
                        double bossX, double bossY, int bossHealth) {
        this.level = level;
        this.playerX = playerX;
        this.playerY = playerY;
        this.guardianX = guardianX;
        this.guardianY = guardianY;
        this.bossX = bossX;
        this.bossY = bossY;
        this.bossHealth = bossHealth;
    }

    public static LevelLayout forLevel(int level) {
        int lv = Math.max(1, Math.min(5, level));
        SceneryTheme theme = SceneryTheme.forLevel(lv);
        double spawnX = 960;
        double spawnY = 640;

        LevelLayout l;

        switch (lv) {
            case 2:
                l = new LevelLayout(2, spawnX, spawnY, 330, 800,
                        1740, 643, 10);
                l.enemy("Ruins Sentinel", 250, 260, 3);
                l.enemy("Ancient Slime", 1050, 200, 3);
                l.enemy("Stone Knight", 1350, 930, 3);
                l.chest(350, 610, 25);
                l.chest(1000, 570, 35);
                l.chest(1500, 650, 50);
                break;

            case 3:
                l = new LevelLayout(3, spawnX, spawnY, 800, 880,
                        1740, 643, 12);
                l.enemy("Frost Wolf", 300, 420, 3);
                l.enemy("Ice Golem", 1000, 260, 4);
                l.enemy("Frozen Knight", 1500, 980, 4);
                l.chest(300, 650, 30);
                l.chest(1050, 590, 40);
                l.chest(1550, 700, 60);
                break;

            case 4:
                l = new LevelLayout(4, spawnX, spawnY, 1450, 900,
                        1700, 200, 14);
                l.enemy("Crystal Bat", 350, 300, 4);
                l.enemy("Glow Slime", 1100, 460, 4);
                l.enemy("Cave Troll", 500, 1000, 4);
                l.chest(400, 580, 40);
                l.chest(1100, 650, 50);
                l.chest(1600, 600, 70);
                break;

            case 5:
                l = new LevelLayout(5, spawnX, spawnY, 300, 1000,
                        960, 150, 16);
                l.enemy("Shadow Wraith", 300, 250, 4);
                l.enemy("Citadel Guard", 1000, 380, 5);
                l.enemy("Flame Imp", 1500, 900, 5);
                l.chest(350, 600, 50);
                l.chest(1000, 620, 65);
                l.chest(1550, 680, 80);
                break;

            case 1:
            default:
                l = new LevelLayout(1, spawnX, spawnY, 9 * 32 + 5, 6 * 32 + 5,
                        1740, 643, 8);
                l.enemy("Forest Goblin", 220, 150, 2);
                l.enemy("Dark Slime", 780, 270, 2);
                l.enemy("Orc Scout", 1330, 570, 2);
                l.chest(500, 590, 15);
                l.chest(960, 590, 25);
                l.chest(1450, 790, 40);
                break;
        }

        return l;
    }

    private void enemy(String name, double x, double y, int hp) {
        enemies.add(new EnemySpawn(name, x, y, hp));
    }

    private void chest(double x, double y, int xp) {
        chests.add(new ChestSpawn(x, y, xp));
    }

    public int getLevel() { return level; }
    public double getPlayerX() { return playerX; }
    public double getPlayerY() { return playerY; }
    public double getGuardianX() { return guardianX; }
    public double getGuardianY() { return guardianY; }
    public double getBossX() { return bossX; }
    public double getBossY() { return bossY; }
    public int getBossHealth() { return bossHealth; }
    public List<EnemySpawn> getEnemies() { return enemies; }
    public List<ChestSpawn> getChests() { return chests; }

    public String getBossName() {
        return SceneryTheme.forLevel(level).getBossName();
    }

    /** Names of the regular (non-boss) enemies, used by the save system. */
    public List<String> regularEnemyNames() {
        List<String> names = new ArrayList<>();

        for (EnemySpawn e : enemies) {
            names.add(e.name);
        }

        return names;
    }
}
