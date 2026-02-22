package engine;

import java.awt.event.*;
import java.util.*;

/**
 * Centralized input handler for keyboard and mouse events.
 */
public class InputHandler implements KeyListener, MouseListener {

    private final Set<Integer> heldKeys = new HashSet<>();
    private final Set<Integer> pressedThisFrame = new HashSet<>();

    @Override
    public void keyPressed(KeyEvent e) {
        heldKeys.add(e.getKeyCode());
        pressedThisFrame.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        heldKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public boolean isKeyHeld(int keyCode) {
        return heldKeys.contains(keyCode);
    }

    public boolean wasKeyPressedThisFrame(int keyCode) {
        return pressedThisFrame.contains(keyCode);
    }

    public void clearFrameState() {
        pressedThisFrame.clear();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
