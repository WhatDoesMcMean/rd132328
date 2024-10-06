package me.kalmemarq.render.vertex;

import me.kalmemarq.util.MathUtils;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class BufferBuilder {
    private final long pointer;
    private int cursor;
    private int vertexCount;

    public BufferBuilder(long pointer) {
        this.pointer = pointer;
    }

    public void begin() {
        this.cursor = 0;
        this.vertexCount = 0;
    }

    public BufferBuilder vertex(Matrix4f matrix, float x, float y, float z) {
        float tX = MathUtils.transformXByMatrix(matrix, x, y, z);
        float tY = MathUtils.transformYByMatrix(matrix, x, y, z);
        float tZ = MathUtils.transformZByMatrix(matrix, x, y, z);
        return this.vertex(tX, tY, tZ);
    }

    public BufferBuilder vertex(float x, float y, float z) {
        MemoryUtil.memPutFloat(this.pointer + this.cursor, x);
        MemoryUtil.memPutFloat(this.pointer + this.cursor + 4, y);
        MemoryUtil.memPutFloat(this.pointer + this.cursor + 8, z);
        this.cursor += 12;
        this.vertexCount++;
        return this;
    }

    public BufferBuilder normal(float x, float y, float z) {
        MemoryUtil.memPutByte(this.pointer + this.cursor, (byte) (x * 127.0f));
        MemoryUtil.memPutByte(this.pointer + this.cursor + 1, (byte) (y * 127.0f));
        MemoryUtil.memPutByte(this.pointer + this.cursor + 2, (byte) (z * 127.0f));
        this.cursor += 3;
        return this;
    }

    public BufferBuilder uv(float u, float v) {
        MemoryUtil.memPutFloat(this.pointer + this.cursor, u);
        MemoryUtil.memPutFloat(this.pointer + this.cursor + 4, v);
        this.cursor += 8;
        return this;
    }

    public BufferBuilder color(float r, float g, float b) {
        return this.color(r, g, b, 1.0f);
    }

    public BufferBuilder color(float r, float g, float b, float a) {
        return this.color((int) (r * 255.0f), (int) (g * 255.0f), (int) (b * 255.0f), (int) (a * 255.0f));
    }

    public BufferBuilder color(int r, int g, int b, int a) {
        MemoryUtil.memPutByte(this.pointer + this.cursor, (byte) r);
        MemoryUtil.memPutByte(this.pointer + this.cursor + 1, (byte) g);
        MemoryUtil.memPutByte(this.pointer + this.cursor + 2, (byte) b);
        MemoryUtil.memPutByte(this.pointer + this.cursor + 3, (byte) a);
        this.cursor += 4;
        return this;
    }

    public int end() {
        return this.vertexCount;
    }
}
