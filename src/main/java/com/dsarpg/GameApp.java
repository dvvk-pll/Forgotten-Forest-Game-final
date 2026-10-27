
package com.dsarpg;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.dsarpg.ai.AdaptiveCoach;
import com.dsarpg.ai.AiProfileStore;
import com.dsarpg.scenery.SceneryTheme;
import com.dsarpg.usability.UsabilityLogger;
import com.dsarpg.usability.UserSettings;

/**
 * Application shell: login, level selection, settings, the game screen and
 * the pause menu.
 *
 * Usability notes
 *  - every screen supports keyboard and mouse, has visible focus and
 *    accessible names, and scales with the text-size setting
 *  - feedback such as "saved" is a short on-screen toast, never a dialog
 *    that interrupts play
 *  - the level screen explains why a level is locked and recommends the
 *    next one using the AI Coach's mastery estimates
 */
public class GameApp extends Application {

    private final AccountService accountService = new AccountService();
    private final UserSettings settings = UserSettings.get();

    private Stage stage;
    private PlayerProgress loggedInPlayer;
    private GameWorld world;
    private AnimationTimer gameTimer;
    private AdaptiveCoach coach;
    private UsabilityLogger logger;
    private Runnable currentScreen;
    private boolean ratedThisSession = false;

    private final Set<KeyCode> keys = new HashSet<>();

    private boolean paused = false;
    private boolean victoryDialogShown = false;
    private StackPane pauseOverlay;
    private int selectedLevel = 1;

    @Override
    public void start(Stage stage) {

        this.stage = stage;
        stage.setTitle("DSA RPG - The Forgotten Forest");

        showLoginScreen();
        stage.show();
    }

    // --------------------------------------------------
    // SCENE HELPERS
    // --------------------------------------------------

    private Scene newScene(Parent root, double width, double height) {
        root.setStyle("-fx-font-size: " + Math.round(14 * settings.getTextScale()) + "px;");

        if (settings.isHighContrast()) {
            root.getStyleClass().add("high-contrast");
        }

        Scene scene = new Scene(root, width, height);
        addStylesheet(scene.getStylesheets());
        return scene;
    }

    private void addStylesheet(List<String> sheets) {
        java.net.URL css = getClass().getResource("/styles/game.css");

        if (css != null) {
            sheets.add(css.toExternalForm());
        }
    }

    private void showScreen(Scene scene, boolean resizable, double minWidth) {
        stage.setScene(scene);
        stage.setResizable(resizable);
        stage.setMinWidth(minWidth);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private Button button(String text, String styleClass, String help) {
        Button b = new Button(text);

        if (styleClass != null) {
            b.getStyleClass().add(styleClass);
        }

        b.setAccessibleText(help == null ? text : help);

        if (help != null) {
            b.setTooltip(new Tooltip(help));
        }

        return b;
    }

    // --------------------------------------------------
    // LOGIN AND REGISTRATION SCREEN
    // --------------------------------------------------

    private void showLoginScreen() {

        currentScreen = this::showLoginScreen;

        Label title = new Label("THE FORGOTTEN FOREST");
        title.getStyleClass().add("title-dark");

        Label subtitle = new Label("Sign in or create an account to begin your adventure.");
        subtitle.getStyleClass().add("subtitle-dark");
        subtitle.setWrapText(true);

        TextField usernameField = new TextField(settings.getLastUsername());
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);
        usernameField.setAccessibleText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        passwordField.setAccessibleText("Password");

        TextField visiblePassword = new TextField();
        visiblePassword.setPromptText("Password");
        visiblePassword.setMaxWidth(300);
        visiblePassword.setAccessibleText("Password (visible)");
        visiblePassword.setVisible(false);
        visiblePassword.setManaged(false);

        CheckBox showPassword = new CheckBox("Show password");
        showPassword.setOnAction(event -> {
            if (showPassword.isSelected()) {
                visiblePassword.setText(passwordField.getText());
                visiblePassword.setVisible(true);
                visiblePassword.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                visiblePassword.requestFocus();
            } else {
                passwordField.setText(visiblePassword.getText());
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                visiblePassword.setVisible(false);
                visiblePassword.setManaged(false);
                passwordField.requestFocus();
            }
        });

        Label message = new Label();
        message.setWrapText(true);
        message.setMaxWidth(320);
        message.getStyleClass().add("error-text");
        message.setAccessibleText("Message");

        Button loginButton = button("Login", "button-primary",
                "Log in to your existing account");
        Button registerButton = button("Create Account", null,
                "Create a new account with this username and password");
        loginButton.setDefaultButton(true);

        Button settingsButton = button("Settings", "button-quiet",
                "Open accessibility and comfort settings");
        settingsButton.setStyle("-fx-text-fill: #263D2D; -fx-border-color: #405744;");
        settingsButton.setOnAction(event -> openSettings());

        java.util.function.Supplier<char[]> readPassword = () ->
                (showPassword.isSelected() ? visiblePassword.getText()
                        : passwordField.getText()).toCharArray();

        java.util.function.Supplier<String> validate = () -> {
            if (usernameField.getText().trim().isEmpty()) {
                usernameField.requestFocus();
                return "Please enter your username.";
            }

            if (readPassword.get().length == 0) {
                (showPassword.isSelected() ? visiblePassword : passwordField).requestFocus();
                return "Please enter your password.";
            }

            return null;
        };

        loginButton.setOnAction(event -> {

            String problem = validate.get();

            if (problem != null) {
                message.setText(problem);
                return;
            }

            char[] password = readPassword.get();

            try {

                PlayerProgress player = accountService.login(
                        usernameField.getText(), password);

                if (player == null) {
                    message.setText("That username and password do not match. "
                            + "Check them or choose Create Account.");
                    return;
                }

                startSession(player);

            } catch (SQLException | GeneralSecurityException ex) {

                showError("Login failed", ex.getMessage());

            } finally {

                Arrays.fill(password, '\0');
                passwordField.clear();
                visiblePassword.clear();
            }
        });

        registerButton.setOnAction(event -> {

            String problem = validate.get();

            if (problem != null) {
                message.setText(problem);
                return;
            }

            char[] password = readPassword.get();

            try {

                PlayerProgress player = accountService.register(
                        usernameField.getText(), password);

                startSession(player);

            } catch (IllegalArgumentException ex) {

                message.setText(ex.getMessage());

            } catch (SQLException ex) {

                message.setText("Account creation failed. "
                        + "That username may already exist.");

            } catch (GeneralSecurityException ex) {

                showError("Registration failed", ex.getMessage());

            } finally {

                Arrays.fill(password, '\0');
                passwordField.clear();
                visiblePassword.clear();
            }
        });

        Label tip = new Label("New here? Choose a username and password, then press Create Account.");
        tip.getStyleClass().add("hint-text");
        tip.setWrapText(true);
        tip.setMaxWidth(320);

        HBox buttons = new HBox(12, loginButton, registerButton);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(14, title, subtitle, usernameField, passwordField,
                visiblePassword, showPassword, buttons, message, tip, settingsButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30, 36, 26, 36));
        card.getStyleClass().add("panel-light");
        card.setMaxWidth(420);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("screen");
        root.setPadding(new Insets(24));

        showScreen(newScene(root, 620, 600), false, 0);

        Platform.runLater(() -> {
            if (usernameField.getText().isEmpty()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
        });
    }

    /** Starts the AI coach and usability tracking for a logged-in player. */
    private void startSession(PlayerProgress player) {

        loggedInPlayer = player;
        ratedThisSession = false;

        coach = AiProfileStore.load(player.getUsername());
        coach.setAiEnabled(settings.isAiCoach());
        logger = new UsabilityLogger(player.getUsername(), coach.isAiEnabled());

        settings.setLastUsername(player.getUsername());
        settings.save();

        showLevelSelectionScreen();
    }

    // --------------------------------------------------
    // SETTINGS
    // --------------------------------------------------

    private void openSettings() {

        if (logger != null) {
            logger.settingsOpened();
        }

        boolean wasPaused = paused;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Comfort and accessibility");

        ComboBox<UserSettings.TextSize> sizeBox = new ComboBox<>();
        sizeBox.getItems().addAll(UserSettings.TextSize.values());
        sizeBox.setValue(settings.getTextSize());
        sizeBox.setAccessibleText("Text size");

        CheckBox contrast = new CheckBox("High contrast (black panels, white text)");
        contrast.setSelected(settings.isHighContrast());

        CheckBox blind = new CheckBox("Colour-blind friendly colours (blue and orange)");
        blind.setSelected(settings.isColorblindSafe());

        CheckBox calm = new CheckBox("Reduce motion and effects (no particles or shimmer)");
        calm.setSelected(settings.isReduceMotion());

        CheckBox minimap = new CheckBox("Show the minimap");
        minimap.setSelected(settings.isShowMinimap());

        CheckBox ai = new CheckBox("AI Coach: adapt questions, help and rewards to me");
        ai.setSelected(settings.isAiCoach());

        Label aiNote = new Label("Turn this off to play with the original rule-based difficulty only.");
        aiNote.getStyleClass().add("hint-text");
        aiNote.setWrapText(true);
        aiNote.setMaxWidth(420);

        Button tutorial = button("Show the how-to-play guide next time", null, null);
        tutorial.setOnAction(event -> {
            settings.setTutorialSeen(false);
            tutorial.setText("Guide will show when you start a level");
            tutorial.setDisable(true);
        });

        HBox sizeRow = new HBox(10, new Label("Text size"), sizeBox);
        sizeRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, sizeRow, contrast, blind, calm, minimap,
                ai, aiNote, tutorial);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        addStylesheet(dialog.getDialogPane().getStylesheets());
        dialog.getDialogPane().setStyle("-fx-font-size: "
                + Math.round(14 * settings.getTextScale()) + "px;");

        if (stage != null) {
            dialog.initOwner(stage);
        }

        dialog.showAndWait();

        settings.setTextSize(sizeBox.getValue());
        settings.setHighContrast(contrast.isSelected());
        settings.setColorblindSafe(blind.isSelected());
        settings.setReduceMotion(calm.isSelected());
        settings.setShowMinimap(minimap.isSelected());
        settings.setAiCoach(ai.isSelected());
        settings.save();

        applySettingsToSession();

        // Rebuild menu screens so new sizes and colours apply immediately.
        if (world == null && currentScreen != null) {
            currentScreen.run();
        }

        if (world != null) {
            setPaused(wasPaused);
            world.toast("Settings saved");
        }
    }

    private void applySettingsToSession() {

        if (coach == null || loggedInPlayer == null) {
            return;
        }

        if (coach.isAiEnabled() != settings.isAiCoach()) {
            // Start a new log so each record has a single, clear mode.
            if (logger != null) {
                logger.flush();
            }

            coach.setAiEnabled(settings.isAiCoach());
            logger = new UsabilityLogger(loggedInPlayer.getUsername(), coach.isAiEnabled());

            if (world != null) {
                world.attach(coach, logger);
            }
        }
    }

    // --------------------------------------------------
    // LEVEL SELECTION SCREEN
    // --------------------------------------------------

    private boolean isCompleted(int level) {
        String completed = loggedInPlayer.getCompletedLevels();

        if (completed == null || completed.isBlank()) {
            return false;
        }

        for (String id : completed.split(",")) {
            if (id.trim().equals(String.valueOf(level))) {
                return true;
            }
        }

        return false;
    }

    private void showLevelSelectionScreen() {

        currentScreen = this::showLevelSelectionScreen;

        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }

        world = null;
        paused = false;

        Label title = new Label("CHOOSE YOUR ADVENTURE");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Welcome back, " + loggedInPlayer.getUsername()
                + ". AI Coach: " + (coach != null && coach.isAiEnabled() ? "ON" : "OFF")
                + ". Progress is saved to your account.");
        subtitle.getStyleClass().add("subtitle");
        subtitle.setWrapText(true);

        int recommended = 0;

        for (int level = 1; level <= 5; level++) {
            if (loggedInPlayer.getUnlockedLevel() >= level && !isCompleted(level)) {
                recommended = level;
                break;
            }
        }

        VBox levels = new VBox(12);
        levels.setAlignment(Pos.TOP_CENTER);
        levels.setFillWidth(true);

        for (int level = 1; level <= 5; level++) {
            final int chosen = level;
            levels.getChildren().add(createLevelCard(
                    level,
                    loggedInPlayer.getUnlockedLevel() >= level,
                    isCompleted(level),
                    level == recommended,
                    () -> startLevel(chosen)));
        }

        ScrollPane scroll = new ScrollPane(levels);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(430);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button settingsButton = button("Settings", "button-quiet",
                "Open accessibility and comfort settings");
        settingsButton.setOnAction(event -> openSettings());

        Button reportButton = button("Session report", "button-quiet",
                "See your usability metrics for this session");
        reportButton.setOnAction(event -> showSessionReport());

        Button logoutButton = button("Log out", "button-quiet", "Save and return to the login screen");
        logoutButton.setOnAction(event -> logout());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottom = new HBox(10, settingsButton, reportButton, spacer, logoutButton);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(14, title, subtitle, scroll, bottom);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(24));
        layout.getStyleClass().add("screen");

        showScreen(newScene(layout, 740, 700), true, 600);
    }

    private HBox createLevelCard(int level, boolean unlocked, boolean completed,
                                 boolean recommended, Runnable action) {

        SceneryTheme theme = SceneryTheme.forLevel(level);
        List<String> topics = AdaptiveEngine.topicsForLevel(level);

        Region strip = new Region();
        strip.setMinWidth(10);
        strip.setPrefWidth(10);
        strip.setStyle("-fx-background-color: " + theme.getAccent()
                + "; -fx-background-radius: 6;");

        Label name = new Label("LEVEL " + level + " - " + theme.getTitle());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);

        Label tagline = new Label(theme.getTagline());
        tagline.getStyleClass().add("card-text");
        tagline.setWrapText(true);

        Label topicLabel = new Label("Topics: " + String.join(", ", topics));
        topicLabel.getStyleClass().add("card-text");
        topicLabel.setWrapText(true);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        if (completed) {
            Label done = new Label("Completed");
            done.getStyleClass().add("badge-done");
            badges.getChildren().add(done);
        }

        if (recommended) {
            Label next = new Label("Recommended next");
            next.getStyleClass().add("badge-next");
            badges.getChildren().add(next);
        }

        if (!unlocked) {
            Label locked = new Label("Locked - defeat the "
                    + SceneryTheme.forLevel(level - 1).getBossName() + " in Level " + (level - 1));
            locked.getStyleClass().add("badge-locked");
            locked.setWrapText(true);
            badges.getChildren().add(locked);
        }

        VBox text = new VBox(4, name, tagline, topicLabel);

        if (coach != null && coach.isAiEnabled()) {
            double readiness = coach.readiness(topics);
            ProgressBar bar = new ProgressBar(readiness);
            bar.setPrefWidth(180);
            bar.setAccessibleText("Readiness " + Math.round(readiness * 100) + " percent");

            Label readinessLabel = new Label("Readiness " + Math.round(readiness * 100) + "%");
            readinessLabel.getStyleClass().add("card-text");

            HBox readinessRow = new HBox(8, bar, readinessLabel);
            readinessRow.setAlignment(Pos.CENTER_LEFT);
            text.getChildren().add(readinessRow);
        }

        if (!badges.getChildren().isEmpty()) {
            text.getChildren().add(badges);
        }

        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button play = new Button(!unlocked ? "LOCKED" : (completed ? "REPLAY" : "PLAY"));
        play.setMinWidth(96);
        play.setDisable(!unlocked);
        play.setAccessibleText((unlocked ? "Play level " : "Locked level ") + level + ", " + theme.getTitle());

        if (unlocked) {
            play.setOnAction(event -> action.run());
        }

        HBox row = new HBox(14, strip, text, play);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.getStyleClass().add("card");

        if (!unlocked) {
            row.getStyleClass().add("card-locked");
        }

        return row;
    }

    private void startLevel(int level) {
        if (loggedInPlayer == null) return;
        if (level > loggedInPlayer.getUnlockedLevel()) {
            showInformation("Level Locked", "Defeat the previous level's boss first.");
            return;
        }
        // The selected level decides which region is built.
        selectedLevel = level;
        loggedInPlayer.setCurrentLevel(level);
        try {
            accountService.saveProgress(loggedInPlayer);
            showGameScreen();
        } catch (SQLException ex) {
            showError("Could Not Start Level", "Your selected level could not be saved.\n" + ex.getMessage());
        }
    }

    // --------------------------------------------------
    // GAME SCREEN AND PAUSE MENU
    // --------------------------------------------------

    private void showGameScreen() {

        currentScreen = null;

        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }

        world = new GameWorld(selectedLevel, coach, logger);
        world.loadProgress(loggedInPlayer);
        // Do not show the victory popup again when loading an already completed region.
        victoryDialogShown = world.isBossDefeated();

        paused = false;
        keys.clear();

        Canvas canvas = new Canvas(GameWorld.VIEW_WIDTH, GameWorld.VIEW_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // ---- Toolbar: every keyboard shortcut also has a button
        Label accountLabel = new Label("Player: " + loggedInPlayer.getUsername());
        accountLabel.getStyleClass().add("toolbar-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button helpButton = toolbarButton("Help (H)", "How to play");
        helpButton.setOnAction(event -> {
            world.toggleHelp();
            focusGame();
        });

        Button insightsButton = toolbarButton("Insights (M)", "See what the AI Coach has learned about you");
        insightsButton.setOnAction(event -> {
            world.toggleInsights();
            focusGame();
        });

        Button saveButton = toolbarButton("Save (F5)", "Save your progress");
        saveButton.setOnAction(event -> {
            saveProgress();
            focusGame();
        });

        Button pauseButton = toolbarButton("Pause (Esc)", "Open the pause menu");
        pauseButton.setOnAction(event -> setPaused(true));

        HBox toolbar = new HBox(8, accountLabel, spacer, helpButton, insightsButton,
                saveButton, pauseButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 12));
        toolbar.getStyleClass().add("toolbar");

        canvas.setOnMouseMoved(event -> canvas.setCursor(
                world.handleMouseMove(event.getX(), event.getY())
                        ? Cursor.HAND : Cursor.DEFAULT));

        canvas.setOnMouseClicked(event -> {
            if (!paused) {
                world.handleMouseClick(event.getX(), event.getY());
            }

            focusGame();
        });

        VBox gameLayout = new VBox(0, toolbar, canvas);
        gameLayout.setAlignment(Pos.TOP_CENTER);
        gameLayout.setPadding(Insets.EMPTY);
        gameLayout.setStyle("-fx-background-color: #11231A;");

        // ---- Pause menu
        Label pauseTitle = new Label("GAME PAUSED");
        pauseTitle.getStyleClass().add("pause-title");

        Label pauseSubtitle = new Label("Take a moment. Nothing happens while the game is paused.");
        pauseSubtitle.setWrapText(true);
        pauseSubtitle.setMaxWidth(300);
        pauseSubtitle.getStyleClass().add("subtitle");
        pauseSubtitle.setAlignment(Pos.CENTER);

        Button resumeButton = button("Resume (Esc)", null, "Return to the game");
        Button saveGameButton = button("Save game", null, "Save your progress now");
        Button howToButton = button("How to play", null, "Open the how-to-play guide");
        Button insightsMenuButton = button("Learning insights", null, "See your topic mastery and AI Coach notes");
        Button settingsMenuButton = button("Settings", null, "Text size, contrast, colours and AI Coach");
        Button reportButton = button("Session report", null, "See usability metrics for this session");
        Button levelsButton = button("Save and choose another level", null, "Save and go back to the level list");
        Button logoutButton = button("Save and log out", "button-primary", "Save and return to the login screen");

        for (Button b : new Button[] {resumeButton, saveGameButton, howToButton,
                insightsMenuButton, settingsMenuButton, reportButton, levelsButton, logoutButton}) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        resumeButton.setOnAction(event -> setPaused(false));
        saveGameButton.setOnAction(event -> saveProgress());
        howToButton.setOnAction(event -> {
            setPaused(false);
            world.toggleHelp();
        });
        insightsMenuButton.setOnAction(event -> {
            setPaused(false);
            world.toggleInsights();
        });
        settingsMenuButton.setOnAction(event -> openSettings());
        reportButton.setOnAction(event -> showSessionReport());
        levelsButton.setOnAction(event -> saveAndChooseLevel());
        logoutButton.setOnAction(event -> saveAndLogout());

        VBox pauseMenu = new VBox(10, pauseTitle, pauseSubtitle, resumeButton,
                saveGameButton, howToButton, insightsMenuButton, settingsMenuButton,
                reportButton, levelsButton, logoutButton);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setPadding(new Insets(24));
        pauseMenu.setMaxWidth(360);
        pauseMenu.setPrefWidth(360);
        pauseMenu.getStyleClass().add("pause-box");

        pauseOverlay = new StackPane(pauseMenu);
        pauseOverlay.setAlignment(Pos.CENTER);
        pauseOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.78);");
        pauseOverlay.setVisible(false);

        StackPane root = new StackPane(gameLayout, pauseOverlay);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #11231A; -fx-font-size: "
                + Math.round(14 * settings.getTextScale()) + "px;");

        if (settings.isHighContrast()) {
            root.getStyleClass().add("high-contrast");
        }

        Scene scene = new Scene(root);
        addStylesheet(scene.getStylesheets());

        // ---- Keyboard
        scene.setOnKeyPressed(event -> {

            KeyCode key = event.getCode();

            if (key == KeyCode.ESCAPE) {
                // Escape first closes an open overlay, then toggles pause.
                if (!paused && world.closeOverlay()) {
                    event.consume();
                    return;
                }

                setPaused(!paused);
                keys.clear();
                event.consume();
                return;
            }

            if (paused) {
                return;
            }

            if (key == KeyCode.F5) {
                saveProgress();
                event.consume();
                return;
            }

            if (keys.add(key)) {
                world.handleKeyPressed(key);
            }
        });

        scene.setOnKeyReleased(event -> keys.remove(event.getCode()));

        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMinWidth(0);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.setTitle("DSA RPG - " + SceneryTheme.forLevel(selectedLevel).getTitle());

        // ---- Game loop
        gameTimer = new AnimationTimer() {

            private long lastTime = 0;

            @Override
            public void handle(long now) {

                if (lastTime == 0) {
                    lastTime = now;
                    world.render(gc);
                    return;
                }

                double deltaTime = Math.min((now - lastTime) / 1_000_000_000.0, 0.05);
                lastTime = now;

                if (!paused) {
                    world.update(keys, deltaTime);
                    checkForLevelVictory();

                    if (world.consumeAutosaveRequest()) {
                        autosave();
                    }
                }

                world.render(gc);
            }
        };

        gameTimer.start();

        root.requestFocus();

        if (!settings.isTutorialSeen()) {
            world.showHelpFirstTime();
        }
    }

    private Button toolbarButton(String text, String help) {
        Button b = button(text, null, help);
        b.setFocusTraversable(false);
        return b;
    }

    private void focusGame() {
        if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null) {
            stage.getScene().getRoot().requestFocus();
        }
    }

    private void checkForLevelVictory() {
        if (world == null || !world.isBossDefeated() || victoryDialogShown) {
            return;
        }

        victoryDialogShown = true;

        int completedLevel = world.getCurrentLevel();

        // Save the completed level and unlocked level.
        world.updateProgress(loggedInPlayer);

        try {
            accountService.saveProgress(loggedInPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AiProfileStore.save(coach);
        paused = true;
        keys.clear();

        Platform.runLater(() -> {
            SceneryTheme theme = SceneryTheme.forLevel(completedLevel);
            boolean last = completedLevel >= 5;

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Level Completed!");
            alert.setHeaderText("Victory!");
            alert.setContentText(
                    "You defeated the " + theme.getBossName() + "!\n"
                    + theme.getTitle() + " is complete.\n\n"
                    + (last ? "You have saved the realm. Congratulations!"
                            : "You can now continue to Level " + (completedLevel + 1) + ".")
                    + "\n\nThis session: " + logger.getCorrectAnswers() + " of "
                    + logger.getAnswers() + " answers correct.");
            addStylesheet(alert.getDialogPane().getStylesheets());

            ButtonType continueButton = new ButtonType(
                    last ? "Back to levels" : "Continue to Level " + (completedLevel + 1));
            ButtonType levelsButton = new ButtonType("Back to levels");

            if (last) {
                alert.getButtonTypes().setAll(continueButton);
            } else {
                alert.getButtonTypes().setAll(continueButton, levelsButton);
            }

            alert.showAndWait(); // Safe here: this runs after the animation frame.

            ButtonType result = alert.getResult();
            askSatisfaction("this level");

            if (!last && result == continueButton) {
                selectedLevel = completedLevel + 1;
                loggedInPlayer.setCurrentLevel(selectedLevel);

                try {
                    accountService.saveProgress(loggedInPlayer);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                showGameScreen();
            } else {
                showLevelSelectionScreen();
            }
        });
    }

    // --------------------------------------------------
    // SATISFACTION RATING AND SESSION REPORT
    // --------------------------------------------------

    /** One quick question: how easy was the game to use (1-5)? */
    private void askSatisfaction(String subject) {

        if (logger == null || ratedThisSession || logger.getAnswers() < 3) {
            return;
        }

        List<String> choices = Arrays.asList(
                "5 - Very easy and pleasant",
                "4 - Easy",
                "3 - Okay",
                "2 - Difficult",
                "1 - Very difficult or frustrating",
                "Skip");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Quick feedback");
        dialog.setHeaderText("How easy was it to use the game " + subject + "?");
        dialog.setContentText("Your rating:");
        addStylesheet(dialog.getDialogPane().getStylesheets());

        Optional<String> answer = dialog.showAndWait();

        if (answer.isPresent() && !answer.get().equals("Skip")) {
            logger.setSatisfaction(Integer.parseInt(answer.get().substring(0, 1)));
            ratedThisSession = true;
        }
    }

    private void showSessionReport() {

        if (logger == null) {
            showInformation("Session report", "No session is running yet.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session report");
        alert.setHeaderText("Your usability metrics for this session");
        alert.setContentText(logger.report()
                + "\n\nA copy is saved to ~/.dsarpg/usability when you log out.");
        addStylesheet(alert.getDialogPane().getStylesheets());
        alert.getDialogPane().setMinWidth(460);
        alert.showAndWait();
    }

    // --------------------------------------------------
    // PAUSE / RESUME
    // --------------------------------------------------

    private void setPaused(boolean shouldPause) {

        paused = shouldPause;
        keys.clear();

        if (pauseOverlay != null) {
            pauseOverlay.setVisible(paused);
        }

        if (!paused) {
            focusGame();
        }
    }

    // --------------------------------------------------
    // SAVE GAME
    // --------------------------------------------------

    /** Quiet save after each battle: a short toast, never a dialog. */
    private void autosave() {

        if (loggedInPlayer == null || world == null) {
            return;
        }

        world.updateProgress(loggedInPlayer);

        try {
            accountService.saveProgress(loggedInPlayer);
            AiProfileStore.save(coach);
            world.toast("Progress saved automatically");
        } catch (SQLException ex) {
            world.toast("Autosave failed. Press F5 to try again.");
        }
    }

    private void saveProgress() {

        if (loggedInPlayer == null || world == null) {
            return;
        }

        world.updateProgress(loggedInPlayer);

        try {

            accountService.saveProgress(loggedInPlayer);
            AiProfileStore.save(coach);

            // A toast keeps the player in the game; no dialog to dismiss.
            world.toast("Progress saved");

        } catch (SQLException ex) {

            showError(
                    "Save Failed",
                    "Your progress could not be saved.\n" +
                    ex.getMessage());
        }
    }

    private void saveAndChooseLevel() {

        if (loggedInPlayer == null || world == null) {
            return;
        }

        world.updateProgress(loggedInPlayer);

        try {
            accountService.saveProgress(loggedInPlayer);
            AiProfileStore.save(coach);
            showLevelSelectionScreen();
        } catch (SQLException ex) {
            showError("Could not save",
                    "Progress could not be saved, so you are still in the game.\n" + ex.getMessage());
        }
    }

    // --------------------------------------------------
    // SAVE AND LOG OUT
    // --------------------------------------------------

    private void logout() {

        if (logger != null) {
            askSatisfaction("in this session");
            logger.flush();
        }

        AiProfileStore.save(coach);

        loggedInPlayer = null;
        coach = null;
        logger = null;
        showLoginScreen();
    }

    private void saveAndLogout() {

        if (loggedInPlayer == null || world == null) {
            showLoginScreen();
            return;
        }

        world.updateProgress(loggedInPlayer);

        try {

            accountService.saveProgress(loggedInPlayer);

            if (gameTimer != null) {
                gameTimer.stop();
                gameTimer = null;
            }

            keys.clear();

            world = null;
            pauseOverlay = null;
            paused = false;

            logout();

        } catch (SQLException ex) {

            showError(
                    "Logout Cancelled",
                    "Progress could not be saved. " +
                    "You are still logged in.\n" +
                    ex.getMessage());
        }
    }

    // --------------------------------------------------
    // ALERTS
    // --------------------------------------------------

    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(title);

        alert.setContentText(
                message == null
                        ? "An unknown error occurred."
                        : message);

        alert.showAndWait();
    }

    private void showInformation(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // --------------------------------------------------
    // APPLICATION SHUTDOWN
    // --------------------------------------------------

    @Override
    public void stop() {

        if (gameTimer != null) {
            gameTimer.stop();
        }

        if (loggedInPlayer != null && world != null) {

            world.updateProgress(loggedInPlayer);

            try {

                accountService.saveProgress(loggedInPlayer);

            } catch (SQLException ex) {

                System.err.println(
                        "Could not save progress during shutdown: " +
                        ex.getMessage());
            }
        }

        if (coach != null) {
            AiProfileStore.save(coach);
        }

        if (logger != null) {
            logger.flush();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
