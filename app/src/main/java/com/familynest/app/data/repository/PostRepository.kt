package com.familynest.app.data.repository

import android.net.Uri
import com.familynest.app.data.model.Comment
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostStatus
import com.familynest.app.data.model.TaskItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Reads and writes the shared feed for a family, plus image uploads. */
class PostRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {
    private fun postsRef(familyId: String) =
        db.collection(FamilyRepository.FAMILIES).document(familyId).collection(POSTS)

    /** Live feed, newest first. [type] optionally filters by PostType name. */
    fun postsFlow(familyId: String, type: String? = null): Flow<List<Post>> {
        if (familyId.isBlank()) return flowOf(emptyList())
        var query: Query = postsRef(familyId).orderBy("createdAt", Query.Direction.DESCENDING)
        if (type != null) query = postsRef(familyId)
            .whereEqualTo("type", type)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        return callbackFlow {
            val reg = query.addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList())
            }
            awaitClose { reg.remove() }
        }
    }

    fun postFlow(familyId: String, postId: String): Flow<Post?> = callbackFlow {
        val reg = postsRef(familyId).document(postId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Post::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun createPost(post: Post): Result<String> = runCatching {
        val ref = postsRef(post.familyId).document()
        ref.set(post.copy(id = ref.id)).await()
        ref.id
    }

    suspend fun uploadImage(familyId: String, uri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("families/$familyId/images/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun toggleLike(familyId: String, postId: String, uid: String, liked: Boolean): Result<Unit> =
        runCatching {
            val op = if (liked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            postsRef(familyId).document(postId).update("likedBy", op).await()
        }

    suspend fun updateStatus(familyId: String, postId: String, status: PostStatus): Result<Unit> =
        runCatching {
            postsRef(familyId).document(postId).update("status", status.name).await()
        }

    suspend fun updateTasks(familyId: String, postId: String, tasks: List<TaskItem>): Result<Unit> =
        runCatching {
            postsRef(familyId).document(postId)
                .update("tasks", tasks.map {
                    mapOf(
                        "id" to it.id,
                        "text" to it.text,
                        "done" to it.done,
                        "assigneeId" to it.assigneeId,
                        "assigneeName" to it.assigneeName,
                    )
                })
                .await()
        }

    suspend fun deletePost(familyId: String, postId: String): Result<Unit> = runCatching {
        postsRef(familyId).document(postId).delete().await()
    }

    // --- Comments ---

    private fun commentsRef(familyId: String, postId: String) =
        postsRef(familyId).document(postId).collection(COMMENTS)

    fun commentsFlow(familyId: String, postId: String): Flow<List<Comment>> = callbackFlow {
        val reg = commentsRef(familyId, postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Comment::class.java) } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(familyId: String, postId: String, comment: Comment): Result<Unit> =
        runCatching {
            val ref = commentsRef(familyId, postId).document()
            ref.set(comment.copy(id = ref.id)).await()
        }

    companion object {
        const val POSTS = "posts"
        const val COMMENTS = "comments"
    }
}
