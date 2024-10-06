package me.kalmemarq.render;

import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.render.vertex.VertexBuffer;
import me.kalmemarq.render.vertex.VertexLayout;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Tessellator implements Closeable {
    public static Tessellator instance;
    private final ByteBuffer buffer;
    private final BufferBuilder bufferBuilder;
    private final Map<VertexLayout, VertexBuffer> vertexBuffers;
    private DrawMode mode;
    private VertexLayout layout;

    public Tessellator(int capacity) {
        this.buffer = MemoryUtil.memAlloc(capacity);
        this.vertexBuffers = new HashMap<>();
        this.bufferBuilder = new BufferBuilder(MemoryUtil.memAddress(this.buffer));
    }

    public static Tessellator getInstance() {
        if (instance == null) {
            instance = new Tessellator((12 + 8 + 4 + 3) * 4 * 5000);
        }
        return instance;
    }

    public static void cleanup() {
        if (instance != null) {
            instance.close();
        }
    }

    public void begin(DrawMode mode, VertexLayout vertexLayout) {
        this.mode = mode;
        this.layout = vertexLayout;
        this.bufferBuilder.begin();
    }

    public BufferBuilder getBufferBuilder() {
        return this.bufferBuilder;
    }

    public void draw() {
        int vertexCount = this.bufferBuilder.end();
        VertexBuffer vertexBuffer = this.vertexBuffers.computeIfAbsent(this.layout, (key) -> new VertexBuffer());
        vertexBuffer.bind();
        vertexBuffer.upload(this.mode, this.layout, MemoryUtil.memSlice(this.buffer, 0, vertexCount * this.layout.stride), vertexCount);
        vertexBuffer.draw();
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.buffer);
        this.vertexBuffers.values().forEach(VertexBuffer::close);
    }
}
