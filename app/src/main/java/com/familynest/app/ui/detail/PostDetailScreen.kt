package com.familynest.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Comment
import com.familynest.app.data.model.PostStatus
import com.familynest.app.data.model.TaskItem
import com.familynest.app.ui.components.Avatar
import com.familynest.app.ui.components.TypeChip
import com.familynest.app.ui.components.shortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    user: AppUser,
    postId: String,
    onBack: () -> Unit,
) {
    val viewModel: PostDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { PostDetailViewModel(user, postId) } }
    )
    val post by viewModel.post.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val myUid = user.uid

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(post?.typeEnum?.label ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (post?.authorId == myUid) {
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = post
        if (current == null) {
            Spacer(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(current.authorPhotoUrl, current.authorName)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(current.authorName, style = MaterialTheme.typography.titleMedium)
                    Text(shortDate(current.createdAt), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            TypeChip(current.typeEnum)
            Text(current.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (current.description.isNotBlank()) {
                Text(current.description, style = MaterialTheme.typography.bodyLarge)
            }
            if (current.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = current.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            // Task-manager controls for actionable posts.
            if (current.typeEnum.isActionable) {
                current.dueDate?.let {
                    Text("🎯 Target: ${shortDate(it)}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Status", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PostStatus.entries) { status ->
                        FilterChip(
                            selected = current.statusEnum == status,
                            onClick = { viewModel.setStatus(status) },
                            label = { Text(status.label) },
                        )
                    }
                }

                if (current.tasks.isNotEmpty()) {
                    Text(
                        "Checklist · ${current.completedTaskCount}/${current.tasks.size}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    current.tasks.forEach { task ->
                        TaskRow(
                            task = task,
                            members = members,
                            onToggle = { viewModel.toggleTask(task.id) },
                            onAssign = { viewModel.assignTask(task.id, it) },
                        )
                    }
                }
            }

            // Cheers / likes
            Row(verticalAlignment = Alignment.CenterVertically) {
                val liked = current.likedBy.contains(myUid)
                IconButton(onClick = { viewModel.toggleLike() }) {
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Cheer",
                        tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    when (current.likedBy.size) {
                        0 -> "Be the first to cheer this on"
                        1 -> "1 cheer"
                        else -> "${current.likedBy.size} cheers"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HorizontalDivider()

            // Conversation
            Text("Conversation", style = MaterialTheme.typography.titleMedium)
            if (comments.isEmpty()) {
                Text(
                    "No comments yet. Start the conversation 💬",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                comments.forEach { CommentRow(it) }
            }
            CommentInput(onSend = viewModel::addComment)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskItem,
    members: List<AppUser>,
    onToggle: () -> Unit,
    onAssign: (AppUser?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() })
        Text(
            task.text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Column {
            AssistChip(
                onClick = { menuOpen = true },
                label = {
                    Text(
                        task.assigneeName.ifBlank { "Assign" }.substringBefore(' '),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Unassigned") },
                    onClick = { onAssign(null); menuOpen = false },
                )
                members.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.displayName) },
                        onClick = { onAssign(member); menuOpen = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Avatar(comment.authorPhotoUrl, comment.authorName, size = 32)
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(shortDate(comment.createdAt), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CommentInput(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Add a comment…") },
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                onSend(text)
                text = ""
            },
            enabled = text.isNotBlank(),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
        }
    }
}
