import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk {
                version = release(libs.intVersion("compileSdk")) {
                    minorApiLevel = libs.intVersion("compileSdkMinor")
                }
            }
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                targetSdk = libs.intVersion("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = PbbJavaVersion
                targetCompatibility = PbbJavaVersion
            }
            testOptions {
                unitTests.isReturnDefaultValues = true
            }
            lint {
                checkDependencies = true
                abortOnError = true
                htmlReport = true
            }
        }
        addUnitTestDependencies()
    }
}
