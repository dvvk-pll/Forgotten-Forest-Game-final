package com.dsarpg;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.dsarpg.scenery.LevelLayout;
import com.dsarpg.scenery.MapGenerator;
import com.dsarpg.scenery.SceneryTheme;

/**
 * Plain-Java checks for the scenery generator.
 * Run: java -cp target/classes:target/test-classes com.dsarpg.SceneryTest
 */
public class SceneryTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean ok, String msg) {
        if (ok) { passed++; System.out.println("  PASS  " + msg); }
        else { failed++; System.out.println("  FAIL  " + msg); }
    }

    public static void main(String[] args) {
        for (int level = 1; level <= 5; level++) {
            SceneryTheme theme = SceneryTheme.forLevel(level);
            System.out.println("Level " + level + " - " + theme.getTitle());

            LevelLayout layout = LevelLayout.forLevel(level);
            int[][] map = MapGenerator.generate(layout);

            check(map.length == MapGenerator.ROWS && map[0].length == MapGenerator.COLS,
                    "map size is 60 x 40");

            // Player start is free for the 22 x 22 collision box.
            check(!MapGenerator.boxBlocked(map, layout.getPlayerX(), layout.getPlayerY(), 22, 22),
                    "player start is walkable");

            // Enemies and their whole patrol line are free.
            boolean patrolFree = true;
            for (LevelLayout.EnemySpawn e : layout.getEnemies()) {
                for (double x = e.patrolMin; x <= e.patrolMax; x += 8) {
                    patrolFree &= !MapGenerator.boxBlocked(map, x, e.y, 36, 36);
                }
            }
            check(patrolFree, "every enemy patrol line is free of scenery");

            boolean chestsFree = true;
            for (LevelLayout.ChestSpawn c : layout.getChests()) {
                chestsFree &= !MapGenerator.boxBlocked(map, c.x, c.y, 26, 26);
            }
            check(chestsFree, "treasure chests are not inside scenery");

            check(!MapGenerator.boxBlocked(map, layout.getGuardianX(), layout.getGuardianY(), 28, 30),
                    "guardian stands on free ground");
            check(!MapGenerator.boxBlocked(map, layout.getBossX(), layout.getBossY(), 48, 58),
                    "boss arena is free of scenery");

            // Everything reachable from the player start.
            boolean[][] seen = flood(map, (int) ((layout.getPlayerX() + 11) / 32),
                    (int) ((layout.getPlayerY() + 11) / 32));
            boolean reachable = true;
            for (LevelLayout.EnemySpawn e : layout.getEnemies()) {
                reachable &= seen[(int) ((e.y + 18) / 32)][(int) ((e.x + 18) / 32)];
            }
            for (LevelLayout.ChestSpawn c : layout.getChests()) {
                reachable &= seen[(int) ((c.y + 14) / 32)][(int) ((c.x + 13) / 32)];
            }
            reachable &= seen[(int) ((layout.getGuardianY() + 16) / 32)][(int) ((layout.getGuardianX() + 16) / 32)];
            reachable &= seen[(int) ((layout.getBossY() + 29) / 32)][(int) ((layout.getBossX() + 24) / 32)];
            check(reachable, "all enemies, chests, guardian and boss are reachable");

            int obstacles = 0, liquids = 0, paths = 0;
            for (int[] row : map) {
                for (int t : row) {
                    if (t == MapGenerator.OBSTACLE) obstacles++;
                    if (t == MapGenerator.LIQUID) liquids++;
                    if (t == MapGenerator.PATH) paths++;
                }
            }
            double share = obstacles / (double) (60 * 40);
            System.out.printf("  info  obstacles %.0f%%, liquid tiles %d, path tiles %d%n",
                    share * 100, liquids, paths);
            check(share > 0.10 && share < 0.40, "obstacle share is visually balanced");
            check(liquids >= 6, "each region contains liquid features");

            check(Arrays.deepEquals(map, MapGenerator.generate(level)),
                    "generation is deterministic (saved positions stay valid)");

            // The rescue function lands on walkable ground.
            double[] rescued = MapGenerator.nearestWalkable(map, 5 * 32, 5 * 32, 22, 22);
            check(!MapGenerator.boxBlocked(map, rescued[0], rescued[1], 22, 22),
                    "nearestWalkable returns a free position");
        }

        boolean different = false;
        for (int l = 1; l < 5; l++) {
            different |= !Arrays.deepEquals(MapGenerator.generate(l), MapGenerator.generate(l + 1));
        }
        check(different, "regions have different layouts");

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean[][] flood(int[][] map, int col, int row) {
        boolean[][] seen = new boolean[map.length][map[0].length];
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {col, row});
        seen[row][col] = true;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] d : dirs) {
                int c = p[0] + d[0];
                int r = p[1] + d[1];
                if (r >= 0 && r < map.length && c >= 0 && c < map[0].length
                        && !seen[r][c] && !MapGenerator.isBlocked(map[r][c])) {
                    seen[r][c] = true;
                    queue.add(new int[] {c, r});
                }
            }
        }
        return seen;
    }
}
