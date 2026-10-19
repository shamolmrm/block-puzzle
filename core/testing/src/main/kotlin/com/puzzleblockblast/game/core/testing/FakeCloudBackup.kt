package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.data.sync.CloudBackup
import com.puzzleblockblast.game.core.data.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCloudBackup(
    override val isAvailable: Boolean = true,
    var result: SyncResult = SyncResult.Synced(1_000L, emptyList()),
) : CloudBackup {
    val last = MutableStateFlow<Long?>(null)
    var syncCount = 0
    override val lastSyncMillis: Flow<Long?> = last

    override suspend fun sync(): SyncResult {
        syncCount++
        (result as? SyncResult.Synced)?.let { last.value = it.atMillis }
        return result
    }
}
