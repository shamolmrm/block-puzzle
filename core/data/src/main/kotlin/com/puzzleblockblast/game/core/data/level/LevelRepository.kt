package com.puzzleblockblast.game.core.data.level

import android.content.Context
import android.util.Log
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Level content. Bundled now; Phase 6 may add downloaded packs behind the same interface. */
interface LevelRepository {
    /** All levels in play order; empty if the content could not be loaded. */
    suspend fun levels(): List<LevelDefinition>

    suspend fun level(id: String): LevelDefinition? = levels().firstOrNull { it.id == id }
}

@Singleton
class BundledLevelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : LevelRepository {

    private val mutex = Mutex()
    private var cached: List<LevelDefinition>? = null

    override suspend fun levels(): List<LevelDefinition> = mutex.withLock {
        cached ?: load().also { cached = it }
    }

    private suspend fun load(): List<LevelDefinition> = withContext(Dispatchers.IO) {
        val text = try {
            context.assets.open(PACK_PATH).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Level pack missing", e)
            return@withContext emptyList()
        }
        when (val result = LevelPackParser.parse(text)) {
            is LevelPackResult.Valid -> result.pack.levels
            is LevelPackResult.Invalid -> {
                // Bundled content is validated by tests, so this means a broken build.
                Log.e(TAG, "Level pack rejected: ${result.errors.take(5)}")
                emptyList()
            }
        }
    }

    private companion object {
        const val PACK_PATH = "content/levels/pack_001.json"
        const val TAG = "LevelRepository"
    }
}
