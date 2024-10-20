#version 330

#ifdef COLORBLIND_MODE
#include "colorblindness.glsl"
#endif

uniform sampler2D uSampler0;

in vec2 vTexCoord;

out vec4 outColor;

const float offset = 1.0 / 200.0;

void main() {
#if defined(INVERTED)
    outColor = vec4(1.0 - texture(uSampler0, vTexCoord).rgb, 1.0);
#elif defined(GRAYSCALE)
    vec4 color = texture(uSampler0, vTexCoord);
    float average = 0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b;
    outColor = vec4(average, average, average, 1.0);
#elif defined(BLUR)
    vec2 offsets[9] = vec2[](
        vec2(-offset,  offset), // top-left
        vec2( 0.0f,    offset), // top-center
        vec2( offset,  offset), // top-right
        vec2(-offset,  0.0f),   // center-left
        vec2( 0.0f,    0.0f),   // center-center
        vec2( offset,  0.0f),   // center-right
        vec2(-offset, -offset), // bottom-left
        vec2( 0.0f,   -offset), // bottom-center
        vec2( offset, -offset)  // bottom-right
    );

    float kernel[9] = float[](
        1.0 / 16, 2.0 / 16, 1.0 / 16,
        2.0 / 16, 4.0 / 16, 2.0 / 16,
        1.0 / 16, 2.0 / 16, 1.0 / 16
    );

    vec3 sampleTex[9];
    for (int i = 0; i < 9; i++) {
        sampleTex[i] = vec3(texture(uSampler0, vTexCoord.st + offsets[i]));
    }
    vec3 col = vec3(0.0);

    for (int i = 0; i < 9; i++)
        col += sampleTex[i] * kernel[i];

    outColor = vec4(col, 1.0);
#elif defined(MATRIX)
    vec4 color = texture(uSampler0, vTexCoord);
    outColor = vec4(pow(color.r, 1.4), color.g, pow(color.b, 1.6), color.a);
#elif defined(MEXICO)
    vec4 color = texture(uSampler0, vTexCoord);
    outColor = vec4(color.r, min(pow(color.g, 1.25), 1.0), min(pow(color.b, 1.75), 1.0), color.a);
#elif defined(COLORBLIND_MODE)
    vec4 color = texture(uSampler0, vTexCoord);
	outColor = vec4(simulate_colorblindness(color.rgb, COLORBLIND_MODE), color.a);
#else
    outColor = texture(uSampler0, vTexCoord);
#endif
}