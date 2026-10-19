import java.util.Properties

// Ads (AdMob + UMP consent) and the optional "Remove ads" purchase (Play Billing). Interfaces are
// faked in tests. Ad ids come from git-ignored files (docs/MONETIZATION.md):
//   admob.properties      real ids, used by RELEASE builds only (needed for the Play bundle)
//   ads-debug.properties  debug-only test device ids and consent-form testing (EEA geography)
// Debug builds always use Google's sample TEST ids, so test traffic never touches real ads.
plugins {
    alias(libs.plugins.pbb.android.library)
    alias(libs.plugins.pbb.hilt)
}

fun props(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

val real = props("admob.properties")
val debugAds = props("ads-debug.properties")

// Google's published sample ids: test ads only, earn nothing, safe to click.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testRewarded = "ca-app-pub-3940256099942544/5224354917"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    buildFeatures.buildConfig = true

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_REWARDED", quoted(testRewarded))
            buildConfigField("String", "ADMOB_INTERSTITIAL", quoted(testInterstitial))
            buildConfigField("String", "ADS_TEST_DEVICE_IDS", quoted(debugAds.getProperty("testDeviceIds", "")))
            buildConfigField("boolean", "UMP_DEBUG_EEA", (debugAds.getProperty("umpDebugGeography", "") == "EEA").toString())
        }
        getByName("release") {
            manifestPlaceholders["admobAppId"] = real.getProperty("appId", testAppId)
            buildConfigField("String", "ADMOB_REWARDED", quoted(real.getProperty("rewardedUnitId", testRewarded)))
            buildConfigField("String", "ADMOB_INTERSTITIAL", quoted(real.getProperty("interstitialUnitId", testInterstitial)))
            buildConfigField("String", "ADS_TEST_DEVICE_IDS", quoted(""))
            buildConfigField("boolean", "UMP_DEBUG_EEA", "false")
        }
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:analytics"))
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.play.billing)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
}
