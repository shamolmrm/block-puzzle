plugins {
    alias(libs.plugins.pbb.android.application)
    alias(libs.plugins.pbb.android.compose)
    alias(libs.plugins.pbb.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase is optional: with app/google-services.json (never committed) the plugins wire it up;
// without it the app builds and runs fully offline. See docs/FIREBASE.md.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
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
    implementation(project(":core:firebase"))
    implementation(project(":core:monetization"))
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
