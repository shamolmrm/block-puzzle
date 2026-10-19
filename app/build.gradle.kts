plugins {
    alias(libs.plugins.pbb.android.application)
    alias(libs.plugins.pbb.android.compose)
    alias(libs.plugins.pbb.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.puzzleblockblast.game"

    defaultConfig {
        applicationId = "com.puzzleblockblast.game"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            optimization {
                // R8 shrinking is enabled and verified in Phase 8 (release preparation).
                enable = false
            }
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:analytics"))
    implementation(project(":core:audio"))
    implementation(project(":feature:home"))
    implementation(project(":feature:game"))
    implementation(project(":feature:levels"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
