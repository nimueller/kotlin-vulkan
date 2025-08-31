#version 450
#extension GL_EXT_nonuniform_qualifier : require

layout (set = 1, binding = 0) uniform sampler2D texSampler[];

layout (push_constant) uniform PushConstants {
    mat4 model;
    int materialIndex;
} pushConstants;

layout (location = 0) in vec3 fragColor;
layout (location = 1) in vec2 fragTexCord;

layout (location = 0) out vec4 outColor;

void main() {
    if (pushConstants.materialIndex == 0) {
        outColor = vec4(fragColor, 1.0f);
    } else {
        outColor = texture(texSampler[pushConstants.materialIndex], fragTexCord);
    }
}
