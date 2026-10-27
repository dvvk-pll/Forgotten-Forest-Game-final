package com.dsarpg;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import com.dsarpg.ai.AdaptiveCoach;
import com.dsarpg.ai.AdaptiveCoach.AnswerResult;
import com.dsarpg.ai.AdaptiveCoach.QuestionPlan;
import com.dsarpg.usability.UsabilityLogger;

/**
 * Runs a DSA battle: asks questions chosen by the {@link AdaptiveCoach},
 * applies the result (damage, health, XP) and tells the {@link BattleView}
 * what to draw.
 *
 * Usability: every action works with the keyboard and the mouse, wrong
 * answers always come with an explanation, and nothing is lost when the
 * player pauses.
 */
public class BattleManager {

    public static final int MAX_HEALTH = 5;
    public static final int XP_PER_LEVEL = 50;

    private final AdaptiveEngine engine;
    private final Inventory inventory = new Inventory();
    private final BattleView view = new BattleView();

    private AdaptiveCoach coach;
    private UsabilityLogger logger;

    private Enemy enemy;
    private QuestionPlan plan;
    private AnswerResult result;

    private int playerHealth = MAX_HEALTH;
    private int playerXP = 0;
    private int enemiesDefeated = 0;

    private boolean active = false;
    private boolean answered = false;
    private boolean lastAnswerCorrect = false;
    private boolean gameOver = false;
    private boolean showHint = false;
    private boolean hintUsedByPlayer = false;
    private boolean viaMouse = false;
    private boolean restartRequested = false;
    private boolean battleFinished = false;
    private int selectedIndex = -1;
    private int hoverIndex = -1;
    private int xpGained = 0;
    private String feedbackNote = "";
    private long questionStartNanos;

    private final List<String> notifications = new ArrayList<>();

    public BattleManager() {
        this(1);
    }

    public BattleManager(int level) {
        this(level, new AdaptiveCoach("guest"), null);
    }

    public BattleManager(int level, AdaptiveCoach coach, UsabilityLogger logger) {
        this.engine = new AdaptiveEngine(level);
        this.coach = coach;
        this.logger = logger;
    }

    public void attach(AdaptiveCoach newCoach, UsabilityLogger newLogger) {
        this.coach = newCoach;
        this.logger = newLogger;
    }

    // --------------------------------------------------
    // BATTLE FLOW
    // --------------------------------------------------

    public void startBattle(Enemy target) {
        if (target == null || !target.isAlive() || gameOver || active) {
            return;
        }

        enemy = target;
        active = true;
        battleFinished = false;

        if (logger != null) {
            logger.battleStarted();
        }

        nextQuestion();
    }

    private void nextQuestion() {
        plan = coach.planQuestion(engine);
        result = null;
        answered = false;
        lastAnswerCorrect = false;
        selectedIndex = -1;
        hoverIndex = -1;
        xpGained = 0;
        feedbackNote = "";
        hintUsedByPlayer = false;
        showHint = plan.hintOpenedByCoach();
        questionStartNanos = System.nanoTime();
    }

    private void answerQuestion(int selected, boolean mouse) {
        if (plan == null || enemy == null || answered) {
            return;
        }

        if (selected < 0 || selected >= plan.question.getOptions().length) {
            return;
        }

        if (plan.isOptionRemoved(selected)) {
            notifications.add("That answer was removed by the AI Coach. Pick another.");
            return;
        }

        long millis = (System.nanoTime() - questionStartNanos) / 1_000_000L;

        viaMouse = mouse;
        selectedIndex = selected;
        result = coach.recordAnswer(plan, selected, millis,
                hintUsedByPlayer, engine);
        lastAnswerCorrect = result.correct;

        int levelBefore = getPlayerLevel();

        if (lastAnswerCorrect) {
            enemy.takeDamage(1);
            xpGained = 5 * result.xpMultiplier;
            playerXP += xpGained;
        } else if (result.shieldTriggered) {
            feedbackNote = "Your shield absorbed the hit. No health lost.";
        } else {
            playerHealth = Math.max(0, playerHealth - 1);
        }

        if (getPlayerLevel() > levelBefore) {
            notifications.add("LEVEL UP! You reached level " + getPlayerLevel() + ".");
        }

        if (logger != null) {
            logger.answer(plan.question.getTopic(),
                    plan.question.getDifficulty(), lastAnswerCorrect, millis,
                    hintUsedByPlayer, plan.intervention.name(),
                    plan.state.name(), plan.predictedSuccess, mouse);
        }

        answered = true;
        showHint = false;

        if (playerHealth <= 0) {
            gameOver = true;

            if (logger != null) {
                logger.gameOver();
            }
        }
    }

    /** Moves on after the player has read the feedback. */
    private void continueBattle() {
        if (gameOver || !answered) {
            return;
        }

        if (enemy == null || !enemy.isAlive()) {
            playerXP += 20;
            enemiesDefeated++;

            if (logger != null) {
                logger.battleWon();
            }

            active = false;
            answered = false;
            lastAnswerCorrect = false;
            enemy = null;
            plan = null;
            result = null;
            showHint = false;
            battleFinished = true;
            notifications.add("Enemy defeated! +20 XP");
            return;
        }

        nextQuestion();
    }

    private void openHint() {
        if (answered || plan == null || showHint) {
            return;
        }

        showHint = true;
        hintUsedByPlayer = true;

        if (logger != null) {
            logger.hintUsed();
        }
    }

    // --------------------------------------------------
    // INPUT
    // --------------------------------------------------

    public void handleKey(KeyCode key) {
        if (key == null) {
            return;
        }

        if (gameOver) {
            if (key == KeyCode.R || key == KeyCode.ENTER) {
                restartRequested = true;
            }
            return;
        }

        if (!active) {
            return;
        }

        if (key == KeyCode.P) {
            notifications.add(useHealingPotion());
            return;
        }

        if (answered) {
            if (key == KeyCode.ENTER || key == KeyCode.SPACE) {
                continueBattle();
            }
            return;
        }

        if (key == KeyCode.H) {
            openHint();
            return;
        }

        int selected = -1;

        if (key == KeyCode.DIGIT1 || key == KeyCode.NUMPAD1) {
            selected = 0;
        } else if (key == KeyCode.DIGIT2 || key == KeyCode.NUMPAD2) {
            selected = 1;
        } else if (key == KeyCode.DIGIT3 || key == KeyCode.NUMPAD3) {
            selected = 2;
        } else if (key == KeyCode.DIGIT4 || key == KeyCode.NUMPAD4) {
            selected = 3;
        }

        if (selected >= 0) {
            answerQuestion(selected, false);
        }
    }

    /** Returns true when the pointer is over something clickable. */
    public boolean handleMouseMove(double mx, double my) {
        hoverIndex = -1;
        view.setHover(mx, my);

        if (gameOver) {
            return view.isOverGameOverButton(mx, my);
        }

        if (!active) {
            return false;
        }

        if (!answered) {
            int option = view.optionAt(mx, my);

            if (option >= 0 && !plan.isOptionRemoved(option)) {
                hoverIndex = option;
                return true;
            }
        }

        return view.isOverAction(mx, my, answered);
    }

    /** Returns true when the click was used by the battle screen. */
    public boolean handleMouseClick(double mx, double my) {
        if (gameOver) {
            if (view.isOverGameOverButton(mx, my)) {
                restartRequested = true;
            }

            return true;
        }

        if (!active) {
            return false;
        }

        if (!answered) {
            int option = view.optionAt(mx, my);

            if (option >= 0) {
                answerQuestion(option, true);
                return true;
            }

            if (view.isOverHint(mx, my)) {
                openHint();
                return true;
            }
        } else if (view.isOverContinue(mx, my)) {
            continueBattle();
            return true;
        }

        if (view.isOverPotion(mx, my)) {
            notifications.add(useHealingPotion());
            return true;
        }

        return true;
    }

    // --------------------------------------------------
    // WORLD INTERFACE
    // --------------------------------------------------

    public boolean consumeRestartRequest() {
        boolean value = restartRequested;
        restartRequested = false;
        return value;
    }

    public boolean consumeBattleFinished() {
        boolean value = battleFinished;
        battleFinished = false;
        return value;
    }

    public List<String> consumeNotifications() {
        List<String> copy = new ArrayList<>(notifications);
        notifications.clear();
        return copy;
    }

    public void restart() {
        playerHealth = MAX_HEALTH;
        playerXP = 0;
        enemiesDefeated = 0;

        active = false;
        answered = false;
        lastAnswerCorrect = false;
        gameOver = false;
        showHint = false;
        restartRequested = false;

        enemy = null;
        plan = null;
        result = null;
        // Adaptive learning history and inventory are intentionally retained.
    }

    public void loadProgress(PlayerProgress progress) {
        if (progress == null) {
            return;
        }

        playerXP = Math.max(0, progress.getXp());
        playerHealth = Math.max(0, Math.min(MAX_HEALTH, progress.getHealth()));
        enemiesDefeated = Math.max(0, progress.getEnemiesDefeated());

        inventory.setHealingPotions(Math.max(0, progress.getHealingPotions()));

        active = false;
        answered = false;
        lastAnswerCorrect = false;
        showHint = false;

        enemy = null;
        plan = null;
        result = null;
        gameOver = playerHealth <= 0;
    }

    public void updateProgress(PlayerProgress progress) {
        if (progress == null) {
            return;
        }

        progress.setXp(playerXP);
        progress.setLevel(getPlayerLevel());
        progress.setHealth(playerHealth);
        progress.setEnemiesDefeated(enemiesDefeated);
        progress.setHealingPotions(inventory.getHealingPotions());
    }

    public void render(GraphicsContext gc, double width, double height) {
        if (gc == null) {
            return;
        }

        if (gameOver) {
            view.renderGameOver(gc, width, height, this);
            return;
        }

        if (active && enemy != null && plan != null) {
            view.render(gc, width, height, this);
        }
    }

    // --------------------------------------------------
    // GETTERS FOR THE VIEW AND HUD
    // --------------------------------------------------

    public boolean isActive() { return active; }
    public boolean isGameOver() { return gameOver; }
    public boolean isAnswered() { return answered; }
    public boolean isLastAnswerCorrect() { return lastAnswerCorrect; }
    public boolean isHintShown() { return showHint; }
    public boolean isHintByCoach() { return plan != null && plan.hintOpenedByCoach(); }
    public int getSelectedIndex() { return selectedIndex; }
    public int getHoverIndex() { return hoverIndex; }
    public int getXpGained() { return xpGained; }
    public String getFeedbackNote() { return feedbackNote; }
    public Enemy getEnemy() { return enemy; }
    public QuestionPlan getPlan() { return plan; }
    public AnswerResult getResult() { return result; }
    public AdaptiveCoach getCoach() { return coach; }
    public AdaptiveEngine getEngine() { return engine; }
    public int getPlayerHealth() { return playerHealth; }
    public int getPlayerXP() { return playerXP; }
    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getHealingPotions() { return inventory.getHealingPotions(); }
    public boolean canUsePotion() { return inventory.getHealingPotions() > 0 && playerHealth < MAX_HEALTH; }

    public int getPlayerLevel() {
        return 1 + playerXP / XP_PER_LEVEL;
    }

    public int getXpIntoLevel() {
        return playerXP % XP_PER_LEVEL;
    }

    public void addXP(int amount) {
        if (amount > 0) {
            int before = getPlayerLevel();
            playerXP += amount;

            if (getPlayerLevel() > before) {
                notifications.add("LEVEL UP! You reached level " + getPlayerLevel() + ".");
            }
        }
    }

    public void addHealingPotion() {
        inventory.addHealingPotion();
    }

    public String useHealingPotion() {
        if (inventory.getHealingPotions() <= 0) {
            return "You don't have any healing potions.";
        }

        if (playerHealth >= MAX_HEALTH) {
            return "Your health is already full.";
        }

        boolean used = inventory.useHealingPotion();

        if (!used) {
            return "You don't have any healing potions.";
        }

        playerHealth = Math.min(MAX_HEALTH, playerHealth + 2);
        return "Potion used: health restored.";
    }
}
