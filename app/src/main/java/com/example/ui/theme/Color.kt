package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════
// ELEGANT DARK THEME COLORS (Original – Preserved)
// ══════════════════════════════════════════════
val ElegantDarkBg = Color(0xFF000000)
val ElegantDarkPrimary = Color(0xFF5B3CFF)
val ElegantDarkOnPrimary = Color(0xFFFFFFFF)
val ElegantDarkPrimaryContainer = Color(0xFF1E1E28)
val ElegantDarkOnPrimaryContainer = Color(0xFFEADDFF)

val ElegantDarkSurface = Color(0xFF0A0A0D)
val ElegantDarkOnSurface = Color(0xFFFFFFFF)
val ElegantDarkSurfaceVariant = Color(0xFF13131A)
val ElegantDarkOnSurfaceVariant = Color(0xFF909094)

val ElegantDarkOutline = Color(0xFF212325)
val ElegantDarkOutlineVariant = Color(0xFF1A1A24)
val ElegantDarkTextMuted = Color(0xFF909094)

// ══════════════════════════════════════════════
// GLASSMORPHISM LIGHT THEME COLORS
// ══════════════════════════════════════════════
val LightGlassBg = Color(0xFFF0F4F8)
val LightGlassBgEnd = Color(0xFFE8EDF5)
val LightGlassSurface = Color(0xFFFFFFFF) // Used with alpha for glass effect
val LightGlassSurfaceVariant = Color(0xFFF1F3F8)
val LightGlassOnSurface = Color(0xFF111827)
val LightGlassOnSurfaceVariant = Color(0xFF4B5563)
val LightGlassOutline = Color(0xFFD1D5DB)
val LightGlassNavBar = Color(0xFFFFFFFF) // Used with alpha 0.80 for translucent nav

// ══════════════════════════════════════════════
// CATEGORY COLORS (Shared between themes)
// ══════════════════════════════════════════════
val CategoryHealth = Color(0xFF1DD75B)
val CategoryHealthContainer = Color(0xFF0F2615)
val CategoryHealthContainerLight = Color(0xFFE6F7EB)

val CategoryPersonal = Color(0xFF1982FC)
val CategoryPersonalContainer = Color(0xFF0A182E)
val CategoryPersonalContainerLight = Color(0xFFE6F0FC)

val CategoryMorning = Color(0xFFF59E0B)
val CategoryMorningContainer = Color(0xFF26190B)
val CategoryMorningContainerLight = Color(0xFFFCF3E6)

val CategoryWork = Color(0xFFC882FF)
val CategoryWorkContainer = Color(0xFF1D0E29)
val CategoryWorkContainerLight = Color(0xFFF3E8FC)

val CategoryFitness = Color(0xFFF43F5E)
val CategoryFitnessContainer = Color(0xFF2B0F15)
val CategoryFitnessContainerLight = Color(0xFFFCE8EB)

// ══════════════════════════════════════════════
// BRAND / GRADIENT COLORS
// ══════════════════════════════════════════════
val GradientStart = Color(0xFF2E68FF)
val GradientEnd = Color(0xFFB130FF)
val XpColor = Color(0xFFC882FF)
val StreakColor = Color(0xFFFFB020)

// ══════════════════════════════════════════════
// 60-30-10 ACCENT PALETTES
// Each accent has: primary (10%), container/secondary (30%), onPrimary text
// The 60% is handled by the base theme (dark/light background)
// ══════════════════════════════════════════════

// --- BLUE ACCENT ---
val BlueAccentPrimary = Color(0xFF3B82F6)         // 10% – buttons, active states
val BlueAccentSecondary = Color(0xFFDBEAFE)        // 30% light – tinted card bg, borders
val BlueAccentSecondaryDark = Color(0xFF1E3A8A)    // 30% dark
val BlueAccentPrimaryDark = Color(0xFF60A5FA)      // 10% dark – brighter for visibility
val BlueOnPrimary = Color(0xFFFFFFFF)
val BlueOnPrimaryDark = Color(0xFF000000)

// --- GREEN ACCENT ---
val GreenAccentPrimary = Color(0xFF10B981)
val GreenAccentSecondary = Color(0xFFD1FAE5)
val GreenAccentSecondaryDark = Color(0xFF064E3B)
val GreenAccentPrimaryDark = Color(0xFF34D399)
val GreenOnPrimary = Color(0xFFFFFFFF)
val GreenOnPrimaryDark = Color(0xFF000000)

// --- RED ACCENT ---
val RedAccentPrimary = Color(0xFFEF4444)
val RedAccentSecondary = Color(0xFFFEE2E2)
val RedAccentSecondaryDark = Color(0xFF7F1D1D)
val RedAccentPrimaryDark = Color(0xFFF87171)
val RedOnPrimary = Color(0xFFFFFFFF)
val RedOnPrimaryDark = Color(0xFF000000)

// --- LEMON YELLOW ACCENT ---
// CRITICAL: Primary action color MUST be rgb(237, 230, 85) = 0xFFEDE655
val YellowAccentPrimary = Color(0xFFEDE655)        // Exact rgb(237, 230, 85)
val YellowAccentSecondary = Color(0xFFFEF9C3)      // Soft warm tinted background
val YellowAccentSecondaryDark = Color(0xFF713F12)   // Dark warm tint
val YellowAccentPrimaryDark = Color(0xFFEDE655)     // Same bright yellow for dark mode
val YellowOnPrimary = Color(0xFF1A1A1A)            // Dark text on yellow for accessibility
val YellowOnPrimaryDark = Color(0xFF1A1A1A)         // Always dark text on yellow
