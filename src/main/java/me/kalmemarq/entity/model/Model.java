package me.kalmemarq.entity.model;

import com.fasterxml.jackson.databind.JsonNode;

import me.kalmemarq.entity.Entity;
import me.kalmemarq.render.MatrixStack;
import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.util.IOUtils;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Model<E extends Entity> {
    public static Model.ModelPart loadRoot(String name) {
        try {
            return Model.ModelPart.loadFromJson(IOUtils.OBJECT_MAPPER.readTree(Files.readString(IOUtils.getResourcesPath().resolve("models/" + name + ".json"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    abstract public void render(BufferBuilder builder, E entity, float tickDelta);

    public static class ModelPart {
        public float yaw;
        public float pitch;
        public float roll;
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        private final List<Cuboid> cuboids;
        private final Map<String, ModelPart> children;

        public ModelPart(float pivotX, float pivotY, float pivotZ) {
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.cuboids = new ArrayList<>();
            this.children = new HashMap<>();
        }

        public ModelPart addChild(String name, ModelPart part) {
            this.children.put(name, part);
            return this;
        }

        public ModelPart getChild(String name) {
            return this.children.get(name);
        }

        public ModelPart addCuboid(int u, int v, float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ, float textureWidth, float textureHeight) {
            this.cuboids.add(new Cuboid(u, v, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, textureWidth, textureHeight));
            return this;
        }

        public void render(MatrixStack matrices, BufferBuilder builder) {
            matrices.push();
            matrices.translate(this.pivotX, this.pivotY, this.pivotZ);
            matrices.rotateZYX(this.roll, this.pitch, this.yaw);

            for (Cuboid cuboid : this.cuboids) {
                cuboid.render(matrices, builder);
            }

            for (ModelPart part : this.children.values()) {
                part.render(matrices, builder);
            }

            matrices.pop();
        }

        public static ModelPart loadFromJson(JsonNode node) {
            ModelPart root = new ModelPart(0, 0, 0);
            JsonNode textureSizeNode = node.get("texture_size");
            float textureWidth = textureSizeNode.get(0).floatValue();
            float textureHeight = textureSizeNode.get(1).floatValue();

            JsonNode parts = node.get("parts");
            loadChildren(root, textureWidth, textureHeight, parts);

            return root;
        }
    }

    private static void loadChildren(ModelPart root, float textureWidth, float textureHeight, JsonNode children) {
        for (Map.Entry<String, JsonNode> entry : children.properties()) {
            String name = entry.getKey();
            JsonNode partNode = entry.getValue();
            float pivotX = 0;
            float pivotY = 0;
            float pivotZ = 0;

            if (partNode.has("pivot")) {
                JsonNode pivotNode = partNode.get("pivot");
                pivotX = pivotNode.get(0).floatValue();
                pivotY = pivotNode.get(1).floatValue();
                pivotZ = pivotNode.get(2).floatValue();
            }

            ModelPart part = new ModelPart(pivotX, pivotY, pivotZ);

            JsonNode cuboidsNode = partNode.get("cuboids");
            for (JsonNode cuboidNode : cuboidsNode) {
                JsonNode uvNode = cuboidNode.get("uv");
                JsonNode offsetNode = cuboidNode.get("offset");
                JsonNode sizeNode = cuboidNode.get("size");

                int u = uvNode.get(0).intValue();
                int v = uvNode.get(1).intValue();

                float offsetX = offsetNode.get(0).floatValue();
                float offsetY = offsetNode.get(1).floatValue();
                float offsetZ = offsetNode.get(2).floatValue();

                float sizeX = sizeNode.get(0).floatValue();
                float sizeY = sizeNode.get(1).floatValue();
                float sizeZ = sizeNode.get(2).floatValue();

                part.addCuboid(u, v, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, textureWidth, textureHeight);
            }

            if (partNode.has("parts")) {
                loadChildren(part, textureWidth, textureHeight, partNode.get("parts"));
            }

            root.addChild(name, part);
        }
    }

    public record Cuboid(Quad[] faces) {
        public Cuboid(int u, int v, float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ, float textureWidth, float textureHeight) {
            this(new Quad[6]);

            Vertex v0 = new Vertex(offsetX, offsetY, offsetZ);
            Vertex v1 = new Vertex(offsetX + sizeX, offsetY, offsetZ);
            Vertex v3 = new Vertex(offsetX + sizeX, offsetY + sizeY, offsetZ);
            Vertex v4 = new Vertex(offsetX, offsetY + sizeY, offsetZ);
            Vertex v5 = new Vertex(offsetX, offsetY, offsetZ + sizeZ);
            Vertex v6 = new Vertex(offsetX + sizeX, offsetY, offsetZ + sizeZ);
            Vertex v7 = new Vertex(offsetX + sizeX, offsetY + sizeY, offsetZ + sizeZ);
            Vertex v8 = new Vertex(offsetX, offsetY + sizeY, offsetZ + sizeZ);

            this.faces[0] = new Quad(new Vertex[]{v7, v3, v1, v6}, u + sizeZ + sizeX, v + sizeZ, u + sizeZ + sizeX + sizeZ, v + sizeZ + sizeY, textureWidth, textureHeight);
            this.faces[1] = new Quad(new Vertex[]{v4, v8, v5, v0}, u, v + sizeZ, u + sizeZ, v + sizeZ + sizeY, textureWidth, textureHeight);
            this.faces[2] = new Quad(new Vertex[]{v1, v0, v5, v6}, u + sizeZ, v, u + sizeZ + sizeX, v + sizeZ, textureWidth, textureHeight);
            this.faces[3] = new Quad(new Vertex[]{v7, v8, v4, v3}, u + sizeZ + sizeX, v, u + sizeZ + sizeX + sizeX, v + sizeZ, textureWidth, textureHeight);
            this.faces[4] = new Quad(new Vertex[]{v3, v4, v0, v1}, u + sizeZ, v + sizeZ, u + sizeZ + sizeX, v + sizeZ + sizeY, textureWidth, textureHeight);
            this.faces[5] = new Quad(new Vertex[]{v8, v7, v6, v5}, u + sizeZ + sizeX + sizeZ, v + sizeZ, u + sizeZ + sizeX + sizeZ + sizeX, v + sizeZ + sizeY, textureWidth, textureHeight);
        }

        public void render(MatrixStack matrices, BufferBuilder builder) {
            Matrix4f matrix = matrices.peek();
            for (Quad quad : this.faces) {
                for (Vertex vertex : quad.vertices) {
                    builder.vertex(matrix, vertex.x, vertex.y, vertex.z).uv(vertex.u, vertex.v).color(1f, 1f, 1f);
                }
            }
        }
    }

    public record Vertex(float x, float y, float z, float u, float v) {
        public Vertex(float x, float y, float z) {
            this(x, y, z, 0f, 0f);
        }

        public Vertex remap(float u, float v) {
            return new Vertex(this.x, this.y, this.z, u, v);
        }
    }

    public record Quad(Vertex[] vertices) {
        public Quad(Vertex[] vertices, float u0, float v0, float u1, float v1, float textureWidth, float textureHeight) {
            this(vertices);
            this.vertices[0] = this.vertices[0].remap(u1 / textureWidth, v1 / textureHeight);
            this.vertices[1] = this.vertices[1].remap(u0 / textureWidth, v1 / textureHeight);
            this.vertices[2] = this.vertices[2].remap(u0 / textureWidth, v0 / textureHeight);
            this.vertices[3] = this.vertices[3].remap(u1 / textureWidth, v0 / textureHeight);
        }
    }
}
