package com.familynest.app.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Post
import com.familynest.app.data.model.PostStatus
import com.familynest.app.data.model.PostType
import com.familynest.app.data.model.TaskItem
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateUiState(
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class CreatePostViewModel(private val user: AppUser) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    fun submit(
        type: PostType,
        title: String,
        description: String,
        imageUri: Uri?,
        dueDate: Long?,
        checklist: List<String>,
    ) {
        if (title.isBlank()) {
            _uiState.value = CreateUiState(error = "Add a title so the family knows what this is.")
            return
        }
        _uiState.value = CreateUiState(submitting = true)
        viewModelScope.launch {
            var imageUrl = ""
            if (imageUri != null) {
                Graph.posts.uploadImage(user.familyId, imageUri)
                    .onSuccess { imageUrl = it }
                    .onFailure {
                        _uiState.value = CreateUiState(error = "Image upload failed: ${it.message}")
                        return@launch
                    }
            }
            val tasks = checklist
                .filter { it.isNotBlank() }
                .map { TaskItem(id = UUID.randomUUID().toString(), text = it.trim()) }

            val post = Post(
                familyId = user.familyId,
                authorId = user.uid,
                authorName = user.displayName,
                authorPhotoUrl = user.photoUrl,
                type = type.name,
                title = title.trim(),
                description = description.trim(),
                imageUrl = imageUrl,
                status = if (type.isActionable) PostStatus.PLANNED.name else PostStatus.DREAMING.name,
                dueDate = dueDate,
                tasks = tasks,
            )
            Graph.posts.createPost(post)
                .onSuccess { _uiState.value = CreateUiState(done = true) }
                .onFailure { _uiState.value = CreateUiState(error = it.message ?: "Couldn't share that.") }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
