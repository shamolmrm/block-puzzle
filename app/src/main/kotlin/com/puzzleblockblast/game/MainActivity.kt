package com.puzzleblockblast.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.theme.LocalReducedMotion
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.navigation.PbbNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash until settings load, so the first screen already knows about the tutorial.
        installSplashScreen().setKeepOnScreenCondition { viewModel.appState.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState by viewModel.appState.collectAsStateWithLifecycle()
            val state = appState ?: return@setContent
            CompositionLocalProvider(LocalReducedMotion provides state.settings.reducedMotion) {
                PbbTheme {
                    PbbNavHost(tutorialCompleted = state.tutorialCompleted)
                }
            }
        }
    }
}
