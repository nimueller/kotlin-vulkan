plugins {
    id("anvil-convention")
    application
}

dependencies {
    api(projects.anvilCore)

    implementation(projects.anvilGlfw)
    implementation(projects.anvilVulkan)

    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.lwjgl.core) { natives() }
    runtimeOnly(libs.lwjgl.glfw) { natives() }
    runtimeOnly(libs.lwjgl.openal) { natives() }
    runtimeOnly(libs.lwjgl.vulkan) {
        // Vulkan driver must be manually included for macOS and is pre-installed on Windows and Linux
        artifact {
            classifier = "natives-macos"
        }
    }
}

fun ExternalModuleDependency.natives() {
    artifact {
        classifier = "natives-windows"
    }
    artifact {
        classifier = "natives-linux"
    }
    artifact {
        classifier = "natives-macos"
    }
}

application {
    mainClass.set("dev.cryptospace.vulkan.MainKt")
}
