package com.familynest.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Twilight,
    onPrimary = Color.White,
    primaryContainer = TwilightLight,
    onPrimaryContainer = TwilightDark,
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = Gold,
    onSecondaryContainer = Color(0xFF3A2C00),
    tertiary = Sage,
    onTertiary = Color.White,
    error = Coral,
    background = Cream,
    onBackground = InkOnCream,
    surface = SurfaceLight,
    onSurface = InkOnCream,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF504A40),
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF332600),
    primaryContainer = TwilightDark,
    onPrimaryContainer = TwilightLight,
    secondary = Gold,
    onSecondary = Color(0xFF332600),
    secondaryContainer = TwilightDark,
    onSecondaryContainer = Gold,
    tertiary = Sage,
    onTertiary = Color(0xFF09281A),
    error = Coral,
    background = Color(0xFF15111F),
    onBackground = Color(0xFFEDE7F6),
    surface = SurfaceDark,
    onSurface = Color(0xFFEDE7F6),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCDC4DB),
    outline = OutlineDark,
)

@Composable
fun FamilyNestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = FamilyNestTypography,
        content = content,
    )
}
