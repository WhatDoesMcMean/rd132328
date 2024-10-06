package me.kalmemarq.render.vertex;

import me.kalmemarq.render.DrawMode;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class VertexBuffer implements Closeable {
    private final int vao;
    private final int vbo;
    private final int ibo;
    private DrawMode mode;
    private int arrayBufferCapacity;
    private int capacityIndexCount;
    private int indexCount;
    private VertexLayout layout;
    private IntBuffer indexBuffer;

    public  VertexBuffer() {
        this.vao = GL30.glGenVertexArrays();
        this.vbo = GL30.glGenBuffers();
        this.ibo = GL30.glGenBuffers();
    }

    public void upload(DrawMode mode, VertexLayout layout, ByteBuffer buffer, int vertexCount) {
        this.bind();

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.vbo);
        this.uploadVertexBuffer(layout, buffer, vertexCount);

        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, this.ibo);
        this.uploadIndexBuffer(mode, vertexCount);
    }

    private void uploadVertexBuffer(VertexLayout layout, ByteBuffer buffer, int vertexCount) {
        if (this.layout != null && layout != this.layout) {
            layout.disable();
        }

        int size = layout.stride * vertexCount;
        if (size > this.arrayBufferCapacity) {
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, buffer, GL30.GL_DYNAMIC_DRAW);
            this.arrayBufferCapacity = size;
        } else {
            GL30.glBufferSubData(GL30.GL_ARRAY_BUFFER, 0, buffer);
        }

        if (this.layout == null || layout != this.layout) {
            layout.enable();
            this.layout = layout;
        }
    }

    private void uploadIndexBuffer(DrawMode mode, int vertexCount) {
        int indexCount = mode.getIndexCount(vertexCount);
        if (this.mode != mode || indexCount > this.capacityIndexCount) {
            boolean firstTime = false;
            if (this.indexBuffer == null) {
                firstTime = true;
                this.indexBuffer = MemoryUtil.memAllocInt(indexCount);
            } else {
                this.indexBuffer.position(0);
                this.indexBuffer = MemoryUtil.memRealloc(this.indexBuffer, indexCount);
            }
            if (mode == DrawMode.QUADS) {
                for (int i = 0, j = 0; i < indexCount; i += 6, j += 4) {
                    this.indexBuffer.put(j).put(j + 1).put(j + 2).put(j + 2).put(j + 3).put(j);
                }
            } else {
                for (int i = 0; i < indexCount; ++i) {
                    this.indexBuffer.put(i);
                }
            }
            this.indexBuffer.flip();
            if (indexCount > this.capacityIndexCount || firstTime) {
                GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, GL30.GL_DYNAMIC_DRAW);
            } else {
                GL30.glBufferSubData(GL30.GL_ELEMENT_ARRAY_BUFFER, 0, this.indexBuffer);
            }
            this.capacityIndexCount = this.indexCount;
        }

        this.mode = mode;
        this.indexCount = indexCount;
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public void bind() {
        GL30.glBindVertexArray(this.vao);
    }

    public void draw() {
        this.draw(this.indexCount, 0);
    }

    public void draw(int indexCount, int offset) {
        GL30.glDrawElements(this.mode.glEnum, indexCount, GL30.GL_UNSIGNED_INT, offset);
    }

    @Override
    public void close() {
        GL30.glDeleteVertexArrays(this.vao);
        GL30.glDeleteBuffers(this.vbo);
        GL30.glDeleteBuffers(this.ibo);
        if (this.indexBuffer != null) {
            MemoryUtil.memFree(this.indexBuffer);
        }
    }
}
