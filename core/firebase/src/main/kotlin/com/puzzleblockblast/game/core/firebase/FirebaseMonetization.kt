package com.puzzleblockblast.game.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.puzzleblockblast.game.core.data.sync.CloudStore
import com.puzzleblockblast.game.core.monetization.billing.EntitlementSource
import com.puzzleblockblast.game.core.monetization.billing.PurchaseVerifier
import com.puzzleblockblast.game.core.monetization.billing.VerificationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/** Calls the `verifyPurchase` Cloud Function (firebase/functions). */
internal class CallablePurchaseVerifier(
    private val functions: FirebaseFunctions,
    private val cloud: CloudStore,
) : PurchaseVerifier {

    override val isAvailable: Boolean = true

    override suspend fun accountTag(): String? = runCatching { cloud.userId() }.getOrNull()?.let(::accountTagFor)

    override suspend fun verify(productId: String, purchaseToken: String): VerificationResult = try {
        cloud.userId() // make sure we're signed in: the function requires auth
        val result = functions.getHttpsCallable(VERIFY_FUNCTION)
            .call(mapOf("productId" to productId, "purchaseToken" to purchaseToken))
            .await()
        val data = result.getData() as? Map<*, *>
        if (data?.get("verified") == true) VerificationResult.Verified else VerificationResult.Rejected(data?.get("reason")?.toString() ?: "unknown")
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFunctionsException) {
        if (e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION || e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED) {
            VerificationResult.Rejected(e.message ?: e.code.name)
        } else {
            VerificationResult.Failed(e)
        }
    } catch (e: Exception) {
        VerificationResult.Failed(e)
    }

    companion object {
        const val VERIFY_FUNCTION = "verifyPurchase"

        /** Play's obfuscated account id: a SHA-256 of the user id, never the id itself. Must match the function. */
        fun accountTagFor(uid: String): String =
            MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

/** Server-written entitlements at `users/{uid}/entitlements/main` (clients can read, never write). */
internal class FirestoreEntitlementSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : EntitlementSource {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val removeAds: Flow<Boolean?> = flow { emit(auth.currentUser?.uid) }.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else entitlement(uid)
    }

    private fun entitlement(uid: String): Flow<Boolean?> = callbackFlow {
        val registration = firestore.document("users/$uid/entitlements/main").addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(null) // offline / denied: unknown, keep the cached value
                snapshot == null || !snapshot.exists() -> trySend(false)
                else -> trySend(snapshot.getBoolean("removeAds") == true)
            }
        }
        awaitClose { registration.remove() }
    }
}
