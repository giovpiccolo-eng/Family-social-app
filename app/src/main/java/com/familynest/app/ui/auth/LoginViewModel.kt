package com.familynest.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.di.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onSignInResult(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            _uiState.value = LoginUiState(error = "Google sign-in was cancelled or failed.")
            return
        }
        _uiState.value = LoginUiState(loading = true)
        viewModelScope.launch {
            Graph.auth.signInWithGoogle(idToken)
                .onFailure { e ->
                    _uiState.value = LoginUiState(error = e.message ?: "Sign-in failed.")
                }
            // On success, SessionViewModel reacts to the auth-state change and navigates.
        }
    }

    fun onError(message: String) {
        _uiState.value = LoginUiState(error = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
