package me.kalmemarq.render;

import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;

public class MatrixStack {
    private static final int CACHE_STACK_LIMIT = 32;
    public static final MatrixStack INSTANCE = new MatrixStack();

    private final Deque<Matrix4f> stack = new ArrayDeque<>();
    private final Deque<Matrix4f> cacheStack = new ArrayDeque<>();

    public MatrixStack() {
        this.stack.addLast(new Matrix4f());
    }

    public Matrix4f peek() {
        return this.stack.getLast();
    }

    public void push() {
        if (this.cacheStack.isEmpty()) {
            this.stack.addLast(new Matrix4f(this.stack.getLast()));
        } else {
            this.stack.addLast(this.cacheStack.removeLast().set(this.stack.getLast()));
        }
    }

    public void pop() {
        if (this.cacheStack.size() < CACHE_STACK_LIMIT) {
            this.cacheStack.addLast(this.stack.removeLast());
        } else {
            this.stack.removeLast();
        }
    }

    public void translate(float x, float y, float z) {
        this.peek().translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        this.peek().scale(x, y, z);
    }

    public void rotateX(float angle) {
        this.peek().rotate(angle, 1, 0, 0);
    }

    public void rotateY(float angle) {
        this.peek().rotate(angle, 0, 1, 0);
    }

    public void rotateZ(float angle) {
        this.peek().rotate(angle, 0, 0, 1);
    }

    public void rotateZYX(float zAngle, float yAngle, float xAngle) {
        this.peek().rotateZYX(zAngle, yAngle, xAngle);
    }

    public void rotateXDegrees(float degrees) {
        this.rotateX((float) Math.toRadians(degrees));
    }

    public void rotateYDegrees(float degrees) {
        this.rotateY((float) Math.toRadians(degrees));
    }

    public void rotateZDegrees(float degrees) {
        this.rotateZ((float) Math.toRadians(degrees));
    }
}
