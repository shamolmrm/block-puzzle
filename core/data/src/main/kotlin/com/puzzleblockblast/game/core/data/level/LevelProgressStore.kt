package com.puzzleblockblast.game.core.data.level

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelId: String,
    val stars: Int,
    val bestScore: Int,
    val attempts: Int,
    val completedAtMillis: Long?,
)

@Dao
abstract class LevelProgressDao {
    @Query("SELECT * FROM level_progress")
    abstract fun observeAll(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress")
    abstract suspend fun all(): List<LevelProgressEntity>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    abstract suspend fun get(levelId: String): LevelProgressEntity?

    @Upsert
    abstract suspend fun upsert(entity: LevelProgressEntity)

    /** Read-modify-write in one transaction so concurrent updates can't lose a result. */
    @Transaction
    open suspend fun update(levelId: String, transform: (LevelProgress) -> LevelProgress) {
        val current = get(levelId)?.toModel() ?: LevelProgress(levelId)
        upsert(transform(current).toEntity())
    }
}

interface LevelProgressRepository {
    /** Progress by level id. Levels never played are absent. */
    val progress: Flow<Map<String, LevelProgress>>

    suspend fun recordAttempt(levelId: String)

    suspend fun recordWin(levelId: String, stars: Int, score: Int, atMillis: Long)
}

@Singleton
class RoomLevelProgressRepository @Inject constructor(
    private val dao: LevelProgressDao,
) : LevelProgressRepository {

    override val progress: Flow<Map<String, LevelProgress>> =
        dao.observeAll().map { rows -> rows.associate { it.levelId to it.toModel() } }

    override suspend fun recordAttempt(levelId: String) = dao.update(levelId) { it.withAttempt() }

    override suspend fun recordWin(levelId: String, stars: Int, score: Int, atMillis: Long) =
        dao.update(levelId) { it.withWin(stars, score, atMillis) }
}

internal fun LevelProgressEntity.toModel() = LevelProgress(levelId, stars, bestScore, attempts, completedAtMillis)

internal fun LevelProgress.toEntity() = LevelProgressEntity(levelId, stars, bestScore, attempts, completedAtMillis)
