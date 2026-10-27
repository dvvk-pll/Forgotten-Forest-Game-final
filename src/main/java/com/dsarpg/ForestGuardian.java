
package com.dsarpg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ForestGuardian {

    private final double x;
    private final double y;

    private final String name;
    private final javafx.scene.paint.Color robe;

    public ForestGuardian(double x, double y) {
        this(x, y, "Guardian", Color.DARKBLUE);
    }

    public ForestGuardian(double x, double y, String name, Color robe) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.robe = robe;
    }


public boolean isNear(Player player) {
    double playerCenterX = player.getX() + player.getWidth() / 2;
    double playerCenterY = player.getY() + player.getHeight() / 2;

    double guardianCenterX = x + 16;
    double guardianCenterY = y + 16;

    double dx = playerCenterX - guardianCenterX;
    double dy = playerCenterY - guardianCenterY;

    return Math.sqrt(dx * dx + dy * dy) < 80;
}


    public void render(GraphicsContext gc) {
        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillOval(x + 2, y + 24, 24, 8);

        // Robe
        gc.setFill(robe);
        gc.fillRoundRect(x + 3, y + 12, 20, 17, 5, 5);

        // Face
        gc.setFill(Color.PEACHPUFF);
        gc.fillOval(x + 5, y + 2, 16, 16);

        // Hair
        gc.setFill(Color.SADDLEBROWN);
        gc.fillArc(x + 4, y, 18, 12, 0, 180,
                javafx.scene.shape.ArcType.ROUND);

        // Staff
        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(3);
        gc.strokeLine(x + 24, y + 10, x + 27, y + 29);

        gc.setFill(Color.GOLD);
        gc.fillOval(x + 21, y + 5, 8, 8);

        // Name label (sized to fit the name)
        gc.setFont(com.dsarpg.ui.UiKit.font(11, true));
        double labelWidth = com.dsarpg.ui.UiKit.width(name, 11, true) + 12;

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(x + 14 - labelWidth / 2.0, y - 18, labelWidth, 16, 6, 6);

        gc.setFill(Color.BLACK);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(name, x + 14, y - 6);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
    }

    public double getX() { return x; }

    public double getY() { return y; }
}
