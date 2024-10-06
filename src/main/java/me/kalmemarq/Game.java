package me.kalmemarq;

import me.kalmemarq.entity.PlayerEntity;
import me.kalmemarq.entity.ZombieEntity;
import me.kalmemarq.entity.model.ZombieModel;
import me.kalmemarq.render.DrawMode;
import me.kalmemarq.render.Framebuffer;
import me.kalmemarq.render.Frustum;
import me.kalmemarq.render.Shader;
import me.kalmemarq.render.Tessellator;
import me.kalmemarq.render.Texture;
import me.kalmemarq.render.Window;
import me.kalmemarq.render.WorldRenderer;
import me.kalmemarq.render.vertex.BufferBuilder;
import me.kalmemarq.render.vertex.VertexBuffer;
import me.kalmemarq.render.vertex.VertexLayout;
import me.kalmemarq.util.BlockHitResult;
import me.kalmemarq.util.Box;
import me.kalmemarq.util.IOUtils;
import me.kalmemarq.util.Keybinding;
import me.kalmemarq.util.TimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Game implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger("Main");
    private static final float MOUSE_SENSITIVITY = 0.08f;

    private static int entityRenderCount;

    private static Game instance;
    private Window window;
    private Texture terrainTexture;
    private Texture charTexture;
    private Shader selectionShader;
    private Shader terrainShader;
    private Shader terrainShadowShader;
    private Shader entityShader;
    private final double[] mouse = {0, 0, 0, 0};
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private World world;
    private WorldRenderer worldRenderer;
    private final Frustum frustum = new Frustum();
    private PlayerEntity player;
    private BlockHitResult blockHitResult;
    private VertexBuffer blockSelectionVertexBuffer;
    private Framebuffer framebuffer;
    private final List<ZombieEntity> zombies = new ArrayList<>();
    private final ZombieModel zombieModel = new ZombieModel();
    private boolean renderEntityHitboxes = false;

    public Game() {
        instance = this;
    }

    public static Game getInstance() {
        return instance;
    }

    public Window getWindow() {
        return this.window;
    }

    @Override
    public void run() {
        this.window = new Window(1024, 768);
        GLFW.glfwSetCursorPosCallback(this.window.getHandle(), (_w, x, y) -> this.onCursorPos(x, y));
        GLFW.glfwSetKeyCallback(this.window.getHandle(), (_w, k, sc, a, m) -> this.onKey(k, a));
        GLFW.glfwSetMouseButtonCallback(this.window.getHandle(), (_w, b, a, m) -> this.onMouseButton(b, a));

        LOGGER.info("LWJGL {}", Version.getVersion());
        LOGGER.info("GLFW {}", GLFW.glfwGetVersionString());
        LOGGER.info("OpenGL {}", GL11.glGetString(GL11.GL_VERSION));
        LOGGER.info("Renderer {}", GL11.glGetString(GL11.GL_RENDERER));
        LOGGER.info("Java {}", System.getProperty("java.version"));
        Callback debugMessageCallback = null;//GLUtil.setupDebugMessageCallback(System.err);

        this.framebuffer = new Framebuffer(this.window.getWidth(), this.window.getHeight());

        this.terrainTexture = new Texture();
        this.terrainTexture.load(IOUtils.getResourcesPath().resolve("textures/terrain.png"));
        this.charTexture = new Texture();
        this.charTexture.load(IOUtils.getResourcesPath().resolve("textures/char.png"));

        this.selectionShader = new Shader("selection");
        this.terrainShader = new Shader("terrain");
        this.terrainShadowShader = new Shader("terrain_fog");
        this.entityShader = new Shader("entity");

        this.blockSelectionVertexBuffer = this.createBlockSelectionVertexBuffer();

        this.world = new World(256, 256, 64);
        this.worldRenderer = new WorldRenderer(this.world);
        this.world.setStateListener(this.worldRenderer);

        this.player = new PlayerEntity(this.world);
        for (int i = 0; i < 100; ++i) {
            ZombieEntity zombie = new ZombieEntity(this.world);
            zombie.setPosition(128f, zombie.position.y, 128f);
            this.zombies.add(zombie);
        }

        this.window.grabMouse();

        long lastTime = TimeUtils.getCurrentMillis();
        int frameCounter = 0;

        try {
            GL11.glClearColor(0.5f, 0.8f, 1f, 1f);

            int tickCounter = 0;
            long prevTimeMillis = TimeUtils.getCurrentMillis();
            int ticksPerSecond = 60;
            float tickDelta = 0;

            while (!this.window.shouldClose()) {
                long now = TimeUtils.getCurrentMillis();
                float lastFrameDuration = (float)(now - prevTimeMillis) / (1000f / ticksPerSecond);
                prevTimeMillis = now;
                tickDelta += lastFrameDuration;
                int i = (int) tickDelta;
                tickDelta -= (float) i;

                for (; i > 0; --i) {
                    ++tickCounter;
                    this.update();
                }

                this.render(tickDelta);

                this.window.update();
                ++frameCounter;

                while (TimeUtils.getCurrentMillis() - lastTime > 1000L) {
                    lastTime += 1000L;
                    this.window.setTitle(frameCounter + " FPS " + tickCounter + " TPS E: " + entityRenderCount + "/" + this.zombies.size() + " C: " + WorldRenderer.chunksRendererPerFrame + "/" + this.worldRenderer.getChunkCount() + " x=" + String.format("%.3f", this.player.position.x) + ",y=" + String.format("%.4f", this.player.position.y) + ",z=" + String.format("%.3f", this.player.position.z));
                    frameCounter = 0;
                    tickCounter = 0;
                }

                entityRenderCount = 0;
                WorldRenderer.chunksRendererPerFrame = 0;
            }
        } catch (Exception e) {
            LOGGER.throwing(e);
        } finally {
            this.world.save();

            LOGGER.info("Closing");
            this.selectionShader.close();
            this.terrainShader.close();
            this.terrainShadowShader.close();
            this.entityShader.close();
            this.worldRenderer.close();
            this.terrainTexture.close();
            this.charTexture.close();
            this.blockSelectionVertexBuffer.close();
            this.framebuffer.close();
            Tessellator.cleanup();

            GL30.glBindVertexArray(0);
            GL30.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
            GL30.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
            if (debugMessageCallback != null) debugMessageCallback.free();
            this.window.close();
        }
    }

    private void update() {
        this.blockHitResult = this.player.raytrace(8);

        this.player.tick();

        Iterator<ZombieEntity> iter = this.zombies.iterator();
        while (iter.hasNext()) {
            ZombieEntity zombie = iter.next();
            zombie.tick();
            if (zombie.position.y < -64) {
                iter.remove();
            }
        }
    }

    private void render(float tickDelta) {
        this.framebuffer.resize(this.window.getWidth(), this.window.getHeight());

        this.framebuffer.bind();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.out.println(error);
        }

        GL11.glViewport(0, 0, this.window.getWidth(), this.window.getHeight());
        this.projection.setPerspective((float) Math.toRadians(70.0f), this.window.getWidth() / (float) this.window.getHeight(), 0.01f, 1000.0f);

        float cameraPosX = org.joml.Math.lerp(this.player.prevPosition.x, this.player.position.x, tickDelta);
        float cameraPosY = org.joml.Math.lerp(this.player.prevPosition.y, this.player.position.y, tickDelta);
        float cameraPosZ = org.joml.Math.lerp(this.player.prevPosition.z, this.player.position.z, tickDelta);

        this.modelView.identity();
        this.modelView.rotate((float) Math.toRadians(this.player.pitch), 1, 0, 0);
        this.modelView.rotate((float) Math.toRadians(this.player.yaw), 0, 1, 0);
        this.modelView.translate(-cameraPosX, -(cameraPosY + this.player.eyeHeight), -cameraPosZ);

        this.frustum.set(this.projection, this.modelView);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);

        this.terrainTexture.bind(0);

        this.terrainShader.bind();
        this.terrainShader.setUniform("uProjection", this.projection);
        this.terrainShader.setUniform("uModelView", this.modelView);
        this.terrainShader.setUniform("uColor", 1f, 1f, 1f, 1f);
        this.terrainShader.setUniform("uSampler0", 0);

        this.worldRenderer.render(this.terrainShader, this.frustum, 0);

        this.terrainShadowShader.bind();
        this.terrainShadowShader.setUniform("uProjection", this.projection);
        this.terrainShadowShader.setUniform("uModelView", this.modelView);
        this.terrainShadowShader.setUniform("uColor", 1f, 1f, 1f, 1f);
        this.terrainShadowShader.setUniform("uFogDensity", 0.04f);
        this.terrainShadowShader.setUniform("uFogColor", 0.0f, 0.0f, 0.0f, 1f);
        this.terrainShadowShader.setUniform("uSampler0", 0);

        this.worldRenderer.render(this.terrainShadowShader, this.frustum,  1);

        if (this.blockHitResult != null) {
            this.modelView.identity();
            this.modelView.rotate((float) Math.toRadians(this.player.pitch), 1, 0, 0);
            this.modelView.rotate((float) Math.toRadians(this.player.yaw), 0, 1, 0);
            this.modelView.translate(-cameraPosX, -(cameraPosY + this.player.eyeHeight), -cameraPosZ);

            this.selectionShader.bind();
            this.selectionShader.setUniform("uProjection", this.projection);
            this.selectionShader.setUniform("uColor", 1f, 1f, 1f, (float)Math.sin((double)TimeUtils.getCurrentMillis() / 100.0d) * 0.2f + 0.4f);
            this.modelView.translate(this.blockHitResult.x(), this.blockHitResult.y(), this.blockHitResult.z());
            this.selectionShader.setUniform("uModelView", this.modelView);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            this.blockSelectionVertexBuffer.bind();
            this.blockSelectionVertexBuffer.draw(6, (6 * this.blockHitResult.face().index) * 4);
            GL11.glDisable(GL11.GL_BLEND);
        }

        this.modelView.identity();
        this.modelView.rotate((float) Math.toRadians(this.player.pitch), 1, 0, 0);
        this.modelView.rotate((float) Math.toRadians(this.player.yaw), 0, 1, 0);
        this.modelView.translate(-cameraPosX, -(cameraPosY + this.player.eyeHeight), -cameraPosZ);

        this.entityShader.bind();
        this.entityShader.setUniform("uProjection", this.projection);
        this.entityShader.setUniform("uModelView", this.modelView);
        this.entityShader.setUniform("uColor", 1f, 1f, 1f, 1f);
        this.charTexture.bind(0);
        this.entityShader.setUniform("uSampler0", 0);

        Tessellator tessellator = Tessellator.getInstance();
        tessellator.begin(DrawMode.QUADS, VertexLayout.POS_UV_COLOR);
        BufferBuilder builder = tessellator.getBufferBuilder();

        for (ZombieEntity zombie : this.zombies) {
            if (!this.frustum.isVisible(zombie.box)) continue;
            this.zombieModel.render(builder, zombie, tickDelta);
            entityRenderCount++;
        }

        tessellator.draw();

        if (this.renderEntityHitboxes) {
            this.selectionShader.bind();
            this.selectionShader.setUniform("uProjection", this.projection);
            this.selectionShader.setUniform("uModelView", this.modelView);
            this.selectionShader.setUniform("uColor", 1f, 1f, 1f, 1f);

            tessellator.begin(DrawMode.LINES, VertexLayout.POS);
            for (ZombieEntity zombie : this.zombies) {
                if (!this.frustum.isVisible(zombie.box)) continue;
                this.renderBox(builder, zombie.box);
            }
            tessellator.draw();
        }

        GL30.glDisable(GL30.GL_CULL_FACE);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glDisable(GL30.GL_DEPTH_TEST);

        GL11.glViewport(0, 0, this.window.getWidth(), this.window.getHeight());
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

//        this.framebuffer.blitTo(0, 0, 0, this.window.getWidth(), this.window.getHeight());
        this.framebuffer.draw();
    }

    private void renderBox(BufferBuilder builder,Box box) {
        this.renderBox(builder, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private void renderBox(BufferBuilder builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        builder.vertex(minX, minY, minZ); builder.vertex(minX, maxY, minZ);
        builder.vertex(maxX, minY, minZ); builder.vertex(maxX, maxY, minZ);
        builder.vertex(minX, minY, maxZ); builder.vertex(minX, maxY, maxZ);
        builder.vertex(maxX, minY, maxZ); builder.vertex(maxX, maxY, maxZ);

        builder.vertex(minX, minY, minZ); builder.vertex(maxX, minY, minZ);
        builder.vertex(minX, minY, minZ); builder.vertex(minX, minY, maxZ);
        builder.vertex(minX, minY, maxZ); builder.vertex(maxX, minY, maxZ);
        builder.vertex(maxX, minY, minZ); builder.vertex(maxX, minY, maxZ);

        builder.vertex(minX, maxY, minZ); builder.vertex(maxX, maxY, minZ);
        builder.vertex(minX, maxY, minZ); builder.vertex(minX, maxY, maxZ);
        builder.vertex(minX, maxY, maxZ); builder.vertex(maxX, maxY, maxZ);
        builder.vertex(maxX, maxY, minZ); builder.vertex(maxX, maxY, maxZ);
    }

    private void onCursorPos(double x, double y) {
        this.mouse[2] = x - this.mouse[0];
        this.mouse[3] = y - this.mouse[1];
        this.mouse[0] = x;
        this.mouse[1] = y;

        float dx = (float) this.mouse[2];
        float dy = (float) this.mouse[3];
        this.player.turn(dx * MOUSE_SENSITIVITY, dy * MOUSE_SENSITIVITY);
        this.mouse[2] = 0;
        this.mouse[3] = 0;
    }

    private void onMouseButton(int button, int action) {
        if (action != GLFW.GLFW_RELEASE && this.blockHitResult != null) {
            if (button == 1) {
                this.world.setBlockId(this.blockHitResult.x(), this.blockHitResult.y(), this.blockHitResult.z(), 0);
            } else if (button == 0) {
                int x = this.blockHitResult.x() + this.blockHitResult.face().normalX;
                int y = this.blockHitResult.y() + this.blockHitResult.face().normalY;
                int z = this.blockHitResult.z() + this.blockHitResult.face().normalZ;
                if (!this.player.box.intersects(x, y, z, x + 1, y + 1, z + 1)) {
                    this.world.setBlockId(x, y, z, 1);
                }
            }
        }
    }

    private void onKey(int key, int action) {
        if (action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                GLFW.glfwSetWindowShouldClose(this.window.getHandle(), true);
            } else if (Keybinding.TOGGLE_VSYNC.test(key)) {
                this.window.toggleVsync();
            } else if (Keybinding.TOGGLE_FULLSCREEN.test(key)) {
                this.window.toggleFullscreen();
            } else if (Keybinding.SAVE_WORLD_TO_DISK.test(key)) {
                this.world.save();
            } else if (Keybinding.GO_TO_RANDOM_POS.test(key)) {
                this.player.goToRandomPosition();
            } else if (Keybinding.FLY.test(key)) {
                this.player.canFly = !this.player.canFly;
            } else if (Keybinding.NO_CLIP.test(key)) {
                this.player.noClip = !this.player.noClip;
            } else if (key == GLFW.GLFW_KEY_F8) {
                this.renderEntityHitboxes = !this.renderEntityHitboxes;
            }
        }
    }

    private VertexBuffer createBlockSelectionVertexBuffer() {
        VertexBuffer blockSelectionVertexBuffer = new VertexBuffer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(VertexLayout.POS.stride * 4 * 6);
            BufferBuilder builder = new BufferBuilder(MemoryUtil.memAddress(buffer));

            float x0 = 0f;
            float y0 = 0f;
            float z0 = 0f;
            float x1 = 1f;
            float y1 = 1f;
            float z1 = 1f;
            float offset = 0.0009f;

            builder.begin();
            builder.vertex(x0, y0 - offset, z0);
            builder.vertex(x1, y0 - offset, z0);
            builder.vertex(x1, y0 - offset, z1);
            builder.vertex(x0, y0 - offset, z1);

            builder.vertex(x0, y1 + offset, z0);
            builder.vertex(x0, y1 + offset, z1);
            builder.vertex(x1, y1 + offset, z1);
            builder.vertex(x1, y1 + offset, z0);

            builder.vertex(x0, y0, z0 - offset);
            builder.vertex(x0, y1, z0 - offset);
            builder.vertex(x1, y1, z0 - offset);
            builder.vertex(x1, y0, z0 - offset);

            builder.vertex(x0, y0, z1 + offset);
            builder.vertex(x1, y0, z1 + offset);
            builder.vertex(x1, y1, z1 + offset);
            builder.vertex(x0, y1, z1 + offset);

            builder.vertex(x0 - offset, y0, z0);
            builder.vertex(x0 - offset, y0, z1);
            builder.vertex(x0 - offset, y1, z1);
            builder.vertex(x0 - offset, y1, z0);

            builder.vertex(x1 + offset, y0, z0);
            builder.vertex(x1 + offset, y1, z0);
            builder.vertex(x1 + offset, y1, z1);
            builder.vertex(x1 + offset, y0, z1);

            blockSelectionVertexBuffer.upload(DrawMode.QUADS, VertexLayout.POS, buffer, builder.end());
        }
        return blockSelectionVertexBuffer;
    }
}
