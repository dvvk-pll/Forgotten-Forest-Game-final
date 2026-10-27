
package com.dsarpg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import com.dsarpg.scenery.SceneryTheme;
import com.dsarpg.ui.Palette;
import com.dsarpg.ui.UiKit;

/**
 * A regular enemy (patrols horizontally) or a stationary boss.
 * Each region has its own enemy designs and boss, chosen by name and theme.
 */
public class Enemy {

    private final String name;
    private double x;
    private final double y;

    private final double width = 36;
    private final double height = 36;

    private int health;
    private final int maxHealth;

    private double direction = 1;
    private final double movementSpeed = 35;

    private final double patrolMin;
    private final double patrolMax;
    private final boolean boss;
    private final SceneryTheme theme;
    private double animation;

    public Enemy(String name, double x, double y, int health) {
        this(name, x, y, health, 45, 1920 - 45,
                name.toLowerCase().contains("treant"), SceneryTheme.FOREST);
    }

    public Enemy(String name, double x, double y, int health,
                 double patrolMin, double patrolMax,
                 boolean boss, SceneryTheme theme) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.health = Math.max(1, health);
        this.maxHealth = this.health;
        this.patrolMin = patrolMin;
        this.patrolMax = patrolMax;
        this.boss = boss;
        this.theme = theme;
    }

    public void update(double deltaTime, double minX, double maxX) {
        animation += deltaTime;

        if (!isAlive() || isBoss()) {
            return;
        }

        double lo = Math.max(minX, patrolMin);
        double hi = Math.min(maxX, patrolMax);

        x += direction * movementSpeed * deltaTime;

        if (x <= lo) {
            x = lo;
            direction = 1;
        }

        if (x + width >= hi) {
            x = hi - width;
            direction = -1;
        }
    }

    public void takeDamage(int damage) {
        if (damage > 0 && isAlive()) {
            health = Math.max(0, health - damage);
        }
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isBoss() {
        return boss;
    }

    // --------------------------------------------------
    // RENDERING
    // --------------------------------------------------

    public void render(GraphicsContext gc) {
        if (!isAlive()) {
            return;
        }

        if (isBoss()) {
            renderBoss(gc);
            return;
        }

        double bob = Math.sin(animation * 4 + x * 0.05) * 1.5;
        double by = y + bob;

        gc.setFill(Color.color(0, 0, 0, 0.25));
        gc.fillOval(x + 2, y + height - 2, width - 2, 7);

        String n = name.toLowerCase();

        if (n.contains("slime")) {
            drawSlime(gc, by, n.contains("ancient") ? "#9A9A8C"
                    : n.contains("glow") ? "#3DE0C8" : "#8E6FD8");
        } else if (n.contains("goblin")) {
            drawBlob(gc, by, "#3E9B45");
            gc.setFill(Color.web("#3E9B45"));
            gc.fillPolygon(new double[] {x + 1, x - 7, x + 6},
                    new double[] {by + 12, by + 5, by + 6}, 3);
            gc.fillPolygon(new double[] {x + 35, x + 43, x + 30},
                    new double[] {by + 12, by + 5, by + 6}, 3);
            drawEyes(gc, by, "#FFFFFF");
        } else if (n.contains("wolf")) {
            drawBlob(gc, by + 4, "#8A98A8");
            gc.setFill(Color.web("#8A98A8"));
            gc.fillPolygon(new double[] {x + 4, x + 8, x + 12},
                    new double[] {by + 8, by - 4, by + 8}, 3);
            gc.fillPolygon(new double[] {x + 24, x + 28, x + 32},
                    new double[] {by + 8, by - 4, by + 8}, 3);
            gc.setFill(Color.web("#E8EEF4"));
            gc.fillOval(x + 10, by + 20, 16, 11);
            gc.setFill(Color.BLACK);
            gc.fillOval(x + 16, by + 22, 5, 4);
            drawEyes(gc, by + 4, "#FFE08A");
        } else if (n.contains("bat")) {
            double flap = Math.sin(animation * 14) * 6;
            gc.setFill(Color.web("#4B2F7A"));
            gc.fillPolygon(new double[] {x + 4, x - 10, x + 2},
                    new double[] {by + 14, by + 6 - flap, by + 26}, 3);
            gc.fillPolygon(new double[] {x + 32, x + 46, x + 34},
                    new double[] {by + 14, by + 6 - flap, by + 26}, 3);
            gc.fillOval(x + 6, by + 6, 24, 24);
            drawEyes(gc, by + 2, "#7DFFE8");
        } else if (n.contains("golem")) {
            drawBlock(gc, by, n.contains("ice") ? "#9CD0EE" : "#8A7E68");
        } else if (n.contains("sentinel")) {
            drawBlock(gc, by, "#B8AC92");
        } else if (n.contains("troll")) {
            drawBlob(gc, by, "#6B5B45");
            gc.setFill(Color.web("#6B5B45"));
            gc.fillOval(x - 4, by + 10, 12, 18);
            gc.fillOval(x + 28, by + 10, 12, 18);
            drawEyes(gc, by, "#FFD76A");
        } else if (n.contains("wraith")) {
            gc.setFill(Color.web("#2B1F3D"));
            gc.fillOval(x + 2, by, 32, 28);
            gc.fillPolygon(new double[] {x + 2, x + 10, x + 18, x + 26, x + 34, x + 34, x + 2},
                    new double[] {by + 34, by + 28, by + 36, by + 28, by + 34, by + 14, by + 14}, 7);
            drawEyes(gc, by - 1, "#FF4D6D");
        } else if (n.contains("imp")) {
            drawBlob(gc, by, "#D6452B");
            gc.setFill(Color.web("#FFD04A"));
            gc.fillPolygon(new double[] {x + 7, x + 4, x + 12},
                    new double[] {by + 4, by - 8, by + 5}, 3);
            gc.fillPolygon(new double[] {x + 29, x + 32, x + 24},
                    new double[] {by + 4, by - 8, by + 5}, 3);
            drawEyes(gc, by, "#FFF2B0");
        } else if (n.contains("knight") || n.contains("guard")) {
            drawBlob(gc, by, theme == SceneryTheme.FROZEN_PEAKS ? "#7FA6C4"
                    : theme == SceneryTheme.CITADEL ? "#5A5560" : "#8B2A2A");
            gc.setFill(Color.web("#C9CED6"));
            gc.fillRoundRect(x + 4, by - 3, 28, 14, 8, 8);
            gc.setFill(Color.web("#1D2128"));
            gc.fillRect(x + 8, by + 3, 20, 3);
            gc.setFill(Color.web("#E0443E"));
            gc.fillRect(x + 16, by - 9, 4, 8);
        } else {
            drawBlob(gc, by, "#8B1A1A");
            drawEyes(gc, by, "#FFFFFF");
        }

        drawHealthBar(gc, x - 2, y - 9, 40, 5);

        gc.setFont(UiKit.font(11, true));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.color(0, 0, 0, 0.7));
        gc.fillText(name, x + width / 2.0 + 1, y - 13);
        gc.setFill(Color.WHITE);
        gc.fillText(name, x + width / 2.0, y - 14);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawBlob(GraphicsContext gc, double by, String color) {
        gc.setFill(Color.web(color));
        gc.fillOval(x, by, width, height);
        gc.setFill(Color.color(1, 1, 1, 0.18));
        gc.fillOval(x + 6, by + 3, 14, 8);
    }

    private void drawSlime(GraphicsContext gc, double by, String color) {
        double squash = Math.sin(animation * 5 + x * 0.04) * 2;
        gc.setFill(Color.web(color));
        gc.fillOval(x - squash, by + 6 + squash, width + squash * 2, height - 6 - squash);
        gc.setFill(Color.color(1, 1, 1, 0.35));
        gc.fillOval(x + 7, by + 11, 9, 6);
        drawEyes(gc, by + 6, "#FFFFFF");
    }

    private void drawBlock(GraphicsContext gc, double by, String color) {
        gc.setFill(Color.web(color));
        gc.fillRoundRect(x, by + 2, width, height - 2, 8, 8);
        gc.setFill(Color.color(0, 0, 0, 0.18));
        gc.fillRect(x, by + 22, width, 4);
        gc.setFill(Color.color(1, 1, 1, 0.22));
        gc.fillRect(x + 4, by + 5, 12, 4);
        drawEyes(gc, by + 2, "#FFB347");
    }

    private void drawEyes(GraphicsContext gc, double by, String color) {
        gc.setFill(Color.web(color));
        gc.fillOval(x + 5, by + 7, 7, 7);
        gc.fillOval(x + 22, by + 7, 7, 7);
        gc.setFill(Color.BLACK);
        gc.fillOval(x + 8, by + 9, 3, 4);
        gc.fillOval(x + 25, by + 9, 3, 4);
    }

    private void renderBoss(GraphicsContext gc) {
        double bw = 48;
        double bh = 58;
        double pulse = 0.5 + 0.5 * Math.sin(animation * 3);

        gc.setFill(Color.color(0, 0, 0, 0.35));
        gc.fillOval(x - 4, y + bh - 3, bw + 8, 10);

        switch (theme) {
            case RUINS:
                gc.setFill(Color.web("#8A7E68"));
                gc.fillRoundRect(x - 6, y + 18, 16, 30, 6, 6);
                gc.fillRoundRect(x + 38, y + 18, 16, 30, 6, 6);
                gc.setFill(Color.web("#B8AC92"));
                gc.fillRoundRect(x + 4, y + 8, 40, 48, 8, 8);
                gc.fillRoundRect(x + 10, y - 6, 28, 22, 6, 6);
                gc.setFill(Color.color(0.2, 0.9, 0.85, 0.5 + 0.5 * pulse));
                gc.fillRect(x + 14, y + 2, 7, 5);
                gc.fillRect(x + 28, y + 2, 7, 5);
                gc.fillRect(x + 18, y + 26, 12, 4);
                break;

            case FROZEN_PEAKS:
                gc.setFill(Color.web("#8FC3E6"));
                gc.fillPolygon(new double[] {x + 4, x - 4, x + 10},
                        new double[] {y + 4, y - 14, y + 10}, 3);
                gc.fillPolygon(new double[] {x + 44, x + 52, x + 38},
                        new double[] {y + 4, y - 14, y + 10}, 3);
                gc.setFill(Color.web("#CFE8F7"));
                gc.fillRoundRect(x + 2, y + 4, 44, 52, 14, 14);
                gc.setFill(Color.web("#6F9CC0"));
                gc.fillRoundRect(x + 8, y + 30, 32, 22, 8, 8);
                gc.setFill(Color.color(0.2, 0.6, 1.0, 0.6 + 0.4 * pulse));
                gc.fillOval(x + 12, y + 12, 8, 8);
                gc.fillOval(x + 28, y + 12, 8, 8);
                break;

            case CRYSTAL_CAVERNS:
                gc.setFill(Color.color(0.6, 0.4, 1.0, 0.25 + 0.2 * pulse));
                gc.fillOval(x - 14, y - 10, bw + 28, bh + 20);
                gc.setFill(Color.web("#7B6CFF"));
                gc.fillPolygon(new double[] {x + 24, x + 2, x + 10, x + 38, x + 46},
                        new double[] {y - 14, y + 14, y + 56, y + 56, y + 14}, 5);
                gc.setFill(Color.web("#B8B0FF"));
                gc.fillPolygon(new double[] {x + 24, x + 2, x + 18},
                        new double[] {y - 14, y + 14, y + 22}, 3);
                gc.setFill(Color.web("#35D3C4"));
                gc.fillPolygon(new double[] {x - 8, x - 12, x + 4},
                        new double[] {y + 24, y + 52, y + 52}, 3);
                gc.fillPolygon(new double[] {x + 56, x + 60, x + 44},
                        new double[] {y + 24, y + 52, y + 52}, 3);
                gc.setFill(Color.web("#FFF27A"));
                gc.fillOval(x + 14, y + 20, 7, 7);
                gc.fillOval(x + 28, y + 20, 7, 7);
                break;

            case CITADEL:
                gc.setFill(Color.color(0.9, 0.2, 0.1, 0.20 + 0.25 * pulse));
                gc.fillOval(x - 14, y - 12, bw + 28, bh + 24);
                gc.setFill(Color.web("#2B2630"));
                gc.fillRoundRect(x - 2, y + 6, bw + 4, bh - 4, 12, 12);
                gc.setFill(Color.web("#463E4E"));
                gc.fillRoundRect(x + 6, y + 12, bw - 12, 22, 8, 8);
                gc.setFill(Color.web("#E8B84A"));
                gc.fillPolygon(new double[] {x + 6, x + 12, x + 18, x + 24, x + 30, x + 36, x + 42, x + 42, x + 6},
                        new double[] {y + 8, y - 8, y + 4, y - 10, y + 4, y - 8, y + 8, y + 10, y + 10}, 9);
                gc.setFill(Color.color(1.0, 0.25 + 0.2 * pulse, 0.15, 1));
                gc.fillOval(x + 12, y + 18, 8, 8);
                gc.fillOval(x + 28, y + 18, 8, 8);
                break;

            case FOREST:
            default:
                gc.setStroke(Color.SADDLEBROWN);
                gc.setLineWidth(6);
                gc.strokeLine(x + 12, y + 30, x + 3, y + 42);
                gc.strokeLine(x + 35, y + 30, x + 46, y + 40);
                gc.strokeLine(x + 15, y + 8, x + 5, y - 4);
                gc.strokeLine(x + 33, y + 8, x + 44, y - 5);
                gc.setFill(Color.SADDLEBROWN);
                gc.fillRoundRect(x + 8, y + 5, 32, 48, 10, 10);
                gc.setFill(Color.DARKOLIVEGREEN);
                gc.fillOval(x + 3, y + 1, 42, 30);
                gc.setFill(Color.FORESTGREEN);
                gc.fillOval(x + 8, y - 3, 16, 18);
                gc.fillOval(x + 25, y - 5, 18, 20);
                gc.setFill(Color.ORANGERED);
                gc.fillOval(x + 12, y + 11, 7, 7);
                gc.fillOval(x + 29, y + 11, 7, 7);
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(3);
                gc.strokeLine(x + 15, y + 27, x + 33, y + 27);
                break;
        }

        // Boss health bar and label (readable on every theme).
        double barWidth = 64;
        double barX = x - 8;
        double barY = y - 24;
        Palette p = Palette.get();

        gc.setFill(Color.color(0, 0, 0, 0.75));
        gc.fillRoundRect(barX - 3, barY - 17, barWidth + 6, 29, 8, 8);
        UiKit.bar(gc, barX, barY, barWidth, 7, (double) health / maxHealth,
                Color.web("#E0443E"), Color.web("#4A1F1F"), null);

        gc.setFont(UiKit.font(11, true));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(p.accent);
        gc.fillText("BOSS  " + health + "/" + maxHealth, barX + barWidth / 2.0, barY - 4);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawHealthBar(GraphicsContext gc, double barX, double barY,
                               double barWidth, double barHeight) {
        gc.setFill(Color.web("#4A1F1F"));
        gc.fillRect(barX, barY, barWidth, barHeight);
        gc.setFill(Color.web("#E0443E"));
        gc.fillRect(barX, barY, barWidth * health / maxHealth, barHeight);
        gc.setStroke(Color.color(0, 0, 0, 0.8));
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }
}
