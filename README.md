# Kotlin Vulkan

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

> **Disclaimer**: This is a hobby project. Expect bugs. Expect weird design decisions.
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
    - [ ] Vertex buffers and attributes
    - [ ] Textures and materials
    - [ ] Basic lighting system
    - [ ] 3D model loading and animation

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

```bash
./gradlew run
```

or

```powershell
.\gradlew.bat run
```

Supports the following CLI properties:

```bash
-Duse-x11 # Use X11 instead of Wayland, only works on Linux in a Wayland session. Otherwise ignored.
-Duse-validation-layers # Enable Vulkan validation layers for debugging. Not recommended for performance. Requires Vulkan SDK to be installed.
```
