package main;

import engine.GameLoop;
import engine.GamePanel;
import ui.ControlPanel;
import ui.StatsPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Application entry point.
 * Wires together GamePanel, GameLoop, ControlPanel, and StatsPanel.
 */
public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Algorithm Rat — 2D AI Pathfinding Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Core game objects
        GamePanel gamePanel = new GamePanel();
        GameLoop gameLoop = new GameLoop(gamePanel);
        StatsPanel statsPanel = new StatsPanel();
        ControlPanel ctrlPanel = new ControlPanel(gamePanel, gameLoop);

        // Wire stats panel into game panel
        gamePanel.setStatsPanel(statsPanel);

        // Layout: [ControlPanel | GamePanel | StatsPanel]
        setLayout(new BorderLayout());
        add(ctrlPanel, BorderLayout.WEST);
        add(gamePanel, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.EAST);

        // Game panel preferred size (25x25 tiles × 28px + a little padding)
        gamePanel.setPreferredSize(new Dimension(25 * 28 + 40, 25 * 28 + 40));

        pack();
        setLocationRelativeTo(null); // center on screen
        setVisible(true);

        // Start the game loop after frame is visible
        gameLoop.start();

        // Request focus for key input
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        // Use system look and feel as base, then override with dark theme
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Dark defaults for any un-overridden Swing components
        UIManager.put("Panel.background", new Color(22, 22, 38));
        UIManager.put("ComboBox.background", new Color(35, 35, 55));
        UIManager.put("ComboBox.foreground", new Color(200, 220, 255));
        UIManager.put("ComboBox.selectionBackground", new Color(60, 80, 140));
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("List.background", new Color(35, 35, 55));
        UIManager.put("List.foreground", new Color(200, 220, 255));
        UIManager.put("List.selectionBackground", new Color(60, 80, 140));
        UIManager.put("ScrollPane.background", new Color(22, 22, 38));

        SwingUtilities.invokeLater(MainFrame::new);
    }
}
