package me.kalmemarq.entity;

import me.kalmemarq.World;
import me.kalmemarq.util.Box;
import me.kalmemarq.util.MathUtils;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;

public class Entity {
    public final World world;
    public Vector3f position = new Vector3f();
    public Vector3f prevPosition = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public Vector2f size = new Vector2f(0.6f, 1.8f);
    public float yaw = 90f;
    public float pitch;
    public float eyeHeight = this.size.y - 0.18f;
    public Box box;
    public boolean onGround;
    public boolean canFly;
    public boolean noClip;

    public Entity(World world) {
        this.world = world;
        this.goToRandomPosition();
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.prevPosition.set(this.position);
        this.box = new Box(this.position.x - this.size.x / 2, this.position.y, this.position.z - this.size.x / 2, this.position.x + this.size.x / 2, this.position.y + this.size.y, this.position.z + this.size.x / 2);
    }

    public void goToRandomPosition() {
        this.setPosition(
                (float) Math.random() * (float) this.world.width,
                (float) (this.world.depth + 10),
                (float) Math.random() * (float) this.world.height
        );
    }

    public void turn(float dx, float dy) {
        this.yaw = MathUtils.wrapDegrees(this.yaw + dx, -180, 180);
        this.pitch = Math.clamp(this.pitch + dy, -90, 90);
    }

    public Vector3d getPosition() {
        return new Vector3d(this.position);
    }

    public Vector3d getCameraPosition() {
        return this.getPosition().add(0, this.eyeHeight, 0);
    }

    public Vector3d getLook() {
        double cosYaw = Math.cos(Math.toRadians(-this.yaw));
        double sinYaw = Math.sin(Math.toRadians(-this.yaw));
        double cosPitch = -Math.cos(Math.toRadians(-this.pitch));
        double sinPitch = Math.sin(Math.toRadians(-this.pitch));
        return new Vector3d((sinYaw * cosPitch), sinPitch, (cosYaw * cosPitch));
    }

    public void tick() {
        this.prevPosition.set(this.position);
    }

    public void move(float xd, float yd, float zd) {
        float xdOrg = xd;
        float ydOrg = yd;
        float zdOrg = zd;

        if (!this.canFly || !this.noClip) {
            List<Box> boxes = this.world.getCubes(this.box.grow(xd, yd, zd));

            for (Box box : boxes) {
                yd = box.clipYCollide(this.box, yd);
            }
            this.box.move(0, yd, 0);

            for (Box box : boxes) {
                xd = box.clipXCollide(this.box, xd);
            }
            this.box.move(xd, 0, 0);

            for (Box box : boxes) {
                zd = box.clipZCollide(this.box, zd);
            }
            this.box.move(0, 0, zd);

            if (ydOrg != yd) {
                this.velocity.y = 0f;
            }

            if (xdOrg != xd) {
                this.velocity.x = 0f;
            }

            if (zdOrg != zd) {
                this.velocity.z = 0f;
            }
        } else {
            this.box.move(0, yd, 0);
            this.box.move(xd, 0, 0);
            this.box.move(0, 0, zd);
        }

        this.onGround = ydOrg != yd && ydOrg < 0f;

        this.position.x = (this.box.minX + this.box.maxX) / 2f;
        this.position.y = this.box.minY;
        this.position.z = (this.box.minZ + this.box.maxZ) / 2f;
    }

    protected void moveRelative(float xd, float zd, float speed) {
        float distSquared = xd * xd + zd * zd;
        if (distSquared >= 0.01f) {
            float scale = speed / (float) Math.sqrt(distSquared);
            xd *= scale;
            zd *= scale;
            float sin = (float) Math.sin(Math.toRadians(this.yaw));
            float cos = (float) Math.cos(Math.toRadians(this.yaw));
            this.velocity.x += xd * cos - zd * sin;
            this.velocity.z += zd * cos + xd * sin;
        }
    }
}
