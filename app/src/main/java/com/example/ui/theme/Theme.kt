package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BolpikDarkColorScheme = darkColorScheme(
    primary = BolpikGreen,
    onPrimary = Color(0xFF041F12),
    primaryContainer = BolpikGreenContainer,
    onPrimaryContainer = Color(0xFFB5FFE0),
    secondary = BolpikLime,
    onSecondary = Color(0xFF162103),
    secondaryContainer = BolpikLimeContainer,
    onSecondaryContainer = Color(0xFFE4FFB5),
    tertiary = BolpikTeal,
    onTertiary = Color(0xFF03201D),
    tertiaryContainer = BolpikTealContainer,
    onTertiaryContainer = Color(0xFFB8FFF6),
    background = ObsidianBg,
    onBackground = DarkTextPrimary,
    surface = DarkroomSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkroomCard,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerHigh = DarkroomElevated,
    outline = DarkOutline,
    error = BolpikError,
    onError = Color.White
)

private val BolpikLightColorScheme = lightColorScheme(
    primary = Color(0xFF009E5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4FCE0),
    onPrimaryContainer = Color(0xFF00331C),
    secondary = Color(0xFF4F8A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F9C2),
    onSecondaryContainer = Color(0xFF192E00),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7F5EE),
    onTertiaryContainer = Color(0xFF002B26),
    background = StudioLightBg,
    onBackground = LightTextPrimary,
    surface = StudioLightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = StudioLightCard,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerHigh = StudioLightElevated,
    outline = LightOutline,
    error = BolpikError,
    onError = Color.White
)

val BolpikShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BolpikTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BolpikDarkColorScheme else BolpikLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BolpikTypography,
        shapes = BolpikShapes,
        content = content
    )
}
