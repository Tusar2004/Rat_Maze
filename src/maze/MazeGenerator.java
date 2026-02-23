package maze;

import java.awt.Point;
import java.util.*;

/**
 * Generates a BRAIDED maze:
 * 1. Carves a perfect maze via recursive-backtracking DFS.
 * 2. Applies a braiding pass that randomly removes walls to create loops.
 * Loops mean multiple valid routes — so algorithms behave visibly differently.
 *
 * Braiding rules:
 * - Only interior walls (not border) are candidates.
 * - Wall must have ≥ 2 open orthogonal neighbours (removing it actually
 * connects something).
 * - Removing the wall must NOT create a fully open 2×2 block (prevents
 * open-field feel).
 * - 15 % removal probability per qualifying cell (configurable).
 */
public class MazeGenerator {

    private static final Random random = new Random();

    /** Probability that a qualifying wall cell is removed during braiding. */
    private static final double BRAID_CHANCE = 0.18;

    // ── Public entry point ────────────────────────────────────────────────────

    public static void generate(Maze maze, Point start, Point goal) {
        int rows = maze.getRows();
        int cols = maze.getCols();

        // ── Phase 1: Fill with walls ──────────────────────────────────────────
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                maze.setTile(r, c, TileType.WALL);
            }
        }

        // ── Phase 2: Carve perfect maze (recursive backtracking) ──────────────
        boolean[][] carved = new boolean[rows][cols];
        carve(maze, carved, start.y, start.x, rows, cols);

        // Guarantee start and goal are open
        maze.setTile(start.y, start.x, TileType.NORMAL);
        maze.setTile(goal.y, goal.x, TileType.NORMAL);

        // ── Phase 3: Braiding — remove walls to create loops ──────────────────
        braidMaze(maze, rows, cols, start, goal);

        // ── Phase 4: Scatter weighted tiles on open cells ─────────────────────
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (maze.getTile(r, c) == TileType.NORMAL) {
                    if (r == start.y && c == start.x)
                        continue;
                    if (r == goal.y && c == goal.x)
                        continue;
                    int roll = random.nextInt(100);
                    if (roll < 8)
                        maze.setTile(r, c, TileType.WATER);
                    else if (roll < 22)
                        maze.setTile(r, c, TileType.MUD);
                }
            }
        }

        // ── Phase 5 (30% chance): Make maze unsolvable by sealing the goal ────
        // Wall off every open neighbour of the goal tile, creating a wall ring.
        if (random.nextDouble() < 0.30) {
            int gr = goal.y, gc = goal.x;
            int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
            for (int[] d : dirs) {
                int nr = gr + d[0], nc = gc + d[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    maze.setTile(nr, nc, TileType.WALL);
                }
            }
        }
    }

    // ── Phase 2 helper: recursive DFS carver ─────────────────────────────────

    private static void carve(Maze maze, boolean[][] carved,
            int r, int c, int rows, int cols) {
        carved[r][c] = true;
        maze.setTile(r, c, TileType.NORMAL);

        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        List<int[]> shuffled = Arrays.asList(dirs);
        Collections.shuffle(shuffled);

        for (int[] d : shuffled) {
            int nr = r + d[0] * 2;
            int nc = c + d[1] * 2;
            if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && !carved[nr][nc]) {
                maze.setTile(r + d[0], c + d[1], TileType.NORMAL); // remove wall between
                carve(maze, carved, nr, nc, rows, cols);
            }
        }
    }

    // ── Phase 3 helper: braiding pass ────────────────────────────────────────

    /**
     * Iterates over all interior wall cells in random order.
     * Removes a wall if:
     * (a) It has ≥ 2 open orthogonal neighbours → actually bridges something.
     * (b) Removing it does NOT create a 2×2 all-open block → avoids open-field
     * feel.
     * (c) random() < BRAID_CHANCE.
     */
    private static void braidMaze(Maze maze, int rows, int cols,
            Point start, Point goal) {
        // Collect all interior wall cells
        List<int[]> walls = new ArrayList<>();
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (maze.getTile(r, c) == TileType.WALL) {
                    walls.add(new int[] { r, c });
                }
            }
        }
        Collections.shuffle(walls);

        for (int[] wall : walls) {
            int r = wall[0], c = wall[1];

            // Skip border cells (double-check)
            if (r <= 0 || r >= rows - 1 || c <= 0 || c >= cols - 1)
                continue;

            // Count open orthogonal neighbours
            if (openNeighbours(maze, r, c) < 2)
                continue;

            // Skip if removing would create a 2×2 full-open block
            if (wouldCreate2x2Open(maze, r, c))
                continue;

            // Random chance gate
            if (random.nextDouble() >= BRAID_CHANCE)
                continue;

            // Remove the wall
            maze.setTile(r, c, TileType.NORMAL);
        }
    }

    /**
     * Returns the count of orthogonally adjacent cells that are NOT walls.
     */
    private static int openNeighbours(Maze maze, int r, int c) {
        int count = 0;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] d : dirs) {
            if (maze.getTile(r + d[0], c + d[1]) != TileType.WALL)
                count++;
        }
        return count;
    }

    /**
     * Returns true if converting (r, c) to open would create any 2×2 block
     * where all four cells are non-wall. Checks all four 2×2 quadrants that
     * include the candidate cell.
     */
    private static boolean wouldCreate2x2Open(Maze maze, int r, int c) {
        // Temporarily treat (r,c) as open for the check
        int[][] quadrantOffsets = {
                { 0, 0 }, // top-left corner of 2×2 is (r, c)
                { 0, -1 }, // top-right corner
                { -1, 0 }, // bottom-left corner
                { -1, -1 } // bottom-right corner
        };
        for (int[] off : quadrantOffsets) {
            int tr = r + off[0];
            int tc = c + off[1];
            // Check 2×2 block with top-left at (tr, tc)
            if (isOpenOrCandidate(maze, tr, tc, r, c)
                    && isOpenOrCandidate(maze, tr, tc + 1, r, c)
                    && isOpenOrCandidate(maze, tr + 1, tc, r, c)
                    && isOpenOrCandidate(maze, tr + 1, tc + 1, r, c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if (r2,c2) is non-wall, OR if it is the candidate cell being
     * tested.
     */
    private static boolean isOpenOrCandidate(Maze maze, int r2, int c2,
            int candR, int candC) {
        if (r2 == candR && c2 == candC)
            return true; // candidate — treat as open
        if (!maze.inBounds(r2, c2))
            return false; // out of bounds — not open
        return maze.getTile(r2, c2) != TileType.WALL;
    }

    // ── Default start / goal ──────────────────────────────────────────────────

    public static Point getDefaultStart() {
        return new Point(1, 1);
    }

    public static Point getDefaultGoal() {
        return new Point(Maze.COLS - 2, Maze.ROWS - 2);
    }
}
