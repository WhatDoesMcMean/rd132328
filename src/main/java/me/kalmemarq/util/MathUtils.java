package me.kalmemarq.util;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class MathUtils {
    public static float wrapDegrees(float degrees, float low, float high) {
        float range = high - low;
        return (degrees < low) ? (degrees + range) : (degrees >= high) ? (degrees - range) : degrees;
    }

    public static float transformXByMatrix(Matrix4f matrix, float x, float y, float z) {
        return Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
    }

    public static float transformYByMatrix(Matrix4f matrix, float x, float y, float z) {
        return Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
    }

    public static float transformZByMatrix(Matrix4f matrix, float x, float y, float z) {
        return Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));
    }

    public static Vector3d intermediateWithAxis(Vector3d vecStart, Vector3d vecEnd, double planeValue, int axis) {
        double deltaX = vecEnd.x - vecStart.x;
        double deltaY = vecEnd.y - vecStart.y;
        double deltaZ = vecEnd.z - vecStart.z;

        double deltaAxis = switch (axis) {
            case 0 -> deltaX;
            case 1 -> deltaY;
            case 2 -> deltaZ;
            default -> 0;
        };

        float epsilon = 1.0E-7f;
        if (deltaAxis == 0 || deltaAxis * deltaAxis < epsilon) return null;

        double t = (planeValue - (axis == 0 ? vecStart.x : axis == 1 ? vecStart.y : vecStart.z)) / deltaAxis;

        if (t < 0.0 || t > 1.0) return null;

        return new Vector3d(vecStart.x + deltaX * t, vecStart.y + deltaY * t, vecStart.z + deltaZ * t);
    }

    public static Vector3d intermediateWithX(Vector3d vecStart, Vector3d vecEnd, double x) {
        return intermediateWithAxis(vecStart, vecEnd, x, 0);
    }

    public static Vector3d intermediateWithY(Vector3d vecStart, Vector3d vecEnd, double y) {
        return intermediateWithAxis(vecStart, vecEnd, y, 1);
    }

    public static Vector3d intermediateWithZ(Vector3d vecStart, Vector3d vecEnd, double z) {
        return intermediateWithAxis(vecStart, vecEnd, z, 2);
    }
}