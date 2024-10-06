package me.kalmemarq.render.vertex;

import org.lwjgl.opengl.GL30;

public class VertexLayout {
    public static final VertexLayout POS = new VertexLayout(Attribute.POSITION);
    public static final VertexLayout POS_UV = new VertexLayout(Attribute.POSITION, Attribute.UV);
    public static final VertexLayout POS_UV_COLOR = new VertexLayout(Attribute.POSITION, Attribute.UV, Attribute.COLOR);

    private final Attribute[] attributes;
    private final int[] offsets;
    public final int stride;

    public VertexLayout(Attribute... attributes) {
        this.attributes = attributes;
        this.offsets = new int[attributes.length];
        int stride = 0;
        for (int i = 0; i < attributes.length; ++i) {
            this.offsets[i] = stride;
            stride += attributes[i].byteLength;
        }
        this.stride = stride;
    }

    public void enable() {
        for (int i = 0; i < this.attributes.length; ++i) {
            Attribute attribute = this.attributes[i];
            GL30.glVertexAttribPointer(i, attribute.size, attribute.glType, attribute.normalized, this.stride, this.offsets[i]);
            GL30.glEnableVertexAttribArray(i);
        }
    }

    public void disable() {
        for (int i = 0; i < this.attributes.length; ++i) {
            GL30.glDisableVertexAttribArray(i);
        }
    }

    public enum Attribute {
        POSITION(3, GL30.GL_FLOAT, 12, false),
        UV(2, GL30.GL_FLOAT, 8, false),
        COLOR(4, GL30.GL_UNSIGNED_BYTE, 4, true),
        NORMAL(3, GL30.GL_BYTE, 3, false);

        public final int size;
        public final int glType;
        public final int byteLength;
        public final boolean normalized;

        Attribute(int size, int glType, int byteLength, boolean normalized) {
            this.size = size;
            this.glType = glType;
            this.byteLength = byteLength;
            this.normalized = normalized;
        }
    }
}
