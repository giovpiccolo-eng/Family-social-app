package com.codex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.codex.data.repository.CodexRepository
import com.codex.ui.navigation.CodexNavGraph
import com.codex.ui.theme.CodexTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val darkTheme = runBlocking {
            CodexRepository(this@MainActivity).osservaStats().first()?.temaScuro ?: false
        }

        setContent {
            CodexTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                CodexNavGraph(navController = navController)
            }
        }
    }
}
