plugins {
    alias(libs.plugins.pbb.android.feature)
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:audio"))
}
