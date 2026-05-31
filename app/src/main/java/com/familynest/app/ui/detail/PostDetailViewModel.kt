package com.familynest.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Comment
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostStatus
import com.familynest.app.di.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val user: AppUser,
    private val postId: String,
) : ViewModel() {

    private val familyId = user.familyId

    val post: StateFlow<Post?> =
        Graph.posts.postFlow(familyId, postId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val comments: StateFlow<List<Comment>> =
        Graph.posts.commentsFlow(familyId, postId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<AppUser>> =
        Graph.family.familyFlow(familyId)
            .flatMapLatest { fam -> Graph.family.membersFlow(fam?.memberIds.orEmpty()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleTask(taskId: String) {
        val current = post.value ?: return
        val updated = current.tasks.map {
            if (it.id == taskId) it.copy(done = !it.done) else it
        }
        viewModelScope.launch { Graph.posts.updateTasks(familyId, postId, updated) }
    }

    fun assignTask(taskId: String, assignee: AppUser?) {
        val current = post.value ?: return
        val updated = current.tasks.map {
            if (it.id == taskId) it.copy(
                assigneeId = assignee?.uid ?: "",
                assigneeName = assignee?.displayName ?: "",
            ) else it
        }
        viewModelScope.launch { Graph.posts.updateTasks(familyId, postId, updated) }
    }

    fun setStatus(status: PostStatus) {
        viewModelScope.launch { Graph.posts.updateStatus(familyId, postId, status) }
    }

    fun toggleLike() {
        val uid = Graph.auth.currentUid ?: return
        val current = post.value ?: return
        viewModelScope.launch {
            Graph.posts.toggleLike(familyId, postId, uid, current.likedBy.contains(uid))
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        val comment = Comment(
            authorId = user.uid,
            authorName = user.displayName,
            authorPhotoUrl = user.photoUrl,
            text = text.trim(),
        )
        viewModelScope.launch { Graph.posts.addComment(familyId, postId, comment) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            Graph.posts.deletePost(familyId, postId).onSuccess { onDeleted() }
        }
    }
}
