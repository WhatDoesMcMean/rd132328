#version 330

#ifdef FOG
#include "fog.glsl"
#endif

uniform vec4 uColor;
#ifdef FOG
uniform float uFogDensity;
uniform vec4 uFogColor;
#endif
uniform sampler2D uSampler0;

in vec2 vUV;
in vec4 vColor;
#ifdef FOG
in float vVertexDistance;
#endif
out vec4 outColor;

void main() {
    vec4 color = uColor * vColor * texture(uSampler0, vUV);
#ifdef FOG
    color = fogExp(color, vVertexDistance, uFogDensity, uFogColor);
#endif
    outColor = color;
}