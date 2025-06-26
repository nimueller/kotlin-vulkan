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
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.contains("win") -> {
            artifact {
                classifier = "natives-windows"
            }
        }

        osName.contains("linux") -> {
            artifact {
                classifier = "natives-linux"
            }
        }

        osName.contains("mac") -> {
            artifact {
                classifier = "natives-macos"
            }
        }

        else -> error("Unsupported operating system: $osName")
    }
}
