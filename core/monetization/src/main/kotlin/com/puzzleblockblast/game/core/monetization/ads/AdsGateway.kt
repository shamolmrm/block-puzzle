package com.puzzleblockblast.game.core.monetization.ads

import android.app.Activity
import android.content.Context
import android.view.View
import com.puzzleblockblast.game.core.monetization.BuildConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * A loaded-on-create banner. [heightDp] is known up front (adaptive size), so the screen can reserve
 * the space and the board never jumps. Hide it with [setVisible] while something covers it.
 */
class BannerAd(
    val view: View,
    val heightDp: Int,
    private val onVisible: (Boolean) -> Unit,
    private val onDestroy: () -> Unit,
) {
    fun setVisible(visible: Boolean) = onVisible(visible)

    fun destroy() = onDestroy()
}

/**
 * The ad network. Implementations must only request ads after consent (UMP) allows it, and must
 * only report a rewarded ad as earned from the SDK's reward callback.
 */
interface AdsGateway {
    /** True once consent allows ad requests and the SDK has started. */
    val canRequestAds: StateFlow<Boolean>

    /** True when the player must be able to change their ad privacy choices from Settings. */
    val privacyOptionsRequired: StateFlow<Boolean>

    /** A rewarded ad is loaded and can be offered. */
    val rewardedReady: StateFlow<Boolean>

    /** Runs the consent flow (shows the form only when required), then starts the SDK. Call from the first activity. */
    fun gatherConsent(activity: Activity)

    fun showPrivacyOptions(activity: Activity)

    /**
     * Shows a rewarded ad. Returns true **only** if the SDK's earned-reward callback fired; false if
     * nothing was loaded, the ad failed, or the player closed it early.
     */
    suspend fun showRewarded(activity: Activity): Boolean

    /** Shows an interstitial if one is loaded; returns when it's dismissed (or immediately if none). True if shown. */
    suspend fun showInterstitial(activity: Activity): Boolean

    /** An anchored adaptive banner [widthDp] wide that starts loading; null until ads may be requested. */
    fun createBanner(context: Context, widthDp: Int): BannerAd?
}

/**
 * AdMob ad unit ids for this build. Debug builds always get Google's published TEST units (they
 * serve test ads, earn nothing and are safe to click). Release builds get the real units from
 * admob.properties when present (docs/MONETIZATION.md).
 */
object AdUnits {
    val REWARDED: String = BuildConfig.ADMOB_REWARDED
    val INTERSTITIAL: String = BuildConfig.ADMOB_INTERSTITIAL
    val BANNER: String = BuildConfig.ADMOB_BANNER
    const val TEST_PUBLISHER = "ca-app-pub-3940256099942544"

    /** True when this build serves Google's test ads (always in debug; in release until real ids are set). */
    val isTestAds: Boolean
        get() = REWARDED.startsWith(TEST_PUBLISHER) && INTERSTITIAL.startsWith(TEST_PUBLISHER) && BANNER.startsWith(TEST_PUBLISHER)
}
