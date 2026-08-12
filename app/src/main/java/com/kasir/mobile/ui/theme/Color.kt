package com.kasir.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Kasir Mobile design tokens
//
// Identity: rental counter POS. One teal signal (action & money), amber for
// overtime attention, crimson for danger. Two surface "moods":
//   • Dark  — "night counter" (the original web-POS identity)
//   • Light — "paper counter" (daylight-readable, the app default)
//
// Screens consume the theme-aware getters below (KasirSurface, KasirGreen, …);
// they automatically follow whichever theme KasirTheme is rendering.
// ─────────────────────────────────────────────────────────────────────────────

// ── Dark palette (night counter) ────────────────────────────────────────────
val KasirDarkSurface = Color(0xFF0A0E13)        // app background
val KasirDarkSurfaceVariant = Color(0xFF11161D) // bars, columns
val KasirDarkSurfaceCard = Color(0xFF171E27)    // cards, inputs, dialogs
val KasirDarkLine = Color(0xFF26303C)           // dividers, outlines
val KasirDarkOnSurface = Color(0xFFE6EDF3)      // primary text
val KasirDarkOnSurfaceVariant = Color(0xFF94A3B8) // secondary text
val KasirDarkTextLow = Color(0xFF5B6B7B)        // captions, metadata
val KasirDarkGreen = Color(0xFF2DD4BF)          // signal
val KasirDarkGreenDark = Color(0xFF0F766E)      // signal-deep (pressed / container)
val KasirDarkGreenLight = Color(0xFF5EEAD4)     // signal-soft (fills)
val KasirDarkAccent = Color(0xFFF59E0B)         // amber — overtime, warnings
val KasirDarkError = Color(0xFFEF4444)          // crimson — zombie, delete

// ── Light palette (paper counter) ───────────────────────────────────────────
val KasirLightSurface = Color(0xFFF5F7FA)        // app background
val KasirLightSurfaceVariant = Color(0xFFECF0F4) // bars, columns
val KasirLightSurfaceCard = Color(0xFFFFFFFF)    // cards, inputs, dialogs
val KasirLightLine = Color(0xFFD5DCE3)           // dividers, outlines
val KasirLightOnSurface = Color(0xFF131920)      // primary text
val KasirLightOnSurfaceVariant = Color(0xFF45525F) // secondary text
val KasirLightTextLow = Color(0xFF7C8794)        // captions, metadata
val KasirLightGreen = Color(0xFF0F766E)          // signal (deep teal for contrast on white)
val KasirLightGreenDark = Color(0xFF115E59)      // signal-deep (pressed)
val KasirLightGreenLight = Color(0xFFCCFBF1)     // signal-soft (fills)
val KasirLightAccent = Color(0xFFD97706)         // amber — overtime, warnings
val KasirLightError = Color(0xFFDC2626)          // crimson — zombie, delete

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

val KasirGreen: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.green

val KasirGreenDark: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.greenDark

val KasirGreenLight: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.greenLight

val KasirAccent: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.accent

val KasirError: Color
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current.error
