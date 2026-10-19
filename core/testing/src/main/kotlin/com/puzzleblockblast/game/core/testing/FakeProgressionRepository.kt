package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.data.progress.DailyResult
import com.puzzleblockblast.game.core.data.progress.ProgressionRepository
import com.puzzleblockblast.game.core.data.progress.Reward
import com.puzzleblockblast.game.core.model.progress.PlayerSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Records calls; returns [nextReward] from every `record*`. */
class FakeProgressionRepository(
    snapshot: PlayerSnapshot = PlayerSnapshot(),
    daily: Map<Long, DailyResult> = emptyMap(),
) : ProgressionRepository {

    data class LevelEnd(val levelId: String, val stars: Int, val score: Int, val lines: Int, val bestCombo: Int)

    val snapshotState = MutableStateFlow(snapshot)
    private val dailyState = MutableStateFlow(daily)
    var nextReward = Reward()

    val classicGames = mutableListOf<Int>()
    val levelEnds = mutableListOf<LevelEnd>()
    val dailyAttempts = mutableListOf<Long>()

    override fun snapshot(today: Long): Flow<PlayerSnapshot> = snapshotState

    override val dailyResults: Flow<Map<Long, DailyResult>> = dailyState

    override suspend fun recordClassicGame(score: Int, linesCleared: Int, bestCombo: Int, atMillis: Long): Reward {
        classicGames += score
        return nextReward
    }

    override suspend fun recordDailyAttempt(epochDay: Long) {
        dailyAttempts += epochDay
        dailyState.update { it + (epochDay to (it[epochDay] ?: DailyResult(epochDay, 0, 0, 0)).let { r -> r.copy(attempts = r.attempts + 1) }) }
    }

    override suspend fun recordLevelEnd(
        levelId: String,
        stars: Int,
        score: Int,
        linesCleared: Int,
        bestCombo: Int,
        atMillis: Long,
        today: Long,
    ): Reward {
        levelEnds += LevelEnd(levelId, stars, score, linesCleared, bestCombo)
        return nextReward
    }
}
