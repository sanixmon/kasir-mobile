package com.kasir.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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

private val DarkColorScheme = darkColorScheme(
    primary = KasirGreen,
    onPrimary = KasirSurface,
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
    outline = KasirLine,
)

private val KasirTypography = Typography(
    // Display face for headings & the brand
    displayLarge = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.25).sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    // Body — system sans stays quiet and legible
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = KasirDisplay, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)

@Composable
fun KasirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = KasirTypography,
        content = content
    )
}
