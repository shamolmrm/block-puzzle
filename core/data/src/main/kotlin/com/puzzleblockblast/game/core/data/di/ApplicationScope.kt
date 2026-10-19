package com.puzzleblockblast.game.core.data.di

import javax.inject.Qualifier

/**
 * A process-lifetime CoroutineScope for work that must outlive a screen, such as finishing a save
 * after the player leaves the game screen.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
