package me.kalmemarq.entity;

import me.kalmemarq.World;

public class ZombieEntity extends Entity {
    public double yawV;
    public final float timeOffs = (float) Math.random() * 1239813f;

    public ZombieEntity(World world) {
        super(world);
    }

    @Override
    public void tick() {
        super.tick();

        this.yaw += (float) this.yawV;
        this.yawV *= 0.99d;
        this.yawV += (Math.random() - Math.random()) * Math.random() * Math.random() * 0.01d;

        float xd = (float) Math.sin(this.yaw);
        float zd = (float) Math.cos(this.yaw);

        if (this.onGround && Math.random() < 0.01d) {
            this.velocity.y = 0.12F;
        }

        this.velocity.y -= 0.005f;

        float speed = this.onGround ? 0.02f : this.canFly ? 0.02f : 0.005f;

        this.moveRelative(xd, zd, speed);
        this.move(this.velocity.x, this.velocity.y, this.velocity.z);

        this.velocity.mul(0.91f, this.canFly ? 0.91f : 0.98f, 0.91f);

        if (this.onGround) {
            this.velocity.mul(0.8f, 0f, 0.8f);
        }
    }
}
