
package com.dsarpg;

import java.io.InputStream;
import java.util.Set;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;

public class Player {

    private double x;
    private double y;

    private final double width = 22;
    private final double height = 22;
    private final double speed = 150;

    // Each sprite sheet contains four 32x32 frames in one row.
    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 32;
    private static final int FRAME_COUNT = 4;

    private Image downSprite;
    private Image upSprite;
    private Image leftSprite;
    private Image rightSprite;

    private Image currentSprite;

    private int animationFrame = 0;
    private double animationTimer = 0;
    private static final double FRAME_DURATION = 0.15;

    private boolean moving = false;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;

        downSprite = loadSprite("/images/player_down.png");
        upSprite = loadSprite("/images/player_up.png");
        leftSprite = loadSprite("/images/player_left.png");
        rightSprite = loadSprite("/images/player_right.png");

        currentSprite = downSprite;
    }

    private Image loadSprite(String path) {
        try (InputStream stream =
                     getClass().getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                    "Sprite not found: " + path
                    + ". Check src/main/resources/images/"
                );
            }

            Image image = new Image(stream);

            if (image.isError()) {
                throw new IllegalStateException(
                    "Could not load sprite: " + path,
                    image.getException()
                );
            }

            if (image.getWidth() < FRAME_WIDTH * FRAME_COUNT
                    || image.getHeight() < FRAME_HEIGHT) {
                throw new IllegalStateException(
                    "Sprite must be at least 128x32: " + path
                );
            }

            return image;

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load player sprite " + path, e
            );
        }
    }

    public void update(Set<KeyCode> keys,
                       GameWorld world,
                       double deltaTime) {

        double dx = 0;
        double dy = 0;

        moving = false;

        // Choose the facing direction based on the latest input.
        if (keys.contains(KeyCode.UP)
                || keys.contains(KeyCode.W)) {
            dy -= 1;
            currentSprite = upSprite;
            moving = true;
        }

        if (keys.contains(KeyCode.DOWN)
                || keys.contains(KeyCode.S)) {
            dy += 1;
            currentSprite = downSprite;
            moving = true;
        }

        if (keys.contains(KeyCode.LEFT)
                || keys.contains(KeyCode.A)) {
            dx -= 1;
            currentSprite = leftSprite;
            moving = true;
        }

        if (keys.contains(KeyCode.RIGHT)
                || keys.contains(KeyCode.D)) {
            dx += 1;
            currentSprite = rightSprite;
            moving = true;
        }

        // Normalize diagonal movement so it is not faster.
        if (dx != 0 && dy != 0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        double nextX = x + dx * speed * deltaTime;
        double nextY = y + dy * speed * deltaTime;

        // Check horizontal and vertical movement separately.
        // This lets the player slide along walls.
        if (!world.isBlocked(nextX, y, width, height)) {
            x = nextX;
        }

        if (!world.isBlocked(x, nextY, width, height)) {
            y = nextY;
        }

        if (moving) {
            animationTimer += deltaTime;

            while (animationTimer >= FRAME_DURATION) {
                animationTimer -= FRAME_DURATION;
                animationFrame =
                    (animationFrame + 1) % FRAME_COUNT;
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }
    }

    public void render(GraphicsContext gc) {

        if (currentSprite == null) {
            return;
        }

        // Source rectangle: select one 32x32 frame.
        int sourceX = animationFrame * FRAME_WIDTH;

        // Destination rectangle: draw slightly larger than
        // the collision box for a clearer pixel-art character.
        double drawWidth = 32;
        double drawHeight = 32;

        gc.drawImage(
            currentSprite,
            sourceX, 0,
            FRAME_WIDTH, FRAME_HEIGHT,
            x - 5, y - 7,
            drawWidth, drawHeight
        );
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
