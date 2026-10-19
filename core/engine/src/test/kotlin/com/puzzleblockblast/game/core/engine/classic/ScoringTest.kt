package com.puzzleblockblast.game.core.engine.classic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScoringTest {

    private val rules = ClassicRules()

    private fun score(cells: Int = 4, lines: Int = 0, comboBefore: Int = 0, empty: Boolean = false) =
        Scoring.score(rules, cells, lines, comboBefore, empty)

    @Test
    fun placementOnlyScoresOnePointPerCell() {
        val s = score(cells = 5)

        assertThat(s.total).isEqualTo(5)
        assertThat(s.combo).isEqualTo(0)
    }

    @Test
    fun simultaneousClearsScaleQuadratically() {
        assertThat(score(cells = 0, lines = 1).clearPoints).isEqualTo(10)
        assertThat(score(cells = 0, lines = 2).clearPoints).isEqualTo(40)
        assertThat(score(cells = 0, lines = 3).clearPoints).isEqualTo(90)
        assertThat(score(cells = 0, lines = 4).clearPoints).isEqualTo(160)
    }

    @Test
    fun comboMultiplierGrowsByHalfAndCapsAtTriple() {
        assertThat(score(lines = 1, comboBefore = 0).clearPoints).isEqualTo(10) // x1
        assertThat(score(lines = 1, comboBefore = 1).clearPoints).isEqualTo(15) // x1.5
        assertThat(score(lines = 1, comboBefore = 2).clearPoints).isEqualTo(20) // x2
        assertThat(score(lines = 1, comboBefore = 4).clearPoints).isEqualTo(30) // x3
        assertThat(score(lines = 1, comboBefore = 50).clearPoints).isEqualTo(30) // capped
        assertThat(score(lines = 2, comboBefore = 1).clearPoints).isEqualTo(60)
    }

    @Test
    fun comboCountsConsecutiveClearsAndResetsOnMiss() {
        assertThat(score(lines = 1, comboBefore = 0).combo).isEqualTo(1)
        assertThat(score(lines = 1, comboBefore = 3).combo).isEqualTo(4)
        assertThat(score(lines = 0, comboBefore = 3).combo).isEqualTo(0)
    }

    @Test
    fun boardClearBonusOnlyWhenLinesCleared() {
        assertThat(score(cells = 1, lines = 1, empty = true).total).isEqualTo(1 + 10 + 300)
        assertThat(score(cells = 1, lines = 0, empty = true).boardClearBonus).isEqualTo(0)
    }
}
