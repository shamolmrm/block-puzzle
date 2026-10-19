package com.puzzleblockblast.game.core.engine.level

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class DailyChallengeTest {

    /** 2026-10-19. */
    private val day = 20_745L

    @Test
    fun sameDayGivesSameChallengeAndNeighboursDiffer() {
        assertThat(DailyChallenge.levelFor(day)).isEqualTo(DailyChallenge.levelFor(day))
        assertThat(DailyChallenge.levelFor(day).board).isNotEqualTo(DailyChallenge.levelFor(day + 1).board)
    }

    @Test
    fun idsRoundTripAndNeverLookLikePackIds() {
        assertThat(DailyChallenge.idFor(day)).isEqualTo("D20745")
        assertThat(DailyChallenge.epochDayOf("D20745")).isEqualTo(day)
        assertThat(DailyChallenge.epochDayOf("L0001")).isNull()
    }

    @Test
    fun undoIsDisabledForComparableResults() {
        assertThat(DailyChallenge.levelFor(day).undoAllowance).isEqualTo(0)
    }

    /**
     * Content gate for the generator: 63 consecutive days (nine of each weekday theme) must pass
     * validation, be winnable by the bot, and have 3 stars within reach of the bot's fastest win.
     */
    @Test
    fun nineWeeksOfDailiesAreValidAndWinnable() {
        val failures = mutableListOf<String>()
        for (d in day until day + 63) {
            val level = DailyChallenge.levelFor(d)
            val errors = LevelValidator.validate(level)
            if (errors.isNotEmpty()) {
                failures += "$d invalid: $errors"
                continue
            }
            val result = LevelBot.play(LevelGame(level))
            when {
                !result.won -> failures += "$d (${level.objectives}) not winnable"
                result.fewestMoves > level.starMoves[1] ->
                    failures += "$d 3-star needs ${level.starMoves[1]} moves, bot needs ${result.fewestMoves}"
            }
        }
        assertWithMessage("daily generator failures").that(failures).isEmpty()
    }
}
