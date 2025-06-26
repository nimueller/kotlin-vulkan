plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("power-assert"))

    implementation(plugin(libs.plugins.ktlint))
    implementation(plugin(libs.plugins.sonarlint))
//    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
//    implementation("se.solrike.sonarlint:sonarlint-gradle-plugin:2.0.0")
}

fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
