// Sound effects. Every sound is synthesized in code (SoundDesign), so the app ships no audio assets.
plugins {
    alias(libs.plugins.pbb.android.library)
    alias(libs.plugins.pbb.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
