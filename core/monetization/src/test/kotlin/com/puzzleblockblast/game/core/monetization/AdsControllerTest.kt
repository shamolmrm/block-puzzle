package com.puzzleblockblast.game.core.monetization

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.testing.FakeAdsGateway
import com.puzzleblockblast.game.core.testing.FakeAnalyticsTracker
import com.puzzleblockblast.game.core.testing.FakeEntitlements
import com.puzzleblockblast.game.core.testing.FakeWallClock
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AdsControllerTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val gateway = FakeAdsGateway()
    private val entitlements = FakeEntitlements()
    private val analytics = FakeAnalyticsTracker()
    private val clock = FakeWallClock(now = 0)
    private val activity = Activity()

    private fun controller() = AdsController(gateway, entitlements, analytics, clock, CoroutineScope(mainDispatcher.dispatcher))

    @Test
    fun rewardIsReportedOnlyWhenTheSdkSaysItWasEarned() = runTest {
        val ads = controller()

        gateway.nextRewardEarned = false
        assertThat(ads.watchRewarded(activity, "undo")).isFalse()
        assertThat(analytics.events).isEmpty()

        gateway.nextRewardEarned = true
        assertThat(ads.watchRewarded(activity, "undo")).isTrue()
        assertThat(analytics.events.single().name).isEqualTo(AnalyticsEvents.REWARDED_AD_COMPLETED)
    }

    @Test
    fun noRewardedAdWithoutConsentOrWhenNothingLoaded() = runTest {
        gateway.canRequest.value = false
        assertThat(controller().watchRewarded(activity, "hint")).isFalse()

        gateway.canRequest.value = true
        gateway.ready.value = false
        assertThat(controller().watchRewarded(activity, "hint")).isFalse()
        assertThat(gateway.rewardedShows).isEqualTo(0)
    }

    @Test
    fun interstitialsArePacedAndSkippedWhenAdsRemoved() = runTest {
        val ads = controller()
        repeat(6) {
            clock.now += 60_000
            ads.atBreak(activity)
        }
        assertThat(gateway.interstitialShows).isEqualTo(2)

        entitlements.removed.value = true
        repeat(6) {
            clock.now += 60_000
            ads.atBreak(activity)
        }
        assertThat(gateway.interstitialShows).isEqualTo(2)
    }
}
