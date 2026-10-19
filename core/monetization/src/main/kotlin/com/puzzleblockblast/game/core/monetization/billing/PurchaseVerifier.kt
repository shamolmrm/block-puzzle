package com.puzzleblockblast.game.core.monetization.billing

/**
 * Sends a Play purchase token to the server (`verifyPurchase` Cloud Function), which checks it with
 * the Google Play Developer API, binds it to the player, acknowledges it and records the
 * entitlement. The app never grants a paid entitlement on its own (docs/MONETIZATION.md).
 */
interface PurchaseVerifier {
    /** False when there's no server to verify with (Firebase not configured): purchases aren't offered. */
    val isAvailable: Boolean

    /** An opaque per-player tag for Play's obfuscated account id, so a token can't be reused by someone else. */
    suspend fun accountTag(): String?

    suspend fun verify(productId: String, purchaseToken: String): VerificationResult
}

sealed interface VerificationResult {
    data object Verified : VerificationResult

    /** The server checked the purchase and refused it (invalid, refunded, someone else's…). */
    data class Rejected(val reason: String) : VerificationResult

    /** Couldn't reach the server; Play keeps the purchase, so it's retried on the next start. */
    data class Failed(val error: Throwable) : VerificationResult
}

object NoPurchaseVerifier : PurchaseVerifier {
    override val isAvailable: Boolean = false
    override suspend fun accountTag(): String? = null
    override suspend fun verify(productId: String, purchaseToken: String): VerificationResult =
        VerificationResult.Failed(IllegalStateException("Purchase verification isn't configured"))
}
