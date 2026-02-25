package algorithms;

import java.awt.Point;
import java.util.*;
import maze.Maze;

public class AStarPathFinder implements PathFinder {

    private int nodesExplored = 0;
    private long execTimeMs = 0;

    @Override
    public List<Point> findPath(Maze maze, Point start, Point goal) {
        long t0 = System.currentTimeMillis();
        nodesExplored = 0;
        maze.resetVisited();

        Map<Point, Integer> gScore = new HashMap<>();
        Map<Point, Point> parentMap = new HashMap<>();
        PriorityQueue<PointFScore> openSet = new PriorityQueue<>(Comparator.comparingInt(p -> p.fScore));

        gScore.put(start, 0);
        openSet.offer(new PointFScore(start, heuristic(start, goal)));

        Set<Point> closed = new HashSet<>();
        boolean found = false;

        while (!openSet.isEmpty()) {
            PointFScore current = openSet.poll();
            Point cur = current.point;

            if (closed.contains(cur))
                continue;
            closed.add(cur);

            maze.markVisited(cur.y, cur.x);
            nodesExplored++;

            if (cur.equals(goal)) {
                found = true;
                break;
            }

            for (Point neighbor : maze.getNeighbors(cur.y, cur.x)) {
                if (closed.contains(neighbor))
                    continue;
                int tileCost = maze.getCost(neighbor.y, neighbor.x);
                int tentativeG = gScore.getOrDefault(cur, Integer.MAX_VALUE) + tileCost;
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    gScore.put(neighbor, tentativeG);
                    parentMap.put(neighbor, cur);
                    int f = tentativeG + heuristic(neighbor, goal);
                    openSet.offer(new PointFScore(neighbor, f));
                }
            }
        }

        execTimeMs = System.currentTimeMillis() - t0;
        if (!found)
            return Collections.emptyList();
        return reconstructPath(parentMap, start, goal);
    }

    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
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

    private static class PointFScore {
        Point point;
        int fScore;

        PointFScore(Point p, int f) {
            point = p;
            fScore = f;
        }
    }

    @Override
    public String getName() {
        return "A*";
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
