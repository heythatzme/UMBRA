package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NoirColorScheme = darkColorScheme(
    primary = NoirAccentWhite,
    onPrimary = NoirPitchBlack,
    primaryContainer = NoirSurfaceElevated,
    onPrimaryContainer = NoirTextPrimary,
    secondary = NoirAccentSilver,
    onSecondary = NoirPitchBlack,
    secondaryContainer = NoirSurfaceHighlight,
    onSecondaryContainer = NoirTextPrimary,
    tertiary = NoirAccentMuted,
    onTertiary = NoirPitchBlack,
    background = NoirPitchBlack,
    onBackground = NoirTextPrimary,
    surface = NoirDark,
    onSurface = NoirTextPrimary,
    surfaceVariant = NoirSurface,
    onSurfaceVariant = NoirTextSecondary,
    outline = NoirBorder,
    outlineVariant = NoirBorderLight
)

@Composable
fun NoirTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NoirColorScheme,
        typography = Typography,
        content = content
    )
}
