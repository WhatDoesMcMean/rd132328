package me.kalmemarq.render;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
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

    public Window(int width, int height) {
        Configuration.DEBUG.set(false);
        Configuration.DEBUG_LOADER.set(true);
        Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
        Configuration.DEBUG_STACK.set(true);
        GLFWErrorCallback.createPrint(System.err).set();
        GLFW.glfwInit();

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);

        this.width = width;
        this.height = height;
        this.handle = GLFW.glfwCreateWindow(this.width, this.height, "", 0L, 0L);
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

        GLFW.glfwShowWindow(this.handle);
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

    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(this.handle, title);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(this.handle);
    }

    public void grabMouse() {
        GLFW.glfwSetInputMode(this.handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
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
}
