package com.puzzleblockblast.game.core.data.progress

import androidx.room.withTransaction
import com.puzzleblockblast.game.core.data.level.LevelProgress
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.data.level.PbbDatabase
import com.puzzleblockblast.game.core.data.level.toEntity
import com.puzzleblockblast.game.core.data.level.toModel
import com.puzzleblockblast.game.core.engine.level.DailyChallenge
import com.puzzleblockblast.game.core.model.progress.Achievements
import com.puzzleblockblast.game.core.model.progress.PlayerSnapshot
import com.puzzleblockblast.game.core.model.progress.Progression
import com.puzzleblockblast.game.core.model.progress.StatKeys
import com.puzzleblockblast.game.core.model.progress.Streaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** What a recorded result earned, for the result card. */
data class Reward(val xp: Int = 0, val newAchievements: List<String> = emptyList())

data class DailyResult(val epochDay: Long, val stars: Int, val bestScore: Int, val attempts: Int) {
    val isCompleted: Boolean get() = stars > 0
}

/**
 * Stats, XP, achievements and daily results. Each `record*` runs in one transaction: the result,
 * its XP and any achievements it unlocks are written together or not at all, and XP for a level
 * or a date is only paid for improvements, so replays can't farm it.
 */
interface ProgressionRepository {
    /** [today] is the current epoch day, for the daily streak. */
    fun snapshot(today: Long): Flow<PlayerSnapshot>

    val dailyResults: Flow<Map<Long, DailyResult>>

    suspend fun recordClassicGame(score: Int, linesCleared: Int, bestCombo: Int, atMillis: Long): Reward

    suspend fun recordDailyAttempt(epochDay: Long)

    /**
     * A pack level or a daily ([levelId] `D<day>`) ended. [stars] is 0 for a loss. Lines and combo
     * count towards stats either way.
     */
    suspend fun recordLevelEnd(
        levelId: String,
        stars: Int,
        score: Int,
        linesCleared: Int,
        bestCombo: Int,
        atMillis: Long,
        today: Long,
    ): Reward
}

@Singleton
class RoomProgressionRepository @Inject constructor(
    private val db: PbbDatabase,
    private val levels: LevelRepository,
) : ProgressionRepository {

    private val dao = db.progressionDao()
    private val levelDao = db.levelProgressDao()

    override fun snapshot(today: Long): Flow<PlayerSnapshot> = combine(
        dao.observeCounters(),
        levelDao.observeAll(),
        dao.observeDaily(),
        dao.observeUnlocks(),
        flow { emit(levels.levels().size) },
    ) { counters, levelRows, daily, unlocks, levelCount ->
        buildSnapshot(counters, levelRows.map { it.toModel() }, daily, unlocks, levelCount, today)
    }

    override val dailyResults: Flow<Map<Long, DailyResult>> = dao.observeDaily().map { rows ->
        rows.associate { it.epochDay to DailyResult(it.epochDay, it.stars, it.bestScore, it.attempts) }
    }

    override suspend fun recordClassicGame(score: Int, linesCleared: Int, bestCombo: Int, atMillis: Long): Reward =
        db.withTransaction {
            add(StatKeys.CLASSIC_GAMES, 1)
            max(StatKeys.BEST_CLASSIC_SCORE, score.toLong())
            add(StatKeys.LINES_CLEARED, linesCleared.toLong())
            max(StatKeys.BEST_COMBO, bestCombo.toLong())
            award(Progression.classicGameXp(score), atMillis, today = null)
        }

    override suspend fun recordDailyAttempt(epochDay: Long) = db.withTransaction {
        val current = dao.daily(epochDay) ?: DailyResultEntity(epochDay, 0, 0, 0, null)
        dao.upsertDaily(current.copy(attempts = current.attempts + 1))
    }

    override suspend fun recordLevelEnd(
        levelId: String,
        stars: Int,
        score: Int,
        linesCleared: Int,
        bestCombo: Int,
        atMillis: Long,
        today: Long,
    ): Reward = db.withTransaction {
        add(StatKeys.LINES_CLEARED, linesCleared.toLong())
        max(StatKeys.BEST_COMBO, bestCombo.toLong())
        val xp = if (stars <= 0) {
            0
        } else {
            val day = DailyChallenge.epochDayOf(levelId)
            if (day != null) {
                val current = dao.daily(day) ?: DailyResultEntity(day, 0, 0, 0, null)
                dao.upsertDaily(
                    current.copy(
                        stars = maxOf(current.stars, stars),
                        bestScore = maxOf(current.bestScore, score),
                        completedAtMillis = current.completedAtMillis ?: atMillis,
                    ),
                )
                Progression.dailyXp(current.stars, stars)
            } else {
                val current = levelDao.get(levelId)?.toModel() ?: LevelProgress(levelId)
                levelDao.upsert(current.withWin(stars, score, atMillis).toEntity())
                Progression.levelXp(current.stars, stars)
            }
        }
        award(xp, atMillis, today)
    }

    /** Adds [xp], then unlocks every achievement the new state meets. Call inside a transaction. */
    private suspend fun award(xp: Int, atMillis: Long, today: Long?): Reward {
        if (xp > 0) add(StatKeys.XP, xp.toLong())
        val snapshot = buildSnapshot(
            dao.counters(),
            levelDao.all().map { it.toModel() },
            dao.dailyResults(),
            dao.unlocks(),
            levels.levels().size,
            today ?: Long.MIN_VALUE,
        )
        val unlocked = Achievements.newlyMet(snapshot).map { it.id }
        unlocked.forEach { dao.insertUnlock(AchievementUnlockEntity(it, atMillis)) }
        return Reward(xp, unlocked)
    }

    private suspend fun add(name: String, delta: Long) {
        val current = dao.counters().firstOrNull { it.name == name }?.value ?: 0
        dao.upsertCounter(StatCounterEntity(name, current + delta))
    }

    private suspend fun max(name: String, value: Long) {
        val current = dao.counters().firstOrNull { it.name == name }?.value ?: 0
        if (value > current) dao.upsertCounter(StatCounterEntity(name, value))
    }
}

internal fun buildSnapshot(
    counters: List<StatCounterEntity>,
    levelRows: List<LevelProgress>,
    daily: List<DailyResultEntity>,
    unlocks: List<AchievementUnlockEntity>,
    levelCount: Int,
    today: Long,
): PlayerSnapshot {
    val values = counters.associate { it.name to it.value }
    val completedDays = daily.filter { it.stars > 0 }.map { it.epochDay }
    return PlayerSnapshot(
        xp = values[StatKeys.XP] ?: 0,
        classicGames = values[StatKeys.CLASSIC_GAMES] ?: 0,
        bestClassicScore = values[StatKeys.BEST_CLASSIC_SCORE] ?: 0,
        linesCleared = values[StatKeys.LINES_CLEARED] ?: 0,
        bestCombo = values[StatKeys.BEST_COMBO] ?: 0,
        levelsCompleted = levelRows.count { it.isCompleted },
        levelCount = levelCount,
        totalStars = levelRows.sumOf { it.stars },
        dailyCompleted = completedDays.size,
        currentStreak = if (today == Long.MIN_VALUE) 0 else Streaks.current(completedDays, today),
        bestStreak = Streaks.best(completedDays),
        unlockedAchievements = unlocks.associate { it.id to it.unlockedAtMillis },
    )
}
