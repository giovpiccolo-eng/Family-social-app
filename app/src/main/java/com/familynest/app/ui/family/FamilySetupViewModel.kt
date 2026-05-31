package com.familynest.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FamilySetupUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class FamilySetupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FamilySetupUiState())
    val uiState: StateFlow<FamilySetupUiState> = _uiState.asStateFlow()

    fun createFamily(name: String) {
        val uid = Graph.auth.currentUid ?: return
        if (name.isBlank()) {
            _uiState.value = FamilySetupUiState(error = "Give your family a name first.")
            return
        }
        _uiState.value = FamilySetupUiState(loading = true)
        viewModelScope.launch {
            Graph.family.createFamily(uid, name)
                .onFailure { _uiState.value = FamilySetupUiState(error = it.message ?: "Couldn't create family.") }
            // Success flows through SessionViewModel via the updated user doc.
        }
    }

    fun joinFamily(code: String) {
        val uid = Graph.auth.currentUid ?: return
        if (code.isBlank()) {
            _uiState.value = FamilySetupUiState(error = "Enter the invite code.")
            return
        }
        _uiState.value = FamilySetupUiState(loading = true)
        viewModelScope.launch {
            Graph.family.joinFamily(uid, code)
                .onFailure { _uiState.value = FamilySetupUiState(error = it.message ?: "Couldn't join family.") }
        }
    }

    fun signOut() = Graph.auth.signOut()
}
