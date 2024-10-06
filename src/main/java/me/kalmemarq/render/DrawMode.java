package me.kalmemarq.render;

import org.lwjgl.opengl.GL30;

public enum DrawMode {
    LINES(GL30.GL_LINES),
    TRIANGLES(GL30.GL_TRIANGLES),
    QUADS(GL30.GL_TRIANGLES);

    public final int glEnum;

    DrawMode(int glEnum) {
        this.glEnum = glEnum;
    }

    public int getIndexCount(int vertexCount) {
        return switch (this) {
            case LINES, TRIANGLES -> vertexCount;
            case QUADS -> (vertexCount / 2) * 3;
        };
    }
}
