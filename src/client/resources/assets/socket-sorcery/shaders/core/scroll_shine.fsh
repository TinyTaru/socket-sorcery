#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 scroll = texture(Sampler0, texCoord0);
    if (scroll.a < 0.1) {
        discard;
    }

    // A broad, bright band travels from the upper-left to the lower-right and then repeats.
    float diagonal = texCoord0.x + texCoord0.y;
    float progress = fract(GameTime * 0.55);
    float center = mix(-0.45, 2.45, progress);
    float distanceFromBand = abs(diagonal - center);
    float band = 1.0 - smoothstep(0.02, 0.18, distanceFromBand);
    float core = 1.0 - smoothstep(0.005, 0.055, distanceFromBand);
    float alpha = (band * 0.55 + core * 0.45) * scroll.a;

    fragColor = vec4(1.0, 0.98, 0.82, alpha);
}
