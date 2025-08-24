# Kotlin Vulkan [![Build](https://github.com/nimueller/kotlin-vulkan/actions/workflows/build.yml/badge.svg)](https://github.com/nimueller/kotlin-vulkan/actions/workflows/build.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nimueller_kotlin-vulkan&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=nimueller_kotlin-vulkan)

Just some simple Vulkan examples in Kotlin with LWJGL3.
Don't expect anything fancy, just some basic examples to get started as a private project for learning Vulkan.
Don't know where this is going yet.

**Why no existing engine?**

*Short answer:*\
Because learning by doing is the best way to understand how things work, and I want to learn things!

*Long answer:*\
Who needs those fancy pre-built engines when you can spend countless hours debugging memory leaks and questioning your
life choices?
It's like choosing to build a car from scratch instead of buying one, probably a bit crazy, but hey, at least you'll
know exactly why everything breaks!
Plus, there's something magical about the moment when your first triangle finally appears on screen after days
of wrestling with Vulkan boilerplate code.
No existing engine can give you that kind of satisfaction (or frustration).

**Why Kotlin?**

Just have a look at this:

```kotlin
val pipelineTextured3DLayout =
    PipelineLayoutBuilder(logicalDevice = deviceManager.logicalDevice).apply {
        pushConstant(EnumSet.of(ShaderStage.VERTEX), Mat4)
        pushConstant(EnumSet.of(ShaderStage.FRAGMENT), NativeTypeLayout.FLOAT)
        descriptorSetLayouts.add(frameDescriptorSetLayout)
        descriptorSetLayouts.add(textureDescriptorSetLayout)
    }.build()

val pipelineTextured3D: Pipeline =
    PipelineBuilder(
        logicalDevice = deviceManager.logicalDevice,
        renderPass = renderPass,
        pipelineLayout = pipelineTextured3DLayout,
    ).apply {
        vertexLayout = TexturedVertex3
        shaderModules[ShaderStage.VERTEX] = ShaderModule(deviceManager.logicalDevice, "/shaders/vert.spv")
        shaderModules[ShaderStage.FRAGMENT] = ShaderModule(deviceManager.logicalDevice, "/shaders/frag.spv")
    }.build()
```

Have you ever seen such a simple Vulkan pipeline creation?
It is just awesome.

> [!WARNING]
> This is a hobby project. Expect bugs. Expect weird design decisions.
> Also expect random refactors at 3AM.

## Checklist

- [x] Basic initialization
    - [x] Create Vulkan instance
    - [x] Setup validation layers
    - [x] Create surface and select device
    - [x] Create logical device and queues
- [x] Resource management
    - [x] Create swapchain and image views
    - [x] Create render pass
    - [x] Create framebuffers
- [x] Pipeline setup
    - [x] Create shaders and pipeline
    - [x] Configure graphics pipeline
- [x] Drawing
    - [x] Setup command buffers
    - [x] Draw first triangle
- [ ] Advanced features
    - [x] Vertex buffers and attributes
    - [x] Textures
    - [x] Depth buffering
    - [ ] 3D model loading
    - [ ] Basic lightning system
    - [ ] Scene Management
- [ ] Optimization
    - [ ] Proper Transfer queue for staging buffers
    - [ ] Better Vulkan Memory allocation using Vulkan Memory Allocator

## Building

```bash
./gradlew build
```

on Linux or

```powershell
.\gradlew.bat build
```

on Windows.
Requires JDK 21 to be installed, nothing else.

## Running

Examples are located in the `anvil-examples` module.
The following examples are currently supported:

- [Vulkan Tutorial](https://vulkan-tutorial.com/) examples (first tests and learning Vulkan basics)
    - 🎉 My first Vulkan Triangle
      ([anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/Triangle2D.kt](./anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/Triangle2D.kt)):
      after following article
      [Vertex Buffer creation](https://vulkan-tutorial.com/Vertex_buffers/Vertex_buffer_creation)
      ```bash
      ./gradlew run -PmainClass=dev.cryptospace.anvil.app.Triangle2DKt
      ```
    - Colored Quad
      ([anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/ColoredQuad2D.kt](./anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/ColoredQuad2D.kt)):
      after following article
      [Index buffer](https://vulkan-tutorial.com/Vertex_buffers/Index_buffer)
      ```bash
      ./gradlew run -PmainClass=dev.cryptospace.anvil.app.ColoredQuad2DKt
      ```
    - Rotating Quad
      ([anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/RotatingQuad3D.kt](./anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/RotatingQuad3D.kt)):
      after following article
      [Descriptor pool and sets](https://vulkan-tutorial.com/Uniform_buffers/Descriptor_pool_and_sets)
      ```bash
      ./gradlew run -PmainClass=dev.cryptospace.anvil.app.RotatingQuad3DKt
      ```
    - Rotating Textured Quad (3D)
      ([anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/RotatingTexturedQuad3D.kt](./anvil-examples/src/main/kotlin/dev/cryptospace/anvil/app/RotatingTexturedQuad3D.kt)):
      after following article
      [Texture loading](https://vulkan-tutorial.com/Texture_loading/Texture_loading)

Supports the following CLI properties (must be set in the file `build.gradle.kts` in the `anvil-examples` directory)

```bash
-Duse-x11 # Use X11 instead of Wayland, only works on Linux in a Wayland session. Otherwise ignored.
-Duse-validation-layers # Enable Vulkan validation layers for debugging. Not recommended for performance. Requires Vulkan SDK to be installed.
```
