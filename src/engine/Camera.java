package engine;

/**
 * Simple camera/viewport for centering the maze on the game panel.
 * Can be extended for zoom/scroll; currently handles offset translation.
 */
public class Camera {

    private float offsetX;
    private float offsetY;

    public Camera() {
        offsetX = 0;
        offsetY = 0;
    }

    /**
     * Centers the camera so the maze is drawn centered within the given panel
     * dimensions.
     */
    public void center(int mazePixelWidth, int mazePixelHeight, int panelWidth, int panelHeight) {
        offsetX = (panelWidth - mazePixelWidth) / 2f;
        offsetY = (panelHeight - mazePixelHeight) / 2f;
        if (offsetX < 0)
            offsetX = 0;
        if (offsetY < 0)
            offsetY = 0;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}
