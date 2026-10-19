package com.puzzleblockblast.game.core.data.classic

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class DataStoreClassicGameRepository @Inject constructor(
    @param:ClassicDataStore private val dataStore: DataStore<Preferences>,
    private val game: ClassicGame,
) : ClassicGameRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val bestScore: Flow<Int> = prefs.map { it[Keys.BEST_SCORE] ?: 0 }.distinctUntilChanged()

    override suspend fun loadInProgress(): GameState? {
        val raw = prefs.first()[Keys.SAVED_GAME] ?: return null
        val restored = try {
            json.decodeFromString<SavedGame>(raw).toGameState(game)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        // A save that can't be restored is dropped so it can't fail again on every launch.
        if (restored == null || restored.isGameOver) dataStore.edit { it.remove(Keys.SAVED_GAME) }
        return restored?.takeUnless { it.isGameOver }
    }

    override suspend fun saveInProgress(state: GameState) {
        val encoded = json.encodeToString(SavedGame.serializer(), state.toSaved())
        dataStore.edit {
            it[Keys.SAVED_GAME] = encoded
            it.raiseBest(state.score)
        }
    }

    override suspend fun finishGame(finalScore: Int) {
        dataStore.edit {
            it.remove(Keys.SAVED_GAME)
            it.raiseBest(finalScore)
        }
    }

    private fun MutablePreferences.raiseBest(score: Int) {
        if (score > (this[Keys.BEST_SCORE] ?: 0)) this[Keys.BEST_SCORE] = score
    }

    private object Keys {
        val BEST_SCORE = intPreferencesKey("best_score")
        val SAVED_GAME = stringPreferencesKey("saved_game_json")
    }
}
