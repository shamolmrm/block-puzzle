package com.puzzleblockblast.game.feature.game.level

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.LevelGame
import com.puzzleblockblast.game.core.engine.level.LevelResult
import com.puzzleblockblast.game.core.engine.level.Objective
import com.puzzleblockblast.game.feature.game.GameUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevelGameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val level = LevelDefinition(
        id = "L0007",
        index = 7,
        board = Board.empty(),
        seed = 1L,
        pool = listOf("i3"),
        objectives = listOf(Objective.ClearLines(3), Objective.ScoreAtLeast(80)),
        maxMoves = 12,
        starMoves = listOf(9, 6),
        undoAllowance = 1,
        hintAllowance = 1,
    )
    private val engine = LevelGame(level)
    private val start = engine.start()

    private fun state(result: LevelResult = LevelResult.IN_PROGRESS, stars: Int = 0, next: String? = "L0008") = LevelUiState(
        game = GameUiState(isLoading = false, game = start.game, isEnded = result != LevelResult.IN_PROGRESS),
        level = level,
        result = result,
        objectives = engine.progress(start.game),
        movesLeft = 12,
        stars = stars,
        nextLevelId = next,
    )

    private fun show(state: LevelUiState, onNext: (String) -> Unit = {}, onRestart: () -> Unit = {}) {
        compose.setContent {
            PbbTheme {
                LevelGameScreen(
                    state = state,
                    onPlace = { _, _ -> },
                    onUndo = {},
                    onHint = {},
                    onPause = {},
                    onResume = {},
                    onRestart = onRestart,
                    onMessageShown = {},
                    onLevels = {},
                    onNextLevel = onNext,
                )
            }
        }
    }

    @Test
    fun headerShowsLevelGoalsAndMovesLeft() {
        show(state())

        compose.onNodeWithText("Level 7").assertIsDisplayed()
        compose.onNodeWithText("Lines 0/3").assertIsDisplayed()
        compose.onNodeWithText("Score 0/80").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun winShowsStarsAndNextLevel() {
        var next: String? = null
        show(state(LevelResult.WON, stars = 2), onNext = { next = it })

        compose.onNodeWithText("Level complete!").assertIsDisplayed()
        compose.onNodeWithContentDescription("2 of 3 stars").assertIsDisplayed()
        compose.onNodeWithText("Next level").performClick()
        assertEquals("L0008", next)
    }

    @Test
    fun failureOffersRetry() {
        var retries = 0
        show(state(LevelResult.OUT_OF_MOVES), onRestart = { retries++ })

        compose.onNodeWithText("Out of moves").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }
}
