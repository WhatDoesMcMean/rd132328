package me.kalmemarq.util;

public enum Direction {
    DOWN(0, 0, -1, 0),
    UP(1, 0, 1, 0),
    NORTH(2, 0, 0, -1),
    SOUTH(3, 0, 0, 1),
    WEST(4, -1, 0, 0),
    EAST(5,  1, 0, 0);

    public final int index;
    public final int normalX;
    public final int normalY;
    public final int normalZ;

    Direction(int index, int normalX, int normalY, int normalZ) {
        this.index = index;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }
}
