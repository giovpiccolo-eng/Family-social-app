package com.dispensa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dispensa.app.ui.DispensaApp
import com.dispensa.app.ui.theme.DispensaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DispensaTheme {
                DispensaApp()
            }
        }
    }
}
