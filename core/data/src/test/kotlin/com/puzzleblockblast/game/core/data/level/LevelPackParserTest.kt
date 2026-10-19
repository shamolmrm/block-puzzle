package com.puzzleblockblast.game.core.data.level

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.level.Objective
import org.junit.Test

class LevelPackParserTest {

    private fun pack(level: String, schema: Int = 1) =
        """{"schemaVersion": $schema, "contentVersion": 1, "packId": "test", "levels": [$level]}"""

    private fun level(
        board: List<String> = listOf("G........1") + List(9) { ".........." },
        objectives: String = """[{"type": "clearGems"}, {"type": "scoreAtLeast", "value": 50}]""",
    ) = """
        {"id": "L0001", "index": 1, "boardSize": 10,
         "initialBoard": [${board.joinToString { "\"$it\"" }}],
         "pieces": {"seed": 5, "pool": ["i3", "sq2_0"]},
         "objectives": $objectives,
         "limits": {"maxMoves": 12}, "stars": [10, 6], "undoAllowance": 1, "hintAllowance": 2}
    """.trimIndent()

    @Test
    fun parsesAValidLevel() {
        val result = LevelPackParser.parse(pack(level())) as LevelPackResult.Valid

        val parsed = result.pack.levels.single()
        assertThat(parsed.board[0, 0]).isEqualTo(Board.GEM)
        assertThat(parsed.board[0, 9]).isEqualTo(1)
        assertThat(parsed.objectives).containsExactly(Objective.ClearGems(1), Objective.ScoreAtLeast(50)).inOrder()
        assertThat(parsed.pool).containsExactly("i3", "sq2_0")
        assertThat(parsed.starMoves).containsExactly(10, 6).inOrder()
        assertThat(parsed.hintAllowance).isEqualTo(2)
    }

    @Test
    fun rejectsMalformedOrUnsupportedPacks() {
        assertThat(LevelPackParser.parse("{not json")).isInstanceOf(LevelPackResult.Invalid::class.java)
        assertThat(LevelPackParser.parse(pack(level(), schema = 2))).isInstanceOf(LevelPackResult.Invalid::class.java)
        assertThat(LevelPackParser.parse("""{"schemaVersion": 1}""")).isInstanceOf(LevelPackResult.Invalid::class.java)
    }

    @Test
    fun rejectsBadBoardsAndObjectives() {
        val badChar = level(board = listOf("Z.........") + List(9) { ".........." })
        val shortBoard = level(board = List(9) { ".........." })
        val unknownObjective = level(objectives = """[{"type": "collectStars", "count": 3}]""")
        val missingCount = level(objectives = """[{"type": "clearLines"}]""")

        listOf(badChar, shortBoard, unknownObjective, missingCount).forEach {
            assertThat(LevelPackParser.parse(pack(it))).isInstanceOf(LevelPackResult.Invalid::class.java)
        }
    }

    @Test
    fun runsTheEngineValidator() {
        val fullRow = level(board = listOf("1111111111") + List(9) { ".........." }, objectives = """[{"type": "clearLines", "count": 1}]""")

        val result = LevelPackParser.parse(pack(fullRow)) as LevelPackResult.Invalid

        assertThat(result.errors).contains("L0001: starting board already has a full line")
    }

    @Test
    fun bundledPackIsValidWithFiftyOrderedLevels() {
        val pack = bundledPack()

        assertThat(pack.levels.map { it.index }).isEqualTo((1..50).toList())
        assertThat(pack.levels.map { it.id }.distinct()).hasSize(50)
    }
}
