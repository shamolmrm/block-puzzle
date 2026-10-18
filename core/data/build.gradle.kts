// Repositories and local persistence (DataStore now; Room from Phase 4; cloud sync from Phase 6).
plugins {
    alias(libs.plugins.pbb.android.library)
    alias(libs.plugins.pbb.hilt)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
