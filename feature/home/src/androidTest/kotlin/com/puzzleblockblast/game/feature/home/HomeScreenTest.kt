package com.puzzleblockblast.game.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.model.progress.PlayerLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allModesAreEnabledWithTheirStatus() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            PbbTheme {
                HomeScreen(
                    onPlayClassic = { clicks += "classic" },
                    onPlayLevels = { clicks += "levels" },
                    onPlayDaily = { clicks += "daily" },
                    onPlayEvents = { clicks += "events" },
                    onOpenStats = { clicks += "stats" },
                    onOpenSettings = {},
                    onHowToPlay = {},
                    profile = HomeProfile(PlayerLevel(3, 40, 200), dailyDoneToday = true, streak = 2, eventDaysLeft = 4),
                )
            }
        }

        compose.onNodeWithText("Classic").assertIsEnabled().performClick()
        compose.onNodeWithText("Levels").assertIsEnabled().performClick()
        compose.onNodeWithText("Daily Challenge", substring = true).assertIsEnabled().performClick()
        compose.onNodeWithText("✓ Done", substring = true).assertIsDisplayed()
        compose.onNodeWithText("4 days left").assertIsDisplayed()
        compose.onNodeWithText("Events", substring = true).assertIsEnabled().performClick()
        compose.onNodeWithText("Stats").performClick()
        assertEquals(listOf("classic", "levels", "daily", "events", "stats"), clicks)
    }
}
