import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider

fun VersionCatalog.getBundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    findBundle(alias).orElseThrow { NoSuchElementException("Version catalog does not contain bundle '$alias'") }

fun VersionCatalog.getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { NoSuchElementException("Version catalog does not contain library '$alias'") }

fun ExternalModuleDependency.natives() {
    artifact {
        classifier = lwjglNativesClassifier()
    }
}

/**
 * Returns the LWJGL natives classifier for the current OS and CPU architecture, e.g. `natives-linux` on x86_64 or
 * `natives-linux-arm64` on aarch64.
 */
fun lwjglNativesClassifier(): String {
    val osName = System.getProperty("os.name").lowercase()
    val os =
        when {
            osName.contains("win") -> "windows"
            osName.contains("linux") -> "linux"
            osName.contains("mac") -> "macos"
            else -> error("Unsupported operating system: $osName")
        }

    val arch =
        when (val osArch = System.getProperty("os.arch").lowercase()) {
            "amd64", "x86_64" -> ""
            "aarch64", "arm64" -> "-arm64"
            else -> error("Unsupported CPU architecture: $osArch")
        }

    return "natives-$os$arch"
}
