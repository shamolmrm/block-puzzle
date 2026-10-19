package com.puzzleblockblast.game.feature.home

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun classicAndLevelsAreEnabledAndOtherModesAreComingSoon() {
        var classicClicks = 0
        var levelClicks = 0
        compose.setContent {
            PbbTheme {
                HomeScreen(
                    onPlayClassic = { classicClicks++ },
                    onPlayLevels = { levelClicks++ },
                    onOpenSettings = {},
                    onHowToPlay = {},
                )
            }
        }

        compose.onNodeWithText("Classic").assertIsEnabled().performClick()
        compose.onNodeWithText("Levels").assertIsEnabled().performClick()
        compose.onNodeWithText("Daily Challenge", substring = true).assertIsNotEnabled()
        compose.onNodeWithText("Events", substring = true).assertIsNotEnabled()
        assertEquals(1, classicClicks)
        assertEquals(1, levelClicks)
    }
}
