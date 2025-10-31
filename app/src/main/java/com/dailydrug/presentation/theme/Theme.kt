package com.dailydrug.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = SecondaryBlueContainer,
    onSecondaryContainer = PrimaryBlueDark,
    tertiary = TertiaryTeal,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryTealContainer,
    onTertiaryContainer = Color(0xFF004D40),
    error = MissedRed,
    onError = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = PrimaryBlueLight,
    onSurfaceVariant = PrimaryBlueDark
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = PrimaryBlueDark,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,
    secondary = SecondaryBlueContainer,
    onSecondary = PrimaryBlueDark,
    secondaryContainer = PrimaryBlueDark,
    onSecondaryContainer = SecondaryBlueContainer,
    tertiary = TertiaryTealContainer,
    onTertiary = Color(0xFF00332B),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = TertiaryTealContainer,
    error = MissedRed,
    onError = Color.Black,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1E2B3A),
    onSurfaceVariant = PrimaryBlueLight
)

@Composable
fun DailyDrugTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
