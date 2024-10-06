#version 330

uniform mat4 uProjection;
uniform mat4 uModelView;

layout(location = 0) in vec3 aPosition;

void main() {
    gl_Position = uProjection * uModelView * vec4(aPosition, 1.0);
}