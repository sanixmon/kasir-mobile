package com.kasir.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Kasir Mobile design tokens (Role-based Unified Palette)
//
// Identity: rental counter POS.
//   • Primary signal: cobalt blue (actions, active tab, buttons)
//   • Accent: amber (overtime attention, warnings)
//   • Danger: crimson (zombie session, destructive actions)
//   • Cash: emerald (physical currency)
//   • QRIS: cyan (digital payments)
//
// Surface moods:
//   • Light ("paper counter") — default daylight-readable counter theme
//   • Dark ("night counter") — low-glare evening counter theme
// ─────────────────────────────────────────────────────────────────────────────

// ── Dark palette (night counter) ────────────────────────────────────────────
val KasirDarkSurface = Color(0xFF0A0E13)        // app background
val KasirDarkSurfaceVariant = Color(0xFF11161D) // bars, columns
val KasirDarkSurfaceCard = Color(0xFF171E27)    // cards, inputs, dialogs
val KasirDarkLine = Color(0xFF26303C)           // dividers, outlines
val KasirDarkOnSurface = Color(0xFFE6EDF3)      // primary text
val KasirDarkOnSurfaceVariant = Color(0xFF94A3B8) // secondary text
val KasirDarkTextLow = Color(0xFF5B6B7B)        // captions, metadata

val KasirDarkPrimary = Color(0xFF3E5CFF)        // primary signal (blue)
val KasirDarkPrimaryDark = Color(0xFF2E4ED8)    // primary pressed
val KasirDarkPrimaryLight = Color(0xFFA7B8FF)   // primary soft fill
val KasirDarkPrimaryContainer = Color(0xFF1E2D69) // container / chip fill
val KasirDarkOnPrimaryContainer = Color(0xFFA7B8FF)

val KasirDarkAccent = Color(0xFFF59E0B)         // amber — overtime, warnings
val KasirDarkAccentContainer = Color(0xFF452B04)
val KasirDarkOnAccentContainer = Color(0xFFFDE68A)

val KasirDarkError = Color(0xFFEF4444)          // crimson — danger
val KasirDarkErrorContainer = Color(0xFF451414)
val KasirDarkOnErrorContainer = Color(0xFFFECACA)

val KasirDarkCash = Color(0xFF10B981)           // emerald — cash
val KasirDarkCashContainer = Color(0xFF063B29)
val KasirDarkOnCashContainer = Color(0xFFA7F3D0)

val KasirDarkQris = Color(0xFF06B6D4)           // cyan — qris
val KasirDarkQrisContainer = Color(0xFF083344)
val KasirDarkOnQrisContainer = Color(0xFFA5F3FC)

val KasirDarkOnPrimary = Color.White
val KasirDarkOnAccent = Color(0xFF0A0E13)
val KasirDarkOnCash = Color.White
val KasirDarkOnQris = Color.White
val KasirDarkOnError = Color.White

// ── Light palette (paper counter) ───────────────────────────────────────────
val KasirLightSurface = Color(0xFFF5F7FA)        // app background
val KasirLightSurfaceVariant = Color(0xFFECF0F4) // bars, columns
val KasirLightSurfaceCard = Color(0xFFFFFFFF)    // cards, inputs, dialogs
val KasirLightLine = Color(0xFFD5DCE3)           // dividers, outlines
val KasirLightOnSurface = Color(0xFF131920)      // primary text
val KasirLightOnSurfaceVariant = Color(0xFF45525F) // secondary text
val KasirLightTextLow = Color(0xFF5F6B78)        // captions, metadata

val KasirLightPrimary = Color(0xFF2E4ED8)        // primary signal (blue)
val KasirLightPrimaryDark = Color(0xFF2943C9)    // primary pressed
val KasirLightPrimaryLight = Color(0xFFDDE3FF)   // primary soft fill
val KasirLightPrimaryContainer = Color(0xFFE8EDFF) // container / chip fill
val KasirLightOnPrimaryContainer = Color(0xFF1E3A8A)

val KasirLightAccent = Color(0xFFB45309)         // amber — overtime, warnings
val KasirLightAccentContainer = Color(0xFFFEF3C7)
val KasirLightOnAccentContainer = Color(0xFF78350F)

val KasirLightError = Color(0xFFDC2626)          // crimson — danger
val KasirLightErrorContainer = Color(0xFFFEE2E2)
val KasirLightOnErrorContainer = Color(0xFF991B1B)

val KasirLightCash = Color(0xFF047857)           // emerald — cash
val KasirLightCashContainer = Color(0xFFD1FAE5)
val KasirLightOnCashContainer = Color(0xFF065F46)

val KasirLightQris = Color(0xFF0369A1)           // cyan — qris
val KasirLightQrisContainer = Color(0xFFCFFAFE)
val KasirLightOnQrisContainer = Color(0xFF155E75)

val KasirLightOnPrimary = Color.White
val KasirLightOnAccent = Color.White
val KasirLightOnCash = Color.White
val KasirLightOnQris = Color.White
val KasirLightOnError = Color.White

// ── Legacy Aliases ──────────────────────────────────────────────────────────
val KasirDarkGreen = KasirDarkPrimary
val KasirDarkGreenDark = KasirDarkPrimaryDark
val KasirDarkGreenLight = KasirDarkPrimaryLight
val KasirDarkOnGreen = KasirDarkOnPrimary

val KasirLightGreen = KasirLightPrimary
val KasirLightGreenDark = KasirLightPrimaryDark
val KasirLightGreenLight = KasirLightPrimaryLight
val KasirLightOnGreen = KasirLightOnPrimary

// ── Theme-aware tokens (used across screens) ────────────────────────────────
val KasirSurface: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.surface

val KasirSurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.surfaceVariant

val KasirSurfaceCard: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.surfaceCard

val KasirLine: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.line

val KasirOnSurface: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onSurface

val KasirOnSurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onSurfaceVariant

val KasirTextLow: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.textLow

val KasirPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.primary

val KasirPrimaryDark: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.primaryDark

val KasirPrimaryLight: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.primaryLight

val KasirPrimaryContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.primaryContainer

val KasirOnPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onPrimary

val KasirOnPrimaryContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onPrimaryContainer

val KasirAccent: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.accent

val KasirAccentContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.accentContainer

val KasirOnAccent: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onAccent

val KasirOnAccentContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onAccentContainer

val KasirError: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.error

val KasirErrorContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.errorContainer

val KasirOnError: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onError

val KasirOnErrorContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onErrorContainer

val KasirCash: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.cash

val KasirCashContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.cashContainer

val KasirOnCash: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onCash

val KasirOnCashContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onCashContainer

val KasirQris: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.qris

val KasirQrisContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.qrisContainer

val KasirOnQris: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onQris

val KasirOnQrisContainer: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.onQrisContainer

// ── Compatibility aliases (deprecated in favor of KasirPrimary) ─────────────
@Deprecated("Use KasirPrimary instead", ReplaceWith("KasirPrimary"))
val KasirGreen: Color
    @Composable @ReadOnlyComposable get() = KasirPrimary

@Deprecated("Use KasirPrimaryDark instead", ReplaceWith("KasirPrimaryDark"))
val KasirGreenDark: Color
    @Composable @ReadOnlyComposable get() = KasirPrimaryDark

@Deprecated("Use KasirPrimaryLight instead", ReplaceWith("KasirPrimaryLight"))
val KasirGreenLight: Color
    @Composable @ReadOnlyComposable get() = KasirPrimaryLight

@Deprecated("Use KasirOnPrimary instead", ReplaceWith("KasirOnPrimary"))
val KasirOnGreen: Color
    @Composable @ReadOnlyComposable get() = KasirOnPrimary
