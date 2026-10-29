package com.puzzleblockblast.game.core.testing

import android.app.Activity
import android.content.Context
import com.puzzleblockblast.game.core.monetization.ads.BannerAd
import com.puzzleblockblast.game.core.monetization.ads.AdsGateway
import com.puzzleblockblast.game.core.monetization.billing.BillingGateway
import com.puzzleblockblast.game.core.monetization.billing.Entitlements
import com.puzzleblockblast.game.core.monetization.billing.PurchaseState
import com.puzzleblockblast.game.core.monetization.billing.StoreOffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAdsGateway : AdsGateway {
    val canRequest = MutableStateFlow(true)
    val ready = MutableStateFlow(true)
    val privacyRequired = MutableStateFlow(false)
    var nextRewardEarned = true
    var rewardedShows = 0
    var interstitialShows = 0
    var consentGathered = 0
    var privacyOptionsShown = 0

    override val canRequestAds: StateFlow<Boolean> = canRequest
    override val privacyOptionsRequired: StateFlow<Boolean> = privacyRequired
    override val rewardedReady: StateFlow<Boolean> = ready

    override fun gatherConsent(activity: Activity) {
        consentGathered++
    }

    override fun showPrivacyOptions(activity: Activity) {
        privacyOptionsShown++
    }

    override suspend fun showRewarded(activity: Activity): Boolean {
        rewardedShows++
        return nextRewardEarned
    }

    override suspend fun showInterstitial(activity: Activity): Boolean {
        interstitialShows++
        return true
    }

    var bannersCreated = 0

    /** No real view in JVM tests: counts requests and returns nothing to show. */
    override fun createBanner(context: Context, widthDp: Int): BannerAd? {
        bannersCreated++
        return null
    }
}

class FakeEntitlements(adsRemoved: Boolean = false) : Entitlements {
    val removed = MutableStateFlow(adsRemoved)
    override val adsRemoved: StateFlow<Boolean> = removed
}

class FakeBillingGateway(offer: StoreOffer? = StoreOffer("remove_ads", "$2.99")) : BillingGateway {
    val offerState = MutableStateFlow(offer)
    val state = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    var launches = 0
    var restores = 0

    override val removeAdsOffer: StateFlow<StoreOffer?> = offerState
    override val purchaseState: StateFlow<PurchaseState> = state

    override fun start() = Unit

    override fun launchRemoveAdsPurchase(activity: Activity) {
        launches++
    }

    override fun restorePurchases() {
        restores++
    }
}
