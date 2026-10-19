import java.util.Properties

plugins {
    alias(libs.plugins.pbb.android.application)
    alias(libs.plugins.pbb.android.compose)
    alias(libs.plugins.pbb.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

// Firebase is optional: with app/google-services.json (never committed) the plugins wire it up;
// without it the app builds and runs fully offline. See docs/FIREBASE.md.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

// Release signing comes only from keystore.properties (never committed; see docs/RELEASE.md).
// Without it, release builds are unsigned and can't be uploaded by accident.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.puzzleblockblast.game"

    defaultConfig {
        applicationId = "com.puzzleblockblast.game"
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // R8: code shrinking, optimization and resource shrinking (rules in proguard-rules.pro).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
}

// The Play upload bundle must carry real AdMob ids; otherwise players would only ever see test
// ads (and the app would earn nothing). Local release/benchmark APKs are unaffected.
// Deliberate opt-out for internal-testing uploads: -Ppbb.allowTestAdsBundle=true (test ads earn nothing).
val admobConfigured = rootProject.file("admob.properties").exists()
val allowTestAdsBundle = providers.gradleProperty("pbb.allowTestAdsBundle").orNull == "true"
// Gate the packaging task itself: checking only `bundleRelease` let `packageReleaseBundle` write the .aab first.
tasks.matching { it.name == "packageReleaseBundle" || it.name == "bundleRelease" }.configureEach {
    // Locals only: the action must not capture the build script (configuration cache).
    val configured = admobConfigured
    val allowTestAds = allowTestAdsBundle
    doFirst {
        if (!configured && allowTestAds) {
            println("WARNING: building the Play bundle with Google's TEST ads (pbb.allowTestAdsBundle). Internal testing only.")
        }
        check(configured || allowTestAds) {
            "admob.properties is missing: add your real AdMob app id and ad unit ids before building the Play bundle " +
                "(docs/MONETIZATION.md), or pass -Ppbb.allowTestAdsBundle=true for an internal-testing bundle with test ads."
        }
    }
}

baselineProfile {
    // Profiles are generated on a device (docs/RELEASE.md) and committed under src/main/generated.
    automaticGenerationDuringBuild = false
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
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.concurrent.futures)

    baselineProfile(project(":baselineprofile"))
}
