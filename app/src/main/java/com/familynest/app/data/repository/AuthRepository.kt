package com.familynest.app.data.repository

import com.familynest.app.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Handles Google sign-in via Firebase Auth and keeps the `users/{uid}` doc in sync. */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    val currentUid: String? get() = auth.currentUser?.uid

    /** Emits the current Firebase user (null when signed out) and updates on auth changes. */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Live view of a user's profile document. */
    fun userFlow(uid: String): Flow<AppUser?> = callbackFlow {
        val reg = db.collection(USERS).document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(AppUser::class.java))
        }
        awaitClose { reg.remove() }
    }

    /** Exchanges a Google ID token for a Firebase session and ensures a profile exists. */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: error("Sign-in returned no user")
        upsertProfile(user)
    }

    private suspend fun upsertProfile(user: FirebaseUser) {
        val ref = db.collection(USERS).document(user.uid)
        val existing = ref.get().await()
        if (existing.exists()) {
            // Refresh the mutable identity fields, preserve familyId/role.
            ref.update(
                mapOf(
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                )
            ).await()
        } else {
            ref.set(
                AppUser(
                    uid = user.uid,
                    displayName = user.displayName ?: "Family member",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                )
            ).await()
        }
    }

    /** Stores this device's FCM token on the user's profile so pushes can reach it. */
    suspend fun saveFcmToken(token: String): Result<Unit> = runCatching {
        val uid = currentUid ?: return@runCatching
        db.collection(USERS).document(uid)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .await()
    }

    fun signOut() = auth.signOut()

    companion object {
        const val USERS = "users"
    }
}
