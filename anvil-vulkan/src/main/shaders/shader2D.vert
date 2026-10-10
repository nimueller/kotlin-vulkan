#version 450

// Must match dev.cryptospace.anvil.vulkan.pipeline.InstanceData
layout (set = 0, binding = 1) readonly buffer Instances {
    mat4 models[];
} instances;

layout (location = 0) in vec2 inPosition;
layout (location = 1) in vec3 inColor;
layout (location = 2) in vec2 inTexCoord;

layout (location = 0) out vec3 fragColor;
layout (location = 1) out vec2 fragTexCoord;

void main() {
    gl_Position = instances.models[gl_InstanceIndex] * vec4(inPosition, 0.0, 1.0);
    fragColor = inColor;
    fragTexCoord = inTexCoord;
}
