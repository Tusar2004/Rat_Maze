package ui;

import algorithms.PathFinder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Right-side statistics panel.
 * Features:
 * - Graphical stat cards with animated counting + mini horizontal progress bar
 * - Graphical bar-chart comparison panel (grouped vertical bars per metric)
 * - Best row highlighted in green, worst in red
 */
public class StatsPanel extends JPanel {

    // ── Stat cards ────────────────────────────────────────────────────
    private final StatCard algoCard;
    private final StatCard nodesCard;
    private final StatCard pathCard;
    private final StatCard costCard;
    private final StatCard timeCard;
    private final StatCard optCard;

    // ── Status label ──────────────────────────────────────────────────
    private final JLabel statusLabel;

    // ── Animated counting ─────────────────────────────────────────────
    private double animNodes = 0, targetNodes = 0;
    private double animPath = 0, targetPath = 0;
    private double animCost = 0, targetCost = 0;
    private double animTime = 0, targetTime = 0;
    private final javax.swing.Timer animTimer;

    // ── Running max values for bar scaling ────────────────────────────
    private double maxNodes = 1, maxPath = 1, maxCost = 1, maxTime = 1;

    // ── Comparison chart data ─────────────────────────────────────────
    private static final String[] ALGO_NAMES = { "A*", "BFS", "Dijkstra", "DFS" };
    private static final Color[] ALGO_COLORS = {
            new Color(100, 200, 255),
            new Color(100, 230, 150),
            new Color(255, 180, 80),
            new Color(220, 100, 255)
    };

    private final int[] tblNodes = new int[4];
    private final int[] tblPath = new int[4];
    private final long[] tblTime = new long[4];
    private final int[] tblCost = new int[4];
    private final boolean[] tblHasRun = new boolean[4];

    private final AlgoChartPanel chartPanel;

    // ─────────────────────────────────────────────────────────────────
    public StatsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(20, 20, 36));
        setBorder(new EmptyBorder(16, 12, 16, 12));
        setPreferredSize(new Dimension(245, 0));

        // Title
        JLabel title = new JLabel("STATISTICS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(120, 210, 255));
        title.setAlignmentX(CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(10));

        // Status badge
        statusLabel = new JLabel("Idle — press START");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(new Color(150, 190, 255));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        JPanel statusCard = buildCard();
        statusCard.add(statusLabel);
        add(statusCard);
        add(Box.createVerticalStrut(10));

        // Stat cards (2-column grid)
        algoCard = new StatCard("Algorithm", IconType.ALGO, new Color(120, 200, 255), false);
        nodesCard = new StatCard("Nodes", IconType.NODES, new Color(100, 200, 140), true);
        pathCard = new StatCard("Path Steps", IconType.PATH, new Color(200, 160, 255), true);
        costCard = new StatCard("Total Cost", IconType.COST, new Color(255, 170, 80), true);
        timeCard = new StatCard("Time (ms)", IconType.TIME, new Color(255, 220, 80), true);
        optCard = new StatCard("Optimal?", IconType.OPT, new Color(100, 230, 160), false);

        JPanel cardGrid = new JPanel(new GridLayout(3, 2, 5, 5));
        cardGrid.setBackground(new Color(20, 20, 36));
        cardGrid.setAlignmentX(LEFT_ALIGNMENT);
        cardGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        cardGrid.add(algoCard);
        cardGrid.add(nodesCard);
        cardGrid.add(pathCard);
        cardGrid.add(costCard);
        cardGrid.add(timeCard);
        cardGrid.add(optCard);
        add(cardGrid);
        add(Box.createVerticalStrut(12));

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 90));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        add(sep);
        add(Box.createVerticalStrut(10));

        // Chart title
        JLabel tblTitle = new JLabel("ALGORITHM COMPARISON");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tblTitle.setForeground(new Color(150, 150, 200));
        tblTitle.setAlignmentX(LEFT_ALIGNMENT);
        add(tblTitle);
        add(Box.createVerticalStrut(6));

        // Graphical bar-chart panel
        chartPanel = new AlgoChartPanel();
        chartPanel.setAlignmentX(LEFT_ALIGNMENT);
        chartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        add(chartPanel);

        // Legend row
        add(Box.createVerticalStrut(8));
        add(buildLegend());

        // Animation timer
        animTimer = new javax.swing.Timer(18, e -> tickAnimation());
    }

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    public void setRunning(PathFinder finder, int pathLen, int nodes, long ms, int cost) {
        statusLabel.setText("⟳ Pathfinding...");
        statusLabel.setForeground(new Color(100, 200, 255));

        algoCard.setValue(finder.getName(), 0, 1);
        optCard.setValue(finder.isOptimal() ? "✓ Yes" : "✗ No", 0, 1);

        // Update max values for bar scaling
        maxNodes = Math.max(maxNodes, nodes);
        maxPath = Math.max(maxPath, pathLen);
        maxCost = Math.max(maxCost, cost);
        maxTime = Math.max(maxTime, ms);

        // Kick animated counting 0 → target
        targetNodes = nodes;
        animNodes = 0;
        targetPath = pathLen;
        animPath = 0;
        targetCost = cost;
        animCost = 0;
        targetTime = ms;
        animTime = 0;
        animTimer.restart();
    }

    public void showArrived(PathFinder finder, int pathLen, int cost) {
        statusLabel.setText("✓ Goal Reached!");
        statusLabel.setForeground(new Color(80, 255, 130));
        int idx = algoIndex(finder.getName());
        if (idx >= 0) {
            tblNodes[idx] = finder.getNodesExplored();
            tblPath[idx] = pathLen;
            tblTime[idx] = finder.getExecutionTimeMs();
            tblCost[idx] = cost;
            tblHasRun[idx] = true;
            chartPanel.repaint();
        }
    }

    public void showNoPath(PathFinder finder) {
        statusLabel.setText("✗ No Path Found");
        statusLabel.setForeground(new Color(255, 80, 80));
        algoCard.setValue(finder.getName(), 0, 1);
        nodesCard.setValue(String.valueOf(finder.getNodesExplored()),
                finder.getNodesExplored(), maxNodes);
        pathCard.setValue("—", 0, 1);
        costCard.setValue("—", 0, 1);
        timeCard.setValue(finder.getExecutionTimeMs() + " ms",
                finder.getExecutionTimeMs(), maxTime);
        optCard.setValue("—", 0, 1);
        animTimer.stop();
    }

    public void reset() {
        animTimer.stop();
        maxNodes = 1;
        maxPath = 1;
        maxCost = 1;
        maxTime = 1;
        statusLabel.setText("Idle — press START");
        statusLabel.setForeground(new Color(150, 190, 255));
        algoCard.setValue("—", 0, 1);
        nodesCard.setValue("—", 0, 1);
        pathCard.setValue("—", 0, 1);
        costCard.setValue("—", 0, 1);
        timeCard.setValue("—", 0, 1);
        optCard.setValue("—", 0, 1);
    }

    // ─────────────────────────────────────────────────────────────────
    // Animation tick
    // ─────────────────────────────────────────────────────────────────

    private void tickAnimation() {
        double k = 0.17;
        animNodes = approach(animNodes, targetNodes, k);
        animPath = approach(animPath, targetPath, k);
        animCost = approach(animCost, targetCost, k);
        animTime = approach(animTime, targetTime, k);

        nodesCard.setValue(String.valueOf((int) Math.round(animNodes)),
                animNodes, maxNodes);
        pathCard.setValue(String.valueOf((int) Math.round(animPath)),
                animPath, maxPath);
        costCard.setValue(String.valueOf((int) Math.round(animCost)),
                animCost, maxCost);
        timeCard.setValue((int) Math.round(animTime) + " ms",
                animTime, maxTime);

        boolean allDone = Math.abs(animNodes - targetNodes) < 0.5 &&
                Math.abs(animPath - targetPath) < 0.5 &&
                Math.abs(animCost - targetCost) < 0.5 &&
                Math.abs(animTime - targetTime) < 0.5;
        if (allDone) {
            nodesCard.setValue(String.valueOf((int) targetNodes), targetNodes, maxNodes);
            pathCard.setValue(String.valueOf((int) targetPath), targetPath, maxPath);
            costCard.setValue(String.valueOf((int) targetCost), targetCost, maxCost);
            timeCard.setValue((int) targetTime + " ms", targetTime, maxTime);
            animTimer.stop();
        }
    }

    private double approach(double cur, double tgt, double k) {
        return cur + (tgt - cur) * k;
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper builders
    // ─────────────────────────────────────────────────────────────────

    private JPanel buildCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(30, 30, 50));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 55, 90)),
                new EmptyBorder(7, 10, 7, 10)));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    /** Small legend row below chart */
    private JPanel buildLegend() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        row.setBackground(new Color(20, 20, 36));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        for (int i = 0; i < ALGO_NAMES.length; i++) {
            JPanel swatch = new JPanel();
            swatch.setBackground(ALGO_COLORS[i]);
            swatch.setPreferredSize(new Dimension(10, 10));
            row.add(swatch);
            JLabel lbl = new JLabel(ALGO_NAMES[i]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            lbl.setForeground(ALGO_COLORS[i]);
            row.add(lbl);
        }
        return row;
    }

    private int algoIndex(String name) {
        for (int i = 0; i < ALGO_NAMES.length; i++)
            if (ALGO_NAMES[i].equals(name))
                return i;
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────
    // AlgoChartPanel — graphical grouped bar chart
    // ─────────────────────────────────────────────────────────────────

    private class AlgoChartPanel extends JPanel {

        private static final String[] METRICS = { "Nodes", "Steps", "ms", "Cost" };

        AlgoChartPanel() {
            setBackground(new Color(18, 18, 32));
            setBorder(BorderFactory.createLineBorder(new Color(50, 50, 80)));
            setPreferredSize(new Dimension(0, 170));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int padL = 6, padR = 6, padT = 10, padB = 22;
            int chartW = W - padL - padR;
            int chartH = H - padT - padB;

            // Subtle grid lines
            g2.setColor(new Color(40, 40, 65));
            for (int row = 0; row <= 4; row++) {
                int y = padT + chartH - row * chartH / 4;
                g2.drawLine(padL, y, padL + chartW, y);
            }

            // Gather raw values for each metric
            double[][] raw = new double[4][4]; // [metric][algo]
            for (int a = 0; a < 4; a++) {
                if (!tblHasRun[a])
                    continue;
                raw[0][a] = tblNodes[a];
                raw[1][a] = tblPath[a];
                raw[2][a] = tblTime[a];
                raw[3][a] = tblCost[a];
            }

            // Max per metric
            double[] maxM = new double[4];
            for (int m = 0; m < 4; m++) {
                maxM[m] = 1;
                for (int a = 0; a < 4; a++)
                    if (tblHasRun[a])
                        maxM[m] = Math.max(maxM[m], raw[m][a]);
            }

            // Find best & worst algo by cost (metric 3)
            int bestIdx = -1, worstIdx = -1;
            double minC = Double.MAX_VALUE, maxC = Double.MIN_VALUE;
            for (int a = 0; a < 4; a++) {
                if (!tblHasRun[a])
                    continue;
                if (raw[3][a] < minC) {
                    minC = raw[3][a];
                    bestIdx = a;
                }
                if (raw[3][a] > maxC) {
                    maxC = raw[3][a];
                    worstIdx = a;
                }
            }

            int metricGroupW = chartW / 4;
            int barW = Math.max(2, metricGroupW / 5 - 1);

            for (int m = 0; m < 4; m++) {
                int groupX = padL + m * metricGroupW;

                for (int a = 0; a < 4; a++) {
                    if (!tblHasRun[a])
                        continue;

                    double fraction = (maxM[m] > 0) ? raw[m][a] / maxM[m] : 0;
                    int barH = (int) (fraction * chartH);
                    int bx = groupX + a * (barW + 1) + 2;
                    int by = padT + chartH - barH;

                    // Bar glow
                    Color base = ALGO_COLORS[a];
                    Color glow = new Color(base.getRed(), base.getGreen(), base.getBlue(), 50);
                    g2.setColor(glow);
                    g2.fillRoundRect(bx - 1, by - 1, barW + 2, barH + 2, 3, 3);

                    // Bar fill gradient (taller = brighter top)
                    Color dark = base.darker().darker();
                    Color bright = (a == bestIdx && m == 3) ? new Color(80, 255, 140)
                            : (a == worstIdx && m == 3 && bestIdx != worstIdx)
                                    ? new Color(255, 80, 80)
                                    : base;
                    g2.setPaint(new GradientPaint(bx, by, bright, bx, by + barH, dark));
                    g2.fillRoundRect(bx, by, barW, barH, 3, 3);

                    // Value label on top if bar is tall enough
                    if (barH > 14) {
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 7));
                        g2.setColor(Color.WHITE);
                        String val = barValueStr(m, a);
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = bx + (barW - fm.stringWidth(val)) / 2;
                        g2.drawString(val, tx, by + 10);
                    }
                }

                // Metric label below the x-axis
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.setColor(new Color(160, 160, 210));
                FontMetrics fm = g2.getFontMetrics();
                String lbl = METRICS[m];
                g2.drawString(lbl, groupX + (metricGroupW - fm.stringWidth(lbl)) / 2,
                        padT + chartH + 12);
            }

            // Baseline
            g2.setColor(new Color(80, 80, 115));
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            g2.dispose();
        }

        private String barValueStr(int metric, int algo) {
            return switch (metric) {
                case 0 -> String.valueOf(tblNodes[algo]);
                case 1 -> String.valueOf(tblPath[algo]);
                case 2 -> tblTime[algo] + "ms";
                case 3 -> String.valueOf(tblCost[algo]);
                default -> "";
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // StatCard — enhanced with mini progress bar
    // ─────────────────────────────────────────────────────────────────

    private enum IconType {
        ALGO, NODES, PATH, COST, TIME, OPT
    }

    private class StatCard extends JPanel {
        private final String key;
        private final Color accentColor;
        private final IconType iconType;
        private final boolean showBar;
        private String value = "—";
        private double barFill = 0.0; // 0.0 – 1.0

        StatCard(String key, IconType iconType, Color accentColor, boolean showBar) {
            this.key = key;
            this.iconType = iconType;
            this.accentColor = accentColor;
            this.showBar = showBar;
            setOpaque(false);
            setPreferredSize(new Dimension(110, 68));
        }

        void setValue(String v, double current, double max) {
            this.value = v;
            this.barFill = (max > 0 && showBar) ? Math.min(1.0, current / max) : 0.0;
            repaint();
        }

        // Kept for non-bar cards (algo, opt)
        void setValue(String v) {
            setValue(v, 0, 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Drop shadow
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRoundRect(3, 3, w - 2, h - 2, 10, 10);

            // Card background
            g2.setColor(new Color(32, 32, 52));
            g2.fillRoundRect(0, 0, w - 3, h - 3, 10, 10);

            // Card border
            g2.setColor(new Color(55, 55, 88));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 4, h - 4, 10, 10);

            // ── Icon ──────────────────────────────────────────────────
            int ix = 8, iy = 8, is = 12;
            g2.setColor(accentColor);
            drawIcon(g2, iconType, ix, iy, is, accentColor);

            // ── Key label ─────────────────────────────────────────────
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(130, 130, 175));
            g2.drawString(key, ix + is + 4, iy + 9);

            // ── Value text ────────────────────────────────────────────
            int valueY = showBar ? h - 22 : h - 10;
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(new Color(210, 235, 255));
            g2.drawString(value, 8, valueY);

            // ── Progress bar ──────────────────────────────────────────
            if (showBar) {
                int barX = 6, barY = h - 14, barW = w - 12, barH = 6;
                // Track
                g2.setColor(new Color(20, 20, 38));
                g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
                // Fill
                int fillW = (int) (barW * barFill);
                if (fillW > 0) {
                    Color dark = accentColor.darker().darker();
                    g2.setPaint(new GradientPaint(barX, 0, accentColor,
                            barX + fillW, 0, dark));
                    g2.fillRoundRect(barX, barY, fillW, barH, 4, 4);

                    // Glow overlay
                    g2.setColor(new Color(accentColor.getRed(),
                            accentColor.getGreen(), accentColor.getBlue(), 60));
                    g2.fillRoundRect(barX, barY, fillW, barH / 2, 3, 3);
                }
                // Border
                g2.setColor(new Color(60, 60, 90));
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(barX, barY, barW, barH, 4, 4);
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Icon painter (extracted for clarity)
    // ─────────────────────────────────────────────────────────────────

    private void drawIcon(Graphics2D g2, IconType type, int ix, int iy, int is, Color color) {
        g2.setColor(color);
        switch (type) {
            case NODES -> {
                g2.fillOval(ix + 2, iy + 2, is - 4, is - 4);
                g2.setColor(color.darker());
                g2.setStroke(new BasicStroke(1.2f));
                for (int a = 0; a < 360; a += 60) {
                    double rad = Math.toRadians(a);
                    int ex = (int) (ix + is / 2 + Math.cos(rad) * (is / 2.0));
                    int ey = (int) (iy + is / 2 + Math.sin(rad) * (is / 2.0));
                    g2.drawLine(ix + is / 2, iy + is / 2, ex, ey);
                }
            }
            case PATH -> {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ix, iy + is / 2, ix + is, iy + is / 2);
                int[] xp = { ix + is - 5, ix + is, ix + is - 5 };
                int[] yp = { iy + is / 2 - 4, iy + is / 2, iy + is / 2 + 4 };
                g2.fillPolygon(xp, yp, 3);
            }
            case COST -> {
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                g2.drawString("W", ix, iy + is);
            }
            case TIME -> {
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(ix, iy, is, is);
                g2.drawLine(ix + is / 2, iy + 2, ix + is / 2, iy + is / 2);
                g2.drawLine(ix + is / 2, iy + is / 2, ix + is - 2, iy + is / 2);
            }
            case OPT -> {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ix + 1, iy + is / 2, ix + is / 3 + 1, iy + is - 2);
                g2.drawLine(ix + is / 3, iy + is - 2, ix + is, iy + 2);
            }
            case ALGO -> {
                int[] xs = { ix + is / 2, ix + is, ix, ix };
                int[] ys = { iy, iy + is * 3 / 4, iy + is * 3 / 4, iy + is };
                g2.fillPolygon(xs, ys, 4);
            }
        }
    }
}
