package me.kalmemarq.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

public class IOUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    private static Path resourcesPath;

    public static Path getResourcesPath() {
        if (resourcesPath == null) {
            try {
                URI uri = Objects.requireNonNull(IOUtils.class.getResource("/.root")).toURI();
                try {
                    resourcesPath = Path.of(uri).getParent();
                } catch (FileSystemNotFoundException e) {
                    FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    resourcesPath = fileSystem.getPath("/.root").getParent();
                }
            }  catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return resourcesPath;
    }

    public static ByteBuffer readInputStreamToByteBuffer(InputStream inputStream) {
        try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
            return readToByteBuffer(channel, 8196);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static ByteBuffer readFileToByteBuffer(Path path) {
        try (SeekableByteChannel channel = Files.newByteChannel(path)) {
            return readToByteBuffer(channel, (int) channel.size());
        } catch (IOException ignored) {
            return null;
        }
    }

    private static ByteBuffer readToByteBuffer(ReadableByteChannel channel, int bufferInitialSize) {
        ByteBuffer buffer = MemoryUtil.memAlloc(bufferInitialSize);
        try {
            while (channel.read(buffer) != -1) {
                if (buffer.hasRemaining()) continue;
                buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() * 2);
            }
        } catch (IOException ignored) {
            MemoryUtil.memFree(buffer);
            return null;
        }
        buffer.flip();
        return buffer;
    }

    public static boolean ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
