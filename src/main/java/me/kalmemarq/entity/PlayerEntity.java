package me.kalmemarq.entity;

import me.kalmemarq.Game;
import me.kalmemarq.World;
import me.kalmemarq.util.BlockHitResult;
import me.kalmemarq.util.Keybinding;
import org.joml.Vector3d;

public class PlayerEntity extends Entity {
    public PlayerEntity(World world) {
        super(world);
    }

    public BlockHitResult raytrace(double reach) {
        Vector3d start = this.getCameraPosition();
        Vector3d look = this.getLook();
        Vector3d end = start.add(look.x() * reach, look.y() * reach, look.z() * reach, new Vector3d());
        return this.world.raytraceBlock(start.x, start.y, start.z, end.x, end.y, end.z);
    }

    @Override
    public void tick() {
        super.tick();
        float xd = 0;
        float zd = 0;

        if (Keybinding.FOWARDS.isPressed(Game.getInstance().getWindow())) {
            zd -= 1;
        }

        if (Keybinding.BACKWARD.isPressed(Game.getInstance().getWindow())) {
            zd += 1;
        }

        if (Keybinding.STRAFE_LEFT.isPressed(Game.getInstance().getWindow())) {
            xd -= 1;
        }

        if (Keybinding.STRAFE_RIGHT.isPressed(Game.getInstance().getWindow())) {
            xd += 1;
        }

        if ((this.onGround || this.canFly) && Keybinding.JUMP.isPressed(Game.getInstance().getWindow())) {
            this.velocity.y = 0.12f;
        }

        if (Keybinding.DESCEND.isPressed(Game.getInstance().getWindow())) {
            this.velocity.y = -0.12f;
        }

        if (!this.canFly) {
            this.velocity.y -= 0.005f;
        }

        float speed = this.onGround ? 0.02f : this.canFly ? 0.02f : 0.005f;

        this.moveRelative(xd, zd, speed);
        this.move(this.velocity.x, this.velocity.y, this.velocity.z);

        this.velocity.mul(0.91f, this.canFly ? 0.91f : 0.98f, 0.91f);

        if (this.onGround) {
            this.velocity.mul(0.8f, 0f, 0.8f);
        }
    }
}
