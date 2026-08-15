#version 330

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // Keep the projection on the actual visible pixels of the baked block model. This is especially
    // important for crossed/cutout models such as grass, cave vines, flowers and crops.
    float modelAlpha = texture(Sampler0, texCoord0).a;
    if (modelAlpha < 0.10) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * modelAlpha);
}
