package com.puzzleblockblast.game.core.monetization.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Billing for the one-time "Remove ads" product. A completed payment is only sent to
 * [PurchaseVerifier]; the entitlement arrives from the server (EntitlementRepository). The server,
 * not the app, acknowledges the purchase after verifying it.
 */
@Singleton
class PlayBillingGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val verifier: PurchaseVerifier,
    @param:ApplicationScope private val scope: CoroutineScope,
) : BillingGateway {

    private val _offer = MutableStateFlow<StoreOffer?>(null)
    override val removeAdsOffer: StateFlow<StoreOffer?> = _offer.asStateFlow()

    private val _state = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: StateFlow<PurchaseState> = _state.asStateFlow()

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases.orEmpty()) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun start() {
        // No verification server, no sale: we'd have no trustworthy way to grant it.
        if (!verifier.isAvailable || client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadOffer()
                        restore()
                    }
                }
            }

            override fun onBillingServiceDisconnected() = Unit // auto reconnection is enabled
        })
    }

    override fun launchRemoveAdsPurchase(activity: Activity) {
        val product = details ?: return
        scope.launch {
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
                )
                .apply { verifier.accountTag()?.let { setObfuscatedAccountId(it) } }
                .build()
            activity.runOnUiThread { client.launchBillingFlow(activity, params) }
        }
    }

    override fun restorePurchases() {
        if (client.isReady) scope.launch { restore() } else start()
    }

    private suspend fun loadOffer() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Products.REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val product = client.queryProductDetails(params).productDetailsList?.firstOrNull() ?: return
        details = product
        val price = product.oneTimePurchaseOfferDetails?.formattedPrice ?: return
        _offer.value = StoreOffer(product.productId, price)
    }

    private suspend fun restore() {
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        result.purchasesList.forEach { handle(it) }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { purchases.forEach { handle(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = PurchaseState.Idle
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> _state.value = PurchaseState.Failed(result.debugMessage.ifBlank { "Store error ${result.responseCode}" })
        }
    }

    private suspend fun handle(purchase: Purchase) {
        if (Products.REMOVE_ADS !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> _state.value = PurchaseState.Pending
            Purchase.PurchaseState.PURCHASED -> {
                _state.value = PurchaseState.Verifying
                _state.value = when (val verdict = verifier.verify(Products.REMOVE_ADS, purchase.purchaseToken)) {
                    VerificationResult.Verified -> PurchaseState.Verified
                    is VerificationResult.Rejected -> PurchaseState.Failed(verdict.reason)
                    // Play keeps the purchase; the next start retries verification.
                    is VerificationResult.Failed -> PurchaseState.Failed("Couldn't confirm the purchase yet. It will be retried.")
                }
            }
            else -> Unit
        }
    }
}
