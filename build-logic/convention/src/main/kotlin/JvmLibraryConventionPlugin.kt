import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Pure Kotlin/JVM module with no Android dependencies (e.g. the game engine). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        // Lets Android Lint analyse these sources when it runs with checkDependencies from :app.
        pluginManager.apply("com.android.lint")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = PbbJavaVersion
            targetCompatibility = PbbJavaVersion
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
        addUnitTestDependencies()
    }
}
