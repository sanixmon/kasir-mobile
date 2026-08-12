package com.kasir.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// ── Kasir Mobile design tokens ──────────────────────────────────────────────
// Identity: rental counter POS at night — deep blue-black "counter" surfaces,
// a single vivid teal signal (start / money), amber for overtime attention and
// crimson for danger (zombie sessions / destructive actions).
// Kept intentionally restrained: one accent for action, one for attention.

// Surfaces — "the counter"
val KasirSurface = Color(0xFF0A0E13)        // abyss — app background
val KasirSurfaceVariant = Color(0xFF11161D) // panel — app bars, columns
val KasirSurfaceCard = Color(0xFF171E27)    // raised — cards, inputs, dialogs
val KasirLine = Color(0xFF26303C)           // line — dividers, outlines

// Signal — action & money (teal)
val KasirGreen = Color(0xFF2DD4BF)          // signal
val KasirGreenDark = Color(0xFF0F766E)      // signal-deep (pressed / container)
val KasirGreenLight = Color(0xFF5EEAD4)     // signal-soft (fills)

// Attention
val KasirAccent = Color(0xFFF59E0B)         // amber — overtime, grace, warnings

// Danger
val KasirError = Color(0xFFEF4444)          // crimson — zombie, delete, errors

// Text hierarchy
val KasirOnSurface = Color(0xFFE6EDF3)      // primary text
val KasirOnSurfaceVariant = Color(0xFF94A3B8) // secondary text
val KasirTextLow = Color(0xFF5B6B7B)        // captions, metadata
