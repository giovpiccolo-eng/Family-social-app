package com.familynest.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.Post
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A dated item plus the "Month Year" bucket it belongs to. */
data class CalendarEntry(
    val post: Post,
    val monthLabel: String,
    val isPast: Boolean,
)

/** Surfaces every post that has a target date, ordered chronologically. */
class CalendarViewModel(familyId: String) : ViewModel() {

    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val entries: StateFlow<List<CalendarEntry>> =
        Graph.posts.postsFlow(familyId)
            .map { posts ->
                val now = System.currentTimeMillis()
                posts.filter { it.dueDate != null }
                    .sortedBy { it.dueDate }
                    .map { post ->
                        val due = post.dueDate ?: 0L
                        CalendarEntry(
                            post = post,
                            monthLabel = monthFormat.format(Date(due)),
                            isPast = due < startOfToday(now),
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun startOfToday(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
