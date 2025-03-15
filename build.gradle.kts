plugins {
    idea
    application
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "dev.cryptospace"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.bundles.logging)

    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.core) {
        natives()
    }

    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.glfw) {
        natives()
    }

    implementation(libs.lwjgl.openal)
    implementation(libs.lwjgl.openal) {
        natives()
    }

    implementation(libs.lwjgl.vulkan)
    implementation(libs.lwjgl.vulkan) {
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

idea {
    // Automatically download sources and javadocs for dependencies
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
