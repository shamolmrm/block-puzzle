plugins {
    alias(libs.plugins.pbb.android.feature)
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:model"))
}
