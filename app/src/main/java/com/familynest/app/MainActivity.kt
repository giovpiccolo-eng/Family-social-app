package com.familynest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.familynest.app.ui.FamilyNestApp
import com.familynest.app.ui.theme.FamilyNestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FamilyNestTheme {
                FamilyNestApp()
            }
        }
    }
}
