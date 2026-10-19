plugins {
    alias(libs.plugins.pbb.android.feature)
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:audio"))
    implementation(project(":core:monetization"))

    // Accessibility Test Framework checks (AccessibilityChecksTest).
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
}
