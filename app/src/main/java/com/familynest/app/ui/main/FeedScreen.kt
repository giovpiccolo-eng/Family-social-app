package com.familynest.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.PostType
import com.familynest.app.di.Graph
import com.familynest.app.ui.components.PostCard

@Composable
fun FeedScreen(
    user: AppUser,
    onOpenPost: (String) -> Unit,
) {
    val viewModel: FeedViewModel = viewModel(
        factory = viewModelFactory { initializer { FeedViewModel(user.familyId) } }
    )
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val myUid = Graph.auth.currentUid

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    "What are we dreaming up?",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(12.dp))
                FilterRow(filter, viewModel::setFilter)
            }
        }

        if (posts.isEmpty()) {
            item { EmptyFeed() }
        } else {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    likedByMe = myUid != null && post.likedBy.contains(myUid),
                    onClick = { onOpenPost(post.id) },
                    onLike = { viewModel.toggleLike(post) },
                )
            }
        }
    }
}

@Composable
private fun FilterRow(selected: PostType?, onSelect: (PostType?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
            )
        }
        items(PostType.entries) { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text("${type.emoji} ${type.label}") },
            )
        }
    }
}

@Composable
private fun EmptyFeed() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Nothing here yet.\nTap + to share the family's first idea.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
