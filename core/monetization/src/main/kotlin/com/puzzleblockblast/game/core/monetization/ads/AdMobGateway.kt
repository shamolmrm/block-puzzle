package com.puzzleblockblast.game.core.monetization.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AdMob behind Google's consent SDK (UMP): no ad is requested and the Mobile Ads SDK doesn't start
 * until `canRequestAds()` is true. Ads are preloaded after each show.
 */
@Singleton
class AdMobGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
) : AdsGateway {

    init {
        require(AdUnits.REWARDED.startsWith(AdUnits.TEST_PUBLISHER) && AdUnits.INTERSTITIAL.startsWith(AdUnits.TEST_PUBLISHER)) {
            "Only test ad units are allowed until release approval"
        }
    }

    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val started = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    override val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null

    override fun gatherConsent(activity: Activity) {
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onConsentResolved() }
            },
            { onConsentResolved() }, // offline etc.: fall back to what was decided before
        )
        // A returning player who already consented can start right away.
        onConsentResolved()
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onConsentResolved() }
    }

    private fun onConsentResolved() {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds()) return
        if (started.compareAndSet(false, true)) {
            // Initializing on the main thread blocked it for seconds on a real phone (ANR risk):
            // Google recommends a background thread. Ads load on the main thread once it's done.
            scope.launch(Dispatchers.IO) {
                MobileAds.initialize(context) {
                    scope.launch(Dispatchers.Main) {
                        _canRequestAds.value = true
                        loadRewarded()
                        loadInterstitial()
                    }
                }
            }
        }
    }

    private fun loadRewarded() {
        RewardedAd.load(
            context,
            AdUnits.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    _rewardedReady.value = false
                }
            },
        )
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            context,
            AdUnits.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                }
            },
        )
    }

    override suspend fun showRewarded(activity: Activity): Boolean {
        val ad = rewarded ?: return false
        rewarded = null
        _rewardedReady.value = false
        return suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewarded()
                    if (cont.isActive) cont.resume(earned)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadRewarded()
                    if (cont.isActive) cont.resume(false)
                }
            }
            // The only place a reward is granted: the SDK's earned-reward callback.
            ad.show(activity) { earned = true }
        }
    }

    override suspend fun showInterstitial(activity: Activity): Boolean {
        val ad = interstitial ?: return false
        interstitial = null
        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(false)
                }
            }
            ad.show(activity)
        }
    }
}
