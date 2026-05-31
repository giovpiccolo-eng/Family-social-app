package com.familynest.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.AppUser
import com.familynest.app.di.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Top-level navigation state derived from auth + the user's profile document. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class NeedsFamily(val user: AppUser) : SessionState
    data class Ready(val user: AppUser) : SessionState
}

class SessionViewModel : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SessionState> =
        Graph.auth.authStateFlow()
            .flatMapLatest { firebaseUser ->
                if (firebaseUser == null) {
                    flowOf(SessionState.SignedOut)
                } else {
                    Graph.auth.userFlow(firebaseUser.uid).map { profile ->
                        val user = profile ?: AppUser(
                            uid = firebaseUser.uid,
                            displayName = firebaseUser.displayName ?: "Family member",
                            email = firebaseUser.email ?: "",
                            photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        )
                        if (user.familyId.isBlank()) SessionState.NeedsFamily(user)
                        else SessionState.Ready(user)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}
