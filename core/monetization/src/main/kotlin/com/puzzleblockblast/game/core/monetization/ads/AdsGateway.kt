package com.puzzleblockblast.game.core.monetization.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

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
}

/**
 * AdMob ad unit ids. These are Google's published TEST units: they serve test ads that earn
 * nothing, so they're safe to click while developing. Real units must never be used for test
 * traffic; swapping them in is a release step that needs explicit approval (docs/MONETIZATION.md).
 */
object AdUnits {
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_PUBLISHER = "ca-app-pub-3940256099942544"
}
