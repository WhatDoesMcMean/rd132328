package me.kalmemarq.render;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

public abstract class NativeImage implements Closeable {
    protected long pointer;

    protected final PixelFormat pixelFormat;
    protected final int width;
    protected final int height;

    public NativeImage(int width, int height, PixelFormat format) {
        this.width = Math.max(width, 1);
        this.height = Math.max(height, 1);
        this.pixelFormat = format;
    }

    public static NativeImage readFromTexture(int textureId, int width, int height, PixelFormat format) {
        NativeImage image = new SimpleNativeImage(width, height, format);
        GL45.glGetTextureImage(textureId, 0, format.glEnum, GL30.GL_UNSIGNED_BYTE, width * height * format.channelCount, image.pointer);
        return image;
    }

    public void flip(Mirroring mirroring) {
        long lineWidth =  (long) this.width * this.pixelFormat.channelCount;
 
        if (mirroring == Mirroring.VERTICAL || mirroring == Mirroring.BOTH) {
            long tempBuffer = MemoryUtil.nmemAlloc(lineWidth);

            try {
                for (int y = 0; y < this.height / 2; ++y) {
                    long linePointer = this.pointer + (lineWidth * y);
                    long oppositeLinePointer = this.pointer + (lineWidth * (this.height - y - 1));
                    MemoryUtil.memCopy(linePointer, tempBuffer, lineWidth);
                    MemoryUtil.memCopy(oppositeLinePointer, linePointer, lineWidth);
                    MemoryUtil.memCopy(tempBuffer, oppositeLinePointer, lineWidth);
                }
            } finally {
                MemoryUtil.nmemFree(tempBuffer);
            }
        }

        if (mirroring == Mirroring.HORIZONTAL || mirroring == Mirroring.BOTH) {
            byte[] swap = { 0, 0, 0 };

            for (int y = 0; y < this.height; ++y) {
                long linePointer = this.pointer + (lineWidth * y);
            
                long lo = linePointer;
                long hi = linePointer + lineWidth - 1;
            
                while (lo < hi) {
                    swap[0] = MemoryUtil.memGetByte(lo);
                    swap[1] = MemoryUtil.memGetByte(lo + 1);
                    swap[2] = MemoryUtil.memGetByte(lo + 2);
            
                    MemoryUtil.memPutByte(lo, MemoryUtil.memGetByte(hi - 2));
                    MemoryUtil.memPutByte(lo + 1, MemoryUtil.memGetByte(hi - 1));
                    MemoryUtil.memPutByte(lo + 2, MemoryUtil.memGetByte(hi));
            
                    MemoryUtil.memPutByte(hi - 2, swap[0]);
                    MemoryUtil.memPutByte(hi - 1, swap[1]);
                    MemoryUtil.memPutByte(hi, swap[2]);
            
                    lo += 3;
                    hi -= 3;
                }
            }
        }
    }

    public void setPixelAt(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return;
        }
    
        if (this.pixelFormat == PixelFormat.RGBA) {
            MemoryUtil.memPutInt(this.pointer + ((long) y * (long) this.width + (long) x) * ((long) this.pixelFormat.channelCount), color);
            return;
        }
    
        byte red = (byte) ((color >> 16) & 0xFF);
        byte green = (byte) ((color >> 8) & 0xFF);
        byte blue = (byte) (color & 0xFF);
    
        long pixelPointer = this.pointer + ((long) y * (long) this.width + (long) x) * 3L;
        MemoryUtil.memPutByte(pixelPointer, red);
        MemoryUtil.memPutByte(pixelPointer + 1, green);
        MemoryUtil.memPutByte(pixelPointer + 2, blue);
    }
    
    public void fill(int x, int y, int width, int height, ColorSupplier colorSupplier) {
        for (int yy = y; yy < y + height; ++yy) {
            for (int xx = x; xx < x + width; ++xx) {
                this.setPixelAt(xx, yy, colorSupplier.get(xx, yy, width, height));
            }
        }
    }

    public interface ColorSupplier {
        int get(int x, int y, int width, int height);
    
    }
    public void fill(int x, int y, int width, int height, int color) {
        for (int yy = y; yy < y + height; ++yy) {
            for (int xx = x; xx < x + width; ++xx) {
                this.setPixelAt(xx, yy, color);
            }
        }
    }

    public void saveTo(Path path) {
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            STBWriteCallback callback = new STBWriteCallback(channel);
            try {
                if (STBImageWrite.nstbi_write_png_to_func(callback.address(), 0L, this.width, this.height, this.pixelFormat.channelCount, this.pointer, 0) == 0) {
                    return;
                }
          
                if (callback.getException() != null) {
                    System.out.println(callback.getException().getMessage());
                }
            } finally {
                callback.close();
            }
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Override
    abstract public void close();

    public static class SimpleNativeImage extends NativeImage {
        public SimpleNativeImage(int width, int height, PixelFormat format) {
            this(width, height, format, MemoryUtil.nmemAlloc(width * height * (long) format.channelCount));
        }
    
        public SimpleNativeImage(int width, int height, PixelFormat format, long pointer) {
            super(width, height, format);
            this.pointer = pointer;
        }
    
        @Override
        public void close() {
            if (this.pointer != 0L) {
                MemoryUtil.nmemFree(this.pointer);
                this.pointer = 0L;
            }
        }
    }

    public static class StbNativeImage extends NativeImage {
        public StbNativeImage(int width, int height, PixelFormat format, long pointer) {
            super(width, height, format);
            this.pointer = pointer;
        }
    
        @Override
        public void close() {
            if (this.pointer != 0L) {
                STBImage.nstbi_image_free(this.pointer);
                this.pointer = 0L;
            }
        }
    }

    public enum Mirroring {
        HORIZONTAL,
        VERTICAL,
        BOTH;
    }

    public enum PixelFormat {
        RGB(3, GL30.GL_RGB),
        RGBA(4, GL30.GL_RGBA);
    
        public final int channelCount;
        public final int glEnum;
    
        PixelFormat(int channelCount, int glEnum) {
            this.channelCount = channelCount;
            this.glEnum = glEnum;
        }
    }

    static class STBWriteCallback extends STBIWriteCallback {
        private final WritableByteChannel channel;
        private IOException exception;
    
        public STBWriteCallback(WritableByteChannel channel) {
            this.channel = channel;
        }
    
        @Override
        public void invoke(long context, long data, int size) {
            ByteBuffer buffer = STBIWriteCallback.getData(data, size);
            try {
                this.channel.write(buffer);
            } catch (IOException exception) {
                this.exception = exception;
            }
        }
    
        public IOException getException() {
            return this.exception;
        }
    }
}