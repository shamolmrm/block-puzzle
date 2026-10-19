package com.puzzleblockblast.game.core.model.progress

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressionTest {

    @Test
    fun classicXpGrowsWithScoreAndIsCapped() {
        assertThat(Progression.classicGameXp(0)).isEqualTo(5)
        assertThat(Progression.classicGameXp(500)).isEqualTo(15)
        assertThat(Progression.classicGameXp(100_000)).isEqualTo(60)
    }

    @Test
    fun levelXpOnlyPaysForImprovement() {
        assertThat(Progression.levelXp(starsBefore = 0, starsAfter = 2)).isEqualTo(60)
        assertThat(Progression.levelXp(starsBefore = 2, starsAfter = 3)).isEqualTo(20)
        assertThat(Progression.levelXp(starsBefore = 3, starsAfter = 3)).isEqualTo(0)
        assertThat(Progression.levelXp(starsBefore = 3, starsAfter = 1)).isEqualTo(0)
    }

    @Test
    fun dailyCompletionBonusIsOncePerDate() {
        assertThat(Progression.dailyXp(0, 1)).isEqualTo(70)
        assertThat(Progression.dailyXp(1, 3)).isEqualTo(20)
        assertThat(Progression.dailyXp(3, 3)).isEqualTo(0)
    }

    @Test
    fun levelCurve() {
        assertThat(Progression.levelFor(0)).isEqualTo(PlayerLevel(1, 0, 100))
        assertThat(Progression.levelFor(99).level).isEqualTo(1)
        assertThat(Progression.levelFor(100)).isEqualTo(PlayerLevel(2, 0, 150))
        assertThat(Progression.levelFor(260)).isEqualTo(PlayerLevel(3, 10, 200))
    }

    @Test
    fun streaks() {
        val days = listOf(10L, 11L, 12L, 20L, 21L)

        assertThat(Streaks.current(days, today = 21)).isEqualTo(2)
        assertThat(Streaks.current(days, today = 22)).isEqualTo(2) // today not played yet
        assertThat(Streaks.current(days, today = 23)).isEqualTo(0)
        assertThat(Streaks.best(days)).isEqualTo(3)
        assertThat(Streaks.best(emptyList())).isEqualTo(0)
    }

    @Test
    fun achievementsUnlockOnceWhenMet() {
        val snapshot = PlayerSnapshot(classicGames = 1, bestClassicScore = 600, levelsCompleted = 1)

        val met = Achievements.newlyMet(snapshot).map { it.id }
        assertThat(met).containsExactly("first_game", "classic_500", "level_first")

        val after = snapshot.copy(unlockedAchievements = met.associateWith { 0L })
        assertThat(Achievements.newlyMet(after)).isEmpty()
    }

    @Test
    fun achievementIdsAreUnique() {
        assertThat(Achievements.all.map { it.id }.toSet()).hasSize(Achievements.all.size)
    }
}
