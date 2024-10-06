vec4 fogExp(vec4 inColor, float vertexDistance, float fogDensity, vec4 fogColor) {
    return mix(fogColor, inColor, exp(-(fogDensity * vertexDistance)));
}