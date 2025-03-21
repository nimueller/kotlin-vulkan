plugins {
    id("anvil-convention")
}

dependencies {
    api(projects.anvilCore)

    implementation(libs.lwjgl.vulkan)
}
