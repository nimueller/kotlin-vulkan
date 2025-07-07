plugins {
    id("anvil-convention")
}

dependencies {
    api(projects.anvilCore)

    implementation(libs.lwjgl.vulkan)
}

val compileShaders by tasks.registering {
    val sourceSet = sourceSets["main"]
    val inputDir = file("${sourceSet.resources.srcDirs.first()}/shaders")
    val outputDir = file("${sourceSet.output.resourcesDir}/shaders")

    inputs.dir(inputDir)
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()

        fun compileShader(shaderFile: File, outputFile: File) {
            val process = ProcessBuilder(
                "glslc",
                shaderFile.absolutePath,
                "-o",
                outputFile.absolutePath,
            ).inheritIO().start()

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw GradleException("Shader compilation failed for: ${shaderFile.name}")
            }
        }

        compileShader(inputDir.resolve("shader.vert"), outputDir.resolve("vert.spv"))
        compileShader(inputDir.resolve("shader.frag"), outputDir.resolve("frag.spv"))
    }
}

tasks.build {
    dependsOn(compileShaders)
}

tasks.jar {
    dependsOn(compileShaders)
}

tasks.named("sonarlintMain") {
    dependsOn(tasks.named("compileShaders"))
}
