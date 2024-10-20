// Credits: https://github.com/4ian/GDevelop/discussions/6356

#define ROD_MONOCHROMACY 0
#define CONE_MONOCHROMACY 1
#define PROTANOPIA 2
#define DEUTERANOPIA 3
#define TRITANOPIA 4
#define PROTANOMALY 5
#define DEUTERANOMALY 6
#define TRITANOMALY 7

vec3 lms_to_rgb(vec3 lms) {
    vec3 result;
    result.r = 5.47221206 * lms.x + -4.6419601 * lms.y + 0.16963708 * lms.z;
    result.g = -1.1252419 * lms.x + 2.29317094 * lms.y + -0.1678952 * lms.z;
    result.b = 0.02980165 * lms.x + -0.19318073 * lms.y + 1.16364789 * lms.z;
    return result;
}

vec3 simulate_colorblindness(vec3 inputColor, int mode) {
    float l, m, s;

    // Convert to LMS
    float L = 0.31399022 * inputColor.r + 0.63951294 * inputColor.g + 0.04649755 * inputColor.b;
    float M = 0.15537241 * inputColor.r + 0.75789446 * inputColor.g + 0.08670142 * inputColor.b;
    float S = 0.01775239 * inputColor.r + 0.10944209 * inputColor.g + 0.87256922 * inputColor.b;

    // Simulate color blindness
    if (mode == CONE_MONOCHROMACY) {
        // Blue Cone Monochromat (high light conditions): only brightness can
        // be detected, with blues greatly increased and reds nearly invisible
        // (0.001% population)
        // Note: This looks different from what many colorblindness simulators
        // show because this simulation assumes high light conditions. In low
        // light conditions, a blue cone monochromat can see a limited range of
        // color because both rods and cones are active. However, as we expect
        // a player to be looking at a lit screen, this simulation of high
        // light conditions is more useful.
        l = 0.01775 * L + 0.10945 * M + 0.87262 * S;
        m = 0.01775 * L + 0.10945 * M + 0.87262 * S;
        s = 0.01775 * L + 0.10945 * M + 0.87262 * S;
    } else if (mode == ROD_MONOCHROMACY) {
        // Rod Monochromat (Achromatopsia): only brightness can be detected
        // (0.003% population)
        l = 0.212656 * L + 0.715158 * M + 0.072186 * S;
        m = 0.212656 * L + 0.715158 * M + 0.072186 * S;
        s = 0.212656 * L + 0.715158 * M + 0.072186 * S;
    } else if (mode == PROTANOPIA) {
        // Protanopia: reds are greatly reduced (1% men)
        l = 0.0 * L + 1.05118294 * M + -0.05116099 * S;
        m = 0.0 * L + 1.0 * M + 0.0 * S;
        s = 0.0 * L + 0.0 * M + 1.0 * S;
    } else if (mode == DEUTERANOPIA) {
        // Deuteranopia: greens are greatly reduced (1% men)
        l = 1.0 * L + 0.0 * M + 0.0 * S;
        m = 0.9513092*L + 0.0 * M + 0.04866992 * S;
        s = 0.0 * L + 0.0 * M + 1.0 * S;
    } else if (mode == TRITANOPIA) {
        // Tritanopia: blues are greatly reduced (0.003% population)
        l = 1.0 * L + 0.0 * M + 0.0 * S;
        m = 0.0 * L + 1.0 * M + 0.0 * S;
        s = -0.86744736*L + 1.86727089 * M + 0.0 * S;
    } else if (mode == PROTANOMALY) {
        // Protanomaly (moderate severity): reds are green-shifted
        l = 0.458064 * L + 0.679578 * M + -0.137642 * S;
        m = 0.092785 * L + 0.846313 * M + 0.060902 * S;
        s = -0.007494 * L + -0.016807 * M + 1.024301 * S;
    } else if (mode == DEUTERANOMALY) {
        // Deuteranomaly (moderate severity): greens are red-shifted
        l = 0.547494 * L + 0.607765 * M + -0.155259 * S;
        m = 0.181692 * L + 0.781742 * M + 0.036566 * S;
        s = -0.010410 * L + 0.027275 * M + 0.983136 * S;
    } else if (mode == TRITANOMALY) {
        // Tritanomaly (moderate severity): blues are yellow-shifted
        l = 1.017277 * L + 0.027029 * M + -0.044306 * S;
        m = -0.006113 * L + 0.958479 * M + 0.047634 * S;
        s = 0.006379 * L + 0.248708 * M + 0.744913 * S;
    } else {
        // Invalid colorblindness type
        l = 0.212656 * L + 0.715158 * M + 0.072186 * S;
        m = 0.0 * L + 0.0 * M + 0.0 * S;
        s = 0.212656 * L + 0.715158 * M + 0.072186 * S;
    }

    return lms_to_rgb(vec3(l, m, s));
}