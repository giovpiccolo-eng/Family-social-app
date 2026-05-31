package com.familynest.app.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostStatus
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Surfaces the actionable posts (events / plans / projects) grouped by status. */
class ProjectsViewModel(familyId: String) : ViewModel() {

    val grouped: StateFlow<Map<PostStatus, List<Post>>> =
        Graph.posts.postsFlow(familyId)
            .map { posts ->
                posts.filter { it.typeEnum.isActionable }
                    .groupBy { it.statusEnum }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
