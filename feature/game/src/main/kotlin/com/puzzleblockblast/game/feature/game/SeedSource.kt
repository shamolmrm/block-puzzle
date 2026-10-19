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

internal class SecureSeedSource @Inject constructor() : SeedSource {
    private val random = SecureRandom()
    override fun nextSeed(): Long = random.nextLong()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GameModule {
    @Binds
    abstract fun bindSeedSource(impl: SecureSeedSource): SeedSource
}
