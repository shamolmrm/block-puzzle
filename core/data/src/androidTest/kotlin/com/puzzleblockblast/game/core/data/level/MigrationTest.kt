package com.puzzleblockblast.game.core.data.level

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Players updating from Phase 4 (schema v1) must keep their level progress. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PbbDatabase::class.java)

    @Test
    fun v1ToV2KeepsLevelProgressAndAddsNewTables() = runTest {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO level_progress (levelId, stars, bestScore, attempts, completedAtMillis) " +
                    "VALUES ('L0003', 2, 140, 5, 1000)",
            )
        }

        // Validates the migrated schema against the exported v2 schema.
        helper.runMigrationsAndValidate(dbName, 2, true).close()

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PbbDatabase::class.java,
            dbName,
        ).build()
        try {
            val progress = RoomLevelProgressRepository(db.levelProgressDao()).progress.first()
            assertThat(progress.getValue("L0003")).isEqualTo(LevelProgress("L0003", 2, 140, 5, 1000))
            assertThat(db.progressionDao().counters()).isEmpty()
        } finally {
            db.close()
        }
    }
}
