package com.familynest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    likedByMe: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            AuthorRow(post)
            Spacer(Modifier.height(12.dp))
            TypeChip(post.typeEnum)
            Spacer(Modifier.height(8.dp))
            Text(post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (post.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    post.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
            if (post.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
            if (post.typeEnum.isActionable) {
                Spacer(Modifier.height(12.dp))
                ActionableFooter(post)
            }
            Spacer(Modifier.height(8.dp))
            LikeRow(post, likedByMe, onLike)
        }
    }
}

@Composable
private fun AuthorRow(post: Post) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(post.authorPhotoUrl, post.authorName)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(post.authorName, style = MaterialTheme.typography.titleMedium)
            Text(relativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Avatar(photoUrl: String, name: String, size: Int = 40) {
    val initials = name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("")
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp).clip(CircleShape),
            )
        } else {
            Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun TypeChip(type: PostType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "${type.emoji} ${type.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ActionableFooter(post: Post) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusPill(post)
        if (post.tasks.isNotEmpty()) {
            Text(
                "✓ ${post.completedTaskCount}/${post.tasks.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        post.dueDate?.let {
            Text(
                "· due ${shortDate(it)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusPill(post: Post) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            post.statusEnum.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LikeRow(post: Post, likedByMe: Boolean, onLike: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Cheer",
            tint = if (likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable { onLike() },
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (post.likedBy.isEmpty()) "Cheer this on" else "${post.likedBy.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun relativeTime(epoch: Long): String {
    val diff = System.currentTimeMillis() - epoch
    val mins = diff / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        mins < 10080 -> "${mins / 1440}d ago"
        else -> shortDate(epoch)
    }
}

fun shortDate(epoch: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epoch))
