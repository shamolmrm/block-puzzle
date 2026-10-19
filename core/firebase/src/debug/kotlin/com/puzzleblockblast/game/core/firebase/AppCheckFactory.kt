package com.puzzleblockblast.game.core.firebase

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: the debug provider logs a token to register in the Firebase console (App Check →
 * Manage debug tokens). Never ships in release builds.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
