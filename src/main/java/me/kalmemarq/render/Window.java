package me.kalmemarq.render;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import me.kalmemarq.util.IOUtils;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Window implements Closeable {
    private final long handle;
    private int width;
    private int height;
    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;

    private int framebufferWidth;
    private int framebufferHeight;

    private boolean fullscreen;
    private boolean currentFullscreen;
    private boolean vsync = true;

    private ImGuiLayer imGuiLayer;
    private List<EventHandler> eventHandlers;

    public Window(int width, int height, String title) {
        if (System.getProperty("whatDoesMcMean.lwjgl.debug") != null) {
            Configuration.DEBUG.set(false);
            Configuration.DEBUG_LOADER.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        this.eventHandlers = new ArrayList<>();

        GLFWErrorCallback.createPrint(System.err).set();
        GLFW.glfwInit();

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);

        this.width = width;
        this.height = height;
        this.handle = GLFW.glfwCreateWindow(this.width, this.height, title, 0L, 0L);
        GLFW.glfwMakeContextCurrent(this.handle);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        if (vidMode != null) {
            GLFW.glfwSetWindowPos(this.handle, (vidMode.width() - this.width) / 2, (vidMode.height() - this.height) / 2);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long w = stack.nmalloc(4);
            long h = stack.nmalloc(4);
            GLFW.nglfwGetFramebufferSize(this.handle, w, h);
            this.framebufferWidth = MemoryUtil.memGetInt(w);
            this.framebufferHeight = MemoryUtil.memGetInt(h);
        }

        GLFW.glfwSetFramebufferSizeCallback(this.handle, (_w, w, h) -> {
            this.framebufferWidth = w;
            this.framebufferHeight = h;
        });

        GLFW.glfwSetKeyCallback(this.handle, (_w, k, sc, a, m) -> {
            for (EventHandler handler : this.eventHandlers) {
                handler.onKey(k, a);
            }
        });
        
        GLFW.glfwSetCharCallback(this.handle, (_w, c) -> {
            for (EventHandler handler : this.eventHandlers) {
                handler.onCharTyped(c);
            }
        });
        
        GLFW.glfwSetMouseButtonCallback(this.handle, (_w, b, a, m) -> {
            for (EventHandler handler : this.eventHandlers) {
                handler.onMouseButton(b, a);
            }
        });
        
        GLFW.glfwSetCursorPosCallback(this.handle, (_w, x, y) -> {
            for (EventHandler handler : this.eventHandlers) {
                handler.onCursorPos(x, y);
            }
        });
        
        GLFW.glfwSetScrollCallback(this.handle, (_w, xO, yO) -> {
            for (EventHandler handler : this.eventHandlers) {
                handler.onScroll(xO, yO);
            }
        });
        
        GLFW.glfwSetDropCallback(this.handle, (_w, count, names) -> {
            List<Path> paths = new ArrayList<>();

            for (int i = 0; i < count; ++i) {
                paths.add(Path.of(GLFWDropCallback.getName(names, i)));
            }

            paths = Collections.unmodifiableList(paths);
            
            for (EventHandler handler : this.eventHandlers) {
                handler.onDrop(paths);
            }
        });

        this.imGuiLayer = new ImGuiLayer(this);

        GLFW.glfwShowWindow(this.handle);
    }

    public void addEventHandler(EventHandler eventHandler) {
        this.eventHandlers.add(eventHandler);
    }

    public void toggleFullscreen() {
        this.fullscreen = !this.fullscreen;
    }

    public void toggleVsync() {
        this.vsync = !this.vsync;
        GLFW.glfwSwapInterval(this.vsync ? 1 : 0);
    }

    public long getHandle() {
        return this.handle;
    }

    public int getWidth() {
        return this.framebufferWidth;
    }

    public int getHeight() {
        return this.framebufferHeight;
    }

    public ImGuiLayer getImGuiLayer() {
        return this.imGuiLayer;
    }

    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(this.handle, title);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(this.handle);
    }

    public void grabMouse() {
        GLFW.glfwSetInputMode(this.handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

     public void setIcon() {
        String[] icons = {"x16.png", "x32.png", "x48.png", "x64.png", "x128.png", "x256.png"};
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer wP = stack.mallocInt(1);
            IntBuffer hP = stack.mallocInt(1);
            IntBuffer cP = stack.mallocInt(1);
            List<ByteBuffer> imageBuffers = new ArrayList<>();
            GLFWImage.Buffer iconsBuffer = GLFWImage.malloc(icons.length, stack);
            for (int i = 0; i < icons.length; ++i) {
                ByteBuffer iconData = IOUtils.readFileToByteBuffer(IOUtils.getResourcesPath().resolve("icons/" + icons[i]));
                ByteBuffer iconPixels = STBImage.stbi_load_from_memory(iconData, wP, hP, cP, 4);
                if (iconPixels != null) {
                    iconsBuffer.position(i);
                    iconsBuffer.width(wP.get(0));
                    iconsBuffer.height(hP.get(0));
                    iconsBuffer.pixels(iconPixels);
                    imageBuffers.add(iconPixels);
                }
                MemoryUtil.memFree(iconData);
            }
            iconsBuffer.position(0);
            GLFW.glfwSetWindowIcon(this.handle, iconsBuffer);
            imageBuffers.forEach(STBImage::stbi_image_free);
        }
    }

    public void update() {
        GLFW.glfwSwapBuffers(this.handle);
        GLFW.glfwPollEvents();

        if (this.fullscreen != this.currentFullscreen) {
            this.currentFullscreen = this.fullscreen;

            if (this.currentFullscreen) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    long x = stack.nmalloc(4);
                    long y = stack.nmalloc(4);
                    GLFW.nglfwGetWindowPos(this.handle, x, y);
                    this.windowedX = MemoryUtil.memGetInt(x);
                    this.windowedY = MemoryUtil.memGetInt(y);
                    this.windowedWidth = this.width;
                    this.windowedHeight = this.height;
                    long monitor = GLFW.glfwGetWindowMonitor(this.handle);
                    if (monitor == 0L) {
                        monitor = GLFW.glfwGetPrimaryMonitor();
                    }

                    GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
                    this.width = vidMode.width();
                    this.height = vidMode.height();
                    this.framebufferWidth = this.width;
                    this.framebufferHeight = this.height;
                    GLFW.glfwSetWindowMonitor(this.handle, monitor, 0, 0, vidMode.width(), vidMode.height(), GLFW.GLFW_DONT_CARE);
                }
            } else {
                GLFW.glfwSetWindowMonitor(this.handle, 0L, this.windowedX, this.windowedY, this.windowedWidth, this.windowedHeight, GLFW.GLFW_DONT_CARE);
                this.width = this.windowedWidth;
                this.height = this.windowedHeight;
            }
        }
    }

    @Override
    public void close() {
        Callbacks.glfwFreeCallbacks(this.handle);
        GLFW.glfwDestroyWindow(this.handle);
        GLFW.glfwTerminate();
        Objects.requireNonNull(GLFW.glfwSetErrorCallback(null)).free();
    }

    public interface EventHandler {
        default void onKey(int key, int action) {
        }
        default void onCharTyped(int codepoint) {
        }
        default void onMouseButton(int button, int action) {
        }
        
        default void onCursorPos(double x, double y) {
        }
        default void onScroll(double xOffset, double yOffset) {
        }
        default void onDrop(List<Path> paths) {
        }
    }
}
