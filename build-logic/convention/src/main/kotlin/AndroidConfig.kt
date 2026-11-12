import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** SDK levels shared by every Android module. */
object AndroidSdk {
    const val COMPILE = 37
    const val TARGET = 36
    // Android 8.0: adaptive icons, and the Keystore AES-GCM keys that protect the secrets.
    const val MIN = 26
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

/** Kotlin compiler settings shared by Android and JVM modules. */
internal fun Project.configureKotlin() {
    // Hilt generates test sources even in modules without tests; an empty run is fine.
    tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
