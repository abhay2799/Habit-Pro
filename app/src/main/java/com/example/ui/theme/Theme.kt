package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════
// Extended Accent Palette (exposed via CompositionLocal)
// Provides 60-30-10 rule colors beyond MaterialTheme
// ══════════════════════════════════════════════
@Immutable
data class AccentPalette(
    val primary: Color,           // 10% – buttons, toggles, active states
    val secondary: Color,         // 30% – tinted card backgrounds, borders, inactive states
    val onPrimary: Color,         // Text on primary color
    val onSecondary: Color,       // Text on secondary tinted backgrounds
    val name: String              // Accent name for reference
)

val LocalAccentPalette = staticCompositionLocalOf {
    AccentPalette(
        primary = BlueAccentPrimary,
        secondary = BlueAccentSecondary,
        onPrimary = BlueOnPrimary,
        onSecondary = Color(0xFF1E3A5F),
        name = "Blue"
    )
}

// ══════════════════════════════════════════════
// Theme Composable
// ══════════════════════════════════════════════
@Composable
fun MyApplicationTheme(
    isDarkMode: Boolean = true,
    accentColor: String = "Blue",
    content: @Composable () -> Unit
) {
    // Resolve 60-30-10 accent palette
    val accentPalette = when (accentColor) {
        "Green" -> if (isDarkMode) {
            AccentPalette(GreenAccentPrimaryDark, GreenAccentSecondaryDark, GreenOnPrimaryDark, Color(0xFFD1FAE5), "Green")
        } else {
            AccentPalette(GreenAccentPrimary, GreenAccentSecondary, GreenOnPrimary, Color(0xFF064E3B), "Green")
        }
        "Red" -> if (isDarkMode) {
            AccentPalette(RedAccentPrimaryDark, RedAccentSecondaryDark, RedOnPrimaryDark, Color(0xFFFEE2E2), "Red")
        } else {
            AccentPalette(RedAccentPrimary, RedAccentSecondary, RedOnPrimary, Color(0xFF7F1D1D), "Red")
        }
        "Yellow" -> if (isDarkMode) {
            AccentPalette(YellowAccentPrimaryDark, YellowAccentSecondaryDark, YellowOnPrimaryDark, Color(0xFFFEF9C3), "Yellow")
        } else {
            AccentPalette(YellowAccentPrimary, YellowAccentSecondary, YellowOnPrimary, Color(0xFF713F12), "Yellow")
        }
        else -> if (isDarkMode) { // Blue default
            AccentPalette(BlueAccentPrimaryDark, BlueAccentSecondaryDark, BlueOnPrimaryDark, Color(0xFFDBEAFE), "Blue")
        } else {
            AccentPalette(BlueAccentPrimary, BlueAccentSecondary, BlueOnPrimary, Color(0xFF1E3A5F), "Blue")
        }
    }

    val primary = accentPalette.primary
    val primaryContainer = accentPalette.secondary
    // Yellow always gets dark text for accessibility
    val onPrimary = accentPalette.onPrimary
    val onPrimaryContainer = accentPalette.onSecondary

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = primaryContainer,
            onSecondaryContainer = onPrimaryContainer,
            background = ElegantDarkBg,
            onBackground = ElegantDarkOnSurface,
            surface = ElegantDarkSurface,
            onSurface = ElegantDarkOnSurface,
            surfaceVariant = ElegantDarkSurfaceVariant,
            onSurfaceVariant = ElegantDarkOnSurfaceVariant,
            outline = ElegantDarkOutline,
            outlineVariant = ElegantDarkOutlineVariant
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = primaryContainer,
            onSecondaryContainer = onPrimaryContainer,
            background = LightGlassBg,
            onBackground = LightGlassOnSurface,
            surface = LightGlassSurface,
            onSurface = LightGlassOnSurface,
            surfaceVariant = LightGlassSurfaceVariant,
            onSurfaceVariant = LightGlassOnSurfaceVariant,
            outline = LightGlassOutline,
            outlineVariant = Color(0xFFE5E7EB)
        )
    }

    CompositionLocalProvider(
        LocalAccentPalette provides accentPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
