package algorithms;

import maze.Maze;
import java.awt.Point;
import java.util.*;

/**
 * Dijkstra's algorithm pathfinder.
 * Considers tile weights (NORMAL=1, MUD=5, WATER=10).
 * Optimal weighted shortest path.
 */
public class DijkstraPathFinder implements PathFinder {

    private int nodesExplored = 0;
    private long execTimeMs = 0;

    @Override
    public List<Point> findPath(Maze maze, Point start, Point goal) {
        long t0 = System.currentTimeMillis();
        nodesExplored = 0;
        maze.resetVisited();

        Map<Point, Integer> dist = new HashMap<>();
        Map<Point, Point> parentMap = new HashMap<>();
        PriorityQueue<PointCost> pq = new PriorityQueue<>(Comparator.comparingInt(pc -> pc.cost));

        dist.put(start, 0);
        pq.offer(new PointCost(start, 0));

        boolean found = false;

        while (!pq.isEmpty()) {
            PointCost pc = pq.poll();
            Point current = pc.point;
            int curCost = pc.cost;

            if (curCost > dist.getOrDefault(current, Integer.MAX_VALUE))
                continue;

            maze.markVisited(current.y, current.x);
            nodesExplored++;

            if (current.equals(goal)) {
                found = true;
                break;
            }

            for (Point neighbor : maze.getNeighbors(current.y, current.x)) {
                int tileCost = maze.getCost(neighbor.y, neighbor.x);
                int newCost = curCost + tileCost;
                if (newCost < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    dist.put(neighbor, newCost);
                    parentMap.put(neighbor, current);
                    pq.offer(new PointCost(neighbor, newCost));
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

    private static class PointCost {
        Point point;
        int cost;

        PointCost(Point p, int c) {
            point = p;
            cost = c;
        }
    }

    @Override
    public String getName() {
        return "Dijkstra";
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
