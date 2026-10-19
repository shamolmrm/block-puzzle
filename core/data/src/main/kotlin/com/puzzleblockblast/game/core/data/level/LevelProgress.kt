package com.puzzleblockblast.game.core.data.level

import com.puzzleblockblast.game.core.engine.level.LevelDefinition

/** A player's record on one level, keyed by the level's stable id. */
data class LevelProgress(
    val levelId: String,
    val stars: Int = 0,
    val bestScore: Int = 0,
    val attempts: Int = 0,
    /** First completion time (epoch millis), or null if never completed. */
    val completedAtMillis: Long? = null,
) {
    val isCompleted: Boolean get() = stars > 0

    fun withAttempt(): LevelProgress = copy(attempts = attempts + 1)

    /** Results only ever improve: replaying a level can't lose stars or a best score. */
    fun withWin(stars: Int, score: Int, atMillis: Long): LevelProgress {
        require(stars in 1..MAX_STARS && score >= 0)
        return copy(
            stars = maxOf(this.stars, stars),
            bestScore = maxOf(bestScore, score),
            completedAtMillis = completedAtMillis ?: atMillis,
        )
    }

    companion object {
        const val MAX_STARS = 3
    }
}

/** The first level is always open; each later level opens when the one before it is completed. */
fun isUnlocked(levels: List<LevelDefinition>, index: Int, progress: Map<String, LevelProgress>): Boolean {
    if (index <= 1) return true
    val previous = levels.getOrNull(index - 2) ?: return false
    return progress[previous.id]?.isCompleted == true
}
