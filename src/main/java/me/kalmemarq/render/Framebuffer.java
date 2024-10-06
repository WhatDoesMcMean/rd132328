package me.kalmemarq.render;

import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.render.vertex.VertexBuffer;
import me.kalmemarq.render.vertex.VertexLayout;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class Framebuffer implements Closeable {
    private int fbo = -1;
    private int colorAttachmentTxr;
    private int depthTxr;

    private int colorAttachmentRbo;
    private int depthRbo;

    private int width;
    private int height;

    private final AttachmentMode colorAttachmentMode;
    private final AttachmentMode depthAttachmentMode;

    private final Shader blitShader;
    private final VertexBuffer vertexBuffer;

    public Framebuffer(int width, int height) {
        this.colorAttachmentMode = AttachmentMode.TEXTURE;
        this.depthAttachmentMode = AttachmentMode.RENDERBUFFER;

        this.blitShader = new Shader("blit");
        this.vertexBuffer = new VertexBuffer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(VertexLayout.POS_UV.stride * 4);
            BufferBuilder builder = new BufferBuilder(MemoryUtil.memAddress(buffer));
            builder.vertex(-1, -1, 0).uv(0, 0);
            builder.vertex(-1, 1, 0).uv(0, 1);
            builder.vertex(1, 1, 0).uv(1, 1);
            builder.vertex(1, -1, 0).uv(1, 0);
            this.vertexBuffer.upload(DrawMode.QUADS, VertexLayout.POS_UV, buffer, builder.end());
        }

        this.resize(width, height);
    }

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            width = 800;
            height = 600;
        }

        if (this.width == width && this.height == height) {
            return;
        } else {
            System.out.println("Framebuffer resized");
        }

        this.width = width;
        this.height = height;

        if (this.fbo != -1) {
            this.dispose();
        }

        this.fbo = GL45.glCreateFramebuffers();

        if (this.colorAttachmentMode == AttachmentMode.RENDERBUFFER) {
            this.colorAttachmentRbo = GL45.glCreateRenderbuffers();
            GL45.glNamedRenderbufferStorage(this.colorAttachmentRbo, GL30.GL_RGB8, width, height);
            GL45.glNamedFramebufferRenderbuffer(this.fbo, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, this.colorAttachmentRbo);
        } else {
            this.colorAttachmentTxr = GL45.glCreateTextures(GL30.GL_TEXTURE_2D);
            GL45.glTextureParameteri(this.colorAttachmentTxr, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
            GL45.glTextureParameteri(this.colorAttachmentTxr, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
            GL45.glTextureParameteri(this.colorAttachmentTxr, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL45.glTextureParameteri(this.colorAttachmentTxr, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GL45.glTextureStorage2D(this.colorAttachmentTxr, 1, GL30.GL_RGB8, width, height);
            GL45.glNamedFramebufferTexture(this.fbo, GL30.GL_COLOR_ATTACHMENT0, this.colorAttachmentTxr, 0);
        }

        if (this.depthAttachmentMode == AttachmentMode.RENDERBUFFER) {
            this.depthRbo = GL45.glCreateRenderbuffers();
            GL45.glNamedRenderbufferStorage(this.depthRbo, GL30.GL_DEPTH_COMPONENT, width, height);
            GL45.glNamedFramebufferRenderbuffer(this.fbo, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, this.depthRbo);
        } else {
            this.depthTxr = GL45.glCreateTextures(GL30.GL_TEXTURE_2D);
            GL45.glTextureParameteri(this.depthTxr, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
            GL45.glTextureParameteri(this.depthTxr, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
            GL45.glTextureParameteri(this.depthTxr, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL45.glTextureParameteri(this.depthTxr, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GL45.glTextureParameteri(this.depthTxr, GL30.GL_TEXTURE_COMPARE_MODE, 0);
            GL45.glTextureStorage2D(this.depthTxr, 1, GL30.GL_DEPTH_COMPONENT, width, height);
            GL45.glNamedFramebufferTexture(this.fbo, GL30.GL_DEPTH_ATTACHMENT, this.depthTxr, 0);
        }

        int status = GL45.glCheckNamedFramebufferStatus(this.fbo, GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            String errorMessage = switch (status) {
                case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "Incomplete framebuffer: one or more attachments are incomplete.";
                case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "Incomplete framebuffer: no attachments found.";
                case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "Incomplete framebuffer: draw buffer is incomplete.";
                case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "Incomplete framebuffer: read buffer is incomplete.";
                case GL30.GL_FRAMEBUFFER_UNSUPPORTED -> "Framebuffer configuration is unsupported.";
                case GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "Incomplete framebuffer: multisample settings are inconsistent.";
                default -> "Unknown framebuffer status error: " + status;
            };
            System.out.println(errorMessage);
        }
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo);
    }

    public void blitTo(int target, int targetX, int targetY, int targetWidth, int targetHeight) {
        GL45.glBlitNamedFramebuffer(this.fbo, target, 0, 0, this.width, this.height, targetX, targetY, targetWidth, targetHeight, GL45.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
    }

    public void draw() {
        this.blitShader.bind();
        GL45.glBindTextureUnit(0, this.colorAttachmentTxr);
        this.blitShader.setUniform("uSampler0", 0);
        this.vertexBuffer.bind();
        this.vertexBuffer.draw();
    }

    private void dispose() {
        GL30.glDeleteFramebuffers(this.fbo);
        if (this.colorAttachmentMode == AttachmentMode.RENDERBUFFER) {
            GL30.glDeleteRenderbuffers(this.colorAttachmentRbo);
        } else {
            GL30.glDeleteTextures(this.colorAttachmentTxr);
        }
        if (this.depthAttachmentMode == AttachmentMode.RENDERBUFFER) {
            GL30.glDeleteRenderbuffers(this.depthRbo);
        } else {
            GL30.glDeleteTextures(this.depthTxr);
        }
    }

    public int getHandle() {
        return this.fbo;
    }

    @Override
    public void close() {
        this.blitShader.close();
        this.vertexBuffer.close();
        this.dispose();
    }

    public enum AttachmentMode {
        TEXTURE,
        RENDERBUFFER
    }
}
