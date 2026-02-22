package entities;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class Rat extends Entity {

    // ── Movement ──────────────────────────────────────────────────────────────
    private List<Point> path = new ArrayList<>();
    private int pathIndex = 0;
    private float speed = 120f;
    private boolean moving = false;
    private boolean arrived = false;

    // ── Direction ─────────────────────────────────────────────────────────────
    private double angle = 0;

    // ── Walk animation ────────────────────────────────────────────────────────
    private float walkPhase = 0; // drives leg oscillation
    private float headBob = 0; // vertical head offset while moving

    // ── Idle breathing ────────────────────────────────────────────────────────
    private float breathPhase = 0;
    private float breathScale = 1f;

    // ── Tail sway ─────────────────────────────────────────────────────────────
    private float tailPhase = 0;

    // ── Trail ─────────────────────────────────────────────────────────────────
    private final List<float[]> trail = new ArrayList<>();
    private static final int TRAIL_MAX = 16;

    // ── Arrival bounce ────────────────────────────────────────────────────────
    private float bounceOffset = 0;
    private float bounceVelocity = 0;
    private boolean bouncing = false;

    // ── Glow ──────────────────────────────────────────────────────────────────
    private float glowPhase = 0;

    public Rat(int tileCol, int tileRow, int tileSize) {
        super(tileCol, tileRow, tileSize);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public void setPath(List<Point> path) {
        this.path = path;
        this.pathIndex = 0;
        this.moving = !path.isEmpty();
        this.arrived = false;
        this.bouncing = false;
        this.bounceOffset = 0;
        trail.clear();
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void reset(int col, int row) {
        tileCol = col;
        tileRow = row;
        x = col * tileSize;
        y = row * tileSize;
        path.clear();
        pathIndex = 0;
        moving = false;
        arrived = false;
        bouncing = false;
        bounceOffset = 0;
        trail.clear();
        angle = 0;
        walkPhase = 0;
        breathPhase = 0;
        headBob = 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void update(double deltaTime) {
        glowPhase += deltaTime * 2.8;
        tailPhase += deltaTime * (moving ? 6.0 : 1.5);

        // Idle breathing
        if (!moving) {
            breathPhase += deltaTime * 1.6;
            breathScale = 0.97f + (float) Math.sin(breathPhase) * 0.03f;
        } else {
            breathScale = 1.0f;
        }

        // Arrival bounce physic
        if (bouncing) {
            bounceVelocity += 1000 * deltaTime;
            bounceOffset += bounceVelocity * deltaTime;
            if (bounceOffset >= 0) {
                bounceOffset = 0;
                bounceVelocity = 0;
                bouncing = false;
            }
        }

        if (!moving || path.isEmpty() || pathIndex >= path.size())
            return;

        // Walking animation
        walkPhase += deltaTime * 11.0f;
        headBob = (float) Math.sin(walkPhase * 1.9) * 1.4f;

        // Trail capture
        if (trail.isEmpty() || pixelDist(trail.get(trail.size() - 1), x, y) > 5f) {
            trail.add(new float[] { x, y });
            if (trail.size() > TRAIL_MAX)
                trail.remove(0);
        }

        // Move toward next path tile
        Point target = path.get(pathIndex);
        float targetX = target.x * tileSize;
        float targetY = target.y * tileSize;
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 1.2f) {
            x = targetX;
            y = targetY;
            tileCol = target.x;
            tileRow = target.y;
            pathIndex++;
            if (pathIndex >= path.size()) {
                moving = false;
                arrived = true;
                headBob = 0;
                triggerBounce();
            }
        } else {
            double targetAngle = Math.atan2(dy, dx);
            angle = lerpAngle(angle, targetAngle, Math.min(deltaTime * 14.0, 1.0));
            float step = Math.min(speed * (float) deltaTime, dist);
            x += (dx / dist) * step;
            y += (dy / dist) * step;
        }
    }

    private void triggerBounce() {
        bouncing = true;
        bounceVelocity = -220f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g2d) {
        // ── Fading trail ──────────────────────────────────────────────────────
        for (int i = 0; i < trail.size(); i++) {
            float frac = (float) (i + 1) / trail.size();
            float alpha = frac * 0.28f;
            float sz = tileSize * 0.18f * frac;
            float tx = trail.get(i)[0] + tileSize / 2f - sz / 2;
            float ty = trail.get(i)[1] + tileSize / 2f - sz / 2;
            g2d.setColor(new Color(0.3f, 1f, 0.45f, alpha));
            g2d.fillOval((int) tx, (int) ty, (int) sz, (int) sz);
        }

        float cx = x + tileSize / 2f;
        float cy = y + tileSize / 2f + bounceOffset + headBob;
        float r = tileSize * 0.36f * breathScale;

        // ── Outer glow ────────────────────────────────────────────────────────
        float gr = (float) (Math.sin(glowPhase) * 0.5 + 0.5);
        float glowR = r + 5 + gr * 5;
        g2d.setPaint(new RadialGradientPaint(cx, cy, glowR,
                new float[] { 0f, 1f },
                new Color[] { new Color(50, 220, 100, 65), new Color(50, 220, 100, 0) }));
        g2d.fillOval((int) (cx - glowR), (int) (cy - glowR), (int) (glowR * 2), (int) (glowR * 2));

        // Rotate context to face movement direction
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.rotate(angle);

        // ── Tail (cubic bezier, wagging) ──────────────────────────────────────
        float tw = (float) Math.sin(tailPhase) * r * 0.5f;
        CubicCurve2D tail = new CubicCurve2D.Float(
                -r * 0.9f, 0,
                -r * 1.6f, r * 0.7f + tw,
                -r * 2.1f, -r * 0.4f + tw,
                -r * 2.6f, r * 0.15f + tw * 0.5f);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(130, 190, 140));
        g.draw(tail);

        // ── Walking legs ──────────────────────────────────────────────────────
        if (moving) {
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(30, 140, 60));
            float legAmp = r * 0.48f;
            float fl1 = (float) Math.sin(walkPhase) * legAmp;
            float fl2 = (float) Math.sin(walkPhase + Math.PI) * legAmp;
            float rl1 = (float) Math.sin(walkPhase + Math.PI * 0.6) * legAmp;
            float rl2 = (float) Math.sin(walkPhase + Math.PI * 1.6) * legAmp;
            // front pair
            g.drawLine((int) (r * 0.5f), (int) (-r * 0.25f), (int) (r * 0.5f + fl1 * 0.3f), (int) (r * 0.55f));
            g.drawLine((int) (r * 0.5f), (int) (r * 0.25f), (int) (r * 0.5f + fl2 * 0.3f), (int) (r * 0.55f));
            // rear pair
            g.drawLine((int) (-r * 0.25f), (int) (-r * 0.25f), (int) (-r * 0.25f + rl1 * 0.3f), (int) (r * 0.55f));
            g.drawLine((int) (-r * 0.25f), (int) (r * 0.25f), (int) (-r * 0.25f + rl2 * 0.3f), (int) (r * 0.55f));
        }

        // ── Body ──────────────────────────────────────────────────────────────
        Ellipse2D body = new Ellipse2D.Float(-r, -r * 0.82f, r * 2f, r * 1.64f);
        g.setPaint(new RadialGradientPaint(
                -r * 0.2f, -r * 0.3f, r * 1.15f,
                new float[] { 0f, 0.55f, 1f },
                new Color[] { new Color(210, 255, 210), new Color(65, 200, 90), new Color(20, 115, 45) }));
        g.fill(body);

        // Body surface sheen
        Ellipse2D sheen = new Ellipse2D.Float(-r * 0.5f, -r * 0.65f, r * 0.7f, r * 0.4f);
        g.setColor(new Color(255, 255, 255, 35));
        g.fill(sheen);

        // ── Ears ──────────────────────────────────────────────────────────────
        // Outer ear
        g.setColor(new Color(190, 240, 195));
        g.fillOval((int) (r * 0.5f), (int) (-r * 1.0f), (int) (r * 0.6f), (int) (r * 0.55f));
        g.fillOval((int) (r * 0.5f), (int) (r * 0.45f), (int) (r * 0.6f), (int) (r * 0.55f));
        // Inner ear (pink)
        g.setColor(new Color(255, 165, 175));
        g.fillOval((int) (r * 0.57f), (int) (-r * 0.93f), (int) (r * 0.38f), (int) (r * 0.35f));
        g.fillOval((int) (r * 0.57f), (int) (r * 0.52f), (int) (r * 0.38f), (int) (r * 0.35f));

        // ── Eyes ──────────────────────────────────────────────────────────────
        g.setColor(Color.BLACK);
        g.fillOval((int) (r * 0.52f), (int) (-r * 0.44f), 5, 5);
        g.fillOval((int) (r * 0.52f), (int) (r * 0.22f), 5, 5);
        // Specular
        g.setColor(Color.WHITE);
        g.fillOval((int) (r * 0.61f), (int) (-r * 0.40f), 2, 2);
        g.fillOval((int) (r * 0.61f), (int) (r * 0.26f), 2, 2);

        // ── Nose ──────────────────────────────────────────────────────────────
        g.setColor(new Color(255, 75, 90));
        g.fillOval((int) (r * 0.88f), (int) (-r * 0.13f), 6, 4);
        // Whiskers
        g.setStroke(new BasicStroke(0.8f));
        g.setColor(new Color(200, 230, 200, 180));
        g.drawLine((int) (r * 0.85f), (int) (-r * 0.12f), (int) (r * 1.5f), (int) (-r * 0.22f));
        g.drawLine((int) (r * 0.85f), (int) (-r * 0.08f), (int) (r * 1.5f), (int) (r * 0.02f));
        g.drawLine((int) (r * 0.85f), (int) (r * 0.05f), (int) (r * 1.5f), (int) (r * 0.03f));
        g.drawLine((int) (r * 0.85f), (int) (r * 0.10f), (int) (r * 1.5f), (int) (r * 0.20f));

        // ── Body outline ──────────────────────────────────────────────────────
        g.setStroke(new BasicStroke(1.3f));
        g.setColor(new Color(18, 100, 38, 130));
        g.draw(body);

        g.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private double lerpAngle(double a, double b, double t) {
        double diff = b - a;
        while (diff > Math.PI)
            diff -= 2 * Math.PI;
        while (diff < -Math.PI)
            diff += 2 * Math.PI;
        return a + diff * t;
    }

    private float pixelDist(float[] pt, float px, float py) {
        float dx = px - pt[0], dy = py - pt[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean hasArrived() {
        return arrived;
    }

    public float getSpeed() {
        return speed;
    }
}
