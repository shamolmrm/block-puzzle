package com.puzzleblockblast.game

import android.app.Application
import com.puzzleblockblast.game.core.firebase.FirebaseStartup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PuzzleBlockBlastApp : Application() {

    @Inject
    lateinit var firebaseStartup: FirebaseStartup

    override fun onCreate() {
        super.onCreate()
        firebaseStartup.start()
    }
}
