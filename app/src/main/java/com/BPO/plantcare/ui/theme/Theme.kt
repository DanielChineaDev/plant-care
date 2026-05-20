package com.BPO.plantcare.ui.theme

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
    primary = LeafGreen,
    onPrimary = OnLeafGreen,
    primaryContainer = LeafGreenContainer,
    onPrimaryContainer = OnLeafGreenContainer,
    secondary = EarthBrown,
    onSecondary = OnEarthBrown,
    secondaryContainer = EarthBrownContainer,
    onSecondaryContainer = OnEarthBrownContainer,
    tertiary = SunYellow,
    onTertiary = OnSunYellow,
    tertiaryContainer = SunYellowContainer,
    onTertiaryContainer = OnSunYellowContainer,
    background = LightBackground,
    onBackground = OnLightBackground,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreenDark,
    onPrimary = OnLeafGreenDark,
    primaryContainer = LeafGreenContainerDark,
    onPrimaryContainer = OnLeafGreenContainerDark,
    secondary = EarthBrownDark,
    onSecondary = OnEarthBrownDark,
    secondaryContainer = EarthBrownContainerDark,
    onSecondaryContainer = OnEarthBrownContainerDark,
    tertiary = SunYellowDark,
    onTertiary = OnSunYellowDark,
    tertiaryContainer = SunYellowContainerDark,
    onTertiaryContainer = OnSunYellowContainerDark,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

@Composable
fun PlantCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.Green,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> appColorScheme(palette, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Construye el esquema de color final aplicando la familia "primary" de la
 * paleta elegida sobre el esquema base (que aporta los neutros/superficies).
 */
private fun appColorScheme(palette: AppPalette, dark: Boolean) =
    if (dark) {
        val c = palette.dark
        DarkColorScheme.copy(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
        )
    } else {
        val c = palette.light
        LightColorScheme.copy(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
        )
    }
