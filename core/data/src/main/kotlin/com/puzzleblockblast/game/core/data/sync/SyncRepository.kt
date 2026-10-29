package com.puzzleblockblast.game.core.data.sync

import androidx.room.withTransaction
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.data.level.LevelProgress
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.data.level.PbbDatabase
import com.puzzleblockblast.game.core.data.level.toEntity
import com.puzzleblockblast.game.core.data.level.toModel
import com.puzzleblockblast.game.core.data.progress.AchievementUnlockEntity
import com.puzzleblockblast.game.core.data.progress.DailyResultEntity
import com.puzzleblockblast.game.core.data.progress.ProgressListener
import com.puzzleblockblast.game.core.data.progress.StatCounterEntity
import com.puzzleblockblast.game.core.data.progress.buildSnapshot
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.model.progress.Achievements
import com.puzzleblockblast.game.core.model.sync.CloudProgress
import com.puzzleblockblast.game.core.model.sync.ProgressMerge
import com.puzzleblockblast.game.core.model.sync.ResultRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cloud copy of a player's progress. Implementations: Firestore (when Firebase is configured)
 * or [NoCloudStore]. The app never depends on the cloud being there.
 */
interface CloudStore {
    /** False when Firebase isn't configured in this build. */
    val isAvailable: Boolean

    /** The signed-in user (signing in anonymously if needed), or null when cloud sync is unavailable. */
    suspend fun userId(): String?

    /**
     * Atomically reads the user's document, applies [transform] (null when there is none yet) and
     * writes the result, retrying on concurrent writes. Returns what was written.
     */
    suspend fun update(userId: String, transform: (CloudProgress?) -> CloudProgress): CloudProgress
}

/** Used when Firebase isn't configured: everything stays on the device. */
object NoCloudStore : CloudStore {
    override val isAvailable: Boolean = false
    override suspend fun userId(): String? = null
    override suspend fun update(userId: String, transform: (CloudProgress?) -> CloudProgress) = transform(null)
}

sealed interface SyncResult {
    /** Cloud sync isn't set up (no Firebase config) or the player isn't signed in. */
    data object Unavailable : SyncResult
    data class Synced(val atMillis: Long, val newAchievements: List<String>) : SyncResult
    data class Failed(val error: Throwable) : SyncResult
}

/**
 * Two-way progress sync (docs/FIREBASE.md): merge the cloud document with local progress inside a
 * cloud transaction, then bring the merged (and sanitized) result back into the local database.
 * Both sides only ever gain, so syncing is safe to repeat and to run from several devices.
 */
/** What the UI needs to show and trigger cloud backup. */
interface CloudBackup {
    val isAvailable: Boolean
    val lastSyncMillis: Flow<Long?>
    suspend fun sync(): SyncResult
}

@Singleton
class SyncRepository @Inject constructor(
    private val db: PbbDatabase,
    private val cloud: CloudStore,
    private val levels: LevelRepository,
    private val clock: WallClock,
) : CloudBackup {
    override val isAvailable: Boolean get() = cloud.isAvailable

    private val mutex = Mutex()
    private val syncDao = db.syncDao()
    private val progressionDao = db.progressionDao()
    private val levelDao = db.levelProgressDao()

    /** Epoch millis of the last successful sync, or null if never. */
    override val lastSyncMillis: Flow<Long?> = syncDao.observeMeta(LAST_SYNC).map { it?.toLongOrNull() }

    override suspend fun sync(): SyncResult = mutex.withLock {
        try {
            val uid = cloud.userId() ?: return@withLock SyncResult.Unavailable
            val local = exportLocal()
            val written = cloud.update(uid) { remote ->
                ProgressMerge.merge(ProgressMerge.sanitize(remote ?: CloudProgress()), local)
            }
            val now = clock.nowMillis()
            val unlocked = importMerged(ProgressMerge.sanitize(written), now)
            SyncResult.Synced(now, unlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline, permission denied, quota…: progress stays safe locally; try again later.
            SyncResult.Failed(e)
        }
    }

    /** This device's progress as a cloud document. */
    internal suspend fun exportLocal(): CloudProgress = db.withTransaction {
        val counters = progressionDao.counters().associate { it.name to it.value }
        val deviceId = deviceId()
        CloudProgress(
            // Event runs grow every week: uploading them would eventually breach the rules' size cap.
            levels = levelDao.all().filter { it.stars > 0 && ProgressMerge.isSyncedLevelId(it.levelId) }.associate { it.levelId to ResultRecord(it.stars, it.bestScore) },
            daily = progressionDao.dailyResults().filter { it.stars > 0 }.associate { it.epochDay to ResultRecord(it.stars, it.bestScore) },
            achievements = progressionDao.unlocks().associate { it.id to it.unlockedAtMillis },
            bests = counters.filterKeys { it in CloudProgress.MAXIMA },
            counters = syncDao.peers().groupBy { it.deviceId }.mapValues { (_, rows) -> rows.associate { it.name to it.value } } +
                (deviceId to counters.filterKeys { it in CloudProgress.ADDITIVE }),
        )
    }

    /** Raises local records to the merged document; returns achievements the combined progress unlocks. */
    internal suspend fun importMerged(merged: CloudProgress, now: Long): List<String> = db.withTransaction {
        val deviceId = deviceId()
        for ((id, record) in merged.levels) {
            if (record.stars <= 0) continue
            val current = levelDao.get(id)?.toModel() ?: LevelProgress(id)
            levelDao.upsert(current.withWin(record.stars, record.bestScore, current.completedAtMillis ?: now).toEntity())
        }
        for ((day, record) in merged.daily) {
            val current = progressionDao.daily(day) ?: DailyResultEntity(day, 0, 0, 0, null)
            progressionDao.upsertDaily(
                current.copy(
                    stars = maxOf(current.stars, record.stars),
                    bestScore = maxOf(current.bestScore, record.bestScore),
                    completedAtMillis = current.completedAtMillis ?: now.takeIf { record.stars > 0 },
                ),
            )
        }
        merged.achievements.forEach { (id, at) -> progressionDao.insertUnlock(AchievementUnlockEntity(id, at)) }

        val own = progressionDao.counters().associate { it.name to it.value }
        merged.bests.forEach { (name, value) ->
            if (value > (own[name] ?: 0)) progressionDao.upsertCounter(StatCounterEntity(name, value))
        }
        // Our own counters can only have grown locally; take the max in case the cloud saw more.
        merged.counters[deviceId]?.forEach { (name, value) ->
            if (value > (own[name] ?: 0)) progressionDao.upsertCounter(StatCounterEntity(name, value))
        }
        syncDao.clearPeers()
        syncDao.insertPeers(
            merged.counters.filterKeys { it != deviceId }.flatMap { (device, values) ->
                values.map { (name, value) -> PeerCounterEntity(device, name, value) }
            },
        )
        syncDao.putMeta(SyncMetaEntity(LAST_SYNC, now.toString()))

        // Combined progress may cross achievement thresholds (e.g. lines across two devices).
        val snapshot = buildSnapshot(
            progressionDao.counters(),
            syncDao.peers(),
            levelDao.all().map { it.toModel() },
            progressionDao.dailyResults(),
            progressionDao.unlocks(),
            levels.levels().size,
            Long.MIN_VALUE,
        )
        Achievements.newlyMet(snapshot).map { it.id }.onEach { progressionDao.insertUnlock(AchievementUnlockEntity(it, now)) }
    }

    /** A random id for this install, created once. Not linked to the person or the hardware. */
    internal suspend fun deviceId(): String = syncDao.meta(DEVICE_ID) ?: UUID.randomUUID().toString().also {
        syncDao.putMeta(SyncMetaEntity(DEVICE_ID, it))
    }

    private companion object {
        const val DEVICE_ID = "device_id"
        const val LAST_SYNC = "last_sync_millis"
    }
}

/**
 * Batches sync requests: results recorded in quick succession trigger one sync a few seconds later.
 * Runs on the application scope so leaving a screen doesn't cancel it.
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val sync: SyncRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) : ProgressListener {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                delay(DEBOUNCE_MS)
                sync.sync()
            }
        }
    }

    fun request() {
        requests.trySend(Unit)
    }

    override fun onProgressRecorded() = request()

    private companion object {
        const val DEBOUNCE_MS = 3_000L
    }
}
