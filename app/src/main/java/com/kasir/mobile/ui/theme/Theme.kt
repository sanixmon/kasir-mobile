package com.kasir.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kasir.mobile.R

// Display face — brand, screen titles, big totals (used sparingly)
val KasirDisplay = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

// Mono face — timers, queue numbers, prices (POS-machine precision)
val KasirMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

// ── Theme-aware color set ────────────────────────────────────────────────────
data class KasirColors(
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceCard: Color,
    val line: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val textLow: Color,
    val green: Color,
    val greenDark: Color,
    val greenLight: Color,
    val cash: Color,
    val qris: Color,
    val accent: Color,
    val error: Color
)

val DarkKasirColors = KasirColors(
    surface = KasirDarkSurface,
    surfaceVariant = KasirDarkSurfaceVariant,
    surfaceCard = KasirDarkSurfaceCard,
    line = KasirDarkLine,
    onSurface = KasirDarkOnSurface,
    onSurfaceVariant = KasirDarkOnSurfaceVariant,
    textLow = KasirDarkTextLow,
    green = KasirDarkGreen,
    greenDark = KasirDarkGreenDark,
    greenLight = KasirDarkGreenLight,
    cash = KasirDarkCash,
    qris = KasirDarkQris,
    accent = KasirDarkAccent,
    error = KasirDarkError
)

val LightKasirColors = KasirColors(
    surface = KasirLightSurface,
    surfaceVariant = KasirLightSurfaceVariant,
    surfaceCard = KasirLightSurfaceCard,
    line = KasirLightLine,
    onSurface = KasirLightOnSurface,
    onSurfaceVariant = KasirLightOnSurfaceVariant,
    textLow = KasirLightTextLow,
    green = KasirLightGreen,
    greenDark = KasirLightGreenDark,
    greenLight = KasirLightGreenLight,
    cash = KasirLightCash,
    qris = KasirLightQris,
    accent = KasirLightAccent,
    error = KasirLightError
)

val LocalKasirColors = staticCompositionLocalOf { DarkKasirColors }

private val DarkColorScheme = darkColorScheme(
    primary = KasirDarkGreen,
    onPrimary = Color.White,
    primaryContainer = KasirDarkGreenDark,
    onPrimaryContainer = KasirDarkGreenLight,
    secondary = KasirDarkAccent,
    onSecondary = KasirDarkSurface,
    error = KasirDarkError,
    background = KasirDarkSurface,
    onBackground = KasirDarkOnSurface,
    surface = KasirDarkSurfaceVariant,
    onSurface = KasirDarkOnSurface,
    surfaceVariant = KasirDarkSurfaceCard,
    onSurfaceVariant = KasirDarkOnSurfaceVariant,
    outline = KasirDarkLine,
)

private val LightColorScheme = lightColorScheme(
    primary = KasirLightGreen,
    onPrimary = Color.White,
    primaryContainer = KasirLightGreenDark,
    onPrimaryContainer = KasirLightGreenLight,
    secondary = KasirLightAccent,
    onSecondary = Color.White,
    error = KasirLightError,
    background = KasirLightSurface,
    onBackground = KasirLightOnSurface,
    surface = KasirLightSurfaceVariant,
    onSurface = KasirLightOnSurface,
    surfaceVariant = KasirLightSurfaceCard,
    onSurfaceVariant = KasirLightOnSurfaceVariant,
    outline = KasirLightLine,
)

private val KasirTypography = Typography(
    // Display face for headings & the brand
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.25).sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    // Body — system sans stays quiet and legible
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)

/**
 * App theme. Defaults to the LIGHT "paper counter" theme — a rental counter
 * works under daylight, so light is the sensible default. Pass [darkTheme]
 * true to opt into the original "night counter" look.
 */
@Composable
fun KasirTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val kasirColors = if (darkTheme) DarkKasirColors else LightKasirColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalKasirColors provides kasirColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KasirTypography,
            content = content
        )
    }
}
