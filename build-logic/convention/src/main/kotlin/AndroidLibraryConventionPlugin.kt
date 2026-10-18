import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            namespace = pbbNamespace()
            compileSdk {
                version = release(libs.intVersion("compileSdk")) {
                    minorApiLevel = libs.intVersion("compileSdkMinor")
                }
            }
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = PbbJavaVersion
                targetCompatibility = PbbJavaVersion
            }
            testOptions {
                unitTests.isReturnDefaultValues = true
            }
        }
        addUnitTestDependencies()
    }
}
