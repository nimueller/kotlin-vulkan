plugins {
    id("anvil-convention")
}

dependencies {
    api(projects.anvilCore)

    implementation(libs.lwjgl.vulkan)
    implementation(libs.lwjgl.vma)
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
            ).start()

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                throw GradleException("Shader compilation failed for ${shaderFile.name}:\n$error")
            }
        }

        val shaderFiles = inputDir.listFiles { file ->
            file.extension == "vert" || file.extension == "frag"
        }

        for (shaderFile in shaderFiles) {
            val outputFile = outputDir.resolve("${shaderFile.name}.spv")
            compileShader(shaderFile, outputFile)
        }
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
