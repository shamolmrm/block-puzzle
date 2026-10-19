package com.puzzleblockblast.game.core.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DataStore test double. The file-backed DataStore can't overwrite its file on a Windows JVM host,
 * so JVM tests use this; real file I/O belongs in on-device tests.
 */
class InMemoryDataStore<T>(initial: T) : DataStore<T> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()
    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (T) -> T): T =
        mutex.withLock { transform(state.value).also { state.value = it } }
}
