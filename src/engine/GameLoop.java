package engine;

/**
 * Fixed-timestep game loop running at ~60 FPS.
 * Drives update() and render() on the GamePanel.
 * Uses delta time for physics-correct movement.
 */
public class GameLoop implements Runnable {

    private static final int TARGET_FPS = 60;
    private static final long OPTIMAL_TIME = 1_000_000_000L / TARGET_FPS;

    private Thread thread;
    private boolean running = false;
    private final GamePanel panel;

    private double currentFps = 0;

    public GameLoop(GamePanel panel) {
        this.panel = panel;
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long fpsTimer = System.currentTimeMillis();
        int frameCount = 0;

        while (running) {
            long now = System.nanoTime();
            double deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            // Cap delta to avoid spiral of death on lag spikes
            if (deltaTime > 0.05)
                deltaTime = 0.05;

            panel.update(deltaTime);
            panel.repaint();
            frameCount++;

            // FPS counter update every second
            long elapsed = System.currentTimeMillis() - fpsTimer;
            if (elapsed >= 1000) {
                currentFps = frameCount * 1000.0 / elapsed;
                fpsTimer = System.currentTimeMillis();
                frameCount = 0;
            }

            // Sleep to maintain target FPS
            long updateTime = System.nanoTime() - now;
            long sleepTime = (OPTIMAL_TIME - updateTime) / 1_000_000;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public double getCurrentFps() {
        return currentFps;
    }

    public boolean isRunning() {
        return running;
    }
}
