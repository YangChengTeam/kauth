import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: screens and their ViewModels. Features depend on core modules only,
 * never on each other; the app module wires their navigation together.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kauth.android.library")
        pluginManager.apply("kauth.android.compose")
        pluginManager.apply("kauth.hilt")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:otp"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("androidx-activity-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
        }
    }
}
