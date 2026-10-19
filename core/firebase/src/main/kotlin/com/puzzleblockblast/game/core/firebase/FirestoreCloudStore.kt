package com.puzzleblockblast.game.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.puzzleblockblast.game.core.data.sync.CloudStore
import com.puzzleblockblast.game.core.model.sync.CloudProgress
import kotlinx.coroutines.tasks.await

/**
 * Cloud progress in Firestore at `users/{uid}/progress/main`, behind anonymous Firebase Auth.
 * Security rules (firebase/firestore.rules) only let a user read and write their own document.
 */
internal class FirestoreCloudStore(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : CloudStore {

    override val isAvailable: Boolean = true

    override suspend fun userId(): String =
        auth.currentUser?.uid ?: requireNotNull(auth.signInAnonymously().await().user?.uid) { "Sign-in returned no user" }

    override suspend fun update(userId: String, transform: (CloudProgress?) -> CloudProgress): CloudProgress {
        val ref = firestore.collection(USERS).document(userId).collection(PROGRESS).document(MAIN)
        return firestore.runTransaction { tx ->
            val snapshot = tx.get(ref)
            val current = snapshot.data?.let(CloudProgressMapper::fromMap)
            check(!(snapshot.exists() && current == null)) { "Cloud progress has an unsupported schema" }
            val next = transform(current)
            // A full replace: `next` already contains everything (it was merged from `current`).
            tx.set(ref, CloudProgressMapper.toMap(next) + (CloudProgressMapper.UPDATED_AT to FieldValue.serverTimestamp()))
            next
        }.await()
    }

    companion object {
        const val USERS = "users"
        const val PROGRESS = "progress"
        const val MAIN = "main"
    }
}
