package com.dsarpg.scenery;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Draws a region's terrain, atmosphere and ambient particles.
 *
 * Everything is drawn with simple canvas shapes and a deterministic hash per
 * tile, so no image files are needed and the scenery looks the same every
 * time a region is entered.
 */
public final class SceneryRenderer {

    private static final int T = MapGenerator.TILE;

    private final SceneryTheme theme;
    private final int[][] map;

    private final Color groundBase;
    private final Color groundLight;
    private final Color groundDark;
    private final Color pathBase;
    private final Color pathLight;
    private final Color pathDark;
    private final Color liquidDeep;
    private final Color liquidLight;
    private final Color accent;

    private final double[][] particles;   // x, y, speed, phase, size
    private double time;
    private Image minimap;

    public SceneryRenderer(SceneryTheme theme, int[][] map) {
        this.theme = theme;
        this.map = map;

        groundBase = Color.web(theme.getGroundBase());
        groundLight = Color.web(theme.getGroundLight());
        groundDark = Color.web(theme.getGroundDark());
        pathBase = Color.web(theme.getPathBase());
        pathLight = Color.web(theme.getPathLight());
        pathDark = Color.web(theme.getPathDark());
        liquidDeep = Color.web(theme.getLiquidDeep());
        liquidLight = Color.web(theme.getLiquidLight());
        accent = Color.web(theme.getAccent());

        Random rng = new Random(theme.getLevel() * 31L);
        particles = new double[46][5];

        for (double[] p : particles) {
            p[0] = rng.nextDouble();
            p[1] = rng.nextDouble();
            p[2] = 0.4 + rng.nextDouble() * 0.9;
            p[3] = rng.nextDouble() * Math.PI * 2;
            p[4] = 1.2 + rng.nextDouble() * 2.2;
        }
    }

    public SceneryTheme getTheme() {
        return theme;
    }

    public void update(double dt) {
        time += dt;
    }

    private static int hash(int col, int row, int salt) {
        int h = col * 73856093 ^ row * 19349663 ^ salt * 83492791;
        h ^= (h >>> 13);
        h *= 0x5bd1e995;
        h ^= (h >>> 15);
        return h & 0x7fffffff;
    }

    // ------------------------------------------------------------------
    // TERRAIN
    // ------------------------------------------------------------------

    public void drawTerrain(GraphicsContext gc, double camX, double camY,
                            double viewW, double viewH, boolean animate) {

        int firstCol = Math.max(0, (int) (camX / T));
        int firstRow = Math.max(0, (int) (camY / T));
        int lastCol = Math.min(MapGenerator.COLS - 1, (int) ((camX + viewW) / T) + 1);
        int lastRow = Math.min(MapGenerator.ROWS - 1, (int) ((camY + viewH) / T) + 1);

        double t = animate ? time : 0;

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                double x = col * T;
                double y = row * T;
                int tile = map[row][col];

                if (tile == MapGenerator.PATH) {
                    drawPath(gc, x, y, col, row);
                } else if (tile == MapGenerator.LIQUID) {
                    drawLiquid(gc, x, y, col, row, t);
                } else {
                    drawGround(gc, x, y, col, row);
                }
            }
        }

        // Obstacles last so tall objects overlap the ground behind them.
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (map[row][col] == MapGenerator.OBSTACLE) {
                    drawObstacle(gc, col * T, row * T, col, row, t);
                }
            }
        }
    }

    private void drawGround(GraphicsContext gc, double x, double y, int col, int row) {
        int h = hash(col, row, 1);
        gc.setFill(h % 5 == 0 ? groundLight : (h % 7 == 0 ? groundDark : groundBase));
        gc.fillRect(x, y, T, T);

        switch (theme) {
            case FOREST:
                gc.setFill(groundLight);
                gc.fillOval(x + 4 + h % 17, y + 5, 3, 3);
                gc.fillOval(x + 8, y + 17 + h % 9, 2, 3);
                gc.setFill(groundDark);
                gc.fillOval(x + 19, y + 8 + h % 13, 3, 2);

                if (h % 11 == 0) {
                    gc.setFill(Color.web("#F4E8B0"));
                    gc.fillOval(x + 20, y + 20, 4, 4);
                    gc.setFill(Color.web("#E7A7C8"));
                    gc.fillOval(x + 21, y + 19, 3, 3);
                }
                break;

            case RUINS:
                gc.setStroke(groundDark);
                gc.setLineWidth(1);
                gc.strokeLine(x + 3, y + 9 + h % 6, x + 14, y + 9 + h % 6);
                gc.strokeLine(x + 17, y + 22 - h % 5, x + 28, y + 22 - h % 5);
                gc.setFill(groundDark);
                gc.fillOval(x + 6 + h % 15, y + 24, 3, 2);

                if (h % 9 == 0) {
                    gc.setStroke(Color.web("#8C7A55"));
                    gc.strokeRect(x + 5, y + 5, 22, 22);
                    gc.strokeLine(x + 5, y + 5, x + 16, y + 16);
                }
                break;

            case FROZEN_PEAKS:
                gc.setFill(groundDark);
                gc.fillOval(x + 3 + h % 12, y + 20, 14, 5);
                gc.setFill(Color.WHITE);
                gc.fillOval(x + 10 + h % 10, y + 6, 8, 4);

                if (h % 6 == 0) {
                    gc.fillOval(x + 24, y + 22, 2, 2);
                    gc.fillOval(x + 6, y + 12, 2, 2);
                }
                break;

            case CRYSTAL_CAVERNS:
                gc.setFill(groundLight);
                gc.fillOval(x + 5 + h % 15, y + 6, 4, 3);
                gc.setFill(groundDark);
                gc.fillOval(x + 14, y + 18 + h % 8, 7, 4);

                if (h % 8 == 0) {
                    gc.setFill(Color.color(0.4, 1.0, 0.85, 0.55));
                    gc.fillOval(x + 20, y + 10, 4, 4);
                }
                break;

            case CITADEL:
            default:
                gc.setStroke(groundDark);
                gc.setLineWidth(1.5);
                gc.strokeRect(x + 0.5, y + 0.5, T - 1, T - 1);

                if (h % 6 == 0) {
                    gc.strokeLine(x + 6, y + 8, x + 15, y + 17);
                    gc.strokeLine(x + 15, y + 17, x + 14, y + 26);
                }

                if (h % 17 == 0) {
                    gc.setFill(Color.color(1.0, 0.45, 0.15, 0.55));
                    gc.fillOval(x + 12, y + 12, 5, 5);
                }
                break;
        }
    }

    private void drawPath(GraphicsContext gc, double x, double y, int col, int row) {
        int h = hash(col, row, 2);
        gc.setFill(pathBase);
        gc.fillRect(x, y, T, T);

        switch (theme) {
            case RUINS:
            case CITADEL:
                // Cobblestones / marble slabs.
                gc.setStroke(pathDark);
                gc.setLineWidth(1.5);
                gc.strokeRect(x + 1, y + 1, 14, 14);
                gc.strokeRect(x + 16, y + 1, 14, 14);
                gc.strokeRect(x + 1, y + 16, 14, 14);
                gc.strokeRect(x + 16, y + 16, 14, 14);
                gc.setFill(pathLight);
                gc.fillRect(x + 3, y + 3, 6, 2);

                if (theme == SceneryTheme.CITADEL && (col + row) % 2 == 0) {
                    gc.setFill(Color.color(1, 0.8, 0.4, 0.14));
                    gc.fillRect(x, y, T, T);
                }
                break;

            case FROZEN_PEAKS:
                gc.setFill(pathLight);
                gc.fillOval(x + 2, y + 6 + h % 8, 26, 6);
                gc.setFill(pathDark);
                gc.fillOval(x + 6 + h % 10, y + 22, 10, 3);
                break;

            case CRYSTAL_CAVERNS:
                gc.setFill(pathLight);
                gc.fillOval(x + 3 + h % 12, y + 5, 8, 4);
                gc.setFill(Color.color(0.5, 0.95, 1.0, 0.65));
                gc.fillOval(x + 8 + h % 14, y + 18, 4, 4);
                gc.setFill(pathDark);
                gc.fillOval(x + 20, y + 8 + h % 10, 6, 3);
                break;

            case FOREST:
            default:
                gc.setFill(pathLight);
                gc.fillOval(x + 3 + h % 12, y + 5, 8, 4);
                gc.fillOval(x + 17, y + 17 + h % 7, 6, 4);
                gc.setFill(pathDark);
                gc.fillOval(x + 8, y + 23, 3, 2);
                gc.fillOval(x + 23, y + 7 + h % 8, 3, 2);
                break;
        }
    }

    private boolean isLiquid(int col, int row) {
        return row >= 0 && row < map.length && col >= 0 && col < map[0].length
                && map[row][col] == MapGenerator.LIQUID;
    }

    private void drawLiquid(GraphicsContext gc, double x, double y, int col, int row, double t) {
        int h = hash(col, row, 3);
        double pulse = 0.5 + 0.5 * Math.sin(t * 2.0 + h % 7);

        gc.setFill(liquidDeep);
        gc.fillRect(x, y, T, T);

        switch (theme.getLiquid()) {
            case LAVA:
                gc.setFill(Color.web("#FF8A2B"));
                gc.fillOval(x + 4 + h % 10, y + 6, 14, 9);
                gc.setFill(Color.web("#2B1A14"));
                gc.fillOval(x + 14, y + 17 + h % 6, 10, 6);
                gc.setFill(Color.color(1, 0.85, 0.3, 0.25 + 0.35 * pulse));
                gc.fillOval(x + 6, y + 4, 20, 20);
                break;

            case GLOW_POOL:
                gc.setFill(liquidLight);
                gc.fillOval(x + 6, y + 6, 20, 20);
                gc.setFill(Color.color(1, 1, 1, 0.25 + 0.3 * pulse));
                gc.fillOval(x + 11, y + 11, 10, 10);
                break;

            case ICE:
                gc.setFill(liquidLight);
                gc.fillOval(x + 3 + h % 8, y + 5, 12, 6);
                gc.setStroke(Color.web("#EAF6FF"));
                gc.setLineWidth(1);
                gc.strokeLine(x + 4, y + 20, x + 15, y + 12 + h % 6);
                gc.strokeLine(x + 15, y + 12 + h % 6, x + 28, y + 17);
                break;

            case OASIS:
            case POND:
            default:
                gc.setStroke(liquidLight);
                gc.setLineWidth(1.5);
                double shift = Math.sin(t * 1.6 + h % 5) * 3;
                gc.strokeLine(x + 4 + shift, y + 9, x + 15 + shift, y + 9);
                gc.strokeLine(x + 14 - shift, y + 21, x + 27 - shift, y + 21);

                if (theme.getLiquid() == SceneryTheme.Liquid.POND && h % 6 == 0) {
                    gc.setFill(Color.web("#4F9A4B"));
                    gc.fillOval(x + 18, y + 6, 9, 6);
                }
                break;
        }

        // Shoreline on sides that face dry land.
        gc.setFill(liquidLight);

        if (!isLiquid(col, row - 1)) {
            gc.fillRect(x, y, T, 3);
        }

        if (!isLiquid(col, row + 1)) {
            gc.fillRect(x, y + T - 3, T, 3);
        }

        if (!isLiquid(col - 1, row)) {
            gc.fillRect(x, y, 3, T);
        }

        if (!isLiquid(col + 1, row)) {
            gc.fillRect(x + T - 3, y, 3, T);
        }
    }

    private void drawObstacle(GraphicsContext gc, double x, double y, int col, int row, double t) {
        int h = hash(col, row, 4);

        // Soft shadow under every obstacle.
        gc.setFill(Color.color(0, 0, 0, 0.22));
        gc.fillOval(x + 2, y + 19, 29, 12);

        switch (theme.getObstacle()) {
            case PILLAR:
                drawPillar(gc, x, y, h);
                break;
            case PINE:
                drawPine(gc, x, y, h);
                break;
            case CRYSTAL:
                drawCrystal(gc, x, y, h, t);
                break;
            case TOWER:
                drawTower(gc, x, y, h, t);
                break;
            case TREE:
            default:
                drawTree(gc, x, y);
                break;
        }
    }

    private void drawTree(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#68452D"));
        gc.fillRoundRect(x + 12, y + 12, 9, 19, 4, 4);
        gc.setFill(Color.web("#254D32"));
        gc.fillOval(x + 1, y + 1, 30, 25);
        gc.setFill(Color.web("#32683A"));
        gc.fillOval(x + 4, y, 23, 22);
        gc.setFill(Color.web("#438049"));
        gc.fillOval(x + 7, y + 3, 15, 14);
        gc.setFill(Color.web("#629653"));
        gc.fillOval(x + 9, y + 4, 7, 5);
    }

    private void drawPine(GraphicsContext gc, double x, double y, int h) {
        gc.setFill(Color.web("#5A3D2B"));
        gc.fillRect(x + 14, y + 22, 5, 9);
        gc.setFill(Color.web("#2F5D4A"));
        gc.fillPolygon(new double[] {x + 16, x + 3, x + 29},
                new double[] {y + 1, y + 20, y + 20}, 3);
        gc.setFill(Color.web("#3C7058"));
        gc.fillPolygon(new double[] {x + 16, x + 5, x + 27},
                new double[] {y + 9, y + 26, y + 26}, 3);
        gc.setFill(Color.WHITE);
        gc.fillPolygon(new double[] {x + 16, x + 10, x + 22},
                new double[] {y + 1, y + 9, y + 9}, 3);
        gc.fillOval(x + 8 + h % 4, y + 17, 9, 4);
        gc.fillOval(x + 15, y + 23, 9, 3);
    }

    private void drawPillar(GraphicsContext gc, double x, double y, int h) {
        Color stone = Color.web("#B8AC92");
        Color dark = Color.web("#8A7E68");

        if (h % 3 == 0) {
            // Rubble heap.
            gc.setFill(dark);
            gc.fillOval(x + 2, y + 12, 18, 15);
            gc.setFill(stone);
            gc.fillOval(x + 12, y + 8, 17, 17);
            gc.setFill(Color.web("#D6CBB2"));
            gc.fillOval(x + 15, y + 10, 7, 5);
            return;
        }

        gc.setFill(dark);
        gc.fillRect(x + 6, y + 24, 20, 6);
        gc.setFill(stone);
        gc.fillRect(x + 9, y + 6, 14, 20);
        gc.setFill(Color.web("#D6CBB2"));
        gc.fillRect(x + 11, y + 6, 4, 20);
        gc.setFill(dark);
        gc.fillRect(x + 7, y + 3, 18, 4);

        // Broken top.
        gc.setFill(groundBase);
        gc.fillPolygon(new double[] {x + 16, x + 23, x + 23},
                new double[] {y + 3, y + 3, y + 10}, 3);
    }

    private void drawCrystal(GraphicsContext gc, double x, double y, int h, double t) {
        Color main = h % 2 == 0 ? Color.web("#7B6CFF") : Color.web("#35D3C4");
        Color light = h % 2 == 0 ? Color.web("#B8B0FF") : Color.web("#9FF5E8");
        double glow = 0.25 + 0.2 * Math.sin(t * 1.8 + h % 6);

        gc.setFill(Color.color(main.getRed(), main.getGreen(), main.getBlue(), glow));
        gc.fillOval(x - 2, y + 8, 36, 24);

        gc.setFill(main);
        gc.fillPolygon(new double[] {x + 16, x + 8, x + 11, x + 21, x + 24},
                new double[] {y, y + 10, y + 28, y + 28, y + 10}, 5);
        gc.setFill(light);
        gc.fillPolygon(new double[] {x + 16, x + 8, x + 14},
                new double[] {y, y + 10, y + 14}, 3);

        gc.setFill(Color.web("#B65CFF"));
        gc.fillPolygon(new double[] {x + 3, x + 0, x + 11},
                new double[] {y + 14, y + 28, y + 28}, 3);
        gc.fillPolygon(new double[] {x + 27, x + 31, x + 22},
                new double[] {y + 16, y + 28, y + 28}, 3);
    }

    private void drawTower(GraphicsContext gc, double x, double y, int h, double t) {
        gc.setFill(Color.web("#2C2729"));
        gc.fillRect(x, y + 4, T, 27);
        gc.setFill(Color.web("#463E42"));
        gc.fillRect(x + 1, y + 6, T - 2, 23);

        // Brick lines.
        gc.setStroke(Color.web("#2C2729"));
        gc.setLineWidth(1);
        gc.strokeLine(x, y + 14, x + T, y + 14);
        gc.strokeLine(x, y + 22, x + T, y + 22);
        gc.strokeLine(x + 10, y + 6, x + 10, y + 14);
        gc.strokeLine(x + 22, y + 14, x + 22, y + 22);

        // Battlements.
        gc.setFill(Color.web("#2C2729"));
        gc.fillRect(x, y, 7, 6);
        gc.fillRect(x + 12, y, 8, 6);
        gc.fillRect(x + 25, y, 7, 6);

        if (h % 4 == 0) {
            double flicker = 0.6 + 0.4 * Math.sin(t * 9 + h % 5);
            gc.setFill(Color.color(1.0, 0.55, 0.15, 0.30 * flicker));
            gc.fillOval(x + 4, y + 4, 24, 24);
            gc.setFill(Color.web("#FFB347"));
            gc.fillOval(x + 12, y + 10, 8, 11);
            gc.setFill(Color.web("#FFE082"));
            gc.fillOval(x + 14, y + 14, 4, 6);
        }
    }

    // ------------------------------------------------------------------
    // ATMOSPHERE (screen space)
    // ------------------------------------------------------------------

    /**
     * Ambient particles and a gentle tint. Call after the world is drawn.
     * Skipped entirely when effects are disabled (reduced motion or high
     * contrast), keeping the scene as clear as possible.
     */
    public void drawAtmosphere(GraphicsContext gc, double viewW, double viewH,
                               double playerScreenX, double playerScreenY,
                               boolean particlesOn, boolean tintOn) {

        if (tintOn) {
            gc.setFill(Color.web(theme.getTint(), theme.getTintAlpha()));
            gc.fillRect(0, 0, viewW, viewH);

            boolean dark = theme == SceneryTheme.CRYSTAL_CAVERNS
                    || theme == SceneryTheme.CITADEL;
            double edge = dark ? 0.42 : 0.14;

            gc.setFill(new RadialGradient(0, 0, playerScreenX, playerScreenY,
                    Math.max(viewW, viewH) * 0.62, false, CycleMethod.NO_CYCLE,
                    new Stop(0.45, Color.color(0, 0, 0, 0)),
                    new Stop(1.0, Color.color(0, 0, 0, edge))));
            gc.fillRect(0, 0, viewW, viewH);
        }

        if (!particlesOn) {
            return;
        }

        for (double[] p : particles) {
            double sx;
            double sy;
            double flicker = 0.5 + 0.5 * Math.sin(time * 2.0 * p[2] + p[3]);

            switch (theme.getAmbient()) {
                case SNOW:
                    sy = ((p[1] + time * 0.05 * p[2]) % 1.0) * viewH;
                    sx = ((p[0] + Math.sin(time * 0.6 + p[3]) * 0.02) % 1.0) * viewW;
                    gc.setFill(Color.color(1, 1, 1, 0.85));
                    gc.fillOval(sx, sy, p[4], p[4]);
                    break;

                case DUST:
                    sx = ((p[0] + time * 0.03 * p[2]) % 1.0) * viewW;
                    sy = (p[1] + Math.sin(time * 0.5 + p[3]) * 0.02) * viewH;
                    gc.setFill(Color.color(0.93, 0.76, 0.42, 0.40));
                    gc.fillOval(sx, sy, p[4], p[4] * 0.7);
                    break;

                case SPORES:
                    sy = (1.0 - ((p[1] + time * 0.025 * p[2]) % 1.0)) * viewH;
                    sx = (p[0] + Math.sin(time * 0.4 + p[3]) * 0.015) * viewW;
                    gc.setFill(Color.color(0.55, 1.0, 0.9, 0.18 + 0.30 * flicker));
                    gc.fillOval(sx - 2, sy - 2, p[4] + 4, p[4] + 4);
                    gc.setFill(Color.color(0.8, 1.0, 0.95, 0.75));
                    gc.fillOval(sx, sy, p[4] * 0.8, p[4] * 0.8);
                    break;

                case EMBERS:
                    sy = (1.0 - ((p[1] + time * 0.06 * p[2]) % 1.0)) * viewH;
                    sx = (p[0] + Math.sin(time * 0.8 + p[3]) * 0.02) * viewW;
                    gc.setFill(Color.color(1.0, 0.55 + 0.3 * flicker, 0.15, 0.80));
                    gc.fillOval(sx, sy, p[4] * 0.9, p[4] * 0.9);
                    break;

                case FIREFLIES:
                default:
                    sx = (p[0] + Math.sin(time * 0.3 * p[2] + p[3]) * 0.03) * viewW;
                    sy = (p[1] + Math.cos(time * 0.25 * p[2] + p[3]) * 0.03) * viewH;
                    gc.setFill(Color.color(1.0, 0.95, 0.5, 0.10 + 0.22 * flicker));
                    gc.fillOval(sx - 4, sy - 4, 11, 11);
                    gc.setFill(Color.color(1.0, 0.98, 0.7, 0.35 + 0.55 * flicker));
                    gc.fillOval(sx, sy, 3, 3);
                    break;
            }
        }
    }

    // ------------------------------------------------------------------
    // MINIMAP IMAGE (one pixel per tile)
    // ------------------------------------------------------------------

    public Image minimapImage() {
        if (minimap == null) {
            WritableImage image = new WritableImage(MapGenerator.COLS, MapGenerator.ROWS);

            for (int row = 0; row < MapGenerator.ROWS; row++) {
                for (int col = 0; col < MapGenerator.COLS; col++) {
                    Color c;

                    switch (map[row][col]) {
                        case MapGenerator.OBSTACLE:
                            c = Color.web("#1E2B22");
                            break;
                        case MapGenerator.LIQUID:
                            c = liquidDeep;
                            break;
                        case MapGenerator.PATH:
                            c = pathLight;
                            break;
                        default:
                            c = groundBase;
                            break;
                    }

                    image.getPixelWriter().setColor(col, row, c);
                }
            }

            minimap = image;
        }

        return minimap;
    }

    public Color getAccent() {
        return accent;
    }
}
