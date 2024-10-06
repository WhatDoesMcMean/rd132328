package me.kalmemarq.render;

import me.kalmemarq.util.Box;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public class Frustum {
    private final FrustumIntersection frustumIntersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();

    public void set(Matrix4f projectionMatrix, Matrix4f modelViewMatrix) {
        this.frustumIntersection.set(projectionMatrix.mul(modelViewMatrix, this.matrix));
    }

    public boolean isVisible(Box box) {
        int result = this.frustumIntersection.intersectAab(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        return result == FrustumIntersection.INSIDE || result == FrustumIntersection.INTERSECT;
    }
}
