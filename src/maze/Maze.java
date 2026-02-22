package maze;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Maze {

    public static final int COLS = 25;
    public static final int ROWS = 25;
    public static final int TILE_SIZE = 28;

    private TileType[][] grid;
    private boolean[][] visited;
    private List<Point> visitedOrder;
    private long visitedTimestamp[];
    private long[] visitedTimes;

    public Maze() {
        grid = new TileType[ROWS][COLS];
        visited = new boolean[ROWS][COLS];
        visitedOrder = new ArrayList<>();
        visitedTimes = new long[ROWS * COLS];
        initEmpty();
    }

    private void initEmpty() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = TileType.NORMAL;
            }
        }
    }

    public void setTile(int row, int col, TileType type) {
        if (inBounds(row, col)) {
            grid[row][col] = type;
        }
    }

    public TileType getTile(int row, int col) {
        if (!inBounds(row, col))
            return TileType.WALL;
        return grid[row][col];
    }

    public int getCost(int row, int col) {
        if (!inBounds(row, col))
            return Integer.MAX_VALUE / 2;
        return grid[row][col].getCost();
    }

    public boolean isWall(int row, int col) {
        if (!inBounds(row, col))
            return true;
        return grid[row][col] == TileType.WALL;
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public void resetVisited() {
        visited = new boolean[ROWS][COLS];
        visitedOrder = new ArrayList<>();
        visitedTimes = new long[ROWS * COLS];
    }

    public void markVisited(int row, int col) {
        if (inBounds(row, col) && !visited[row][col]) {
            visited[row][col] = true;
            visitedOrder.add(new Point(col, row));
            visitedTimes[visitedOrder.size() - 1] = System.currentTimeMillis();
        }
    }

    public boolean isVisited(int row, int col) {
        return inBounds(row, col) && visited[row][col];
    }

    public List<Point> getVisitedOrder() {
        return new ArrayList<>(visitedOrder);
    }

    public long getVisitedTime(int index) {
        if (index < visitedTimes.length)
            return visitedTimes[index];
        return 0;
    }

    public int getRows() {
        return ROWS;
    }

    public int getCols() {
        return COLS;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    public List<Point> getNeighbors(int row, int col) {
        List<Point> neighbors = new ArrayList<>();
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] d : dirs) {
            int nr = row + d[0];
            int nc = col + d[1];
            if (inBounds(nr, nc) && !isWall(nr, nc)) {
                neighbors.add(new Point(nc, nr));
            }
        }
        return neighbors;
    }
}
