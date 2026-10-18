import org.gradle.api.Plugin
import org.gradle.api.Project

/** A screen-level feature module: Android library + Compose + Hilt + type-safe navigation. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("pbb.android.library")
        pluginManager.apply("pbb.android.compose")
        pluginManager.apply("pbb.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        with(dependencies) {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:analytics"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("androidx-hilt-navigation-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("kotlinx-serialization-json"))
            add("testImplementation", project(":core:testing"))
        }
    }
}
