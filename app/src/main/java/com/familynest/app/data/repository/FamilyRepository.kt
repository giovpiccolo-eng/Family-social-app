package com.familynest.app.data.repository

import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Family
import com.familynest.app.data.model.MemberRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/** Creates and joins family groups, and exposes live family + member data. */
class FamilyRepository(
    private val db: FirebaseFirestore,
) {
    /** Creates a new family owned by [uid] and makes them a PARENT. Returns the family id. */
    suspend fun createFamily(uid: String, familyName: String): Result<String> = runCatching {
        val ref = db.collection(FAMILIES).document()
        val family = Family(
            id = ref.id,
            name = familyName.trim(),
            ownerId = uid,
            inviteCode = generateInviteCode(),
            memberIds = listOf(uid),
        )
        ref.set(family).await()
        // Use set+merge so the profile is created if sign-in hadn't written it yet
        // (e.g. on the very first launch before security rules were in place).
        db.collection(AuthRepository.USERS).document(uid).set(
            mapOf("uid" to uid, "familyId" to ref.id, "role" to MemberRole.PARENT.name),
            SetOptions.merge(),
        ).await()
        ref.id
    }

    /** Joins an existing family by its invite code, as a CHILD. Returns the family id. */
    suspend fun joinFamily(uid: String, inviteCode: String): Result<String> = runCatching {
        val code = inviteCode.trim().uppercase()
        val match = db.collection(FAMILIES)
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .await()
        val doc = match.documents.firstOrNull() ?: error("No family found for that code")
        doc.reference.update("memberIds", FieldValue.arrayUnion(uid)).await()
        db.collection(AuthRepository.USERS).document(uid).set(
            mapOf("uid" to uid, "familyId" to doc.id, "role" to MemberRole.CHILD.name),
            SetOptions.merge(),
        ).await()
        doc.id
    }

    fun familyFlow(familyId: String): Flow<Family?> {
        if (familyId.isBlank()) return flowOf(null)
        return callbackFlow {
            val reg = db.collection(FAMILIES).document(familyId).addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(Family::class.java))
            }
            awaitClose { reg.remove() }
        }
    }

    fun membersFlow(memberIds: List<String>): Flow<List<AppUser>> {
        if (memberIds.isEmpty()) return flowOf(emptyList())
        // Firestore `whereIn` supports up to 30 ids — plenty for one family.
        val ids = memberIds.take(30)
        return callbackFlow {
            val reg = db.collection(AuthRepository.USERS)
                .whereIn("uid", ids)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.documents?.mapNotNull { it.toObject(AppUser::class.java) } ?: emptyList())
                }
            awaitClose { reg.remove() }
        }
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no ambiguous 0/O/1/I
        return (1..6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    companion object {
        const val FAMILIES = "families"
    }
}
