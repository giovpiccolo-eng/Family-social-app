package com.familynest.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostType
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the shared feed. Reads all posts for the family (ordered by recency, which
 * uses Firestore's automatic single-field index) and filters by type in memory so
 * no composite index setup is required.
 */
class FeedViewModel(private val familyId: String) : ViewModel() {

    private val _filter = MutableStateFlow<PostType?>(null)
    val filter: StateFlow<PostType?> = _filter.asStateFlow()

    val posts: StateFlow<List<Post>> =
        combine(Graph.posts.postsFlow(familyId), _filter) { all, type ->
            if (type == null) all else all.filter { it.typeEnum == type }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(type: PostType?) {
        _filter.value = type
    }

    fun toggleLike(post: Post) {
        val uid = Graph.auth.currentUid ?: return
        val liked = post.likedBy.contains(uid)
        viewModelScope.launch {
            Graph.posts.toggleLike(familyId, post.id, uid, liked)
        }
    }
}
