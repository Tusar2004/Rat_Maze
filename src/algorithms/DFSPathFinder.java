package algorithms;

import java.awt.Point;
import java.util.*;
import maze.Maze;

public class DFSPathFinder implements PathFinder {

    private int nodesExplored = 0;
    private long execTimeMs = 0;

    @Override
    public List<Point> findPath(Maze maze, Point start, Point goal) {
        long t0 = System.currentTimeMillis();
        nodesExplored = 0;
        maze.resetVisited();

        Map<Point, Point> parentMap = new HashMap<>();
        boolean[][] seen = new boolean[maze.getRows()][maze.getCols()];
        boolean found = false;

        Deque<Point> stack = new ArrayDeque<>();
        stack.push(start);
        seen[start.y][start.x] = true;

        outer: while (!stack.isEmpty()) {
            Point current = stack.pop();
            maze.markVisited(current.y, current.x);
            nodesExplored++;

            if (current.equals(goal)) {
                found = true;
                break;
            }

            for (Point neighbor : maze.getNeighbors(current.y, current.x)) {
                if (!seen[neighbor.y][neighbor.x]) {
                    seen[neighbor.y][neighbor.x] = true;
                    parentMap.put(neighbor, current);
                    stack.push(neighbor);
                }
            }
        }

        execTimeMs = System.currentTimeMillis() - t0;
        if (!found)
            return Collections.emptyList();
        return reconstructPath(parentMap, start, goal);
    }

    private List<Point> reconstructPath(Map<Point, Point> parentMap, Point start, Point goal) {
        LinkedList<Point> path = new LinkedList<>();
        Point cur = goal;
        while (cur != null && !cur.equals(start)) {
            path.addFirst(cur);
            cur = parentMap.get(cur);
        }
        path.addFirst(start);
        return path;
    }

    @Override
    public String getName() {
        return "DFS";
    }

    @Override
    public int getNodesExplored() {
        return nodesExplored;
    }

    @Override
    public long getExecutionTimeMs() {
        return execTimeMs;
    }

    @Override
    public boolean isOptimal() {
        return false;
    }
}
