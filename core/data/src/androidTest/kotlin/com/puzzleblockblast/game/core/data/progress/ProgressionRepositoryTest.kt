package com.puzzleblockblast.game.core.data.progress

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.data.level.PbbDatabase
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.level.DailyChallenge
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.Objective
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressionRepositoryTest {

    private lateinit var db: PbbDatabase
    private lateinit var repository: RoomProgressionRepository
    private val today = 20_745L

    private val levels = object : LevelRepository {
        override suspend fun levels() = (1..3).map { i ->
            LevelDefinition("L000$i", i, Board.empty(), 1, listOf("dot"), listOf(Objective.ClearLines(1)), 10, listOf(8, 5), 1, 1)
        }
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PbbDatabase::class.java).build()
        repository = RoomProgressionRepository(db, levels) {}
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun classicGameUpdatesStatsXpAndUnlocksAchievements() = runTest {
        val reward = repository.recordClassicGame(score = 600, linesCleared = 12, bestCombo = 4, atMillis = 5)

        assertThat(reward.xp).isEqualTo(17)
        assertThat(reward.newAchievements).containsExactly("first_game", "classic_500", "combo_4")
        val snapshot = repository.snapshot(today).first()
        assertThat(snapshot.classicGames).isEqualTo(1)
        assertThat(snapshot.bestClassicScore).isEqualTo(600)
        assertThat(snapshot.linesCleared).isEqualTo(12)
        assertThat(snapshot.xp).isEqualTo(17)
        assertThat(snapshot.unlockedAchievements.keys).containsExactly("first_game", "classic_500", "combo_4")

        // A worse game keeps the best score and unlocks nothing twice.
        val second = repository.recordClassicGame(score = 100, linesCleared = 1, bestCombo = 1, atMillis = 6)
        assertThat(second.newAchievements).isEmpty()
        assertThat(repository.snapshot(today).first().bestClassicScore).isEqualTo(600)
    }

    @Test
    fun levelXpIsOnlyPaidForImprovements() = runTest {
        val first = repository.recordLevelEnd("L0001", stars = 2, score = 50, linesCleared = 2, bestCombo = 1, atMillis = 1, today = today)
        val replaySame = repository.recordLevelEnd("L0001", stars = 2, score = 70, linesCleared = 2, bestCombo = 1, atMillis = 2, today = today)
        val better = repository.recordLevelEnd("L0001", stars = 3, score = 40, linesCleared = 2, bestCombo = 1, atMillis = 3, today = today)
        val loss = repository.recordLevelEnd("L0002", stars = 0, score = 10, linesCleared = 1, bestCombo = 0, atMillis = 4, today = today)

        assertThat(first.xp).isEqualTo(60)
        assertThat(first.newAchievements).contains("level_first")
        assertThat(replaySame.xp).isEqualTo(0)
        assertThat(better.xp).isEqualTo(20)
        assertThat(loss.xp).isEqualTo(0)
        val snapshot = repository.snapshot(today).first()
        assertThat(snapshot.levelsCompleted).isEqualTo(1)
        assertThat(snapshot.totalStars).isEqualTo(3)
        assertThat(snapshot.levelCount).isEqualTo(3)
        assertThat(snapshot.linesCleared).isEqualTo(7)
    }

    @Test
    fun dailyCompletionPaysOncePerDateAndBuildsStreaks() = runTest {
        repository.recordDailyAttempt(today - 1)
        val yesterday = repository.recordLevelEnd(DailyChallenge.idFor(today - 1), 2, 90, 3, 1, 1, today)
        val again = repository.recordLevelEnd(DailyChallenge.idFor(today - 1), 2, 120, 3, 1, 2, today)
        val todayResult = repository.recordLevelEnd(DailyChallenge.idFor(today), 1, 60, 3, 1, 3, today)

        assertThat(yesterday.xp).isEqualTo(80)
        assertThat(yesterday.newAchievements).contains("daily_first")
        assertThat(again.xp).isEqualTo(0)
        assertThat(todayResult.xp).isEqualTo(70)
        val snapshot = repository.snapshot(today).first()
        assertThat(snapshot.dailyCompleted).isEqualTo(2)
        assertThat(snapshot.currentStreak).isEqualTo(2)
        // Daily results don't count as pack levels.
        assertThat(snapshot.levelsCompleted).isEqualTo(0)
        val daily = repository.dailyResults.first().getValue(today - 1)
        assertThat(daily).isEqualTo(DailyResult(today - 1, stars = 2, bestScore = 120, attempts = 1))
    }
}
