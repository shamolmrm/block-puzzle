package com.puzzleblockblast.game.feature.game.tutorial

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.feature.game.GameUiState
import com.puzzleblockblast.game.feature.game.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun state(finished: Boolean = false) = TutorialUiState(
        stepIndex = 0,
        stepCount = 3,
        instruction = R.string.tutorial_step_row,
        game = GameUiState(isLoading = false, game = ClassicGame().newGame(1L)),
        finished = finished,
    )

    @Test
    fun showsStepInstructionAndHidesUndoHint() {
        var skips = 0
        compose.setContent {
            PbbTheme { TutorialScreen(state(), onPlace = { _, _ -> }, onMessageShown = {}, onSkip = { skips++ }, onPlay = {}) }
        }

        compose.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        compose.onNodeWithText("fill the row", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Undo", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Skip").performClick()
        assertEquals(1, skips)
    }

    @Test
    fun finishedShowsPlayButton() {
        var plays = 0
        compose.setContent {
            PbbTheme {
                TutorialScreen(state(finished = true), onPlace = { _, _ -> }, onMessageShown = {}, onSkip = {}, onPlay = { plays++ })
            }
        }

        compose.onNodeWithText("You're ready!").assertIsDisplayed()
        compose.onNodeWithText("Play Classic").performClick()
        assertEquals(1, plays)
    }
}
