plugins {
    id("anvil-convention")
}

dependencies {
    api(libs.slf4j.api)
    api(libs.lwjgl.core)
    api(libs.lwjgl.glfw)
    api(libs.lwjgl.stb)
    api(libs.lwjgl.assimp)
}
