package com.puzzleblockblast.game.core.data.level

import android.content.Context
import android.util.Log
import com.puzzleblockblast.game.core.engine.level.EventDefinition
import com.puzzleblockblast.game.core.engine.level.LevelValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Weekly events (bundled now; Remote Config / Firestore delivery can sit behind this later). */
interface EventRepository {
    suspend fun events(): List<EventDefinition>
}

sealed interface EventPackResult {
    data class Valid(val events: List<EventDefinition>) : EventPackResult
    data class Invalid(val errors: List<String>) : EventPackResult
}

/** Same strictness as level packs: any problem rejects the whole file. */
object EventPackParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val EVENT_ID = Regex("E\\d{2}")

    fun parse(text: String): EventPackResult {
        val dto = try {
            json.decodeFromString<EventPackDto>(text)
        } catch (e: SerializationException) {
            return EventPackResult.Invalid(listOf("malformed events: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return EventPackResult.Invalid(listOf("malformed events: ${e.message}"))
        }
        if (dto.schemaVersion != LevelPackParser.SUPPORTED_SCHEMA) {
            return EventPackResult.Invalid(listOf("unsupported schemaVersion ${dto.schemaVersion}"))
        }
        val errors = mutableListOf<String>()
        if (dto.events.map { it.id }.toSet().size != dto.events.size) errors += "duplicate event id"
        val events = dto.events.map { event ->
            if (!EVENT_ID.matches(event.id)) errors += "event id '${event.id}' must look like E01"
            if (event.title.isBlank() || event.title.length > 40) errors += "${event.id}: bad title"
            val levels = event.levels.mapNotNull { level ->
                val problems = mutableListOf<String>()
                if (!level.id.startsWith(event.id + "L")) problems += "level id must start with ${event.id}L"
                LevelPackParser.toDefinition(level, problems).also { problems.forEach { p -> errors += "${level.id}: $p" } }
            }
            if (levels.isEmpty()) errors += "${event.id}: no levels"
            errors += LevelValidator.validatePack(levels).map { "${event.id}: $it" }
            EventDefinition(event.id, event.title, levels)
        }
        return if (errors.isEmpty()) EventPackResult.Valid(events) else EventPackResult.Invalid(errors)
    }
}

@Singleton
class BundledEventRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : EventRepository {
    private val mutex = Mutex()
    private var cached: List<EventDefinition>? = null

    override suspend fun events(): List<EventDefinition> = mutex.withLock {
        cached ?: load().also { cached = it }
    }

    private suspend fun load(): List<EventDefinition> = withContext(Dispatchers.IO) {
        val text = try {
            context.assets.open(PATH).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Events missing", e)
            return@withContext emptyList()
        }
        when (val result = EventPackParser.parse(text)) {
            is EventPackResult.Valid -> result.events
            is EventPackResult.Invalid -> {
                Log.e(TAG, "Events rejected: ${result.errors.take(5)}")
                emptyList()
            }
        }
    }

    private companion object {
        const val PATH = "content/events/events_001.json"
        const val TAG = "EventRepository"
    }
}

@Serializable
internal data class EventPackDto(val schemaVersion: Int, val contentVersion: Int, val events: List<EventDto>)

@Serializable
internal data class EventDto(val id: String, val title: String, val levels: List<LevelDto>)
