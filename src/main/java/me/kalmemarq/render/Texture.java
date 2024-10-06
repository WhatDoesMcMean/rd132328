package me.kalmemarq.render;

import me.kalmemarq.util.IOUtils;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Texture implements Closeable {
    private final int id;

    public Texture() {
        this.id = GL45.glCreateTextures(GL30.GL_TEXTURE_2D);
    }

    public void load(Path path) {
        ByteBuffer pixelsBuffer;
        try {
            pixelsBuffer = IOUtils.readInputStreamToByteBuffer(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture: " + path, e);
        }

        GL45.glTextureParameteri(this.id, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GL45.glTextureParameteri(this.id, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL45.glTextureParameteri(this.id, GL30.GL_TEXTURE_WRAP_S, GL30.GL_REPEAT);
        GL45.glTextureParameteri(this.id, GL30.GL_TEXTURE_WRAP_T, GL30.GL_REPEAT);

        if (pixelsBuffer == null) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer wP = stack.mallocInt(1);
            IntBuffer hP = stack.mallocInt(1);
            IntBuffer cP = stack.mallocInt(1);

            ByteBuffer imageData = STBImage.stbi_load_from_memory(pixelsBuffer, wP, hP, cP, 4);
            if (imageData != null) {
                int width = wP.get(0);
                int height = hP.get(0);

                GL45.glTextureStorage2D(this.id, 1, GL30.GL_RGBA8, width, height);
                GL45.glTextureSubImage2D(this.id, 0, 0, 0, width, height, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, imageData);

                STBImage.stbi_image_free(imageData);
            }
        } finally {
            MemoryUtil.memFree(pixelsBuffer);
        }
    }

    public int getId() {
        return this.id;
    }

    public void bind(int unit) {
        GL45.glBindTextureUnit(unit, this.id);
    }

    @Override
    public void close() {
        GL30.glDeleteTextures(this.id);
    }
}
