// Shared test fakes and rules. Only ever used as a testImplementation / androidTestImplementation dependency.
plugins {
    alias(libs.plugins.pbb.android.library)
}

dependencies {
    api(project(":core:data"))
    api(project(":core:analytics"))
    api(project(":core:audio"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.truth)
    api(libs.turbine)
}
