package com.puzzleblockblast.game.core.model.progress

/**
 * XP and player levels. XP is local and cosmetic (it unlocks nothing paid or competitive), so it is
 * computed on the device. Every award is idempotent where replays could farm it.
 */
object Progression {

    /** XP for finishing (or abandoning) a Classic game: a small base plus score, capped. */
    fun classicGameXp(score: Int): Int = minOf(5 + score / 50, 60)

    /** XP for a level result: only stars gained over the previous best count, plus a first-clear bonus. */
    fun levelXp(starsBefore: Int, starsAfter: Int): Int {
        val gained = (starsAfter - starsBefore).coerceAtLeast(0)
        val firstClear = if (starsBefore == 0 && starsAfter > 0) 20 else 0
        return gained * 20 + firstClear
    }

    /** XP for a Daily Challenge result: a completion bonus once per date, plus stars gained. */
    fun dailyXp(starsBefore: Int, starsAfter: Int): Int {
        val gained = (starsAfter - starsBefore).coerceAtLeast(0)
        val firstClear = if (starsBefore == 0 && starsAfter > 0) 60 else 0
        return gained * 10 + firstClear
    }

    /** XP needed to go from [level] to the next: 100, 150, 200 … */
    fun xpForNextLevel(level: Int): Int = 100 + 50 * (level - 1)

    fun levelFor(totalXp: Long): PlayerLevel {
        var level = 1
        var remaining = totalXp
        while (remaining >= xpForNextLevel(level)) {
            remaining -= xpForNextLevel(level)
            level++
        }
        return PlayerLevel(level, remaining.toInt(), xpForNextLevel(level))
    }
}

/** Player level [level], with [xpIntoLevel] of [xpForNext] XP towards the next one. */
data class PlayerLevel(val level: Int, val xpIntoLevel: Int, val xpForNext: Int) {
    val fraction: Float get() = xpIntoLevel.toFloat() / xpForNext
}
