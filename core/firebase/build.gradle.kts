// Firebase implementations of the app's interfaces (analytics, crash reporting, cloud sync, remote
// config, App Check). Every one falls back to an offline implementation when Firebase isn't
// configured (no app/google-services.json), so the game never depends on it. See docs/FIREBASE.md.
plugins {
    alias(libs.plugins.pbb.android.library)
    alias(libs.plugins.pbb.hilt)
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:analytics"))
    implementation(project(":core:monetization"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.kotlinx.coroutines.play.services)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
}
