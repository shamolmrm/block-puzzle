package com.puzzleblockblast.game.feature.stats

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.puzzleblockblast.game.core.designsystem.component.AchievementText
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.model.progress.Achievements
import com.puzzleblockblast.game.core.model.progress.PlayerSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val snapshot = PlayerSnapshot(
        xp = 260,
        classicGames = 4,
        bestClassicScore = 812,
        linesCleared = 57,
        bestCombo = 3,
        levelsCompleted = 6,
        levelCount = 50,
        totalStars = 14,
        dailyCompleted = 2,
        currentStreak = 2,
        bestStreak = 2,
        unlockedAchievements = mapOf("first_game" to 1L, "classic_500" to 2L),
    )

    @Test
    fun showsStats() {
        compose.setContent { PbbTheme { StatsScreen(snapshot = snapshot, onBack = {}) } }

        compose.onNodeWithText("Level 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Best score: 812").assertIsDisplayed()
        compose.onNodeWithContentDescription("Completed: 6 / 50").assertIsDisplayed()
    }

    @Test
    fun everyAchievementIsListedWithItsStatus() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { PbbTheme { StatsScreen(snapshot = snapshot, onBack = {}) } }

        Achievements.all.forEach { achievement ->
            // getString fails if an achievement has no title resource.
            val title = context.getString(AchievementText.title(achievement.id))
            val unlocked = achievement.id in snapshot.unlockedAchievements
            val node = hasContentDescription(title, substring = true)
            compose.onNode(hasScrollAction()).performScrollToNode(node)
            compose.onNodeWithContentDescription(title, substring = true).assertIsDisplayed()
            val description = compose.onNode(node).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
            if (unlocked) check("Unlocked" in description) { "$title should show Unlocked" } else check("/" in description) { "$title should show progress" }
        }
    }
}
