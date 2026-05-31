package com.familynest.app.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostStatus
import com.familynest.app.ui.components.shortDate

@Composable
fun ProjectsScreen(
    user: AppUser,
    onOpenPost: (String) -> Unit,
) {
    val viewModel: ProjectsViewModel = viewModel(
        factory = viewModelFactory { initializer { ProjectsViewModel(user.familyId) } }
    )
    val grouped by viewModel.grouped.collectAsStateWithLifecycle()
    val totalCount = grouped.values.sumOf { it.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Plans in motion", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Events, plans and projects the family is working towards.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (totalCount == 0) {
            item { EmptyProjects() }
        }

        // Show columns in workflow order, skipping empty buckets.
        PostStatus.entries.forEach { status ->
            val posts = grouped[status].orEmpty()
            if (posts.isNotEmpty()) {
                item(key = "header_$status") {
                    Text(
                        "${status.label} · ${posts.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(posts, key = { it.id }) { post ->
                    ProjectRow(post, onClick = { onOpenPost(post.id) })
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(post: Post, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${post.typeEnum.emoji} ${post.title}", style = MaterialTheme.typography.titleMedium)
            if (post.tasks.isNotEmpty() || post.dueDate != null) {
                Spacer(Modifier.height(4.dp))
                val parts = buildList {
                    if (post.tasks.isNotEmpty()) add("✓ ${post.completedTaskCount}/${post.tasks.size}")
                    post.dueDate?.let { add("🎯 ${shortDate(it)}") }
                }
                Text(
                    parts.joinToString("   "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyProjects() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛠️", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "No plans yet.\nShare an Event, Plan or Project to track it here.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
