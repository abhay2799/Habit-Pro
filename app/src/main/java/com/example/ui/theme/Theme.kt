package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Mode (Glassmorphism base colors)
private val LightBg = Color(0xFFF0F4F8) // Very soft off-white gradient base
private val LightSurface = Color(0xB3FFFFFF) // 70% transparent white for true glassmorphism
private val LightSurfaceVariant = Color(0x99FFFFFF) // Slightly more opaque for cards
private val LightOnBg = Color(0xFF1F2937)
private val LightOnSurface = Color(0xFF111827)
private val LightOnSurfaceVariant = Color(0xFF4B5563)
private val LightOutline = Color(0xFFD1D5DB)

// Accent Colors (60-30-10 Rule)
private val BlueAccent = Color(0xFF3B82F6)
private val BlueAccentContainer = Color(0xFFDBEAFE)
private val BlueAccentDark = Color(0xFF60A5FA)
private val BlueAccentContainerDark = Color(0xFF1E3A8A)

private val GreenAccent = Color(0xFF10B981)
private val GreenAccentContainer = Color(0xFFD1FAE5)
private val GreenAccentDark = Color(0xFF34D399)
private val GreenAccentContainerDark = Color(0xFF064E3B)

private val RedAccent = Color(0xFFEF4444)
private val RedAccentContainer = Color(0xFFFEE2E2)
private val RedAccentDark = Color(0xFFF87171)
private val RedAccentContainerDark = Color(0xFF7F1D1D)

private val YellowAccent = Color(0xFFEDE655) // rgb(237, 230, 85)
private val YellowAccentContainer = Color(0xFFFEF3C7)
private val YellowAccentDark = Color(0xFFEDE655) // Same bright yellow for visibility
private val YellowAccentContainerDark = Color(0xFF78350F)

@Composable
fun MyApplicationTheme(
    isDarkMode: Boolean = false,
    accentColor: String = "Blue",
    content: @Composable () -> Unit
) {
    val (primary, primaryContainer) = when (accentColor) {
        "Green" -> if (isDarkMode) Pair(GreenAccentDark, GreenAccentContainerDark) else Pair(GreenAccent, GreenAccentContainer)
        "Red" -> if (isDarkMode) Pair(RedAccentDark, RedAccentContainerDark) else Pair(RedAccent, RedAccentContainer)
        "Yellow" -> if (isDarkMode) Pair(YellowAccentDark, YellowAccentContainerDark) else Pair(YellowAccent, YellowAccentContainer)
        else -> if (isDarkMode) Pair(BlueAccentDark, BlueAccentContainerDark) else Pair(BlueAccent, BlueAccentContainer) // Blue Default
    }

    val onPrimary = if (accentColor == "Yellow") Color(0xFF000000) else if (isDarkMode) Color(0xFF000000) else Color(0xFFFFFFFF)
    val onPrimaryContainer = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF000000)

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            background = ElegantDarkBg,
            onBackground = ElegantDarkOnSurface,
            surface = ElegantDarkSurface,
            onSurface = ElegantDarkOnSurface,
            surfaceVariant = ElegantDarkSurfaceVariant,
            onSurfaceVariant = ElegantDarkOnSurfaceVariant,
            outline = ElegantDarkOutline
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            background = LightBg,
            onBackground = LightOnBg,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
