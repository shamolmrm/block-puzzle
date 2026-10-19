package com.puzzleblockblast.game.core.engine.level

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.boardOf
import org.junit.Test

class LevelValidatorTest {

    private fun errors(level: LevelDefinition) = LevelValidator.validate(level)

    @Test
    fun validLevelHasNoErrors() {
        assertThat(errors(testLevel())).isEmpty()
    }

    @Test
    fun rejectsBadFields() {
        assertThat(errors(testLevel().copy(id = "level1"))).isNotEmpty()
        assertThat(errors(testLevel(pool = listOf("zz9")))).contains("unknown shape 'zz9'")
        assertThat(errors(testLevel(objectives = emptyList()))).contains("no objectives")
        assertThat(errors(testLevel(maxMoves = 0))).isNotEmpty()
        assertThat(errors(testLevel(starMoves = listOf(3, 5)))).isNotEmpty()
        assertThat(errors(testLevel(undo = 9))).isNotEmpty()
        assertThat(errors(testLevel(objectives = listOf(Objective.ClearLines(1), Objective.ClearLines(2))))).isNotEmpty()
    }

    @Test
    fun rejectsStartingFullLineAndWrongBoardSize() {
        val full = boardOf("XXXXXXXXXX", *Array(9) { ".........." })
        assertThat(errors(testLevel(board = full))).contains("starting board already has a full line")
        assertThat(errors(testLevel(board = Board.empty(6)))).isNotEmpty()
    }

    @Test
    fun gemObjectiveMustMatchGemsOnBoard() {
        val board = boardOf("G.........", *Array(9) { ".........." })

        assertThat(errors(testLevel(board = board, objectives = listOf(Objective.ClearGems(1))))).isEmpty()
        assertThat(errors(testLevel(board = board, objectives = listOf(Objective.ClearGems(2))))).isNotEmpty()
    }

    @Test
    fun rejectsBoardWhereNoPoolShapeFits() {
        val checker = Board.fromTiles(10, 10, IntArray(100) { i -> if ((i / 10 + i % 10) % 2 == 0) 1 else 0 })

        assertThat(errors(testLevel(board = checker, pool = listOf("sq2")))).contains("no pool shape fits the starting board")
    }

    @Test
    fun packNeedsUniqueIdsAndOrderedIndexes() {
        val a = testLevel()
        val b = testLevel().copy(index = 3)

        assertThat(LevelValidator.validatePack(listOf(a, b))).containsAtLeast("duplicate id L0001", "indexes must run 1..2 in order")
    }

    @Test
    fun botWinsASimpleLevelInOneMove() {
        val result = LevelBot.play(LevelGame(testLevel()))

        assertThat(result.won).isTrue()
        assertThat(result.fewestMoves).isEqualTo(1)
        assertThat(result.stars).isEqualTo(3)
    }

    @Test
    fun botReportsImpossibleLevels() {
        // Three lines needed but only two moves allowed with single dots.
        val result = LevelBot.play(LevelGame(testLevel(objectives = listOf(Objective.ClearLines(3)), maxMoves = 2)))

        assertThat(result.won).isFalse()
    }
}
