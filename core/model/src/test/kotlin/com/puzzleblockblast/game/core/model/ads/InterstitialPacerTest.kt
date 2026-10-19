package com.puzzleblockblast.game.core.model.ads

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InterstitialPacerTest {

    private val minute = 60_000L

    /** Plays [count] breaks a minute apart from [start]; returns the break numbers (1-based) that showed an ad. */
    private fun InterstitialPacer.run(count: Int, start: Long = 0, adsRemoved: Boolean = false, consent: Boolean = true): List<Int> =
        (1..count).filter { i ->
            val now = start + i * minute
            onBreak(now, adsRemoved, consent).also { if (it) onShown(now) }
        }

    @Test
    fun graceBreaksThenEveryThirdBreakAtMostEveryThreeMinutes() {
        // Breaks 1-2 are grace; break 3 is the first eligible; then every 3rd break (3 minutes apart).
        assertThat(InterstitialPacer().run(12)).containsExactly(3, 6, 9, 12).inOrder()
    }

    @Test
    fun timeLimitHoldsEvenWhenBreaksComeQuickly() {
        val pacer = InterstitialPacer()
        val shown = (1..12).filter { i ->
            val now = i * 10_000L // a break every 10 seconds
            pacer.onBreak(now, adsRemoved = false, canRequestAds = true).also { if (it) pacer.onShown(now) }
        }

        assertThat(shown).containsExactly(3)
    }

    @Test
    fun neverWhenAdsRemovedOrNoConsent() {
        assertThat(InterstitialPacer().run(20, adsRemoved = true)).isEmpty()
        assertThat(InterstitialPacer().run(20, consent = false)).isEmpty()
    }

    @Test
    fun aFailedShowDoesNotResetTheCounters() {
        val pacer = InterstitialPacer()
        repeat(2) { pacer.onBreak(it * minute, false, true) }

        // Eligible at break 3, but the ad failed to show (onShown not called): eligible again at break 4.
        assertThat(pacer.onBreak(3 * minute, false, true)).isTrue()
        assertThat(pacer.onBreak(4 * minute, false, true)).isTrue()
    }
}
