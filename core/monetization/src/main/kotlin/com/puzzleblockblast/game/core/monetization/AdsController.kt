package com.puzzleblockblast.game.core.monetization

import android.app.Activity
import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.model.ads.AdPolicy
import com.puzzleblockblast.game.core.model.ads.InterstitialPacer
import com.puzzleblockblast.game.core.monetization.ads.AdsGateway
import com.puzzleblockblast.game.core.monetization.billing.Entitlements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** What gameplay screens use: rewarded ads on request, interstitials paced at breaks. */
@Singleton
class AdsController @Inject constructor(
    private val gateway: AdsGateway,
    private val entitlements: Entitlements,
    private val analytics: AnalyticsTracker,
    private val clock: WallClock,
    @ApplicationScope scope: CoroutineScope,
) {
    val policy = AdPolicy()
    private val pacer = InterstitialPacer(policy)

    /** A rewarded ad can be offered right now. Rewarded ads stay available (opt-in) even with ads removed. */
    val rewardedAvailable: StateFlow<Boolean> = combine(gateway.canRequestAds, gateway.rewardedReady) { can, ready -> can && ready }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Shows a rewarded ad; true only if the SDK confirmed the reward was earned. */
    suspend fun watchRewarded(activity: Activity, source: String): Boolean {
        if (!rewardedAvailable.value) return false
        val earned = gateway.showRewarded(activity)
        if (earned) {
            analytics.log(AnalyticsEvent(AnalyticsEvents.REWARDED_AD_COMPLETED, mapOf(AnalyticsEvents.Params.SOURCE to source)))
        }
        return earned
    }

    /** Call at a natural break (between games/levels). May show an interstitial; returns when done. */
    suspend fun atBreak(activity: Activity) {
        val now = clock.nowMillis()
        if (pacer.onBreak(now, entitlements.adsRemoved.value, gateway.canRequestAds.value)) {
            if (gateway.showInterstitial(activity)) pacer.onShown(clock.nowMillis())
        }
    }
}
