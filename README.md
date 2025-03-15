# Kotlin Vulkan

Just some simple Vulkan examples in Kotlin with LWJGL3.
Don't expect anything fancy, just some basic examples to get started as a private project for learning Vulkan.
Don't know where this is going yet.

## Building

```bash
./gradlew build
```

on Linux or

```powershell
.\gradlew.bat build
```

on Windows. Requires JDK 21 to be installed, nothing else.

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
