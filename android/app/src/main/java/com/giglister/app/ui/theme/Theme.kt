package com.giglister.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightAccentFg,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightSurface,
    onSurface = LightForeground,
    surfaceVariant = LightLine,
    onSurfaceVariant = LightMuted,
    primaryContainer = LightAccent.copy(alpha = .16f), onPrimaryContainer = LightForeground,
    secondaryContainer = LightAccent.copy(alpha = .14f), onSecondaryContainer = LightAccent,
    outline = LightMuted, outlineVariant = LightLine
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkAccentFg,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkSurface,
    onSurface = DarkForeground,
    surfaceVariant = DarkLine,
    onSurfaceVariant = DarkMuted,
    primaryContainer = DarkAccent.copy(alpha = .2f), onPrimaryContainer = DarkForeground,
    secondaryContainer = DarkAccent.copy(alpha = .16f), onSecondaryContainer = DarkAccent,
    outline = DarkMuted, outlineVariant = DarkLine
)

@Composable
fun GigListerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default - Material You's per-device dynamic palette would override the
    // app's own brand colors above, which is exactly what this app doesn't want.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GigTypography,
        content = content
    )
}
