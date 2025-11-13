package com.dedm.batterywidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    surface = BackgroundLight,
    onPrimary = BackgroundLight,
    onSecondary = BackgroundLight,
    onTertiary = BackgroundLight,
    onBackground = PrimaryLight,
    onSurface = PrimaryLight
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = BackgroundDark,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = BackgroundDark,
    onBackground = SecondaryDark,
    onSurface = SecondaryDark
)

@Composable
fun BatteryWidgetTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
