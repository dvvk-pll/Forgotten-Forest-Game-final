
package com.dsarpg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import com.dsarpg.ai.AdaptiveCoach;
import com.dsarpg.scenery.LevelLayout;
import com.dsarpg.scenery.MapGenerator;
import com.dsarpg.scenery.SceneryRenderer;
import com.dsarpg.scenery.SceneryTheme;
import com.dsarpg.usability.UsabilityLogger;
import com.dsarpg.usability.UserSettings;

/**
 * One region of the game: terrain, enemies, chests, guardian and the
 * battle system. Each of the five levels has its own scenery theme, map,
 * enemies and boss (see {@link SceneryTheme} and {@link LevelLayout}).
 */
public class GameWorld {

    public static final int TILE_SIZE = MapGenerator.TILE;
    public static final int COLS = MapGenerator.COLS;
    public static final int ROWS = MapGenerator.ROWS;

    public static final int VIEW_WIDTH = 960;
    public static final int VIEW_HEIGHT = 640;

    private final int currentLevel;
    private final SceneryTheme theme;
    private final LevelLayout layout;
    private final int[][] map;
    private final SceneryRenderer renderer;
    private final GameHud hud = new GameHud();

    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final BattleManager battle;
    private final ForestGuardian guardian;
    private final QuestManager quest;
    private final List<TreasureChest> treasureChests = new ArrayList<>();

    private double cameraX;
    private double cameraY;

    private String dialogue = "";
    private double dialogueTimer;

    private boolean bossSpawned;
    private boolean bossDefeated;
    private boolean questRewardGranted;

    private boolean helpVisible;
    private boolean insightsVisible;
    private boolean autosaveRequested;
    private UsabilityLogger logger;

    public GameWorld() {
        this(1);
    }

    public GameWorld(int currentLevel) {
        this(currentLevel, new AdaptiveCoach("guest"), null);
    }

    public GameWorld(int currentLevel, AdaptiveCoach coach, UsabilityLogger logger) {
        this.currentLevel = Math.max(1, Math.min(5, currentLevel));
        this.theme = SceneryTheme.forLevel(this.currentLevel);
        this.layout = LevelLayout.forLevel(this.currentLevel);
        this.map = MapGenerator.generate(layout);
        this.renderer = new SceneryRenderer(theme, map);
        this.logger = logger;

        this.battle = new BattleManager(this.currentLevel, coach, logger);
        this.player = new Player(layout.getPlayerX(), layout.getPlayerY());
        this.guardian = new ForestGuardian(
                layout.getGuardianX(), layout.getGuardianY(),
                theme.getGuardianName(), guardianRobe());
        this.quest = new QuestManager(theme.getGuardianName(), regionWord());

        createEnemies();
        createTreasureChests();
        updateCamera();
    }

    /** Connects (or replaces) the AI coach and usability logger. */
    public void attach(AdaptiveCoach coach, UsabilityLogger newLogger) {
        this.logger = newLogger;
        battle.attach(coach, newLogger);
    }

    private Color guardianRobe() {
        switch (theme) {
            case RUINS:
                return Color.web("#8B3A62");
            case FROZEN_PEAKS:
                return Color.web("#2F6DA3");
            case CRYSTAL_CAVERNS:
                return Color.web("#5B3FA8");
            case CITADEL:
                return Color.web("#7A1F2B");
            case FOREST:
            default:
                return Color.DARKBLUE;
        }
    }

    private String regionWord() {
        switch (theme) {
            case RUINS:
                return "ruins";
            case FROZEN_PEAKS:
                return "peaks";
            case CRYSTAL_CAVERNS:
                return "caverns";
            case CITADEL:
                return "citadel";
            case FOREST:
            default:
                return "forest";
        }
    }

    private String bossName() {
        return theme.getBossName();
    }

    // --------------------------------------------------
    // CREATION
    // --------------------------------------------------

    private void createEnemies() {
        enemies.clear();

        for (LevelLayout.EnemySpawn spawn : layout.getEnemies()) {
            enemies.add(new Enemy(spawn.name, spawn.x, spawn.y, spawn.health,
                    spawn.patrolMin, spawn.patrolMax, false, theme));
        }

        bossSpawned = false;
        bossDefeated = false;
    }

    private void createTreasureChests() {
        treasureChests.clear();

        for (LevelLayout.ChestSpawn chest : layout.getChests()) {
            treasureChests.add(new TreasureChest(chest.x, chest.y, chest.rewardXp));
        }
    }

    private Enemy newBoss() {
        return new Enemy(bossName(), layout.getBossX(), layout.getBossY(),
                layout.getBossHealth(), 0, 0, true, theme);
    }

    private void spawnBoss() {
        if (bossSpawned || bossDefeated) {
            return;
        }

        enemies.add(newBoss());
        bossSpawned = true;

        dialogue = "A terrible presence appears!\n"
                + bossName().toUpperCase() + " blocks your way.\n"
                + "Answer the final DSA challenge to defeat it.";
        dialogueTimer = 7;
        hud.toast("Boss appeared: " + bossName());
    }

    // --------------------------------------------------
    // COLLISION
    // --------------------------------------------------

    public boolean isBlocked(double x, double y, double width, double height) {
        return MapGenerator.boxBlocked(map, x, y, width, height);
    }

    private boolean touching(Player p, Enemy e) {
        double dx = (p.getX() + p.getWidth() / 2.0) - (e.getX() + e.getWidth() / 2.0);
        double dy = (p.getY() + p.getHeight() / 2.0) - (e.getY() + e.getHeight() / 2.0);
        double radius = e.isBoss() ? 40 : 28;

        return Math.sqrt(dx * dx + dy * dy) < radius;
    }

    // --------------------------------------------------
    // GAME UPDATE
    // --------------------------------------------------

    public void update(Set<KeyCode> keys, double deltaTime) {

        hud.update(deltaTime);
        renderer.update(deltaTime);

        for (String note : battle.consumeNotifications()) {
            hud.toast(note);
        }

        if (battle.consumeBattleFinished()) {
            autosaveRequested = true;
        }

        if (battle.consumeRestartRequest()) {
            restartLevel();
        }

        if (dialogueTimer > 0) {
            dialogueTimer -= deltaTime;

            if (dialogueTimer <= 0) {
                dialogue = "";
            }
        }

        if (helpVisible || insightsVisible
                || battle.isActive() || battle.isGameOver()) {
            return;
        }

        player.update(keys, this, deltaTime);

        for (Enemy enemy : enemies) {
            enemy.update(deltaTime, 45, COLS * TILE_SIZE - 45);
        }

        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && touching(player, enemy)) {
                battle.startBattle(enemy);
                return;
            }
        }

        enemies.removeIf(enemy -> !enemy.isAlive());

        // The boss appears once every regular enemy is defeated.
        if (!bossSpawned && !bossDefeated && enemies.isEmpty()) {
            spawnBoss();
        }

        if (bossSpawned && !bossDefeated
                && enemies.stream().noneMatch(Enemy::isBoss)) {
            bossDefeated = true;
            dialogue = "VICTORY! " + bossName() + " has been defeated!\n"
                    + theme.getTitle() + " is safe once more.";
            dialogueTimer = 8;
        }

        updateCamera();
    }

    private void restartLevel() {
        battle.restart();
        createEnemies();
        player.setPosition(layout.getPlayerX(), layout.getPlayerY());

        dialogue = "";
        dialogueTimer = 0;
        hud.toast("Fresh start. Your learning progress is kept.");
        updateCamera();
    }

    // --------------------------------------------------
    // CAMERA
    // --------------------------------------------------

    private void updateCamera() {
        double targetX = player.getX() + player.getWidth() / 2.0 - VIEW_WIDTH / 2.0;
        double targetY = player.getY() + player.getHeight() / 2.0 - VIEW_HEIGHT / 2.0;

        cameraX = Math.max(0, Math.min(targetX, COLS * TILE_SIZE - VIEW_WIDTH));
        cameraY = Math.max(0, Math.min(targetY, ROWS * TILE_SIZE - VIEW_HEIGHT));
    }

    // --------------------------------------------------
    // INPUT
    // --------------------------------------------------

    public void handleKeyPressed(KeyCode key) {

        if (key == KeyCode.M) {
            insightsVisible = !insightsVisible;
            helpVisible = false;

            if (insightsVisible && logger != null) {
                logger.insightsOpened();
            }

            return;
        }

        if (insightsVisible) {
            return;
        }

        boolean inBattle = battle.isActive() || battle.isGameOver();

        if (key == KeyCode.F1 || (key == KeyCode.H && !inBattle)) {
            helpVisible = !helpVisible;

            if (helpVisible && logger != null) {
                logger.helpOpened();
            }

            UserSettings.get().setTutorialSeen(true);
            return;
        }

        if (helpVisible) {
            return;
        }

        if (inBattle) {
            battle.handleKey(key);
            return;
        }

        if (key == KeyCode.SPACE) {
            dialogue = "";
            dialogueTimer = 0;
            return;
        }

        if (key == KeyCode.P) {
            dialogue = battle.useHealingPotion();
            dialogueTimer = 3;
            return;
        }

        if (key == KeyCode.E) {
            interact();
        }
    }

    private void interact() {
        for (TreasureChest chest : treasureChests) {
            if (chest.isNear(player)) {
                if (chest.open()) {
                    battle.addXP(chest.getRewardXP());
                    battle.addHealingPotion();

                    dialogue = "TREASURE FOUND!\n"
                            + "You collected " + chest.getRewardXP() + " bonus XP!\n"
                            + "You also found a healing potion!";
                } else {
                    dialogue = "This treasure chest is empty.";
                }

                dialogueTimer = 5;
                return;
            }
        }

        if (guardian.isNear(player)) {
            dialogue = quest.interact(battle.getEnemiesDefeated());

            if (quest.isCompleted()) {
                dialogueTimer = 12;

                if (!questRewardGranted) {
                    battle.addXP(quest.getRewardXp());
                    questRewardGranted = true;
                    dialogue += "\nQuest reward: " + quest.getRewardXp() + " XP!";
                }
            } else {
                dialogueTimer = 10;
            }
        } else {
            dialogue = "Nothing nearby to interact with.";
            dialogueTimer = 3;
        }
    }

    /** True when the pointer is over something clickable. */
    public boolean handleMouseMove(double x, double y) {
        if (helpVisible || insightsVisible) {
            return true;
        }

        if (battle.isActive() || battle.isGameOver()) {
            return battle.handleMouseMove(x, y);
        }

        return false;
    }

    public boolean handleMouseClick(double x, double y) {
        if (helpVisible || insightsVisible) {
            helpVisible = false;
            insightsVisible = false;
            return true;
        }

        if (battle.isActive() || battle.isGameOver()) {
            return battle.handleMouseClick(x, y);
        }

        return false;
    }

    /** Opens the Learning Insights overlay (also used by toolbar buttons). */
    public void toggleInsights() {
        handleKeyPressed(KeyCode.M);
    }

    public void toggleHelp() {
        if (battle.isActive() || battle.isGameOver()) {
            return;
        }

        handleKeyPressed(KeyCode.F1);
    }

    /** Closes an open overlay; returns true if one was open (Esc handling). */
    public boolean closeOverlay() {
        if (helpVisible || insightsVisible) {
            helpVisible = false;
            insightsVisible = false;
            return true;
        }

        return false;
    }

    public boolean isOverlayOpen() {
        return helpVisible || insightsVisible;
    }

    public void showHelpFirstTime() {
        helpVisible = true;

        if (logger != null) {
            logger.helpOpened();
        }

        UserSettings.get().setTutorialSeen(true);
    }

    public void toast(String message) {
        hud.toast(message);
    }

    /** True once after each finished battle, so the app can autosave. */
    public boolean consumeAutosaveRequest() {
        boolean value = autosaveRequested;
        autosaveRequested = false;
        return value;
    }

    // --------------------------------------------------
    // SAVE AND LOAD
    // --------------------------------------------------

    public void loadProgress(PlayerProgress progress) {

        if (progress == null) {
            return;
        }

        battle.loadProgress(progress);

        boolean thisLevelAlreadyCompleted = false;

        if (progress.getCompletedLevels() != null
                && !progress.getCompletedLevels().isBlank()) {
            for (String id : progress.getCompletedLevels().split(",")) {
                if (id.trim().equals(String.valueOf(currentLevel))) {
                    thisLevelAlreadyCompleted = true;
                    break;
                }
            }
        }

        if (currentLevel >= 2) {
            if (thisLevelAlreadyCompleted) {
                enemies.clear();
                bossSpawned = true;
                bossDefeated = true;
            } else {
                createEnemies();
                createTreasureChests();
                questRewardGranted = false;
                player.setPosition(layout.getPlayerX(), layout.getPlayerY());
            }

            restoreTreasureChests(progress);
            updateCamera();
            return;
        }

        if (thisLevelAlreadyCompleted) {
            enemies.clear();
            bossSpawned = true;
            bossDefeated = true;

            restoreTreasureChests(progress);
            updateCamera();
            return;
        }

        // Restore the saved position, moving it out of scenery if needed.
        double savedX = progress.getPlayerX();
        double savedY = progress.getPlayerY();

        if (savedX > 0 && savedY > 0
                && savedX < COLS * TILE_SIZE && savedY < ROWS * TILE_SIZE) {
            if (isBlocked(savedX, savedY, player.getWidth(), player.getHeight())) {
                double[] free = MapGenerator.nearestWalkable(
                        map, savedX, savedY, player.getWidth(), player.getHeight());
                player.setPosition(free[0], free[1]);
            } else {
                player.setPosition(savedX, savedY);
            }
        }

        quest.loadProgress(
                progress.isQuestAccepted(),
                progress.isQuestCompleted(),
                progress.getQuestStartingDefeatedCount());

        questRewardGranted = progress.isQuestRewardGranted();

        Set<String> defeatedIds = new HashSet<>();

        if (progress.getDefeatedEnemyIds() != null
                && !progress.getDefeatedEnemyIds().isBlank()) {
            for (String id : progress.getDefeatedEnemyIds().split(",")) {
                defeatedIds.add(id.trim());
            }
        }

        enemies.removeIf(enemy -> defeatedIds.contains(enemy.getName()));

        restoreTreasureChests(progress);

        bossSpawned = progress.isBossSpawned();
        bossDefeated = progress.isBossDefeated();

        enemies.removeIf(enemy -> bossName().equals(enemy.getName()));

        if (bossSpawned && !bossDefeated) {
            enemies.add(newBoss());
        }

        updateCamera();
    }

    public void updateProgress(PlayerProgress progress) {

        if (progress == null) {
            return;
        }

        battle.updateProgress(progress);

        progress.setPlayerX(player.getX());
        progress.setPlayerY(player.getY());

        progress.setQuestAccepted(quest.isAccepted());
        progress.setQuestCompleted(quest.isCompleted());
        progress.setQuestStartingDefeatedCount(quest.getStartingDefeatedCount());
        progress.setQuestRewardGranted(questRewardGranted);

        Set<String> defeatedIds = new HashSet<>();

        for (String id : layout.regularEnemyNames()) {
            boolean stillAlive = enemies.stream()
                    .anyMatch(enemy -> enemy.isAlive() && id.equals(enemy.getName()));

            if (!stillAlive) {
                defeatedIds.add(id);
            }
        }

        if (bossDefeated) {
            defeatedIds.add(bossName());
        }

        progress.setDefeatedEnemyIds(String.join(",", defeatedIds));

        Set<String> openedIds = new HashSet<>();

        for (int i = 0; i < treasureChests.size(); i++) {
            if (treasureChests.get(i).isOpened()) {
                openedIds.add(currentLevel + ":" + i);
            }
        }

        progress.setOpenedChestIds(String.join(",", openedIds));

        progress.setBossSpawned(bossSpawned);
        progress.setBossDefeated(bossDefeated);

        progress.setCurrentLevel(currentLevel);

        if (bossDefeated) {
            progress.setUnlockedLevel(Math.max(
                    progress.getUnlockedLevel(), Math.min(5, currentLevel + 1)));

            Set<String> completed = new HashSet<>();

            if (!progress.getCompletedLevels().isBlank()) {
                for (String id : progress.getCompletedLevels().split(",")) {
                    completed.add(id.trim());
                }
            }

            completed.add(String.valueOf(currentLevel));
            progress.setCompletedLevels(String.join(",", completed));
        }
    }

    private void restoreTreasureChests(PlayerProgress progress) {
        Set<String> openedIds = new HashSet<>();
        String savedIds = progress.getOpenedChestIds();

        if (savedIds != null && !savedIds.isBlank()) {
            for (String id : savedIds.split(",")) {
                String trimmed = id.trim();

                if (trimmed.startsWith(currentLevel + ":")) {
                    openedIds.add(trimmed.substring(trimmed.indexOf(':') + 1));
                } else if (currentLevel == 1 && !trimmed.contains(":")) {
                    openedIds.add(trimmed);
                }
            }
        }

        for (int i = 0; i < treasureChests.size(); i++) {
            treasureChests.get(i).setOpened(openedIds.contains(String.valueOf(i)));
        }
    }

    // --------------------------------------------------
    // ACCESSORS (used by the HUD and the app)
    // --------------------------------------------------

    public boolean isBossDefeated() { return bossDefeated; }
    public int getCurrentLevel() { return currentLevel; }
    public BattleManager getBattle() { return battle; }

    BattleManager battle() { return battle; }
    SceneryTheme theme() { return theme; }
    String dialogue() { return dialogue; }
    double camX() { return cameraX; }
    double camY() { return cameraY; }
    double playerCx() { return player.getX() + player.getWidth() / 2.0; }
    double playerCy() { return player.getY() + player.getHeight() / 2.0; }
    Image minimap() { return renderer.minimapImage(); }
    List<TreasureChest> chests() { return treasureChests; }
    ForestGuardian guardian() { return guardian; }
    List<Enemy> enemies() { return enemies; }

    String objectiveText() {
        if (bossDefeated) {
            return "Region cleared! Press Esc to open the menu.";
        }

        if (bossSpawned) {
            return "Defeat " + bossName() + "!";
        }

        int total = layout.getEnemies().size();
        long alive = enemies.stream().filter(e -> !e.isBoss() && e.isAlive()).count();

        String text = "Defeat the enemies: " + (total - alive) + "/" + total;

        if (!quest.isCompleted()) {
            text += "  (bonus quest: " + theme.getGuardianName() + ")";
        }

        return text;
    }

    /** World position of the current objective, or null. */
    double[] objectiveTarget() {
        if (bossDefeated) {
            return null;
        }

        Enemy best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Enemy e : enemies) {
            if (!e.isAlive()) {
                continue;
            }

            double dx = e.getX() - player.getX();
            double dy = e.getY() - player.getY();
            double d = dx * dx + dy * dy;

            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }

        return best == null ? null
                : new double[] {best.getX() + best.getWidth() / 2.0,
                        best.getY() + best.getHeight() / 2.0};
    }

    String promptText() {
        if (battle.isActive() || battle.isGameOver()
                || helpVisible || insightsVisible) {
            return null;
        }

        for (TreasureChest chest : treasureChests) {
            if (chest.isNear(player) && !chest.isOpened()) {
                return "Open chest";
            }
        }

        if (guardian.isNear(player)) {
            return "Talk to " + theme.getGuardianName();
        }

        return null;
    }

    double[] promptPos() {
        for (TreasureChest chest : treasureChests) {
            if (chest.isNear(player) && !chest.isOpened()) {
                return new double[] {chest.getX() + 13, chest.getY() - 8};
            }
        }

        return new double[] {guardian.getX() + 14, guardian.getY() - 28};
    }

    // --------------------------------------------------
    // RENDER
    // --------------------------------------------------

    public void render(GraphicsContext gc) {

        updateCamera();

        UserSettings settings = UserSettings.get();
        boolean calm = settings.isReduceMotion();
        boolean plain = settings.isHighContrast();

        gc.setFill(Color.web(theme.getGroundDark()));
        gc.fillRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        gc.save();
        gc.translate(-cameraX, -cameraY);

        renderer.drawTerrain(gc, cameraX, cameraY, VIEW_WIDTH, VIEW_HEIGHT, !calm);

        for (TreasureChest chest : treasureChests) {
            chest.render(gc);
        }

        guardian.render(gc);

        // A soft ring keeps the hero easy to find on every background.
        gc.setStroke(Color.color(1, 1, 1, 0.55));
        gc.setLineWidth(2);
        gc.strokeOval(player.getX() - 5, player.getY() + 12, 32, 13);
        player.render(gc);

        for (Enemy enemy : enemies) {
            enemy.render(gc);
        }

        gc.restore();

        renderer.drawAtmosphere(gc, VIEW_WIDTH, VIEW_HEIGHT,
                playerCx() - cameraX, playerCy() - cameraY, !calm && !plain, !plain);

        hud.render(gc, this, VIEW_WIDTH, VIEW_HEIGHT);
        battle.render(gc, VIEW_WIDTH, VIEW_HEIGHT);

        if (helpVisible) {
            hud.renderHelp(gc, VIEW_WIDTH, VIEW_HEIGHT);
        }

        if (insightsVisible) {
            hud.renderInsights(gc, this, VIEW_WIDTH, VIEW_HEIGHT);
        }

        hud.renderToasts(gc, VIEW_WIDTH);
    }
}
