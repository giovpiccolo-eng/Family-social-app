package com.codex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkMode = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = OroImperiale,
    onPrimary = InchiostroProfondo,
    primaryContainer = OroChiaro,
    onPrimaryContainer = InchiostroProfondo,
    secondary = PinoForesta,
    onSecondary = Pergamena,
    secondaryContainer = PergamenaScura,
    onSecondaryContainer = InchiostroProfondo,
    background = Pergamena,
    onBackground = InchiostroProfondo,
    surface = Pergamena,
    onSurface = InchiostroProfondo,
    surfaceVariant = PergamenaScura,
    onSurfaceVariant = InchiostroMedio,
    error = RossoErrore,
    onError = Pergamena
)

private val DarkColors = darkColorScheme(
    primary = ArchivioOro,
    onPrimary = ArchivioSfondo,
    primaryContainer = InchiostroMedio,
    onPrimaryContainer = ArchivioOro,
    secondary = VerdeSuccesso,
    onSecondary = ArchivioSfondo,
    secondaryContainer = ArchivioSfondoAlt,
    onSecondaryContainer = ArchivioTesto,
    background = ArchivioSfondo,
    onBackground = ArchivioTesto,
    surface = ArchivioSfondoCard,
    onSurface = ArchivioTesto,
    surfaceVariant = ArchivioSfondoAlt,
    onSurfaceVariant = ArchivioTestoMuto,
    error = Color(0xFFCF6679),
    onError = ArchivioSfondo
)

@Composable
fun CodexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalDarkMode provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = CodexTypography,
            content = content
        )
    }
}
