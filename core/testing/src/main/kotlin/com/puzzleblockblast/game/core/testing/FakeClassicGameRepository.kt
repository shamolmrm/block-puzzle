package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.data.classic.ClassicGameRepository
import com.puzzleblockblast.game.core.engine.classic.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory repository. Share one instance between ViewModels to simulate process death. */
class FakeClassicGameRepository(initialBest: Int = 0) : ClassicGameRepository {
    private val best = MutableStateFlow(initialBest)
    override val bestScore: Flow<Int> = best

    var saved: GameState? = null
        private set
    var saveCount = 0
        private set

    override suspend fun loadInProgress(): GameState? = saved?.takeUnless { it.isGameOver }

    override suspend fun saveInProgress(state: GameState) {
        saved = state
        saveCount++
        best.update { maxOf(it, state.score) }
    }

    override suspend fun finishGame(finalScore: Int) {
        saved = null
        best.update { maxOf(it, finalScore) }
    }
}
