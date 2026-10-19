package com.puzzleblockblast.game.feature.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val newGame = ClassicGame().newGame(1L)

    private fun setScreen(state: GameUiState, onPause: () -> Unit = {}, onRestart: () -> Unit = {}) {
        compose.setContent {
            PbbTheme {
                GameScreen(
                    state = state,
                    onPlace = { _, _ -> },
                    onUndo = {},
                    onHint = {},
                    onPause = onPause,
                    onResume = {},
                    onRestart = onRestart,
                    onMessageShown = {},
                    onHome = {},
                )
            }
        }
    }

    @Test
    fun freshGameShowsHudWithUndoDisabledAndHintEnabled() {
        setScreen(GameUiState(isLoading = false, game = newGame, slotsThatFit = listOf(true, true, true)))

        compose.onNodeWithText("Score").assertIsDisplayed()
        compose.onNodeWithText("Undo (1)").assertIsNotEnabled()
        compose.onNodeWithText("Hint (3)").assertIsEnabled()
    }

    @Test
    fun pauseButtonInvokesCallback() {
        var paused = 0
        setScreen(GameUiState(isLoading = false, game = newGame), onPause = { paused++ })

        compose.onNodeWithContentDescription("Pause").performClick()

        assertEquals(1, paused)
    }

    @Test
    fun pausedStateShowsPauseMenuWithRestartConfirmation() {
        var restarts = 0
        setScreen(GameUiState(isLoading = false, game = newGame, isPaused = true), onRestart = { restarts++ })

        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Restart").performClick()
        assertEquals(0, restarts) // first tap only asks for confirmation
        compose.onNodeWithText("Restart").performClick()
        assertEquals(1, restarts)
    }

    @Test
    fun gameOverShowsResults() {
        val over = newGame.copy(score = 420, status = GameStatus.GAME_OVER)
        setScreen(GameUiState(isLoading = false, game = over, bestScore = 420, isNewBest = true))

        compose.onNodeWithText("No more moves").assertIsDisplayed()
        compose.onNodeWithText("Score 420").assertIsDisplayed()
        compose.onNodeWithText("New best score!").assertIsDisplayed()
        compose.onNodeWithText("Play again").assertIsDisplayed()
    }
}
