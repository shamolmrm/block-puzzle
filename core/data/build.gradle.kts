// Repositories and local persistence: DataStore (settings, Classic save), Room (level progress),
// bundled level content. Cloud sync arrives in Phase 6.
plugins {
    alias(libs.plugins.pbb.android.library)
    alias(libs.plugins.pbb.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
}

room {
    // Exported schemas are committed so future migrations can be written and tested against them.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:engine"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
}
