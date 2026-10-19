package com.puzzleblockblast.game.core.data.progress

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One Daily Challenge date (epoch day, device-local calendar). Results only ever improve. */
@Entity(tableName = "daily_results")
data class DailyResultEntity(
    @PrimaryKey val epochDay: Long,
    val stars: Int,
    val bestScore: Int,
    val attempts: Int,
    val completedAtMillis: Long?,
)

/** Named counters (see `StatKeys`), including total XP. */
@Entity(tableName = "stat_counters")
data class StatCounterEntity(@PrimaryKey val name: String, val value: Long)

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(@PrimaryKey val id: String, val unlockedAtMillis: Long)

@Dao
interface ProgressionDao {
    @Query("SELECT * FROM stat_counters")
    fun observeCounters(): Flow<List<StatCounterEntity>>

    @Query("SELECT * FROM stat_counters")
    suspend fun counters(): List<StatCounterEntity>

    @Upsert
    suspend fun upsertCounter(counter: StatCounterEntity)

    @Query("SELECT * FROM daily_results")
    fun observeDaily(): Flow<List<DailyResultEntity>>

    @Query("SELECT * FROM daily_results")
    suspend fun dailyResults(): List<DailyResultEntity>

    @Query("SELECT * FROM daily_results WHERE epochDay = :epochDay")
    suspend fun daily(epochDay: Long): DailyResultEntity?

    @Upsert
    suspend fun upsertDaily(result: DailyResultEntity)

    @Query("SELECT * FROM achievement_unlocks")
    fun observeUnlocks(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT * FROM achievement_unlocks")
    suspend fun unlocks(): List<AchievementUnlockEntity>

    /** IGNORE: an unlock keeps its first timestamp. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(unlock: AchievementUnlockEntity)
}
