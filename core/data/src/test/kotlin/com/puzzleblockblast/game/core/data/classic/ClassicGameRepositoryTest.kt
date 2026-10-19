package com.puzzleblockblast.game.core.data.classic

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.data.InMemoryDataStore
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.MoveResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class ClassicGameRepositoryTest {

    private val game = ClassicGame()
    private val store = InMemoryDataStore<Preferences>(emptyPreferences())
    private val repo = DataStoreClassicGameRepository(store, game)

    /** A mid-game state with an undo snapshot and a few moves played. */
    private fun midGame(seed: Long = 9L, moves: Int = 7): GameState {
        var state = game.newGame(seed)
        repeat(moves) { state = (game.place(state, game.bestMove(state)!!) as MoveResult.Accepted).state }
        return state
    }

    private suspend fun writeRaw(json: String) = store.edit { it[stringPreferencesKey("saved_game_json")] = json }

    private fun encode(state: GameState) = Json.encodeToString(SavedGame.serializer(), state.toSaved())

    @Test
    fun savedGameRoundTripsExactlyIncludingUndoSnapshotAndRng() {
        val state = midGame()

        assertThat(state.toSaved().toGameState(game)).isEqualTo(state)
    }

    @Test
    fun nothingSavedLoadsNull() = runTest {
        assertThat(repo.loadInProgress()).isNull()
        assertThat(repo.bestScore.first()).isEqualTo(0)
    }

    @Test
    fun saveThenLoadRestoresGameAndRaisesBest() = runTest {
        val state = midGame()

        repo.saveInProgress(state)

        assertThat(repo.loadInProgress()).isEqualTo(state)
        assertThat(repo.bestScore.first()).isEqualTo(state.score)
    }

    @Test
    fun finishGameClearsSaveAndNeverLowersBest() = runTest {
        repo.saveInProgress(midGame())
        repo.finishGame(finalScore = 500)
        repo.finishGame(finalScore = 120)

        assertThat(repo.loadInProgress()).isNull()
        assertThat(repo.bestScore.first()).isEqualTo(500)
    }

    @Test
    fun corruptJsonIsDiscarded() = runTest {
        writeRaw("{ not json")

        assertThat(repo.loadInProgress()).isNull()
        assertThat(store.data.first()[stringPreferencesKey("saved_game_json")]).isNull()
    }

    @Test
    fun otherSchemaVersionIsDiscarded() = runTest {
        writeRaw(encode(midGame()).replace("\"schemaVersion\":1", "\"schemaVersion\":99"))

        assertThat(repo.loadInProgress()).isNull()
    }

    @Test
    fun unknownShapeIdIsDiscarded() = runTest {
        val state = midGame()
        val id = state.tray.first { it != null }!!.shape.id
        writeRaw(encode(state).replace("\"$id\"", "\"not_a_shape\""))

        assertThat(repo.loadInProgress()).isNull()
    }

    @Test
    fun tamperedImpossibleStateIsDiscarded() = runTest {
        val saved = midGame().toSaved()
        val fullFirstRow = saved.copy(tiles = List(10) { 1 } + saved.tiles.drop(10))
        writeRaw(Json.encodeToString(SavedGame.serializer(), fullFirstRow))

        assertThat(repo.loadInProgress()).isNull()
    }

    @Test
    fun missingFieldsAreDiscarded() = runTest {
        writeRaw("""{"schemaVersion":1,"seed":1}""")

        assertThat(repo.loadInProgress()).isNull()
    }

    @Test
    fun finishedGameIsNotResumed() = runTest {
        val over = midGame().copy(status = com.puzzleblockblast.game.core.engine.classic.GameStatus.GAME_OVER)
        writeRaw(encode(over))

        assertThat(repo.loadInProgress()).isNull()
    }
}
