package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface
)

private val AmoledColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    secondary = AmoledSecondary,
    tertiary = AmoledTertiary,
    background = AmoledBackground,
    surface = AmoledSurface,
    onPrimary = AmoledOnPrimary,
    onSecondary = AmoledOnSecondary,
    onBackground = AmoledOnBackground,
    onSurface = AmoledOnSurface
)

private val BlueColorScheme = darkColorScheme(
    primary = BlueThemePrimary,
    secondary = BlueThemeSecondary,
    tertiary = BlueThemeTertiary,
    background = BlueThemeBackground,
    surface = BlueThemeSurface,
    onPrimary = BlueThemeOnPrimary,
    onSecondary = BlueThemeOnSecondary,
    onBackground = BlueThemeOnBackground,
    onSurface = BlueThemeOnSurface
)

private val GreenColorScheme = darkColorScheme(
    primary = GreenThemePrimary,
    secondary = GreenThemeSecondary,
    tertiary = GreenThemeTertiary,
    background = GreenThemeBackground,
    surface = GreenThemeSurface,
    onPrimary = GreenThemeOnPrimary,
    onSecondary = GreenThemeOnSecondary,
    onBackground = GreenThemeOnBackground,
    onSurface = GreenThemeOnSurface
)

@Composable
fun VaultFlowTheme(
    themeMode: String = "System",
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    
    val colorScheme = when (themeMode) {
        "Light" -> LightColorScheme
        "Dark" -> DarkColorScheme
        "AMOLED" -> AmoledColorScheme
        "Blue" -> BlueColorScheme
        "Green" -> GreenColorScheme
        else -> {
            // "System" mode
            if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemInDark) DarkColorScheme else LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
