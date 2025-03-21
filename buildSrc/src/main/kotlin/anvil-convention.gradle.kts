plugins {
    idea
    application
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

idea {
    // Automatically download sources and javadocs for dependencies
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
