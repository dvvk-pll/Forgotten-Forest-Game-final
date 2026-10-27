
package com.dsarpg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class TreasureChest {

    private final double x;
    private final double y;
    private final int rewardXP;

    private boolean opened = false;

    public TreasureChest(double x, double y, int rewardXP) {
        this.x = x;
        this.y = y;
        this.rewardXP = rewardXP;
    }

    public boolean isNear(Player player) {
        double playerX = player.getX() + player.getWidth() / 2.0;
        double playerY = player.getY() + player.getHeight() / 2.0;

        double chestX = x + 12;
        double chestY = y + 12;

        double dx = playerX - chestX;
        double dy = playerY - chestY;

        return Math.sqrt(dx * dx + dy * dy) < 48;
    }

    public boolean open() {
        if (opened) {
            return false;
        }

        opened = true;
        return true;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getRewardXP() {
        return rewardXP;
    }

    public boolean isOpened() {
        return opened;
    }
    
    public void setOpened(boolean opened) {
        this.opened = opened;
    }
    
    public void render(GraphicsContext gc) {
        // Shadow beneath the chest.
        gc.setFill(Color.color(0, 0, 0, 0.25));
        gc.fillOval(x + 1, y + 19, 28, 8);

        // Chest body.
        gc.setFill(opened
                ? Color.web("#68452D")
                : Color.web("#9A5A2D"));
        gc.fillRoundRect(x, y + 7, 26, 18, 3, 3);

        // Top of the chest.
        gc.setFill(opened
                ? Color.web("#76583C")
                : Color.web("#C18A42"));
        gc.fillRoundRect(x, y + 3, 26, 10, 3, 3);

        // Gold latch.
        gc.setFill(Color.GOLD);
        gc.fillRect(x + 11, y + 10, 4, 6);

        // Opening indicator.
        if (!opened) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + 10, y - 5, 6, 6);
        }
    }
}
