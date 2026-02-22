package entities;

import java.awt.Graphics2D;
import java.awt.Point;

public abstract class Entity {

    protected float x; // pixel x
    protected float y; // pixel y
    protected int tileCol;
    protected int tileRow;
    protected int tileSize;

    public Entity(int tileCol, int tileRow, int tileSize) {
        this.tileCol = tileCol;
        this.tileRow = tileRow;
        this.tileSize = tileSize;
        this.x = tileCol * tileSize;
        this.y = tileRow * tileSize;
    }

    public abstract void update(double deltaTime);

    public abstract void render(Graphics2D g2d);

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getTileCol() {
        return tileCol;
    }

    public int getTileRow() {
        return tileRow;
    }

    public void setTilePosition(int col, int row) {
        this.tileCol = col;
        this.tileRow = row;
        this.x = col * tileSize;
        this.y = row * tileSize;
    }

    public Point getTilePoint() {
        return new Point(tileCol, tileRow);
    }
}
