package com.puzzleblockblast.game.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders every [Sound] to a small WAV in the cache directory on a background thread, then plays it
 * through a [SoundPool]. A sound requested before it has loaded is silently skipped.
 */
@Singleton
class SoundPoolSoundEffects @Inject constructor(
    @ApplicationContext context: Context,
) : SoundEffects {

    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = ConcurrentHashMap<Sound, Int>()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
            Sound.entries.forEach { sound ->
                val file = File(dir, "${sound.name.lowercase()}.wav")
                file.writeBytes(Wav.encode(SoundDesign.render(sound), SoundDesign.SAMPLE_RATE))
                loaded[sound] = pool.load(file.path, 1)
            }
        }
    }

    override fun play(sound: Sound) {
        val id = loaded[sound] ?: return
        pool.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val VOLUME = 0.8f
    }
}
