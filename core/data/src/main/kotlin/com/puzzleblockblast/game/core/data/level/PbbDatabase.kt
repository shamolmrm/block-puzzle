package com.puzzleblockblast.game.core.data.level

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.puzzleblockblast.game.core.data.progress.AchievementUnlockEntity
import com.puzzleblockblast.game.core.data.progress.DailyResultEntity
import com.puzzleblockblast.game.core.data.progress.ProgressionDao
import com.puzzleblockblast.game.core.data.progress.StatCounterEntity
import com.puzzleblockblast.game.core.data.sync.PeerCounterEntity
import com.puzzleblockblast.game.core.data.sync.SyncDao
import com.puzzleblockblast.game.core.data.sync.SyncMetaEntity

/**
 * Local database. Keep it in this package: the schema folder is named after the class. Schemas are exported to core/data/schemas; every version bump needs a migration
 * (auto or manual) and a MigrationTestHelper test, so players never lose progress on update.
 *
 * v1 (Phase 4): level_progress. v2 (Phase 5): daily_results, stat_counters, achievement_unlocks.
 * v3 (Phase 6): peer_counters, sync_meta.
 */
@Database(
    entities = [
        LevelProgressEntity::class,
        DailyResultEntity::class,
        StatCounterEntity::class,
        AchievementUnlockEntity::class,
        PeerCounterEntity::class,
        SyncMetaEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class PbbDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun progressionDao(): ProgressionDao
    abstract fun syncDao(): SyncDao

    companion object {
        const val NAME = "pbb.db"
    }
}
