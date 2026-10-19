package com.puzzleblockblast.game.core.model.progress

/**
 * An achievement: [id] is stored (never rename), [isMet] judges a snapshot, and [progress] gives
 * (current, target) for the UI. Titles and descriptions live in string resources keyed by id.
 */
data class Achievement(
    val id: String,
    val target: Long,
    val measure: (PlayerSnapshot) -> Long,
) {
    fun progress(snapshot: PlayerSnapshot): Long = minOf(measure(snapshot), target)

    fun isMet(snapshot: PlayerSnapshot): Boolean = measure(snapshot) >= target
}

object Achievements {
    val all: List<Achievement> = listOf(
        Achievement("first_game", 1) { it.classicGames },
        Achievement("classic_500", 500) { it.bestClassicScore },
        Achievement("classic_2000", 2_000) { it.bestClassicScore },
        Achievement("combo_4", 4) { it.bestCombo },
        Achievement("lines_100", 100) { it.linesCleared },
        Achievement("lines_1000", 1_000) { it.linesCleared },
        Achievement("level_first", 1) { it.levelsCompleted.toLong() },
        Achievement("levels_10", 10) { it.levelsCompleted.toLong() },
        Achievement("levels_all", 50) { it.levelsCompleted.toLong() },
        Achievement("stars_100", 100) { it.totalStars.toLong() },
        Achievement("daily_first", 1) { it.dailyCompleted.toLong() },
        Achievement("daily_streak_7", 7) { it.bestStreak.toLong() },
        Achievement("player_level_10", 10) { it.playerLevel.level.toLong() },
    )

    /** Achievements met by [snapshot] that aren't unlocked yet. */
    fun newlyMet(snapshot: PlayerSnapshot): List<Achievement> =
        all.filter { it.id !in snapshot.unlockedAchievements && it.isMet(snapshot) }
}
