#version 450
#extension GL_EXT_nonuniform_qualifier : require

layout (set = 1, binding = 0) uniform sampler2D texSampler[];

layout (push_constant) uniform PushConstants {
    layout (offset = 64) int textureIndex;
    vec4 albedo;
    float roughness;
    float metallic;
    float emissive;
    float ao;
} pushConstants;

layout (location = 0) in vec3 fragColor;
layout (location = 1) in vec2 fragTexCord;

layout (location = 0) out vec4 outColor;

void main() {
    vec4 color;
    if (pushConstants.textureIndex == 0) {
        color = vec4(fragColor, 1.0f) * pushConstants.albedo;
    } else {
        color = texture(texSampler[pushConstants.textureIndex], fragTexCord) * pushConstants.albedo;
    }

    // Very basic lighting/material application for now
    outColor = color;
}
