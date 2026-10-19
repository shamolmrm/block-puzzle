package com.puzzleblockblast.game.core.monetization.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** The "Remove ads" product as the store presents it. */
data class StoreOffer(val productId: String, val formattedPrice: String)

sealed interface PurchaseState {
    data object Idle : PurchaseState

    /** Paid; waiting for the server to verify it. Ads stay on until it does. */
    data object Verifying : PurchaseState

    /** The payment isn't complete yet (e.g. cash at a shop). Nothing is granted until it is. */
    data object Pending : PurchaseState
    data object Verified : PurchaseState
    data class Failed(val reason: String) : PurchaseState
}

interface BillingGateway {
    /** Null until the store returns the product (not listed yet, offline, no Play Store…). */
    val removeAdsOffer: StateFlow<StoreOffer?>
    val purchaseState: StateFlow<PurchaseState>

    /** Connects to the store, loads the offer and re-verifies any purchase not yet verified (restore). */
    fun start()

    fun launchRemoveAdsPurchase(activity: Activity)

    /** Re-checks the store for purchases (e.g. "Restore purchases"). */
    fun restorePurchases()
}
