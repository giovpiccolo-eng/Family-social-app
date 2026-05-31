package com.familynest.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    user: AppUser,
    onOpenPost: (String) -> Unit,
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = viewModelFactory { initializer { CalendarViewModel(user.familyId) } }
    )
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Calendar", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Everything with a date — events, plans and project deadlines.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (entries.isEmpty()) {
            item { EmptyCalendar() }
        }

        // Group consecutive entries by their month label.
        val grouped = entries.groupBy { it.monthLabel }
        grouped.forEach { (month, monthEntries) ->
            item(key = "month_$month") {
                Text(
                    month,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(monthEntries, key = { it.post.id }) { entry ->
                CalendarRow(entry.post, entry.isPast, onClick = { onOpenPost(entry.post.id) })
            }
        }
    }
}

@Composable
private fun CalendarRow(post: Post, isPast: Boolean, onClick: () -> Unit) {
    val due = post.dueDate ?: 0L
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateBadge(due, isPast)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("${post.typeEnum.emoji} ${post.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    post.statusEnum.label + if (isPast) " · past" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun DateBadge(epoch: Long, isPast: Boolean) {
    val day = SimpleDateFormat("d", Locale.getDefault()).format(Date(epoch))
    val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epoch))
    val container = if (isPast) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.primaryContainer
    val content = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, style = MaterialTheme.typography.titleLarge, color = content, fontWeight = FontWeight.Bold)
            Text(weekday.uppercase(), style = MaterialTheme.typography.labelSmall, color = content)
        }
    }
}

@Composable
private fun EmptyCalendar() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📅", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Nothing scheduled yet.\nAdd a target date to an event or plan.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
