#version 330

uniform vec4 uColor;
uniform sampler2D uSampler0;

in vec2 vUV;
in vec4 vColor;

out vec4 outColor;

void main() {
    outColor = vColor * uColor * texture(uSampler0, vUV);
}