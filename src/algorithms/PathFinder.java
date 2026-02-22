package algorithms;

import java.awt.Point;
import java.util.List;
import maze.Maze;

/**
 * Interface for all pathfinding algorithms.
 * Each implementation must return a List of Points (col, row) representing
 * the path from start to goal. Visited nodes must be marked on the Maze object
 * for visualization. Algorithm must NOT block — it runs synchronously and the
 * game loop handles animation separately.
 */
public interface PathFinder {

    /**
     * Finds a path from start to goal within the maze.
     *
     * @param maze  the maze (will have markVisited called on explored nodes)
     * @param start start tile (x=col, y=row)
     * @param goal  goal tile (x=col, y=row)
     * @return ordered list of tile Points from start to goal (inclusive), or empty
     *         if no path
     */
    List<Point> findPath(Maze maze, Point start, Point goal);

    /**
     * Returns human-readable name of this algorithm.
     */
    String getName();

    /**
     * Returns number of nodes explored in the last call to findPath().
     */
    int getNodesExplored();

    /**
     * Returns execution time in milliseconds of the last call to findPath().
     */
    long getExecutionTimeMs();

    /**
     * Returns whether the path found is guaranteed optimal for the given maze.
     */
    boolean isOptimal();
}
