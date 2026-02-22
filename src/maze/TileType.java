package maze;

public enum TileType {
    WALL(0, "WALL"),
    NORMAL(1, "NORMAL"),
    MUD(5, "MUD"),
    WATER(10, "WATER");

    private final int cost;
    private final String label;

    TileType(int cost, String label) {
        this.cost = cost;
        this.label = label;
    }

    public int getCost() {
        return cost;
    }

    public String getLabel() {
        return label;
    }
}
