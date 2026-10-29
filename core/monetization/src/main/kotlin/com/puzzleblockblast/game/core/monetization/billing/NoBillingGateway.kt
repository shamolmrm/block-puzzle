package com.puzzleblockblast.game.core.monetization.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * "Remove ads" is not sold in this release: it needs the verifyPurchase function (Blaze plan and a
 * Play service account) to grant purchases safely. With no offer, Settings hides the store section.
 * To bring it back, restore PlayBillingGateway and the Play Billing dependency from git history
 * (commit 178cd3d) and bind it in MonetizationModule.
 */
class NoBillingGateway @Inject constructor() : BillingGateway {
    override val removeAdsOffer: StateFlow<StoreOffer?> = MutableStateFlow<StoreOffer?>(null).asStateFlow()
    override val purchaseState: StateFlow<PurchaseState> = MutableStateFlow<PurchaseState>(PurchaseState.Idle).asStateFlow()

    override fun start() = Unit

    override fun launchRemoveAdsPurchase(activity: Activity) = Unit

    override fun restorePurchases() = Unit
}
