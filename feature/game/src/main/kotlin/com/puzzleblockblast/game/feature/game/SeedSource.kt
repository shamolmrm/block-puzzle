package com.puzzleblockblast.game.feature.game

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Inject

/** Seeds for new Classic games. Injected so tests can use fixed seeds. */
fun interface SeedSource {
    fun nextSeed(): Long
}

/** Current time for records such as a level's first completion. Injected so tests control it. */
fun interface WallClock {
    fun nowMillis(): Long
}

internal class SecureSeedSource @Inject constructor() : SeedSource {
    private val random = SecureRandom()
    override fun nextSeed(): Long = random.nextLong()
}

internal class SystemWallClock @Inject constructor() : WallClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GameModule {
    @Binds
    abstract fun bindSeedSource(impl: SecureSeedSource): SeedSource

    @Binds
    abstract fun bindWallClock(impl: SystemWallClock): WallClock
}
