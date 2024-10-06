package me.kalmemarq.render;

import me.kalmemarq.Block;
import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.render.vertex.VertexBuffer;
import me.kalmemarq.render.vertex.VertexLayout;
import me.kalmemarq.util.Box;
import me.kalmemarq.World;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class WorldRenderer implements Closeable, World.WorldStateListener {
    public static int chunksRendererPerFrame = 0;
    public static int currentChunksRendererPerFrame = 0;

    private final Chunk[] chunks;
    private final int xChunks;
    private final int yChunks;
    private final int zChunks;

    public WorldRenderer(World world) {
        this.xChunks = world.width / World.CHUNK_SIZE;
        this.yChunks = world.depth / World.CHUNK_SIZE;
        this.zChunks = world.height / World.CHUNK_SIZE;
        this.chunks = new Chunk[this.xChunks * this.yChunks * this.zChunks];

        for (int x = 0; x < this.xChunks; x++) {
            for (int y = 0; y < this.yChunks; y++) {
                for (int z = 0; z < this.zChunks; z++) {
                    this.chunks[(x + y * this.xChunks) * this.zChunks + z] = new Chunk(world, x, y, z);
                }
            }
        }
    }

    public int getChunkCount() {
        return this.chunks.length;
    }

    @Override
    public void onChunkModified(int chunkX, int chunkY, int chunkZ) {
        if (chunkX < 0 || chunkY < 0 || chunkZ < 0 || chunkX >= this.xChunks || chunkY >= this.yChunks || chunkZ >= this.zChunks) return;
        this.chunks[(chunkX + chunkY * this.xChunks) * this.zChunks + chunkZ].markDirty();
    }

    public void render(Shader terrainShader, Frustum frustum, int layer) {
        Chunk.rebuiltThisFrame = 0;
        chunksRendererPerFrame = currentChunksRendererPerFrame;
        currentChunksRendererPerFrame = 0;

        for (Chunk chunk : this.chunks) {
            if (chunk != null && frustum.isVisible(chunk.box)) {
                terrainShader.setUniform("uMeshOffset", (float) (chunk.x * World.CHUNK_SIZE), (float) (chunk.y * World.CHUNK_SIZE), (float) (chunk.z * World.CHUNK_SIZE));
                chunk.render(layer);
                if (layer == 0) currentChunksRendererPerFrame++;
            }
        }
    }

    @Override
    public void close() {
        for (Chunk chunk : this.chunks) {
            if (chunk != null) {
                chunk.close();
            }
        }
    }

    public static class Chunk implements Closeable {
        private static int rebuiltThisFrame;

        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private boolean dirty;
        private final VertexBuffer[] vertexBuffers;
        public final Box box;
        private ByteBuffer buffer;

        public Chunk(World world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.box = new Box(x * World.CHUNK_SIZE, y * World.CHUNK_SIZE, z * World.CHUNK_SIZE, (x + 1) * World.CHUNK_SIZE, (y + 1) * World.CHUNK_SIZE, (z + 1) * World.CHUNK_SIZE);
            this.vertexBuffers = new VertexBuffer[2];
            this.markDirty();
        }

        public void markDirty() {
            this.dirty = true;
        }

        private void rebuild(int layer) {
            if (Chunk.rebuiltThisFrame == 2 * 5) return;
            ++Chunk.rebuiltThisFrame;

            if (this.vertexBuffers[layer] == null) {
                this.vertexBuffers[layer] = new VertexBuffer();
            }

            if (this.buffer == null) {
                this.buffer = MemoryUtil.memAlloc((((12 + 8 + 4) * 4) * 6) * (World.CHUNK_SIZE * World.CHUNK_SIZE * World.CHUNK_SIZE));
            }

            BufferBuilder builder = new BufferBuilder(MemoryUtil.memAddress(this.buffer));
            MatrixStack matrices = new MatrixStack();
            builder.begin();
            for (int y = 0; y < World.CHUNK_SIZE; ++y) {
                for (int z = 0; z < World.CHUNK_SIZE; ++z) {
                    for (int x = 0; x < World.CHUNK_SIZE; ++x) {
                        int blockX = this.x * World.CHUNK_SIZE + x;
                        int blockY = this.y * World.CHUNK_SIZE + y;
                        int blockZ = this.z * World.CHUNK_SIZE + z;

                        int blockId = this.world.getBlockId(blockX, blockY, blockZ);
                        if (blockId == 0) continue;

                        matrices.push();
                        matrices.translate(x, y, z);
                        Block.render(this.world, matrices, builder, blockX, blockY, blockZ, this.world.isTopLayer(blockY), layer);
                        matrices.pop();
                    }
                }
            }

            this.vertexBuffers[layer].upload(DrawMode.QUADS, VertexLayout.POS_UV_COLOR, MemoryUtil.memSlice(this.buffer, 0, builder.end() * (12 + 8 + 4)), builder.end());
            this.dirty = false;
        }

        public void render(int layer) {
            if (this.dirty) {
                this.rebuild(0);
                this.rebuild(1);
            }

            if (!this.dirty && this.vertexBuffers[layer].getIndexCount() > 0) {
                this.vertexBuffers[layer].bind();
                this.vertexBuffers[layer].draw();
            }
        }

        @Override
        public void close() {
            for (VertexBuffer vertexBuffer : this.vertexBuffers) {
                if (vertexBuffer != null) vertexBuffer.close();
            }

            if (this.buffer != null) {
                MemoryUtil.memFree(this.buffer);
            }
        }
    }
}
