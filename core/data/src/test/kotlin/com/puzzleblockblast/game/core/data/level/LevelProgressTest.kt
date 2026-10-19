package com.puzzleblockblast.game.core.data.level

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LevelProgressTest {

    @Test
    fun winsOnlyEverImprove() {
        val first = LevelProgress("L0001").withWin(stars = 2, score = 300, atMillis = 1_000)
        val worse = first.withWin(stars = 1, score = 100, atMillis = 2_000)
        val better = worse.withWin(stars = 3, score = 500, atMillis = 3_000)

        assertThat(worse).isEqualTo(first)
        assertThat(better.stars).isEqualTo(3)
        assertThat(better.bestScore).isEqualTo(500)
        assertThat(better.completedAtMillis).isEqualTo(1_000)
    }

    @Test
    fun attemptsCount() {
        assertThat(LevelProgress("L0001").withAttempt().withAttempt().attempts).isEqualTo(2)
    }

    @Test
    fun levelsUnlockInOrder() {
        val levels = bundledPack().levels.take(3)
        val none = emptyMap<String, LevelProgress>()
        val firstDone = mapOf("L0001" to LevelProgress("L0001").withWin(1, 10, 0))

        assertThat(isUnlocked(levels, 1, none)).isTrue()
        assertThat(isUnlocked(levels, 2, none)).isFalse()
        assertThat(isUnlocked(levels, 2, firstDone)).isTrue()
        assertThat(isUnlocked(levels, 3, firstDone)).isFalse()
        // Attempted but not won does not unlock the next level.
        assertThat(isUnlocked(levels, 2, mapOf("L0001" to LevelProgress("L0001").withAttempt()))).isFalse()
    }
}
