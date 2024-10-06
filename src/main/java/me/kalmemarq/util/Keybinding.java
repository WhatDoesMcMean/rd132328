package me.kalmemarq.util;

import me.kalmemarq.render.Window;
import org.lwjgl.glfw.GLFW;

public record Keybinding(int... possibleKeys) {
    public static final Keybinding FOWARDS = new Keybinding(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP);
    public static final Keybinding BACKWARD = new Keybinding(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN);
    public static final Keybinding STRAFE_LEFT = new Keybinding(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT);
    public static final Keybinding STRAFE_RIGHT = new Keybinding(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT);
    public static final Keybinding JUMP = new Keybinding(GLFW.GLFW_KEY_SPACE);
    public static final Keybinding DESCEND = new Keybinding(GLFW.GLFW_KEY_LEFT_SHIFT);

    public static final Keybinding TOGGLE_FULLSCREEN = new Keybinding(GLFW.GLFW_KEY_F11);
    public static final Keybinding TOGGLE_VSYNC = new Keybinding(GLFW.GLFW_KEY_F10);
    public static final Keybinding SAVE_WORLD_TO_DISK = new Keybinding(GLFW.GLFW_KEY_ENTER);
    public static final Keybinding GO_TO_RANDOM_POS = new Keybinding(GLFW.GLFW_KEY_R);
    public static final Keybinding FLY = new Keybinding(GLFW.GLFW_KEY_J);
    public static final Keybinding NO_CLIP = new Keybinding(GLFW.GLFW_KEY_N);

    public boolean isPressed(Window window) {
        for (int key : this.possibleKeys) {
            if (GLFW.glfwGetKey(window.getHandle(), key) != GLFW.GLFW_RELEASE) {
                return true;
            }
        }
        return false;
    }

    public boolean test(int key) {
        for (int i = 0; i < this.possibleKeys.length; ++i) {
            if (this.possibleKeys[i] == key) {
                return true;
            }
        }
        return false;
    }
}
