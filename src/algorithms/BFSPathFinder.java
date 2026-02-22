package algorithms;

import maze.Maze;
import java.awt.Point;
import java.util.*;

/**
 * Breadth-First Search pathfinder.
 * Behavior: explores uniformly outward, guaranteed shortest path (hop count).
 * Great for unweighted grids.
 */
public class BFSPathFinder implements PathFinder {

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

        Queue<Point> queue = new ArrayDeque<>();
        queue.offer(start);
        seen[start.y][start.x] = true;

        while (!queue.isEmpty()) {
            Point current = queue.poll();
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
                    queue.offer(neighbor);
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
        return "BFS";
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
        return true;
    }
}
