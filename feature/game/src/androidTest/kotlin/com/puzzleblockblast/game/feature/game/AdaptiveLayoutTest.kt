package com.puzzleblockblast.game.feature.game

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * QA (Phase 8): the game screen must stay fully usable on small phones with large text, phones in
 * landscape and tablets. "Displayed" means inside the visible window, so overflow fails.
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val state = GameUiState(isLoading = false, game = ClassicGame().newGame(3L), slotsThatFit = listOf(true, true, true))

    private fun show(size: DpSize, fontScale: Float = 1f) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(fontScale)) {
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
        }
    }

    private fun assertPlayable() {
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
        compose.onNodeWithContentDescription("Puzzle board", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Slot 3", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Undo", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Hint", substring = true).assertIsDisplayed()
    }

    @Test
    fun smallPhoneWithLargeText() {
        show(DpSize(360.dp, 640.dp), fontScale = 1.3f)
        assertPlayable()
    }

    @Test
    fun phoneInLandscape() {
        show(DpSize(780.dp, 360.dp))
        assertPlayable()
    }

    @Test
    fun tabletPortraitAndLandscape() {
        show(DpSize(800.dp, 1280.dp))
        assertPlayable()
    }

    @Test
    fun tabletLandscape() {
        show(DpSize(1280.dp, 800.dp))
        assertPlayable()
    }
}
