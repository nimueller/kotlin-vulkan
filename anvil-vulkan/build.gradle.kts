plugins {
    id("anvil-convention")
}

dependencies {
    api(projects.anvilCore)

    implementation(libs.lwjgl.vulkan)
    implementation(libs.lwjgl.vma)
}

val compileShaders by tasks.registering {
    description = "Compiles GLSL shaders to SPIR-V using glslc"

    val inputDir = file("src/main/shaders")
    val outputDir = layout.buildDirectory.dir("generated/shaders")

    inputs.dir(inputDir)
    outputs.dir(outputDir)

    doLast {
        val shaderOutputDir = outputDir.get().dir("shaders").asFile
        shaderOutputDir.mkdirs()

        fun compileShader(shaderFile: File, outputFile: File) {
            val process = ProcessBuilder(
                "glslc",
                shaderFile.absolutePath,
                "-o",
                outputFile.absolutePath,
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw GradleException("Shader compilation failed for ${shaderFile.name}:\n$output")
            }
        }

        val shaderFiles = inputDir.listFiles { file ->
            file.extension == "vert" || file.extension == "frag"
        }.orEmpty()

        for (shaderFile in shaderFiles) {
            compileShader(shaderFile, shaderOutputDir.resolve("${shaderFile.name}.spv"))
        }
    }
}

// Registering the task output as a resource directory makes processResources (and therefore jar, run and test)
// depend on it, and keeps the compiled SPIR-V out of the source tree.
sourceSets.main {
    resources.srcDir(compileShaders)
}

tasks.named("sonarlintMain") {
    dependsOn(compileShaders)
}
