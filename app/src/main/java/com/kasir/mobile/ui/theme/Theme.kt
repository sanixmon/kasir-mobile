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

// Single face — Plus Jakarta Sans, used everywhere.
private val JakartaSans = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold)
)

// Kept as distinct names so existing call sites stay unchanged.
val KasirBody = JakartaSans
val KasirDisplay = JakartaSans
val KasirMono = JakartaSans

// ── Theme-aware color set ────────────────────────────────────────────────────
data class KasirColors(
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceCard: Color,
    val line: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val textLow: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val cash: Color,
    val cashContainer: Color,
    val onCashContainer: Color,
    val qris: Color,
    val qrisContainer: Color,
    val onQrisContainer: Color,
    val accent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val onPrimary: Color = Color.White,
    val onAccent: Color,
    val onCash: Color = Color.White,
    val onQris: Color = Color.White,
    val onError: Color = Color.White,
    val green: Color = primary,
    val greenDark: Color = primaryDark,
    val greenLight: Color = primaryLight,
    val onGreen: Color = onPrimary
)

val DarkKasirColors = KasirColors(
    surface = KasirDarkSurface,
    surfaceVariant = KasirDarkSurfaceVariant,
    surfaceCard = KasirDarkSurfaceCard,
    line = KasirDarkLine,
    onSurface = KasirDarkOnSurface,
    onSurfaceVariant = KasirDarkOnSurfaceVariant,
    textLow = KasirDarkTextLow,
    primary = KasirDarkPrimary,
    primaryDark = KasirDarkPrimaryDark,
    primaryLight = KasirDarkPrimaryLight,
    primaryContainer = KasirDarkPrimaryContainer,
    onPrimaryContainer = KasirDarkOnPrimaryContainer,
    cash = KasirDarkCash,
    cashContainer = KasirDarkCashContainer,
    onCashContainer = KasirDarkOnCashContainer,
    qris = KasirDarkQris,
    qrisContainer = KasirDarkQrisContainer,
    onQrisContainer = KasirDarkOnQrisContainer,
    accent = KasirDarkAccent,
    accentContainer = KasirDarkAccentContainer,
    onAccentContainer = KasirDarkOnAccentContainer,
    error = KasirDarkError,
    errorContainer = KasirDarkErrorContainer,
    onErrorContainer = KasirDarkOnErrorContainer,
    onPrimary = KasirDarkOnPrimary,
    onAccent = KasirDarkOnAccent,
    onCash = KasirDarkOnCash,
    onQris = KasirDarkOnQris,
    onError = KasirDarkOnError
)

val LightKasirColors = KasirColors(
    surface = KasirLightSurface,
    surfaceVariant = KasirLightSurfaceVariant,
    surfaceCard = KasirLightSurfaceCard,
    line = KasirLightLine,
    onSurface = KasirLightOnSurface,
    onSurfaceVariant = KasirLightOnSurfaceVariant,
    textLow = KasirLightTextLow,
    primary = KasirLightPrimary,
    primaryDark = KasirLightPrimaryDark,
    primaryLight = KasirLightPrimaryLight,
    primaryContainer = KasirLightPrimaryContainer,
    onPrimaryContainer = KasirLightOnPrimaryContainer,
    cash = KasirLightCash,
    cashContainer = KasirLightCashContainer,
    onCashContainer = KasirLightOnCashContainer,
    qris = KasirLightQris,
    qrisContainer = KasirLightQrisContainer,
    onQrisContainer = KasirLightOnQrisContainer,
    accent = KasirLightAccent,
    accentContainer = KasirLightAccentContainer,
    onAccentContainer = KasirLightOnAccentContainer,
    error = KasirLightError,
    errorContainer = KasirLightErrorContainer,
    onErrorContainer = KasirLightOnErrorContainer,
    onPrimary = KasirLightOnPrimary,
    onAccent = KasirLightOnAccent,
    onCash = KasirLightOnCash,
    onQris = KasirLightOnQris,
    onError = KasirLightOnError
)

val LocalKasirColors = staticCompositionLocalOf { LightKasirColors }

private val DarkColorScheme = darkColorScheme(
    primary = KasirDarkPrimary,
    onPrimary = KasirDarkOnPrimary,
    primaryContainer = KasirDarkPrimaryContainer,
    onPrimaryContainer = KasirDarkOnPrimaryContainer,
    secondary = KasirDarkAccent,
    onSecondary = KasirDarkOnAccent,
    secondaryContainer = KasirDarkAccentContainer,
    onSecondaryContainer = KasirDarkOnAccentContainer,
    error = KasirDarkError,
    onError = KasirDarkOnError,
    errorContainer = KasirDarkErrorContainer,
    onErrorContainer = KasirDarkOnErrorContainer,
    background = KasirDarkSurface,
    onBackground = KasirDarkOnSurface,
    surface = KasirDarkSurfaceVariant,
    onSurface = KasirDarkOnSurface,
    surfaceVariant = KasirDarkSurfaceCard,
    onSurfaceVariant = KasirDarkOnSurfaceVariant,
    outline = KasirDarkLine,
    outlineVariant = KasirDarkLine.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = KasirLightPrimary,
    onPrimary = KasirLightOnPrimary,
    primaryContainer = KasirLightPrimaryContainer,
    onPrimaryContainer = KasirLightOnPrimaryContainer,
    secondary = KasirLightAccent,
    onSecondary = KasirLightOnAccent,
    secondaryContainer = KasirLightAccentContainer,
    onSecondaryContainer = KasirLightOnAccentContainer,
    error = KasirLightError,
    onError = KasirLightOnError,
    errorContainer = KasirLightErrorContainer,
    onErrorContainer = KasirLightOnErrorContainer,
    background = KasirLightSurface,
    onBackground = KasirLightOnSurface,
    surface = KasirLightSurfaceVariant,
    onSurface = KasirLightOnSurface,
    surfaceVariant = KasirLightSurfaceCard,
    onSurfaceVariant = KasirLightOnSurfaceVariant,
    outline = KasirLightLine,
    outlineVariant = KasirLightLine.copy(alpha = 0.5f)
)

private val KasirTypography = Typography(
    // Display face for headings & the brand
    displayLarge = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.25).sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    // Body — system sans stays quiet and legible
    bodyLarge = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = KasirBody, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
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
