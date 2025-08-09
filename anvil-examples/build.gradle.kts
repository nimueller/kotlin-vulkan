plugins {
    id("anvil-convention")
    application
}

dependencies {
    api(projects.anvilCore)

    implementation(projects.anvilOpengl)
    implementation(projects.anvilVulkan)
    implementation(libs.lwjgl.opengl)

    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.lwjgl.core) { natives() }
    runtimeOnly(libs.lwjgl.glfw) { natives() }
    runtimeOnly(libs.lwjgl.openal) { natives() }
    runtimeOnly(libs.lwjgl.opengl) { natives() }
    runtimeOnly(libs.lwjgl.stb) { natives() }
    runtimeOnly(libs.lwjgl.assimp) { natives() }
    runtimeOnly(libs.lwjgl.vulkan) {
        // Vulkan driver must be manually included for macOS and is pre-installed on Windows and Linux
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            artifact {
                classifier = "natives-macos"
            }
        }
    }
}

fun ExternalModuleDependency.natives() {
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.contains("win") -> {
            artifact {
                classifier = "natives-windows"
            }
        }

        osName.contains("linux") -> {
            artifact {
                classifier = "natives-linux"
            }
        }

        osName.contains("mac") -> {
            artifact {
                classifier = "natives-macos"
            }
        }

        else -> error("Unsupported operating system: $osName")
    }
}

application {
    if (project.hasProperty("mainClass")) {
        mainClass.set(project.property("mainClass") as String)
    } else {
        mainClass.set("dev.cryptospace.anvil.app.RotatingQuad3DKt")
    }

    applicationDefaultJvmArgs = listOf("-Duse-validation-layers")
}
