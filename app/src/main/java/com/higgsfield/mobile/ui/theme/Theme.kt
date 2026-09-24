package com.higgsfield.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    primary = Color(0xFF80D5D4),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF005050),
    secondary = Color(0xFFAFC6FF),
    secondaryContainer = Color(0xFF17458E),
    background = Color(0xFF171310),
    surface = Color(0xFF171310),
    surfaceVariant = Color(0xFF514640),
)

@Composable
fun HiggsfieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
