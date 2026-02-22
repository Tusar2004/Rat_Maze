package ui;

import algorithms.*;
import engine.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private final GamePanel gamePanel;
    private final GameLoop gameLoop;

    private final JLabel fpsLabel;

    private final PathFinder[] finders = {
            new AStarPathFinder(),
            new BFSPathFinder(),
            new DijkstraPathFinder(),
            new DFSPathFinder()
    };

    public ControlPanel(GamePanel gamePanel, GameLoop gameLoop) {
        this.gamePanel = gamePanel;
        this.gameLoop = gameLoop;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(20, 20, 36));
        setBorder(new EmptyBorder(18, 12, 18, 12));
        setPreferredSize(new Dimension(195, 0));

        addTitle();
        gap(14);
        addAlgoSelector();
        gap(12);
        addSpeedControl();
        gap(16);
        addButtons();
        gap(18);
        addLegend();
        gap(10);
        fpsLabel = styledLabel("FPS: --", new Color(80, 80, 120), 11);
        fpsLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(fpsLabel);

        // FPS refresh timer
        new javax.swing.Timer(400, e -> {
            if (gameLoop != null)
                fpsLabel.setText(String.format("FPS: %.0f", gameLoop.getCurrentFps()));
        }).start();
    }

    private void addTitle() {
        JLabel t = styledLabel("ALGORITHM RAT", new Color(110, 215, 255), 15);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setAlignmentX(CENTER_ALIGNMENT);
        add(t);

        JLabel sub = styledLabel("Pathfinding Visualizer", new Color(130, 130, 175), 11);
        sub.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        sub.setAlignmentX(CENTER_ALIGNMENT);
        add(sub);
    }

    private void addAlgoSelector() {
        add(sectionHeader("Algorithm"));
        gap(5);

        String[] names = { "A*  — Optimal (heuristic)", "BFS — Shortest hops", "Dijkstra (weighted)",
                "DFS — Deep explorer" };
        JComboBox<String> combo = new JComboBox<>(names);
        combo.setBackground(new Color(32, 32, 52));
        combo.setForeground(new Color(200, 220, 255));
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setBorder(BorderFactory.createLineBorder(new Color(65, 65, 105)));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        combo.addActionListener(e -> {
            gamePanel.setAlgorithm(finders[combo.getSelectedIndex()]);
            gamePanel.reset();
        });
        add(combo);
    }

    private void addSpeedControl() {
        add(sectionHeader("Rat Speed"));
        gap(4);

        JSlider slider = new JSlider(40, 280, 120);
        slider.setBackground(new Color(20, 20, 36));
        slider.setForeground(new Color(160, 160, 210));
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(60);
        slider.setSnapToTicks(false);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel valLabel = styledLabel("120 px/s", new Color(120, 190, 255), 11);
        valLabel.setAlignmentX(CENTER_ALIGNMENT);

        slider.addChangeListener(e -> valLabel.setText(slider.getValue() + " px/s"));

        add(slider);
        add(valLabel);
    }

    private void addButtons() {
        add(makeButton("▶  START", new Color(35, 180, 90), new Color(20, 130, 55),
                e -> gamePanel.startGame()));
        gap(8);
        add(makeButton("↺  RESET", new Color(65, 130, 210), new Color(40, 85, 155),
                e -> gamePanel.reset()));
        gap(8);
        add(makeButton("⚙  NEW MAZE", new Color(195, 90, 35), new Color(145, 55, 18),
                e -> gamePanel.regenerateMaze()));
    }

    private void addLegend() {
        add(sectionHeader("Tile Legend"));
        gap(5);
        add(legendRow(new Color(34, 34, 52), "Floor   (cost  1)"));
        add(legendRow(new Color(88, 57, 28), "Mud     (cost  5)"));
        add(legendRow(new Color(0, 90, 175), "Water   (cost 10)"));
        add(legendRow(new Color(42, 42, 65), "Wall"));
        add(legendRow(new Color(40, 120, 220, 160), "Visited (glow)"));
        add(legendRow(new Color(80, 255, 120, 180), "Path trail"));
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private JLabel sectionHeader(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(155, 155, 205));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel styledLabel(String text, Color fg, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, size));
        l.setForeground(fg);
        return l;
    }

    private JButton makeButton(String text, Color bg, Color bgDark, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? bg.brighter() : getModel().isPressed() ? bgDark.darker() : bg;
                // Shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 10, 10);
                // Button face
                g2.setPaint(new GradientPaint(0, 0, c.brighter(), 0, getHeight(), c.darker()));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 10, 10);
                // Highlight edge
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JPanel legendRow(Color swatch, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
        row.setBackground(new Color(20, 20, 36));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JPanel sw = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(swatch);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                g2.setColor(swatch.darker());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
                g2.dispose();
            }
        };
        sw.setPreferredSize(new Dimension(13, 13));
        sw.setOpaque(false);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lbl.setForeground(new Color(150, 150, 190));

        row.add(sw);
        row.add(lbl);
        return row;
    }

    private void gap(int h) {
        add(Box.createVerticalStrut(h));
    }
}
