// Ads (AdMob + UMP consent) and the optional "Remove ads" purchase (Play Billing). Interfaces are
// faked in tests; ad units are Google's TEST units until release approval (docs/MONETIZATION.md).
plugins {
    alias(libs.plugins.pbb.android.library)
    alias(libs.plugins.pbb.hilt)
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
