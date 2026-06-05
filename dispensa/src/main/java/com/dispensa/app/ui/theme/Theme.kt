package com.dispensa.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DispensaLightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Cream,
    primaryContainer = GoldSoft,
    onPrimaryContainer = NavyDeep,
    secondary = Gold,
    onSecondary = NavyDeep,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = NavyDeep,
    tertiary = Blue,
    onTertiary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EDE3),
    onSurfaceVariant = InkSoft,
    outline = Muted,
    error = Red,
    onError = Color.White,
)

@Composable
fun DispensaTheme(content: @Composable () -> Unit) {
    val colors = DispensaLightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Navy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DispensaTypography,
        content = content,
    )
}
