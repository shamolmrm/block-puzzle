import org.gradle.api.Plugin
import org.gradle.api.Project

/** Hilt via KSP (kapt is not supported with AGP 9 built-in Kotlin). Apply after the Android plugin. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")

        with(dependencies) {
            add("implementation", libs.lib("hilt-android"))
            add("ksp", libs.lib("hilt-compiler"))
        }
    }
}
