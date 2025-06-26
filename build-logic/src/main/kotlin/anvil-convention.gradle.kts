@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    idea
    application

    kotlin("jvm")
    kotlin("plugin.power-assert")

    id("org.jlleitschuh.gradle.ktlint")
    id("se.solrike.sonarlint")
}

repositories {
    mavenCentral()
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation(libs.getBundle("test"))

    testRuntimeOnly(libs.getLibrary("lwjgl-core")) { natives() }
}

idea {
    // Automatically download sources and Javadocs for dependencies
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

powerAssert {
    functions = listOf("kotlin.assert", "kotlin.test.assertEquals")
}
