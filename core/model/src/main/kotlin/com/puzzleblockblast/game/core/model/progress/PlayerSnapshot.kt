package com.puzzleblockblast.game.core.model.progress

/** Everything progression and achievements are judged on, assembled from local storage. */
data class PlayerSnapshot(
    val xp: Long = 0,
    val classicGames: Long = 0,
    val bestClassicScore: Long = 0,
    val linesCleared: Long = 0,
    val bestCombo: Long = 0,
    val levelsCompleted: Int = 0,
    val levelCount: Int = 0,
    val totalStars: Int = 0,
    val dailyCompleted: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val unlockedAchievements: Map<String, Long> = emptyMap(),
) {
    val playerLevel: PlayerLevel get() = Progression.levelFor(xp)
}

/** Stat counter names (Room `stat_counters`). Never rename: they are stored. */
object StatKeys {
    const val XP = "xp"
    const val CLASSIC_GAMES = "classic_games"
    const val BEST_CLASSIC_SCORE = "best_classic_score"
    const val LINES_CLEARED = "lines_cleared"
    const val BEST_COMBO = "best_combo"
}

/** Daily streaks over completed days, given as epoch days (any order, duplicates ignored). */
object Streaks {
    /** Consecutive completed days ending today, or yesterday if today isn't done yet. */
    fun current(completedDays: Collection<Long>, today: Long): Int {
        val days = completedDays.toSet()
        var day = if (today in days) today else today - 1
        var streak = 0
        while (day in days) {
            streak++
            day--
        }
        return streak
    }

    fun best(completedDays: Collection<Long>): Int {
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in completedDays.toSortedSet()) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }
}
