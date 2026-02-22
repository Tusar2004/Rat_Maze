package engine;

import algorithms.*;
import entities.*;
import maze.*;
import ui.*;

import javax.swing.*;
import java.awt.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel {

    private static final int TILE = Maze.TILE_SIZE;

    // ── Game state ────────────────────────────────────────────────────────────
    public enum State {
        IDLE, RUNNING, ARRIVED, NO_PATH
    }

    private State state = State.IDLE;

    // ── Core objects ──────────────────────────────────────────────────────────
    private final Maze maze;
    private final Rat rat;
    private final Cheese cheese;
    private final Camera camera;

    // ── Algorithm + path ──────────────────────────────────────────────────────
    private PathFinder currentFinder;
    private List<Point> currentPath = new ArrayList<>();
    private int lastPathCost = 0;

    // ── Visited flash ─────────────────────────────────────────────────────────
    // Maps point → timestamp when its flash animation began
    private final Map<Point, Long> visitedFlashTimes = new LinkedHashMap<>();
    private static final long FLASH_FADE_MS = 1200;

    // ── Stats panel ref ───────────────────────────────────────────────────────
    private StatsPanel statsPanel;

    // ── Path reveal animation ─────────────────────────────────────────────────
    private float pathRevealT = 0;

    // ── Starfield background (static, pre-computed) ───────────────────────────
    private final float[][] stars;

    // ── Tile surface noise pre-compute ────────────────────────────────────────
    // For mud: 3 "clumps" per tile stored as relative (rx, ry, rw, rh)
    private final float[][][] mudClumps; // [row][col][6 params: x0y0w0h0 x1y1...]

    // ── Start/goal ────────────────────────────────────────────────────────────
    private final Point startTile;
    private final Point goalTile;

    // ── Double buffer ─────────────────────────────────────────────────────────
    private BufferedImage backBuffer;

    // ── Global time ───────────────────────────────────────────────────────────
    private double time = 0;

    public GamePanel() {
        maze = new Maze();
        camera = new Camera();
        startTile = MazeGenerator.getDefaultStart();
        goalTile = MazeGenerator.getDefaultGoal();
        MazeGenerator.generate(maze, startTile, goalTile);

        rat = new Rat(startTile.x, startTile.y, TILE);
        cheese = new Cheese(goalTile.x, goalTile.y, TILE);
        currentFinder = new AStarPathFinder();

        // Pre-compute starfield
        Random rng = new Random(42);
        stars = new float[90][3];
        for (float[] s : stars) {
            s[0] = rng.nextFloat();
            s[1] = rng.nextFloat();
            s[2] = rng.nextFloat() * 1.6f + 0.5f;
        }

        // Pre-compute mud clump noise
        mudClumps = new float[Maze.ROWS][Maze.COLS][12];
        for (int r = 0; r < Maze.ROWS; r++) {
            for (int c = 0; c < Maze.COLS; c++) {
                Random seed = new Random(r * 97L + c * 31L);
                for (int k = 0; k < 12; k++)
                    mudClumps[r][c][k] = seed.nextFloat();
            }
        }

        setBackground(new Color(14, 14, 24));
        setFocusable(true);
    }

    public void setStatsPanel(StatsPanel sp) {
        this.statsPanel = sp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public void setAlgorithm(PathFinder finder) {
        this.currentFinder = finder;
    }

    public void startGame() {
        if (state == State.RUNNING)
            return;

        maze.resetVisited();
        visitedFlashTimes.clear();
        currentPath.clear();
        pathRevealT = 0;
        rat.reset(startTile.x, startTile.y);
        cheese.reset(goalTile.x, goalTile.y);

        long t0 = System.nanoTime();
        List<Point> path = currentFinder.findPath(maze, startTile, goalTile);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

        // Schedule visited flash animations — stagger by 15ms each
        List<Point> visitedOrder = maze.getVisitedOrder();
        long now = System.currentTimeMillis();
        for (int i = 0; i < visitedOrder.size(); i++) {
            visitedFlashTimes.put(visitedOrder.get(i), now + i * 15L);
        }

        if (path.isEmpty()) {
            state = State.NO_PATH;
            if (statsPanel != null)
                statsPanel.showNoPath(currentFinder);
            return;
        }

        // Calculate weighted path cost
        int totalCost = 0;
        for (int i = 1; i < path.size(); i++) {
            totalCost += maze.getCost(path.get(i).y, path.get(i).x);
        }
        lastPathCost = totalCost;

        currentPath = path;
        rat.setPath(currentPath);
        state = State.RUNNING;

        if (statsPanel != null) {
            statsPanel.setRunning(currentFinder, path.size(),
                    currentFinder.getNodesExplored(), elapsedMs, totalCost);
        }
    }

    public void regenerateMaze() {
        MazeGenerator.generate(maze, startTile, goalTile);
        // Re-compute mud clumps for new maze
        for (int r = 0; r < Maze.ROWS; r++) {
            for (int c = 0; c < Maze.COLS; c++) {
                Random seed = new Random(r * 97L + c * 31L + System.currentTimeMillis());
                for (int k = 0; k < 12; k++)
                    mudClumps[r][c][k] = seed.nextFloat();
            }
        }
        reset();
    }

    public void reset() {
        state = State.IDLE;
        maze.resetVisited();
        visitedFlashTimes.clear();
        currentPath.clear();
        pathRevealT = 0;
        rat.reset(startTile.x, startTile.y);
        cheese.reset(goalTile.x, goalTile.y);
        if (statsPanel != null)
            statsPanel.reset();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game loop callbacks
    // ─────────────────────────────────────────────────────────────────────────

    public void update(double dt) {
        time += dt;
        rat.update(dt);
        cheese.update(dt);

        if (state == State.RUNNING && rat.hasArrived() && !rat.isMoving()) {
            state = State.ARRIVED;
            cheese.startCelebration();
            if (statsPanel != null) {
                statsPanel.showArrived(currentFinder, currentPath.size(), lastPathCost);
            }
        }

        if ((state == State.RUNNING || state == State.ARRIVED) && !currentPath.isEmpty()) {
            pathRevealT = Math.min(1f, pathRevealT + (float) (dt * 3.0));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();

        if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
            backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2 = backBuffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark background
        g2.setColor(new Color(14, 14, 24));
        g2.fillRect(0, 0, w, h);
        drawStarfield(g2, w, h);

        // Center the maze
        int mazeW = Maze.COLS * TILE;
        int mazeH = Maze.ROWS * TILE;
        camera.center(mazeW, mazeH, w, h);
        int ox = (int) camera.getOffsetX();
        int oy = (int) camera.getOffsetY();

        g2.translate(ox, oy);

        drawMaze(g2);
        drawVisitedGlow(g2);
        drawPathTrail(g2);
        cheese.render(g2);
        rat.render(g2);
        drawStartGoalMarkers(g2);
        drawMazeBorder(g2, mazeW, mazeH);

        if (state == State.ARRIVED)
            drawArrivalOverlay(g2, mazeW, mazeH);
        if (state == State.NO_PATH)
            drawNoPathOverlay(g2, mazeW, mazeH);

        g2.dispose();
        g.drawImage(backBuffer, 0, 0, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drawing
    // ─────────────────────────────────────────────────────────────────────────

    private void drawStarfield(Graphics2D g2, int w, int h) {
        for (float[] s : stars) {
            float alpha = 0.25f + (float) (Math.sin(time * 0.9 + s[1] * 10) * 0.2);
            g2.setColor(new Color(1f, 1f, 1f, Math.max(0.05f, alpha)));
            int sx = (int) (s[0] * w), sy = (int) (s[1] * h);
            int ss = (int) s[2];
            g2.fillOval(sx, sy, ss, ss);
        }
    }

    private void drawMaze(Graphics2D g2) {
        long t = System.currentTimeMillis();

        for (int r = 0; r < Maze.ROWS; r++) {
            for (int c = 0; c < Maze.COLS; c++) {
                int px = c * TILE, py = r * TILE;
                TileType type = maze.getTile(r, c);

                switch (type) {
                    case WALL -> drawWall(g2, px, py);
                    case NORMAL -> drawFloor(g2, px, py, r, c);
                    case MUD -> drawMud(g2, px, py, r, c);
                    case WATER -> drawWater(g2, px, py, r, c, t);
                }
            }
        }
    }

    /** Dark stone wall with bevel highlight/shadow edges. */
    private void drawWall(Graphics2D g2, int px, int py) {
        // Base fill — gradient
        g2.setPaint(new GradientPaint(px, py, new Color(42, 42, 65),
                px + TILE, py + TILE, new Color(22, 22, 38)));
        g2.fillRect(px, py, TILE, TILE);

        // Inner shadow (dark inset)
        g2.setColor(new Color(12, 12, 22, 120));
        g2.fillRect(px + 2, py + 2, TILE - 4, TILE - 4);

        // Bevel: top & left highlight
        g2.setColor(new Color(80, 80, 115, 160));
        g2.drawLine(px, py, px + TILE - 1, py); // top
        g2.drawLine(px, py, px, py + TILE - 1); // left

        // Bevel: bottom & right shadow
        g2.setColor(new Color(10, 10, 18, 200));
        g2.drawLine(px + TILE - 1, py, px + TILE - 1, py + TILE - 1); // right
        g2.drawLine(px, py + TILE - 1, px + TILE - 1, py + TILE - 1); // bottom

        // Corner specular dot
        g2.setColor(new Color(100, 100, 140, 60));
        g2.fillRect(px + 2, py + 2, 3, 3);
    }

    /** Floor tile: subtle checker + soft gradient. */
    private void drawFloor(Graphics2D g2, int px, int py, int r, int c) {
        boolean even = (r + c) % 2 == 0;
        Color base = even ? new Color(34, 34, 52) : new Color(30, 30, 47);
        g2.setColor(base);
        g2.fillRect(px, py, TILE, TILE);

        // Very subtle inner gradient shading from top-left
        g2.setPaint(new GradientPaint(px, py, new Color(255, 255, 255, 8),
                px + TILE, py + TILE, new Color(0, 0, 0, 15)));
        g2.fillRect(px, py, TILE, TILE);

        // Thin grid line
        g2.setColor(new Color(50, 50, 75, 55));
        g2.drawRect(px, py, TILE, TILE);
    }

    /** Mud tile: brown base with deterministic texture clumps. */
    private void drawMud(Graphics2D g2, int px, int py, int r, int c) {
        g2.setColor(new Color(88, 57, 28));
        g2.fillRect(px, py, TILE, TILE);

        // Gradient fade
        g2.setPaint(new GradientPaint(px, py, new Color(110, 72, 36, 60),
                px + TILE, py + TILE, new Color(60, 36, 12, 60)));
        g2.fillRect(px, py, TILE, TILE);

        // Pre-computed texture clumps
        float[] mc = mudClumps[r][c];
        for (int k = 0; k < 3; k++) {
            int bx = px + 2 + (int) (mc[k * 4] * (TILE - 8));
            int by = py + 2 + (int) (mc[k * 4 + 1] * (TILE - 8));
            int bw = 3 + (int) (mc[k * 4 + 2] * 5);
            int bh = 2 + (int) (mc[k * 4 + 3] * 4);
            g2.setColor(new Color(65, 40, 15, 140));
            g2.fillOval(bx, by, bw, bh);
        }

        // Border
        g2.setColor(new Color(65, 40, 18, 100));
        g2.drawRect(px, py, TILE, TILE);
    }

    /** Water tile: animated color shift + wave shimmer. */
    private void drawWater(Graphics2D g2, int px, int py, int r, int c, long t) {
        double wave = Math.sin(t * 0.0025 + c * 0.55 + r * 0.35);
        double wave2 = Math.sin(t * 0.0018 + c * 0.3 - r * 0.4);

        int blue = (int) (155 + wave * 30);
        int green = (int) (65 + wave2 * 20);
        g2.setColor(new Color(0, Math.max(0, Math.min(255, green)), Math.max(0, Math.min(255, blue))));
        g2.fillRect(px, py, TILE, TILE);

        // Shimmer stripe
        float shimmer = (float) (Math.sin(t * 0.004 + c * 1.2 + r * 0.8) * 0.5 + 0.5);
        g2.setColor(new Color(140, 220, 255, (int) (30 + shimmer * 40)));
        g2.fillRect(px + 2, py + 2, TILE - 4, 3);

        // Ripple lines
        g2.setColor(new Color(80, 180, 240, 50));
        g2.drawLine(px + 3, py + TILE / 2, px + TILE - 3, py + TILE / 2);

        // Border
        g2.setColor(new Color(60, 160, 220, 90));
        g2.drawRect(px, py, TILE, TILE);
    }

    /** Radial glow for visited cells — fades out over time. */
    private void drawVisitedGlow(Graphics2D g2) {
        long now = System.currentTimeMillis();
        for (Map.Entry<Point, Long> entry : visitedFlashTimes.entrySet()) {
            long startMs = entry.getValue();
            if (now < startMs)
                continue;
            long age = now - startMs;
            long totalLife = FLASH_FADE_MS * 3;
            if (age > totalLife)
                continue;

            float alpha;
            if (age < FLASH_FADE_MS) {
                // Fade in
                alpha = (float) age / FLASH_FADE_MS * 0.50f;
            } else {
                // Fade out
                alpha = (1f - (float) (age - FLASH_FADE_MS) / (totalLife - FLASH_FADE_MS)) * 0.28f;
            }
            alpha = Math.max(0, Math.min(1, alpha));

            Point p = entry.getKey();
            float cx = p.x * TILE + TILE / 2f;
            float cy = p.y * TILE + TILE / 2f;
            float gr = TILE * 0.65f;

            g2.setPaint(new RadialGradientPaint(cx, cy, gr,
                    new float[] { 0f, 1f },
                    new Color[] { new Color(0.15f, 0.55f, 1f, alpha),
                            new Color(0.15f, 0.55f, 1f, 0f) }));
            g2.fillRect(p.x * TILE, p.y * TILE, TILE, TILE);
        }
    }

    /** Glowing path trail: wide soft glow layer + thin bright core. */
    private void drawPathTrail(Graphics2D g2) {
        if (currentPath.size() < 2)
            return;
        int totalDraw = Math.max(2, (int) (currentPath.size() * pathRevealT));

        // Glow pass (wider, translucent)
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < totalDraw; i++) {
            Point a = currentPath.get(i - 1);
            Point b = currentPath.get(i);
            float frac = (float) i / currentPath.size();
            int alpha = (int) (50 + 30 * frac);
            g2.setColor(new Color(80, 255, 120, alpha));
            g2.drawLine(a.x * TILE + TILE / 2, a.y * TILE + TILE / 2,
                    b.x * TILE + TILE / 2, b.y * TILE + TILE / 2);
        }
        // Core pass (narrow, bright)
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < totalDraw; i++) {
            Point a = currentPath.get(i - 1);
            Point b = currentPath.get(i);
            float frac = (float) i / currentPath.size();
            int r = (int) (80 + 175 * frac);
            int gr2 = (int) (255 - 60 * frac);
            g2.setColor(new Color(r, gr2, 80, 200));
            g2.drawLine(a.x * TILE + TILE / 2, a.y * TILE + TILE / 2,
                    b.x * TILE + TILE / 2, b.y * TILE + TILE / 2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawStartGoalMarkers(Graphics2D g2) {
        // START
        int sx = startTile.x * TILE + 2, sy = startTile.y * TILE + 2;
        g2.setColor(new Color(0, 200, 100, 90));
        g2.fillRoundRect(sx, sy, TILE - 4, TILE - 4, 5, 5);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(0, 240, 120));
        g2.drawRoundRect(sx, sy, TILE - 4, TILE - 4, 5, 5);
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.setColor(Color.WHITE);
        g2.drawString("S", sx + TILE / 2 - 3, sy + TILE / 2 + 3);

        // GOAL
        int gx = goalTile.x * TILE + 2, gy = goalTile.y * TILE + 2;
        g2.setColor(new Color(255, 200, 0, 70));
        g2.fillRoundRect(gx, gy, TILE - 4, TILE - 4, 5, 5);
        g2.setColor(new Color(255, 220, 0));
        g2.drawRoundRect(gx, gy, TILE - 4, TILE - 4, 5, 5);
        g2.setColor(Color.WHITE);
        g2.drawString("G", gx + TILE / 2 - 3, gy + TILE / 2 + 3);
    }

    private void drawMazeBorder(Graphics2D g2, int mw, int mh) {
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(80, 80, 120, 180));
        g2.drawRect(0, 0, mw, mh);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawArrivalOverlay(Graphics2D g2, int mw, int mh) {
        float pulse = (float) (Math.sin(time * 5.0) * 0.5 + 0.5);
        g2.setColor(new Color(0f, 1f, 0.3f, 0.04f + pulse * 0.03f));
        g2.fillRect(0, 0, mw, mh);
    }

    private void drawNoPathOverlay(Graphics2D g2, int mw, int mh) {
        g2.setColor(new Color(1f, 0f, 0f, 0.10f));
        g2.fillRect(0, 0, mw, mh);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        String msg = "NO PATH FOUND";
        g2.setColor(new Color(255, 70, 70, 210));
        g2.drawString(msg, (mw - fm.stringWidth(msg)) / 2, mh / 2);
    }

    public State getGameState() {
        return state;
    }
}
