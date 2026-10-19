package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.data.level.LevelProgress
import com.puzzleblockblast.game.core.data.level.LevelProgressRepository
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeLevelRepository(private val all: List<LevelDefinition>) : LevelRepository {
    override suspend fun levels(): List<LevelDefinition> = all
}

class FakeLevelProgressRepository(initial: Map<String, LevelProgress> = emptyMap()) : LevelProgressRepository {
    private val state = MutableStateFlow(initial)
    override val progress: Flow<Map<String, LevelProgress>> = state

    val current: Map<String, LevelProgress> get() = state.value

    override suspend fun recordAttempt(levelId: String) = edit(levelId) { it.withAttempt() }

    override suspend fun recordWin(levelId: String, stars: Int, score: Int, atMillis: Long) =
        edit(levelId) { it.withWin(stars, score, atMillis) }

    private fun edit(levelId: String, transform: (LevelProgress) -> LevelProgress) = state.update {
        it + (levelId to transform(it[levelId] ?: LevelProgress(levelId)))
    }
}
