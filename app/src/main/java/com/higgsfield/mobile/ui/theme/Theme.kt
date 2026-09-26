package com.higgsfield.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.higgsfield.mobile.core.preferences.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F0),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF315DA8),
    secondaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFFFFF9F5),
    surface = Color(0xFFFFF9F5),
    surfaceVariant = Color(0xFFF1E6DF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF12C5DC),
    onPrimary = Color(0xFF002B33),
    primaryContainer = Color(0xFF043F49),
    secondary = Color(0xFFC3A1FF),
    secondaryContainer = Color(0xFF302245),
    background = Color(0xFF0D1316),
    surface = Color(0xFF10181B),
    surfaceVariant = Color(0xFF263237),
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
