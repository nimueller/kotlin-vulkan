package dev.cryptospace.vulkan

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryUtil.NULL

fun main() {
    if (useWayland()) {
        println("Using Wayland")
        glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)
    } else if (glfwPlatformSupported(GLFW_PLATFORM_X11)) {
        println("Using X11")
        glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11)
    }

    GLFWErrorCallback.createPrint().set()

    val initSuccessful = glfwInit()
    check(initSuccessful) { "Unable to initialize GLFW" }

    try {
        val window = createWindow()
        val vulkan = Vulkan()

        try {
            println(vulkan.instance.capabilities)
        } finally {
            vulkan.close()
        }

        glfwShowWindow(window.handle);
        loop(window)
    } finally {
        glfwTerminate()
    }
}

private fun useWayland(): Boolean {
    val preferX11 = System.getProperty("use-x11") != null

    if (preferX11) {
        return false
    }

    val waylandSupported = glfwPlatformSupported(GLFW_PLATFORM_WAYLAND)
    return waylandSupported
}

private fun createWindow(): Window {
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)

    val windowHandle = glfwCreateWindow(800, 600, "Hello World", NULL, NULL)
    check(windowHandle != NULL) { "Unable to create window" }
    return Window(windowHandle)
}

private fun loop(window: Window) {
    var lastFrameTime = System.nanoTime()

    while (!window.shouldClose()) {
        val currentFrameTime = System.nanoTime()
        val timeSinceLastFrame = currentFrameTime - lastFrameTime
        val fps = 1_000_000_000.0 / timeSinceLastFrame.toDouble()
        println()
        print("Time since last frame: $timeSinceLastFrame ns ")
        print("FPS: $fps")

        glfwPollEvents()
        lastFrameTime = System.nanoTime()
    }
}
