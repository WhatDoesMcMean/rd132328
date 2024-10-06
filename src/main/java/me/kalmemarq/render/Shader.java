package me.kalmemarq.render;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.kalmemarq.util.IOUtils;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shader implements Closeable {
    private final int id;
    private final Object2IntMap<String> uniformLocations;
    private FloatBuffer matrixBuffer;

    public Shader(String name) {
        ObjectNode node;
        try {
            node = IOUtils.OBJECT_MAPPER.readValue(Files.readString(IOUtils.getResourcesPath().resolve("shaders/" + name + ".json")), ObjectNode.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader config", e);
        }

       Defines defines = readDefines(node);

        this.id = GL30.glCreateProgram();
        this.uniformLocations = new Object2IntOpenHashMap<>();

        JsonNode sourcesNode = node.get("sources");
        int vertex = createShaderStage(GL30.GL_VERTEX_SHADER, sourcesNode.get("vertex").asText(), defines);
        int fragment = createShaderStage(GL30.GL_FRAGMENT_SHADER, sourcesNode.get("fragment").asText(), defines);

        GL30.glAttachShader(this.id, vertex);
        GL30.glAttachShader(this.id, fragment);

        GL30.glLinkProgram(this.id);

        if (GL30.glGetProgrami(this.id, GL30.GL_LINK_STATUS) == 0) {
            System.out.println(GL30.glGetProgramInfoLog(this.id));
        }

        GL30.glDetachShader(this.id, vertex);
        GL30.glDetachShader(this.id, fragment);
        GL30.glDeleteShader(vertex);
        GL30.glDeleteShader(fragment);
    }

    private static int createShaderStage(int type, String sourceFile, Defines defines) {
        int shader = GL30.glCreateShader(type);

        try {
            GL30.glShaderSource(shader, processSource(defines, Files.readString(IOUtils.getResourcesPath().resolve("shaders/" + sourceFile))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GL30.glCompileShader(shader);
        if (GL30.glGetShaderi(shader, GL30.GL_COMPILE_STATUS) == 0) {
            System.out.println(GL30.glGetShaderInfoLog(shader));
        }

        return shader;
    }

    private static Defines readDefines(JsonNode shaderConfig) {
        Defines defines = new Defines();
        if (shaderConfig.has("defines")) {
            JsonNode definesNode = shaderConfig.get("defines");
            if (definesNode.has("values")) {
                for (Map.Entry<String, JsonNode> item : definesNode.get("values").properties()) {
                    defines.values.put(item.getKey(), item.getValue().asText());
                }
            }

            if (definesNode.has("flags")) {
                for (JsonNode item : definesNode.get("flags")) {
                    defines.flags.add(item.asText());
                }
            }
        }
        return defines;
    }

    public int getUniformLocation(String name) {
        return this.uniformLocations.computeIfAbsent(name, (key) -> GL30.glGetUniformLocation(this.id, (String) key));
    }

    public void setUniform(String name, int... values) {
        int location = this.getUniformLocation(name);
        if (location == -1) {
            return;
        }

        switch (values.length) {
            case 1 -> GL30.glUniform1i(location, values[0]);
            case 2 -> GL30.glUniform2i(location, values[0], values[1]);
            case 3 -> GL30.glUniform3i(location, values[0], values[1], values[2]);
            case 4 -> GL30.glUniform4i(location, values[0], values[1], values[2], values[3]);
            default -> throw new IllegalStateException("Uniform int can only have 1 to 4 values");
        }
    }

    public void setUniform(String name, float... values) {
        int location = this.getUniformLocation(name);
        if (location == -1) {
            return;
        }

        switch (values.length) {
            case 1 -> GL30.glUniform1f(location, values[0]);
            case 2 -> GL30.glUniform2f(location, values[0], values[1]);
            case 3 -> GL30.glUniform3f(location, values[0], values[1], values[2]);
            case 4 -> GL30.glUniform4f(location, values[0], values[1], values[2], values[3]);
            default -> throw new IllegalStateException("Uniform int can only have 1 to 4 values");
        }
    }

    public void setUniform(String name, Matrix4f matrix) {
        if (this.matrixBuffer == null) {
            this.matrixBuffer = MemoryUtil.memAllocFloat(16);
        }

        int location = this.getUniformLocation(name);
        if (location == -1) {
            return;
        }

        GL30.glUniformMatrix4fv(location, false, matrix.get(this.matrixBuffer));
    }

    public void bind() {
        GL30.glUseProgram(this.id);
    }

    public void close() {
        GL30.glDeleteProgram(this.id);
        if (this.matrixBuffer != null) {
            MemoryUtil.memFree(this.matrixBuffer);
        }
    }

    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^#include\\s+[\\\"<](?<includepath>[a-zA-Z.]+)[\\\">]", Pattern.MULTILINE);

    private static String processSource(Defines defines, String source) {
        StringBuilder builder = new StringBuilder();
        builder.append(source, 0, source.indexOf("\n", source.indexOf("#version")) + 1);

        for (Map.Entry<String, String> define : defines.values.entrySet()) {
            builder.append("#define ").append(define.getKey()).append(' ').append(define.getValue()).append('\n');
        }

        for (String flag : defines.flags) {
            builder.append("#define ").append(flag).append('\n');
        }
        builder.append(source.substring(source.indexOf("\n", source.indexOf("#version")) + 1));

        Matcher matcher = INCLUDE_PATTERN.matcher(builder.toString());
        StringBuilder processedSource = new StringBuilder();

        while (matcher.find()) {
            String includePath = matcher.group("includepath");
            try {
                matcher.appendReplacement(processedSource, Files.readString(IOUtils.getResourcesPath().resolve("shaders/" + includePath)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        matcher.appendTail(processedSource);
        return processedSource.toString();
    }

    public static class Defines {
        public final Map<String, String> values;
        public final Set<String> flags;

        public Defines() {
            this.values = new HashMap<>();
            this.flags = new HashSet<>();
        }
    }
}
