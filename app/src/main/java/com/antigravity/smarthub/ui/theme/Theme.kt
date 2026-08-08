package com.antigravity.smarthub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D8FF),
    onPrimary = Color(0xFF00364C),
    primaryContainer = Color(0xFF004D6B),
    onPrimaryContainer = Color(0xFFBFE9FF),
    secondary = Color(0xFFB2D7DF),
    onSecondary = Color(0xFF1D353C),
    secondaryContainer = Color(0xFF334B53),
    onSecondaryContainer = Color(0xFFCEE8F0),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE1E2E4),
    surfaceVariant = Color(0xFF282C30),
    onSurfaceVariant = Color(0xFFC0C7CD)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00668B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E8FF),
    onPrimaryContainer = Color(0xFF001E2C),
    secondary = Color(0xFF4C626A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F1),
    onSecondaryContainer = Color(0xFF081E26),
    surface = Color(0xFFF6FAFD),
    onSurface = Color(0xFF181C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D)
)

@Composable
fun SmartHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
