package ui;

import algorithms.PathFinder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Right-side statistics panel.
 * Features: icon stat cards with animated counting, 6-col comparison table
 * with best (green) / worst (red) highlighting.
 */
public class StatsPanel extends JPanel {

    // ── Stat cards ────────────────────────────────────────────────────────────
    private final StatCard algoCard;
    private final StatCard nodesCard;
    private final StatCard pathCard;
    private final StatCard costCard;
    private final StatCard timeCard;
    private final StatCard optCard;

    // ── Status label ──────────────────────────────────────────────────────────
    private final JLabel statusLabel;

    // ── Animated counting ─────────────────────────────────────────────────────
    private double animNodes = 0, targetNodes = 0;
    private double animPath = 0, targetPath = 0;
    private double animCost = 0, targetCost = 0;
    private double animTime = 0, targetTime = 0;
    private final javax.swing.Timer animTimer;

    // ── Comparison table data ─────────────────────────────────────────────────
    private static final String[] ALGO_NAMES = { "A*", "BFS", "Dijkstra", "DFS" };
    private final int[] tblNodes = new int[4];
    private final int[] tblPath = new int[4];
    private final long[] tblTime = new long[4];
    private final int[] tblCost = new int[4];
    private final boolean[] tblOptimal = { true, true, true, false };
    private final boolean[] tblHasRun = new boolean[4];

    // Table cell references
    private final JLabel[] tblNodesLbl = new JLabel[4];
    private final JLabel[] tblPathLbl = new JLabel[4];
    private final JLabel[] tblTimeLbl = new JLabel[4];
    private final JLabel[] tblCostLbl = new JLabel[4];
    private final JPanel[] tblRows = new JPanel[4];

    public StatsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(20, 20, 36));
        setBorder(new EmptyBorder(16, 12, 16, 12));
        setPreferredSize(new Dimension(230, 0));

        // Title
        JLabel title = new JLabel("STATISTICS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(120, 210, 255));
        title.setAlignmentX(CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(10));

        // Status
        statusLabel = new JLabel("Idle — press START");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(new Color(150, 190, 255));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        JPanel statusCard = buildCard();
        statusCard.add(statusLabel);
        add(statusCard);
        add(Box.createVerticalStrut(10));

        // Stat cards (2-column grid)
        algoCard = new StatCard("Algorithm", IconType.ALGO, new Color(120, 200, 255));
        nodesCard = new StatCard("Nodes", IconType.NODES, new Color(100, 200, 140));
        pathCard = new StatCard("Path Steps", IconType.PATH, new Color(200, 160, 255));
        costCard = new StatCard("Total Cost", IconType.COST, new Color(255, 170, 80));
        timeCard = new StatCard("Time (ms)", IconType.TIME, new Color(255, 220, 80));
        optCard = new StatCard("Optimal?", IconType.OPT, new Color(100, 230, 160));

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

        // Comparison table
        JLabel tblTitle = new JLabel("ALGORITHM COMPARISON");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tblTitle.setForeground(new Color(150, 150, 200));
        tblTitle.setAlignmentX(LEFT_ALIGNMENT);
        add(tblTitle);
        add(Box.createVerticalStrut(5));
        add(buildComparisonTable());

        // Animation timer
        animTimer = new javax.swing.Timer(18, e -> tickAnimation());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public void setRunning(PathFinder finder, int pathLen, int nodes, long ms, int cost) {
        statusLabel.setText("⟳ Pathfinding...");
        statusLabel.setForeground(new Color(100, 200, 255));

        algoCard.setValue(finder.getName());
        optCard.setValue(finder.isOptimal() ? "✓ Yes" : "✗ No");

        // Kick animated counting from 0 → target
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
            refreshTable();
        }
    }

    public void showNoPath(PathFinder finder) {
        statusLabel.setText("✗ No Path Found");
        statusLabel.setForeground(new Color(255, 80, 80));
        algoCard.setValue(finder.getName());
        nodesCard.setValue(String.valueOf(finder.getNodesExplored()));
        pathCard.setValue("—");
        costCard.setValue("—");
        timeCard.setValue(finder.getExecutionTimeMs() + " ms");
        optCard.setValue("—");
        animTimer.stop();
    }

    public void reset() {
        animTimer.stop();
        statusLabel.setText("Idle — press START");
        statusLabel.setForeground(new Color(150, 190, 255));
        algoCard.setValue("—");
        nodesCard.setValue("—");
        pathCard.setValue("—");
        costCard.setValue("—");
        timeCard.setValue("—");
        optCard.setValue("—");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animation
    // ─────────────────────────────────────────────────────────────────────────

    private void tickAnimation() {
        double k = 0.17;
        animNodes = approach(animNodes, targetNodes, k);
        animPath = approach(animPath, targetPath, k);
        animCost = approach(animCost, targetCost, k);
        animTime = approach(animTime, targetTime, k);

        nodesCard.setValue(String.valueOf((int) Math.round(animNodes)));
        pathCard.setValue(String.valueOf((int) Math.round(animPath)));
        costCard.setValue(String.valueOf((int) Math.round(animCost)));
        timeCard.setValue((int) Math.round(animTime) + " ms");

        boolean allDone = Math.abs(animNodes - targetNodes) < 0.5
                && Math.abs(animPath - targetPath) < 0.5
                && Math.abs(animCost - targetCost) < 0.5
                && Math.abs(animTime - targetTime) < 0.5;
        if (allDone) {
            nodesCard.setValue(String.valueOf((int) targetNodes));
            pathCard.setValue(String.valueOf((int) targetPath));
            costCard.setValue(String.valueOf((int) targetCost));
            timeCard.setValue((int) targetTime + " ms");
            animTimer.stop();
        }
    }

    private double approach(double current, double target, double k) {
        return current + (target - current) * k;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comparison table builder + refresh
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildComparisonTable() {
        // 5 rows (header + 4 algos) × 5 cols
        JPanel table = new JPanel(new GridLayout(5, 5, 1, 1));
        table.setBackground(new Color(15, 15, 28));
        table.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 80)));
        table.setAlignmentX(LEFT_ALIGNMENT);

        // Header
        String[] headers = { "Algo", "Nodes", "Steps", "ms", "Cost" };
        for (String h : headers) {
            table.add(tableHeader(h));
        }

        // Data rows
        Color[] rowColors = {
                new Color(28, 28, 46),
                new Color(24, 24, 40)
        };
        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new GridLayout(1, 5, 1, 0));
            row.setBackground(rowColors[i % 2]);
            tblRows[i] = row;

            row.add(tableCell(ALGO_NAMES[i], new Color(190, 190, 230), true));
            tblNodesLbl[i] = tableCell("—", new Color(120, 200, 160), false);
            tblPathLbl[i] = tableCell("—", new Color(180, 150, 255), false);
            tblTimeLbl[i] = tableCell("—", new Color(240, 210, 80), false);
            tblCostLbl[i] = tableCell("—", new Color(255, 170, 80), false);
            row.add(tblNodesLbl[i]);
            row.add(tblPathLbl[i]);
            row.add(tblTimeLbl[i]);
            row.add(tblCostLbl[i]);
            table.add(row);
        }

        return table;
    }

    private void refreshTable() {
        // Find best (min cost) and worst (max cost) among run algos
        int bestIdx = -1, worstIdx = -1;
        int minCost = Integer.MAX_VALUE, maxCost = Integer.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            if (!tblHasRun[i])
                continue;
            if (tblCost[i] < minCost) {
                minCost = tblCost[i];
                bestIdx = i;
            }
            if (tblCost[i] > maxCost) {
                maxCost = tblCost[i];
                worstIdx = i;
            }
        }

        Color baseEven = new Color(28, 28, 46);
        Color baseOdd = new Color(24, 24, 40);
        Color bestColor = new Color(20, 60, 35);
        Color worstColor = new Color(60, 20, 20);

        for (int i = 0; i < 4; i++) {
            if (tblHasRun[i]) {
                tblNodesLbl[i].setText(String.valueOf(tblNodes[i]));
                tblPathLbl[i].setText(String.valueOf(tblPath[i]));
                tblTimeLbl[i].setText(tblTime[i] + "ms");
                tblCostLbl[i].setText(String.valueOf(tblCost[i]));
            }

            // Row background highlight
            Color rowBg;
            if (i == bestIdx) {
                rowBg = bestColor;
                tblCostLbl[i].setForeground(new Color(100, 255, 140));
            } else if (i == worstIdx && bestIdx != worstIdx) {
                rowBg = worstColor;
                tblCostLbl[i].setForeground(new Color(255, 100, 100));
            } else {
                rowBg = (i % 2 == 0) ? baseEven : baseOdd;
                tblCostLbl[i].setForeground(new Color(255, 170, 80));
            }
            tblRows[i].setBackground(rowBg);
            for (Component c : tblRows[i].getComponents()) {
                c.setBackground(rowBg);
                if (c instanceof JLabel lbl)
                    lbl.setOpaque(true);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper builders
    // ─────────────────────────────────────────────────────────────────────────

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

    private JLabel tableHeader(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(200, 200, 240));
        l.setBackground(new Color(35, 35, 60));
        l.setOpaque(true);
        return l;
    }

    private JLabel tableCell(String text, Color fg, boolean bold) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, 10));
        l.setForeground(fg);
        l.setOpaque(true);
        return l;
    }

    private int algoIndex(String name) {
        for (int i = 0; i < ALGO_NAMES.length; i++) {
            if (ALGO_NAMES[i].equals(name))
                return i;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // StatCard — inner class with icon, label, and value
    // ─────────────────────────────────────────────────────────────────────────

    private enum IconType {
        ALGO, NODES, PATH, COST, TIME, OPT
    }

    private class StatCard extends JPanel {
        private final String key;
        private final Color iconColor;
        private final IconType iconType;
        private String value = "—";

        StatCard(String key, IconType iconType, Color iconColor) {
            this.key = key;
            this.iconType = iconType;
            this.iconColor = iconColor;
            setOpaque(false);
            setPreferredSize(new Dimension(100, 60));
        }

        void setValue(String v) {
            this.value = v;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Drop shadow
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRoundRect(3, 3, w - 2, h - 2, 10, 10);

            // Card BG
            g2.setColor(new Color(32, 32, 52));
            g2.fillRoundRect(0, 0, w - 3, h - 3, 10, 10);

            // Border
            g2.setColor(new Color(55, 55, 88));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 4, h - 4, 10, 10);

            // ── Icon (16x16 at top-left) ──────────────────────────────────────
            int ix = 8, iy = 8, is = 12;
            g2.setColor(iconColor);
            switch (iconType) {
                case NODES -> {
                    g2.fillOval(ix + 2, iy + 2, is - 4, is - 4);
                    g2.setColor(iconColor.darker());
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

            // ── Key text ──────────────────────────────────────────────────────
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(130, 130, 175));
            g2.drawString(key, ix + is + 4, iy + 9);

            // ── Value text ────────────────────────────────────────────────────
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            g2.setColor(new Color(210, 235, 255));
            g2.drawString(value, 8, h - 10);

            g2.dispose();
        }
    }
}
