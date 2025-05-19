plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "anvil"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("anvil-core")
include("anvil-vulkan")
include("anvil-app")
