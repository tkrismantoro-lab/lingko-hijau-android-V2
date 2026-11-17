package org.lingkohijau.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Lingko Hijau Brand Design Tokens (Sesuai Tab 4 Design System)
val EmeraldPrimary = Color(0xFF047857)      // emerald-700
val EmeraldDark = Color(0xFF064E3B)         // emerald-900
val EmeraldContainer = Color(0xFFD1FAE5)    // emerald-100
val TealAccent = Color(0xFF0F766E)          // teal-700
val EarthAmber = Color(0xFFD97706)          // amber-600 (Tanah & Panen)
val SurfaceLight = Color(0xFFF8FAFC)        // slate-50
val CardLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF0F172A)    // slate-900
val TextSecondaryLight = Color(0xFF475569)  // slate-600

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = TealAccent,
    onSecondary = Color.White,
    tertiary = EarthAmber,
    background = SurfaceLight,
    surface = CardLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF2DD4BF),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun LingkoHijauTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
