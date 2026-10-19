package com.puzzleblockblast.game.core.data.level

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.LevelValidator
import com.puzzleblockblast.game.core.engine.level.Objective
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class LevelPack(val packId: String, val contentVersion: Int, val levels: List<LevelDefinition>)

sealed interface LevelPackResult {
    data class Valid(val pack: LevelPack) : LevelPackResult
    data class Invalid(val errors: List<String>) : LevelPackResult
}

/**
 * Parses and validates a level pack (docs/PLAN.md Â§5). Anything unexpected (unsupported schema,
 * malformed JSON, unknown objective, bad board characters, failed [LevelValidator] checks) makes the
 * whole pack invalid: a partly loaded pack could renumber levels under the player.
 */
object LevelPackParser {
    const val SUPPORTED_SCHEMA = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): LevelPackResult {
        val dto = try {
            json.decodeFromString<LevelPackDto>(text)
        } catch (e: SerializationException) {
            return LevelPackResult.Invalid(listOf("malformed pack: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return LevelPackResult.Invalid(listOf("malformed pack: ${e.message}"))
        }
        if (dto.schemaVersion != SUPPORTED_SCHEMA) {
            return LevelPackResult.Invalid(listOf("unsupported schemaVersion ${dto.schemaVersion}"))
        }

        val errors = mutableListOf<String>()
        val levels = dto.levels.mapNotNull { level ->
            val problems = mutableListOf<String>()
            val definition = toDefinition(level, problems)
            problems.forEach { errors += "${level.id}: $it" }
            definition
        }
        if (errors.isEmpty()) errors += LevelValidator.validatePack(levels)
        return if (errors.isEmpty()) {
            LevelPackResult.Valid(LevelPack(dto.packId, dto.contentVersion, levels))
        } else {
            LevelPackResult.Invalid(errors)
        }
    }

    private fun toDefinition(dto: LevelDto, problems: MutableList<String>): LevelDefinition? {
        val board = parseBoard(dto.boardSize, dto.initialBoard, problems) ?: return null
        val gems = board.toTileArray().count { it == Board.GEM }
        val objectives = dto.objectives.mapNotNull { o ->
            when (o.type) {
                "scoreAtLeast" -> o.value?.let { Objective.ScoreAtLeast(it) }
                "clearLines" -> o.count?.let { Objective.ClearLines(it) }
                "clearGems" -> Objective.ClearGems(gems)
                else -> null
            } ?: run {
                problems += "bad objective ${o.type}"
                null
            }
        }
        if (problems.isNotEmpty()) return null
        return LevelDefinition(
            id = dto.id,
            index = dto.index,
            board = board,
            seed = dto.pieces.seed,
            pool = dto.pieces.pool,
            objectives = objectives,
            maxMoves = dto.limits.maxMoves,
            starMoves = dto.stars,
            undoAllowance = dto.undoAllowance,
            hintAllowance = dto.hintAllowance,
        )
    }

    /** `.` empty, `G` gem, `1`..`7` a coloured block. */
    private fun parseBoard(size: Int, rows: List<String>, problems: MutableList<String>): Board? {
        if (size !in LevelValidator.MIN_BOARD..Board.CLASSIC_SIZE || rows.size != size || rows.any { it.length != size }) {
            problems += "initialBoard must be $size rows of $size cells"
            return null
        }
        val tiles = IntArray(size * size)
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                tiles[r * size + c] = when (ch) {
                    '.' -> Board.EMPTY
                    'G' -> Board.GEM
                    in '1'..'7' -> ch - '0'
                    else -> {
                        problems += "bad board character '$ch' at ($r, $c)"
                        Board.EMPTY
                    }
                }
            }
        }
        return Board.fromTiles(size, size, tiles)
    }
}

@Serializable
internal data class LevelPackDto(
    val schemaVersion: Int,
    val contentVersion: Int,
    val packId: String,
    val levels: List<LevelDto>,
)

@Serializable
internal data class LevelDto(
    val id: String,
    val index: Int,
    val boardSize: Int,
    val initialBoard: List<String>,
    val pieces: PiecesDto,
    val objectives: List<ObjectiveDto>,
    val limits: LimitsDto,
    val stars: List<Int>,
    val undoAllowance: Int = 1,
    val hintAllowance: Int = 1,
)

@Serializable
internal data class PiecesDto(val seed: Long, val pool: List<String>)

@Serializable
internal data class ObjectiveDto(val type: String, val count: Int? = null, val value: Int? = null)

@Serializable
internal data class LimitsDto(val maxMoves: Int)
