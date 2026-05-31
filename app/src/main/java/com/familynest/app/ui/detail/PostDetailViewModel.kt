package com.familynest.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostStatus
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val familyId: String,
    private val postId: String,
) : ViewModel() {

    val post: StateFlow<Post?> =
        Graph.posts.postFlow(familyId, postId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleTask(taskId: String) {
        val current = post.value ?: return
        val updated = current.tasks.map {
            if (it.id == taskId) it.copy(done = !it.done) else it
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

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            Graph.posts.deletePost(familyId, postId).onSuccess { onDeleted() }
        }
    }
}
