package com.kasir.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = KasirGreen,
    onPrimary = KasirOnSurface,
    primaryContainer = KasirGreenDark,
    onPrimaryContainer = KasirGreenLight,
    secondary = KasirAccent,
    onSecondary = KasirSurface,
    error = KasirError,
    background = KasirSurface,
    onBackground = KasirOnSurface,
    surface = KasirSurfaceVariant,
    onSurface = KasirOnSurface,
    surfaceVariant = KasirSurfaceCard,
    onSurfaceVariant = KasirOnSurfaceVariant,
)

@Composable
fun KasirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
