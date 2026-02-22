package entities;

import java.awt.*;
import java.awt.geom.*;

public class Cheese extends Entity {

    private float bouncePhase = 0;
    private float pulsePhase = 0;
    private float glowPhase = 0;
    private boolean celebrating = false;
    private float celebrationTimer = 0;
    private float spinAngle = 0;

    public Cheese(int tileCol, int tileRow, int tileSize) {
        super(tileCol, tileRow, tileSize);
    }

    public void startCelebration() {
        celebrating = true;
        celebrationTimer = 0;
    }

    @Override
    public void update(double deltaTime) {
        bouncePhase += deltaTime * 2.1;
        pulsePhase += deltaTime * 3.4;
        glowPhase += deltaTime * 1.8;
        if (celebrating) {
            celebrationTimer += deltaTime;
            spinAngle += (float) (deltaTime * 380);
            if (celebrationTimer > 2.5) {
                celebrating = false;
                spinAngle = 0;
            }
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        float cx = x + tileSize / 2f;
        float cy = y + tileSize / 2f;

        float bob = (float) (Math.sin(bouncePhase) * 2.0);
        float pulse = (float) (Math.sin(pulsePhase) * 0.06 + 0.94);
        float sz = tileSize * 0.42f * pulse;

        cy += bob;

        // ── Outer glow ────────────────────────────────────────────────────────
        float gr = (float) (Math.sin(glowPhase) * 0.5 + 0.5);
        float glowR = sz * 2.2f + gr * 7;
        g2d.setPaint(new RadialGradientPaint(cx, cy, glowR,
                new float[] { 0f, 1f },
                new Color[] {
                        new Color(255, 215, 0, (int) (70 + gr * 50)),
                        new Color(255, 215, 0, 0)
                }));
        g2d.fillOval((int) (cx - glowR), (int) (cy - glowR), (int) (glowR * 2), (int) (glowR * 2));

        // ── Draw in local coords ──────────────────────────────────────────────
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        if (celebrating)
            g.rotate(Math.toRadians(spinAngle));
        g.rotate(0.18); // gentle wedge tilt

        float hw = sz * 1.15f; // half-width
        float hh = sz * 0.95f; // half-height

        // ── Cheese wedge (triangle) ───────────────────────────────────────────
        Path2D.Float wedge = new Path2D.Float();
        wedge.moveTo(hw, 0); // right tip
        wedge.lineTo(-hw, -hh); // top-left
        wedge.lineTo(-hw, hh); // bottom-left
        wedge.closePath();

        // Gradient: bright yellow tip → amber base
        g.setPaint(new GradientPaint(-hw, -hh, new Color(255, 248, 100),
                hw, hh, new Color(205, 138, 10)));
        g.fill(wedge);

        // ── Top-edge highlight sheen ──────────────────────────────────────────
        Path2D.Float sheen = new Path2D.Float();
        sheen.moveTo(hw * 0.85f, 0);
        sheen.lineTo(-hw * 0.75f, -hh * 0.78f);
        sheen.lineTo(-hw * 0.75f, -hh * 0.5f);
        sheen.lineTo(hw * 0.55f, 0);
        sheen.closePath();
        g.setColor(new Color(255, 255, 220, 85));
        g.fill(sheen);

        // ── Cheese holes ──────────────────────────────────────────────────────
        g.setColor(new Color(165, 105, 8, 185));
        g.fillOval((int) (-hw * 0.58f), (int) (-hh * 0.52f), (int) (hh * 0.46f), (int) (hh * 0.38f));
        g.fillOval((int) (-hw * 0.12f), (int) (hh * 0.12f), (int) (hh * 0.34f), (int) (hh * 0.28f));
        g.fillOval((int) (-hw * 0.68f), (int) (hh * 0.32f), (int) (hh * 0.24f), (int) (hh * 0.20f));

        // Inner hole rims (lighter edge)
        g.setColor(new Color(230, 170, 30, 90));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval((int) (-hw * 0.58f), (int) (-hh * 0.52f), (int) (hh * 0.46f), (int) (hh * 0.38f));
        g.drawOval((int) (-hw * 0.12f), (int) (hh * 0.12f), (int) (hh * 0.34f), (int) (hh * 0.28f));
        g.drawOval((int) (-hw * 0.68f), (int) (hh * 0.32f), (int) (hh * 0.24f), (int) (hh * 0.20f));

        // ── Outline ───────────────────────────────────────────────────────────
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(155, 95, 5, 210));
        g.draw(wedge);

        g.dispose();
    }

    public void reset(int col, int row) {
        tileCol = col;
        tileRow = row;
        x = col * tileSize;
        y = row * tileSize;
        celebrating = false;
        celebrationTimer = 0;
        spinAngle = 0;
    }
}
