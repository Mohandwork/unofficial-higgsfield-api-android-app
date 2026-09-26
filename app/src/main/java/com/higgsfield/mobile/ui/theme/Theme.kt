package com.higgsfield.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.higgsfield.mobile.core.preferences.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF121212),
    onPrimary = Color(0xFFC0FF00),
    primaryContainer = Color(0xFFC0FF00),
    onPrimaryContainer = Color(0xFF101010),
    secondary = Color(0xFF00838F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF00363B),
    tertiary = Color(0xFF101010),
    tertiaryContainer = Color(0xFFF0F2F5),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF0A0A0C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0C),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFF64748B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC0FF00),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF314300),
    onPrimaryContainer = Color(0xFFC0FF00),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF003B44),
    onSecondaryContainer = Color(0xFF00E5FF),
    tertiary = Color(0xFF00E5FF),
    tertiaryContainer = Color(0xFF1A1D24),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1D24),
    onSurfaceVariant = Color(0xFF8A8F9E),
    outline = Color(0xFF8A8F9E),
)

@Composable
fun HiggsfieldTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
