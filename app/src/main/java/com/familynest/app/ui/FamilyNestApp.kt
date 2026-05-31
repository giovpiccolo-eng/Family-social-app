package com.familynest.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familynest.app.di.Graph
import com.familynest.app.ui.auth.LoginScreen
import com.familynest.app.ui.family.FamilySetupScreen
import com.familynest.app.ui.main.MainScaffold
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Root composable: routes between auth, family onboarding, and the main app. */
@Composable
fun FamilyNestApp(sessionViewModel: SessionViewModel = viewModel()) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()

    // Ask for notification permission once (Android 13+).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by the system; nothing to do */ }

    // When signed in, request permission and register this device's push token.
    val signedInUid = state.signedInUid()
    LaunchedEffect(signedInUid) {
        if (signedInUid != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                Graph.auth.saveFcmToken(token)
            }
        }
    }

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

private fun SessionState.signedInUid(): String? = when (this) {
    is SessionState.NeedsFamily -> user.uid
    is SessionState.Ready -> user.uid
    else -> null
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
