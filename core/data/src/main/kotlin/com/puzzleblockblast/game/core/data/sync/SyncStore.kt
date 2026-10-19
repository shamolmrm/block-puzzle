package com.puzzleblockblast.game.core.data.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Additive counters earned on other devices, as last seen in the cloud. Kept apart from this
 * device's own `stat_counters` so totals (own + peers) are never double counted.
 */
@Entity(tableName = "peer_counters", primaryKeys = ["deviceId", "name"])
data class PeerCounterEntity(val deviceId: String, val name: String, val value: Long)

/** Small key/value facts about sync: this install's device id, the last successful sync time. */
@Entity(tableName = "sync_meta", primaryKeys = ["key"])
data class SyncMetaEntity(val key: String, val value: String)

@Dao
interface SyncDao {
    @Query("SELECT * FROM peer_counters")
    fun observePeers(): Flow<List<PeerCounterEntity>>

    @Query("SELECT * FROM peer_counters")
    suspend fun peers(): List<PeerCounterEntity>

    @Query("DELETE FROM peer_counters")
    suspend fun clearPeers()

    @Insert
    suspend fun insertPeers(peers: List<PeerCounterEntity>)

    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    fun observeMeta(key: String): Flow<String?>

    @Upsert
    suspend fun putMeta(entity: SyncMetaEntity)
}
