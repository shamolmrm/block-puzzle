plugins {
    alias(libs.plugins.pbb.android.feature)
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:monetization"))
}
