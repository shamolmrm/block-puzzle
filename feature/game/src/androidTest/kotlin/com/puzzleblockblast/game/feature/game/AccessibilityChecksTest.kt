package com.puzzleblockblast.game.feature.game

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameStatus
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * QA (Phase 8): Google's Accessibility Test Framework on the gameplay screens: touch target size,
 * text contrast, missing labels and duplicate descriptions fail the test.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityChecksTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val game = ClassicGame().newGame(3L)

    @Before
    fun enableChecks() = compose.enableAccessibilityChecks()

    private fun show(state: GameUiState) {
        compose.setContent {
            PbbTheme {
                GameScreen(
                    state = state,
                    onPlace = { _, _ -> },
                    onUndo = {},
                    onHint = {},
                    onPause = {},
                    onResume = {},
                    onRestart = {},
                    onMessageShown = {},
                    onHome = {},
                )
            }
        }
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun gameScreen() = show(GameUiState(isLoading = false, game = game, slotsThatFit = listOf(true, true, true)))

    @Test
    fun pauseMenu() = show(GameUiState(isLoading = false, game = game, isPaused = true))

    @Test
    fun gameOverCard() = show(GameUiState(isLoading = false, game = game.copy(score = 420, status = GameStatus.GAME_OVER), bestScore = 300))
}
