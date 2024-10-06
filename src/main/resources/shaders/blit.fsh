#version 330

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
#else
    outColor = texture(uSampler0, vTexCoord);
#endif
}