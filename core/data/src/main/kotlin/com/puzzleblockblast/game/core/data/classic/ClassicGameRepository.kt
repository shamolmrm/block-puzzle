package com.puzzleblockblast.game.core.data.classic

import com.puzzleblockblast.game.core.engine.classic.GameState
import kotlinx.coroutines.flow.Flow

interface ClassicGameRepository {
    val bestScore: Flow<Int>

    /** The interrupted game, or null if there is none or it couldn't be restored safely. */
    suspend fun loadInProgress(): GameState?

    /** Saves an in-progress game and raises the best score if [state] beats it, in one atomic write. */
    suspend fun saveInProgress(state: GameState)

    /** Records a finished game: raises the best score if beaten and removes the saved game. */
    suspend fun finishGame(finalScore: Int)
}
