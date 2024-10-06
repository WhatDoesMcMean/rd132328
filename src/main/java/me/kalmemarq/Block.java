package me.kalmemarq;

import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.render.MatrixStack;
import me.kalmemarq.util.BlockHitResult;
import me.kalmemarq.util.Box;
import me.kalmemarq.util.Direction;
import me.kalmemarq.util.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class Block {
    public static final Box VOXEL_SHAPE = new Box(0, 0, 0, 1, 1, 1);
    public static final Block BLOCK = new Block();

    public static void render(World world, MatrixStack matrices, BufferBuilder builder, int x, int y, int z, boolean isGrass, int layer) {
        float x0 = 0f;
        float y0 = 0f;
        float z0 = 0f;
        float x1 = 1f;
        float y1 = 1f;
        float z1 = 1f;

        int u = isGrass ? 0 : 16;
        int v = 0;
        float u0 = u / 256.0f;
        float v0 = v / 256.0f;
        float u1 = (u + 16) / 256.0f;
        float v1 = (v + 16) / 256.0f;

        Matrix4f matrix = matrices.peek();

        boolean shouldRenderBottom = world.getBlockId(x, y - 1, z) == 0;
        boolean shouldRenderTop = world.getBlockId(x, y + 1, z) == 0;
        boolean shouldRenderNorth = world.getBlockId(x, y, z - 1) == 0;
        boolean shouldRenderSouth = world.getBlockId(x, y, z + 1) == 0;
        boolean shouldRenderWest = world.getBlockId(x - 1, y, z) == 0;
        boolean shouldRenderEast = world.getBlockId(x + 1, y, z) == 0;

        if (shouldRenderBottom) {
            float light = world.getBrigthness(x, y - 1, z);
            if (light == 1.0f ^ layer == 1) {
                builder.vertex(matrix, x0, y0, z0).uv(u0, v0).color(light, light, light);
                builder.vertex(matrix, x1, y0, z0).uv(u1, v0).color(light, light, light);
                builder.vertex(matrix, x1, y0, z1).uv(u1, v1).color(light, light, light);
                builder.vertex(matrix, x0, y0, z1).uv(u0, v1).color(light, light, light);
            }
        }

        if (shouldRenderTop) {
            float light = world.getBrigthness(x, y + 1, z);
            if (light == 1.0f ^ layer == 1) {
                builder.vertex(matrix, x0, y1, z0).uv(u0, v0).color(light, light, light);
                builder.vertex(matrix, x0, y1, z1).uv(u0, v1).color(light, light, light);
                builder.vertex(matrix, x1, y1, z1).uv(u1, v1).color(light, light, light);
                builder.vertex(matrix, x1, y1, z0).uv(u1, v0).color(light, light, light);
            }
        }

        if (shouldRenderNorth) {
            float light = world.getBrigthness(x, y, z - 1) * 0.8f;
            if (light == 0.8f ^ layer == 1) {
                builder.vertex(matrix, x0, y0, z0).uv(u1, v1).color(light, light, light);
                builder.vertex(matrix, x0, y1, z0).uv(u1, v0).color(light, light, light);
                builder.vertex(matrix, x1, y1, z0).uv(u0, v0).color(light, light, light);
                builder.vertex(matrix, x1, y0, z0).uv(u0, v1).color(light, light, light);
            }
        }

        if (shouldRenderSouth) {
            float light = world.getBrigthness(x, y, z + 1) * 0.8f;
            if (light == 0.8f ^ layer == 1) {
                builder.vertex(matrix, x0, y0, z1).uv(u0, v1).color(light, light, light);
                builder.vertex(matrix, x1, y0, z1).uv(u1, v1).color(light, light, light);
                builder.vertex(matrix, x1, y1, z1).uv(u1, v0).color(light, light, light);
                builder.vertex(matrix, x0, y1, z1).uv(u0, v0).color(light, light, light);
            }
        }

        if (shouldRenderWest) {
            float light = world.getBrigthness(x - 1, y, z) * 0.6f;
           if (light == 0.6f ^ layer == 1) {
               builder.vertex(matrix, x0, y0, z0).uv(u0, v1).color(light, light, light);
               builder.vertex(matrix, x0, y0, z1).uv(u1, v1).color(light, light, light);
               builder.vertex(matrix, x0, y1, z1).uv(u1, v0).color(light, light, light);
               builder.vertex(matrix, x0, y1, z0).uv(u0, v0).color(light, light, light);
           }
        }

        if (shouldRenderEast) {
            float light = world.getBrigthness(x + 1, y, z) * 0.6f;
            if (light == 0.6f ^ layer == 1) {
                builder.vertex(matrix, x1, y0, z0).uv(u1, v1).color(light, light, light);
                builder.vertex(matrix, x1, y1, z0).uv(u1, v0).color(light, light, light);
                builder.vertex(matrix, x1, y1, z1).uv(u0, v0).color(light, light, light);
                builder.vertex(matrix, x1, y0, z1).uv(u0, v1).color(light, light, light);
            }
        }
    }

    public BlockHitResult raytrace(int x, int y, int z, Vector3d start, Vector3d end) {
        Vector3d downVec = MathUtils.intermediateWithY(start, end, VOXEL_SHAPE.minY);
        Vector3d upVec = MathUtils.intermediateWithY(start, end, VOXEL_SHAPE.maxY);
        Vector3d northVec = MathUtils.intermediateWithZ(start, end, VOXEL_SHAPE.minZ);
        Vector3d southVec = MathUtils.intermediateWithZ(start, end, VOXEL_SHAPE.maxZ);
        Vector3d westVec = MathUtils.intermediateWithX(start, end, VOXEL_SHAPE.minX);
        Vector3d eastVec = MathUtils.intermediateWithX(start, end, VOXEL_SHAPE.maxX);

        Vector3d closestHit = null;
        Direction closestSide = null;

        if (VOXEL_SHAPE.containsInXZPlane(downVec)) {
            closestHit = downVec;
            closestSide = Direction.DOWN;
        }

        if (VOXEL_SHAPE.containsInXZPlane(upVec) && (closestHit == null || start.distance(upVec) < start.distance(closestHit))) {
            closestHit = upVec;
            closestSide = Direction.UP;
        }

        if (VOXEL_SHAPE.containsInYZPlane(northVec) && (closestHit == null || start.distance(northVec) < start.distance(closestHit))) {
            closestHit = northVec;
            closestSide = Direction.NORTH;
        }

        if (VOXEL_SHAPE.containsInYZPlane(southVec) && (closestHit == null || start.distance(southVec) < start.distance(closestHit))) {
            closestHit = southVec;
            closestSide = Direction.SOUTH;
        }

        if (VOXEL_SHAPE.containsInXYPlane(westVec) && (closestHit == null || start.distance(westVec) < start.distance(closestHit))) {
            closestHit = westVec;
            closestSide = Direction.WEST;
        }

        if (VOXEL_SHAPE.containsInXYPlane(eastVec) && (closestHit == null || start.distance(eastVec) < start.distance(closestHit))) {
            closestHit = eastVec;
            closestSide = Direction.EAST;
        }

        if (closestHit != null) {
            return new BlockHitResult(x, y, z, closestSide);
        }

        return null;
    }
}
