package com.codex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.codex.data.repository.CodexRepository
import com.codex.ui.navigation.CodexNavGraph
import com.codex.ui.theme.CodexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repo = CodexRepository(this)

        setContent {
            val stats by repo.osservaStats().collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = stats?.temaScuro ?: false

            CodexTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                CodexNavGraph(navController = navController, repo = repo)
            }
        }
    }
}
