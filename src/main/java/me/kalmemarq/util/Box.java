package me.kalmemarq.util;

import org.joml.Vector3d;

public class Box {
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    public Box(Box other) {
        this(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    public Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return minX < this.maxX && maxX > this.minX && minY < this.maxY && maxY > this.minY && minZ < this.maxZ && maxZ > this.minZ;
    }

    public void move(float x, float y, float z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }

    public Box grow(float x, float y, float z) {
        Box box = new Box(this);

        if (x < 0f) box.minX += x;
        else if (x > 0f) box.maxX += x;

        if (y < 0f) box.minY += y;
        else if (y > 0f) box.maxY += y;

        if (z < 0f) box.minZ += z;
        else if (z > 0f) box.maxZ += z;

        return box;
    }

    public boolean containsInYZPlane(Vector3d vec) {
        return vec != null && vec.y >= this.minY && vec.y <= this.maxY && vec.z >= this.minZ && vec.z <= this.maxZ;
    }

    public boolean containsInXZPlane(Vector3d vec) {
        return vec != null && vec.x >= this.minX && vec.x <= this.maxX && vec.z >= this.minZ && vec.z <= this.maxZ;
    }

    public boolean containsInXYPlane(Vector3d vec) {
        return vec != null && vec.x >= this.minX && vec.x <= this.maxX && vec.y >= this.minY && vec.y <= this.maxY;
    }

    private float adjustDeltaByDirection(float thisMin, float thisMax, float otherMin, float otherMax, float delta) {
        if (delta > 0f && otherMax <= thisMin) {
            delta = Math.min(thisMin - otherMax, delta);
        }
        if (delta < 0f && otherMin >= thisMax) {
            delta = Math.max(thisMax - otherMin, delta);
        }
        return delta;
    }

    public float clipXCollide(Box other, float xd) {
        if ((other.maxY > this.minY && other.minY < this.maxY) && (other.maxZ > this.minZ && other.minZ < this.maxZ)) {
            return this.adjustDeltaByDirection(this.minX, this.maxX, other.minX, other.maxX, xd);
        }
        return xd;
    }

    public float clipYCollide(Box other, float yd) {
        if ((other.maxX > this.minX && other.minX < this.maxX) && (other.maxZ > this.minZ && other.minZ < this.maxZ)) {
            return this.adjustDeltaByDirection(this.minY, this.maxY, other.minY, other.maxY, yd);
        }
        return yd;
    }

    public float clipZCollide(Box other, float zd) {
        if ((other.maxX > this.minX && other.minX < this.maxX) && (other.maxY > this.minY && other.minY < this.maxY)) {
            return this.adjustDeltaByDirection(this.minZ, this.maxZ, other.minZ, other.maxZ, zd);
        }
        return zd;
    }
}
