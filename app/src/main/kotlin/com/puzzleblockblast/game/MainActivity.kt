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
import com.puzzleblockblast.game.core.monetization.ads.AdsGateway
import com.puzzleblockblast.game.core.monetization.billing.BillingGateway
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var ads: AdsGateway

    @Inject
    lateinit var billing: BillingGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash until settings load, so the first screen already knows about the tutorial.
        installSplashScreen().setKeepOnScreenCondition { viewModel.appState.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Consent first: no ad is requested until UMP allows it. Billing restores verified purchases.
        ads.gatherConsent(this)
        billing.start()
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
