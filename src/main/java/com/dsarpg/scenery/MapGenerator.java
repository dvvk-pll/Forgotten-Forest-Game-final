package com.dsarpg.scenery;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the tile map of a region.
 *
 * Tile types: GROUND (walkable), OBSTACLE (blocks), PATH (walkable road),
 * LIQUID (blocks).
 *
 * The generator first lays roads and reserves open space around every
 * important point (player start, enemies and their patrol range, chests,
 * guardian, boss). Obstacles and liquids are only placed on unreserved
 * ground, so every objective is always reachable. The layout is seeded by
 * the level number, so saved positions stay valid between sessions.
 */
public final class MapGenerator {

    public static final int GROUND = 0;
    public static final int OBSTACLE = 1;
    public static final int PATH = 2;
    public static final int LIQUID = 3;

    public static final int TILE = 32;
    public static final int COLS = 60;
    public static final int ROWS = 40;

    private MapGenerator() {
    }

    public static boolean isBlocked(int tile) {
        return tile == OBSTACLE || tile == LIQUID;
    }

    public static int[][] generate(int level) {
        return generate(LevelLayout.forLevel(level));
    }

    public static int[][] generate(LevelLayout layout) {
        int level = layout.getLevel();
        SceneryTheme theme = SceneryTheme.forLevel(level);
        Random rng = new Random(level * 7919L + 17);

        int[][] map = new int[ROWS][COLS];
        boolean[][] reserved = new boolean[ROWS][COLS];

        // Border wall.
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (r == 0 || r == ROWS - 1 || c == 0 || c == COLS - 1) {
                    map[r][c] = OBSTACLE;
                    reserved[r][c] = true;
                }
            }
        }

        // Main crossing roads.
        for (int c = 1; c < COLS - 1; c++) {
            map[20][c] = PATH;
        }

        for (int r = 1; r < ROWS - 1; r++) {
            map[r][30] = PATH;
        }

        // A small plaza at the crossing in the citadel and ruins.
        if (theme == SceneryTheme.CITADEL || theme == SceneryTheme.RUINS) {
            for (int r = 18; r <= 22; r++) {
                for (int c = 28; c <= 32; c++) {
                    map[r][c] = PATH;
                }
            }
        }

        // Points of interest in tile coordinates.
        List<int[]> points = new ArrayList<>();

        points.add(tile(layout.getPlayerX() + 11, layout.getPlayerY() + 11));
        points.add(tile(layout.getGuardianX() + 16, layout.getGuardianY() + 16));
        points.add(tile(layout.getBossX() + 24, layout.getBossY() + 29));

        for (LevelLayout.ChestSpawn chest : layout.getChests()) {
            points.add(tile(chest.x + 13, chest.y + 14));
        }

        for (LevelLayout.EnemySpawn enemy : layout.getEnemies()) {
            points.add(tile(enemy.x + 18, enemy.y + 18));
        }

        // Connect every point to the nearest main road.
        for (int[] p : points) {
            carveRoad(map, p[0], p[1]);
        }

        // Reserve space around roads.
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (map[r][c] == PATH) {
                    mark(reserved, c - 1, r - 1, c + 1, r + 1);
                }
            }
        }

        // Reserve space around important points.
        mark(reserved, tileCol(layout.getPlayerX()) - 3,
                tileRow(layout.getPlayerY()) - 3,
                tileCol(layout.getPlayerX()) + 3,
                tileRow(layout.getPlayerY()) + 3);

        int[] guardian = tile(layout.getGuardianX() + 16, layout.getGuardianY() + 16);
        mark(reserved, guardian[0] - 3, guardian[1] - 3, guardian[0] + 3, guardian[1] + 3);

        int[] boss = tile(layout.getBossX() + 24, layout.getBossY() + 29);
        mark(reserved, boss[0] - 4, boss[1] - 4, boss[0] + 4, boss[1] + 4);

        for (LevelLayout.ChestSpawn chest : layout.getChests()) {
            int[] t = tile(chest.x + 13, chest.y + 14);
            mark(reserved, t[0] - 2, t[1] - 2, t[0] + 2, t[1] + 2);
        }

        for (LevelLayout.EnemySpawn enemy : layout.getEnemies()) {
            int r0 = tileRow(enemy.y) - 1;
            int r1 = tileRow(enemy.y + 36) + 1;
            int c0 = tileCol(enemy.patrolMin) - 1;
            int c1 = tileCol(enemy.patrolMax) + 1;
            mark(reserved, c0, r0, c1, r1);
        }

        placeObstacles(map, reserved, theme, rng);
        placeLiquids(map, reserved, theme, rng);

        return map;
    }

    // --------------------------------------------------
    // ROADS
    // --------------------------------------------------

    private static void carveRoad(int[][] map, int col, int row) {
        int toHorizontal = Math.abs(row - 20);
        int toVertical = Math.abs(col - 30);

        if (toHorizontal <= toVertical) {
            int step = row < 20 ? 1 : -1;

            for (int r = row; r != 20; r += step) {
                setPath(map, col, r);
            }
        } else {
            int step = col < 30 ? 1 : -1;

            for (int c = col; c != 30; c += step) {
                setPath(map, c, row);
            }
        }
    }

    private static void setPath(int[][] map, int col, int row) {
        if (col > 0 && col < COLS - 1 && row > 0 && row < ROWS - 1) {
            map[row][col] = PATH;
        }
    }

    // --------------------------------------------------
    // OBSTACLES
    // --------------------------------------------------

    private static void placeObstacles(int[][] map, boolean[][] reserved,
                                       SceneryTheme theme, Random rng) {

        int attempts = 140;

        for (int i = 0; i < attempts; i++) {
            List<int[]> shape = shapeFor(theme, rng);

            int baseCol = 2 + rng.nextInt(COLS - 8);
            int baseRow = 2 + rng.nextInt(ROWS - 7);

            boolean fits = true;

            for (int[] offset : shape) {
                int c = baseCol + offset[0];
                int r = baseRow + offset[1];

                if (c < 1 || c >= COLS - 1 || r < 1 || r >= ROWS - 1
                        || reserved[r][c] || map[r][c] != GROUND) {
                    fits = false;
                    break;
                }
            }

            if (!fits) {
                continue;
            }

            for (int[] offset : shape) {
                map[baseRow + offset[1]][baseCol + offset[0]] = OBSTACLE;
            }
        }
    }

    private static List<int[]> shapeFor(SceneryTheme theme, Random rng) {
        List<int[]> tiles = new ArrayList<>();

        switch (theme.getObstacle()) {
            case PILLAR: {
                // Broken colonnades and rubble heaps.
                if (rng.nextBoolean()) {
                    int length = 3 + rng.nextInt(4);
                    boolean horizontal = rng.nextBoolean();

                    for (int k = 0; k < length; k += 2) {
                        tiles.add(horizontal ? new int[] {k, 0} : new int[] {0, k});
                    }
                } else {
                    tiles.add(new int[] {0, 0});
                    tiles.add(new int[] {1, 0});
                    tiles.add(new int[] {0, 1});

                    if (rng.nextBoolean()) {
                        tiles.add(new int[] {1, 1});
                    }
                }
                break;
            }

            case TOWER: {
                // Wall segments with a gap in the middle.
                int length = 4 + rng.nextInt(5);
                boolean horizontal = rng.nextBoolean();
                int gap = length / 2;

                for (int k = 0; k < length; k++) {
                    if (k == gap) {
                        continue;
                    }

                    tiles.add(horizontal ? new int[] {k, 0} : new int[] {0, k});
                }
                break;
            }

            case CRYSTAL: {
                // Irregular crystal clusters (random walk).
                int cells = 3 + rng.nextInt(5);
                int c = 0;
                int r = 0;
                tiles.add(new int[] {0, 0});

                for (int k = 1; k < cells; k++) {
                    int dir = rng.nextInt(4);
                    c += dir == 0 ? 1 : (dir == 1 ? -1 : 0);
                    r += dir == 2 ? 1 : (dir == 3 ? -1 : 0);

                    boolean exists = false;

                    for (int[] t : tiles) {
                        if (t[0] == c && t[1] == r) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        tiles.add(new int[] {c, r});
                    }
                }

                // Shift into positive space.
                int minC = 0;
                int minR = 0;

                for (int[] t : tiles) {
                    minC = Math.min(minC, t[0]);
                    minR = Math.min(minR, t[1]);
                }

                for (int[] t : tiles) {
                    t[0] -= minC;
                    t[1] -= minR;
                }
                break;
            }

            case PINE:
            case TREE:
            default: {
                // Rounded blobs of trees.
                int w = 2 + rng.nextInt(4);
                int h = 2 + rng.nextInt(3);

                for (int r = 0; r < h; r++) {
                    for (int c = 0; c < w; c++) {
                        boolean corner = (r == 0 || r == h - 1)
                                && (c == 0 || c == w - 1);

                        if (corner && w > 2 && h > 2 && rng.nextBoolean()) {
                            continue;
                        }

                        tiles.add(new int[] {c, r});
                    }
                }
                break;
            }
        }

        return tiles;
    }

    // --------------------------------------------------
    // LIQUIDS
    // --------------------------------------------------

    private static void placeLiquids(int[][] map, boolean[][] reserved,
                                     SceneryTheme theme, Random rng) {

        int wanted = 4;
        int placed = 0;

        for (int i = 0; i < 200 && placed < wanted; i++) {
            int rx = 2 + rng.nextInt(2);
            int ry = 1 + rng.nextInt(2);
            int cx = 4 + rng.nextInt(COLS - 8);
            int cy = 4 + rng.nextInt(ROWS - 8);

            List<int[]> cells = new ArrayList<>();
            boolean fits = true;

            for (int r = cy - ry; r <= cy + ry && fits; r++) {
                for (int c = cx - rx; c <= cx + rx; c++) {
                    double dx = (c - cx) / (double) rx;
                    double dy = (r - cy) / (double) ry;

                    if (dx * dx + dy * dy > 1.0) {
                        continue;
                    }

                    if (c < 1 || c >= COLS - 1 || r < 1 || r >= ROWS - 1
                            || reserved[r][c] || map[r][c] != GROUND
                            || touchesObstacleOrPath(map, c, r)) {
                        fits = false;
                        break;
                    }

                    cells.add(new int[] {c, r});
                }
            }

            if (!fits || cells.isEmpty()) {
                continue;
            }

            for (int[] cell : cells) {
                map[cell[1]][cell[0]] = LIQUID;
            }

            placed++;
        }
    }

    private static boolean touchesObstacleOrPath(int[][] map, int col, int row) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = row + dr;
                int c = col + dc;

                if (r < 0 || r >= ROWS || c < 0 || c >= COLS) {
                    continue;
                }

                if (map[r][c] == OBSTACLE || map[r][c] == PATH) {
                    return true;
                }
            }
        }

        return false;
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private static int tileCol(double x) {
        return Math.max(1, Math.min(COLS - 2, (int) (x / TILE)));
    }

    private static int tileRow(double y) {
        return Math.max(1, Math.min(ROWS - 2, (int) (y / TILE)));
    }

    private static int[] tile(double x, double y) {
        return new int[] {tileCol(x), tileRow(y)};
    }

    private static void mark(boolean[][] reserved,
                             int c0, int r0, int c1, int r1) {

        for (int r = Math.max(1, r0); r <= Math.min(ROWS - 2, r1); r++) {
            for (int c = Math.max(1, c0); c <= Math.min(COLS - 2, c1); c++) {
                reserved[r][c] = true;
            }
        }
    }

    /**
     * Finds the closest walkable tile centre to a pixel position, used to
     * rescue players whose saved position is now inside scenery.
     */
    public static double[] nearestWalkable(int[][] map, double x, double y,
                                           double width, double height) {

        int startCol = (int) (x / TILE);
        int startRow = (int) (y / TILE);

        for (int radius = 0; radius < Math.max(COLS, ROWS); radius++) {
            for (int dr = -radius; dr <= radius; dr++) {
                for (int dc = -radius; dc <= radius; dc++) {
                    if (Math.max(Math.abs(dr), Math.abs(dc)) != radius) {
                        continue;
                    }

                    int c = startCol + dc;
                    int r = startRow + dr;

                    if (c < 1 || c >= COLS - 1 || r < 1 || r >= ROWS - 1) {
                        continue;
                    }

                    double px = c * TILE + (TILE - width) / 2.0;
                    double py = r * TILE + (TILE - height) / 2.0;

                    if (!boxBlocked(map, px, py, width, height)) {
                        return new double[] {px, py};
                    }
                }
            }
        }

        return new double[] {x, y};
    }

    public static boolean boxBlocked(int[][] map, double x, double y,
                                     double width, double height) {

        return blockedAt(map, x, y)
                || blockedAt(map, x + width - 1, y)
                || blockedAt(map, x, y + height - 1)
                || blockedAt(map, x + width - 1, y + height - 1);
    }

    public static boolean blockedAt(int[][] map, double x, double y) {
        int col = (int) Math.floor(x / TILE);
        int row = (int) Math.floor(y / TILE);

        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return true;
        }

        return isBlocked(map[row][col]);
    }
}
