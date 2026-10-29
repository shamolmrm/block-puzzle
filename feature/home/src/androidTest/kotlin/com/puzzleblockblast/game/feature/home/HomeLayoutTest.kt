package com.puzzleblockblast.game.feature.home

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.model.progress.PlayerLevel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Home must work on every phone and tablet shape: main actions visible, everything reachable. */
@RunWith(AndroidJUnit4::class)
class HomeLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(size: DpSize, fontScale: Float = 1f) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(fontScale)) {
                PbbTheme {
                    HomeScreen(
                        onPlayClassic = {},
                        onPlayLevels = {},
                        onPlayDaily = {},
                        onPlayEvents = {},
                        onOpenStats = {},
                        onOpenSettings = {},
                        onHowToPlay = {},
                        profile = HomeProfile(PlayerLevel(2, 30, 150), dailyDoneToday = false, streak = 1),
                    )
                }
            }
        }
    }

    private fun assertUsable() {
        compose.onNodeWithText("Classic").assertIsDisplayed()
        compose.onNodeWithText("Levels").assertIsDisplayed()
        // Lower items may need a scroll on short screens, but must be reachable.
        listOf("Daily Challenge", "Settings", "Stats", "How to play").forEach {
            compose.onNodeWithText(it, substring = true).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun phonePortraitLargeText() {
        show(DpSize(360.dp, 640.dp), fontScale = 1.3f)
        compose.onNodeWithText("Classic").performScrollTo().assertIsDisplayed()
        listOf("Levels", "Daily Challenge", "Settings").forEach {
            compose.onNodeWithText(it, substring = true).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun phoneLandscape() {
        show(DpSize(780.dp, 360.dp))
        assertUsable()
    }

    @Test
    fun tabletPortrait() {
        show(DpSize(800.dp, 1280.dp))
        assertUsable()
    }

    @Test
    fun tabletLandscape() {
        show(DpSize(1280.dp, 800.dp))
        assertUsable()
    }
}
