package me.kalmemarq.entity.model;

import me.kalmemarq.entity.ZombieEntity;
import me.kalmemarq.render.MatrixStack;
import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.util.TimeUtils;
import org.joml.Math;

public class ZombieModel extends Model<ZombieEntity> {
    private final Model.ModelPart root;
    private final Model.ModelPart head;
    private final Model.ModelPart body;
    private final Model.ModelPart leftArm;
    private final Model.ModelPart rightArm;
    private final Model.ModelPart leftLeg;
    private final Model.ModelPart rightLeg;

    public ZombieModel() {
        this.root = Model.loadRoot("zombie");
        this.head = this.root.getChild("head");
        this.body = this.root.getChild("body");
        this.leftArm = this.root.getChild("left_arm");
        this.rightArm = this.root.getChild("right_arm");
        this.leftLeg = this.root.getChild("left_leg");
        this.rightLeg = this.root.getChild("right_leg");
    }

    @Override
    public void render(BufferBuilder builder, ZombieEntity entity, float tickDelta) {
        MatrixStack matrices = MatrixStack.INSTANCE;
        matrices.push();

        matrices.translate(org.joml.Math.lerp(entity.prevPosition.x, entity.position.x, tickDelta),
                org.joml.Math.lerp(entity.prevPosition.y, entity.position.y, tickDelta),
                org.joml.Math.lerp(entity.prevPosition.z, entity.position.z, tickDelta));
        float size = 1.86f / 32f;
        double time = (double) TimeUtils.millisTime() / 1E3d * 10d + (double)entity.timeOffs;
        float yy = (float)(-Math.abs(Math.sin(time * 0.6662d)) * 5d - 23d);
        matrices.scale(1f, -1f, 1f);
        matrices.scale(size, size, size);
        matrices.translate(0f, yy, 0f);
        matrices.rotateY(entity.yaw + ((float) Math.PI / 2f));

        this.head.pitch = (float) Math.sin(time * 0.83d);
        this.head.yaw = (float) Math.sin(time) * 0.8f;
        this.leftArm.yaw = (float) Math.sin(time * 0.6662d + Math.PI) * 2f;
        this.leftArm.roll = (float) (Math.sin(time * 0.2312d) + 1d);
        this.rightArm.yaw = (float) Math.sin(time * 0.6662d) * 2f;
        this.rightArm.roll = (float) (Math.sin(time * 0.2812d) - 1d);
        this.leftLeg.yaw = (float) Math.sin(time * 0.6662d) * 1.4f;
        this.rightLeg.yaw = (float) Math.sin(time * 0.6662d + Math.PI) * 1.4f;

        this.root.render(matrices, builder);

        matrices.pop();
    }
}
