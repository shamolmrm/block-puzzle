package com.puzzleblockblast.game.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.puzzleblockblast.game.core.designsystem.R

/** Display strings for an achievement id (ids are defined in `core:model` `Achievements`). */
object AchievementText {
    @StringRes
    fun title(id: String): Int = when (id) {
        "first_game" -> R.string.achievement_first_game_title
        "classic_500" -> R.string.achievement_classic_500_title
        "classic_2000" -> R.string.achievement_classic_2000_title
        "combo_4" -> R.string.achievement_combo_4_title
        "lines_100" -> R.string.achievement_lines_100_title
        "lines_1000" -> R.string.achievement_lines_1000_title
        "level_first" -> R.string.achievement_level_first_title
        "levels_10" -> R.string.achievement_levels_10_title
        "levels_all" -> R.string.achievement_levels_all_title
        "stars_100" -> R.string.achievement_stars_100_title
        "daily_first" -> R.string.achievement_daily_first_title
        "daily_streak_7" -> R.string.achievement_daily_streak_7_title
        "player_level_10" -> R.string.achievement_player_level_10_title
        else -> error("No title for achievement '$id'")
    }

    @StringRes
    fun description(id: String): Int = when (id) {
        "first_game" -> R.string.achievement_first_game_desc
        "classic_500" -> R.string.achievement_classic_500_desc
        "classic_2000" -> R.string.achievement_classic_2000_desc
        "combo_4" -> R.string.achievement_combo_4_desc
        "lines_100" -> R.string.achievement_lines_100_desc
        "lines_1000" -> R.string.achievement_lines_1000_desc
        "level_first" -> R.string.achievement_level_first_desc
        "levels_10" -> R.string.achievement_levels_10_desc
        "levels_all" -> R.string.achievement_levels_all_desc
        "stars_100" -> R.string.achievement_stars_100_desc
        "daily_first" -> R.string.achievement_daily_first_desc
        "daily_streak_7" -> R.string.achievement_daily_streak_7_desc
        "player_level_10" -> R.string.achievement_player_level_10_desc
        else -> error("No description for achievement '$id'")
    }
}

/** "+N XP" and any newly unlocked achievements, for result cards. Shows nothing for an empty reward. */
@Composable
fun RewardSummary(xp: Int, newAchievements: List<String>) {
    if (xp <= 0 && newAchievements.isEmpty()) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (xp > 0) {
            Text(
                stringResource(R.string.xp_gained, xp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        newAchievements.forEach { id ->
            Text(
                stringResource(R.string.achievement_unlocked, stringResource(AchievementText.title(id))),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
