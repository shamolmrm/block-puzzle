package com.puzzleblockblast.game.core.data.level

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevelProgressDaoTest {

    private lateinit var db: PbbDatabase
    private lateinit var repository: RoomLevelProgressRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PbbDatabase::class.java).build()
        repository = RoomLevelProgressRepository(db.levelProgressDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun attemptsAndWinsArePersistedAndOnlyImprove() = runTest {
        repository.recordAttempt("L0001")
        repository.recordAttempt("L0001")
        repository.recordWin("L0001", stars = 2, score = 120, atMillis = 10)
        repository.recordWin("L0001", stars = 1, score = 90, atMillis = 20)

        val saved = repository.progress.first().getValue("L0001")
        assertThat(saved).isEqualTo(LevelProgress("L0001", stars = 2, bestScore = 120, attempts = 2, completedAtMillis = 10))
    }

    @Test
    fun levelsAreIndependent() = runTest {
        repository.recordWin("L0001", stars = 3, score = 50, atMillis = 1)
        repository.recordAttempt("L0002")

        val all = repository.progress.first()
        assertThat(all.keys).containsExactly("L0001", "L0002")
        assertThat(all.getValue("L0002").isCompleted).isFalse()
    }
}
