package com.familynest.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familynest.app.ui.auth.LoginScreen
import com.familynest.app.ui.family.FamilySetupScreen
import com.familynest.app.ui.main.MainScaffold

/** Root composable: routes between auth, family onboarding, and the main app. */
@Composable
fun FamilyNestApp(sessionViewModel: SessionViewModel = viewModel()) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it.key() },
            label = "session",
        ) { current ->
            when (current) {
                SessionState.Loading -> LoadingScreen()
                SessionState.SignedOut -> LoginScreen()
                is SessionState.NeedsFamily -> FamilySetupScreen(current.user)
                is SessionState.Ready -> MainScaffold(current.user)
            }
        }
    }
}

// Coarse key so AnimatedContent only animates between top-level destinations.
private fun SessionState.key(): String = when (this) {
    SessionState.Loading -> "loading"
    SessionState.SignedOut -> "signedOut"
    is SessionState.NeedsFamily -> "needsFamily"
    is SessionState.Ready -> "ready"
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
