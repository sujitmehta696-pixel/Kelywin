package com.example.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KelywinLightColorScheme = lightColorScheme(
    primary = KelyCoralPrimary,
    onPrimary = Color.White,
    primaryContainer = KelyCoralSoft,
    onPrimaryContainer = KelyCoralDark,
    secondary = KelyGreen,
    onSecondary = Color.White,
    secondaryContainer = KelyGreenSoft,
    onSecondaryContainer = KelyGreen,
    tertiary = KelyDragon,
    onTertiary = Color.White,
    tertiaryContainer = KelyDragonSoft,
    onTertiaryContainer = KelyDragonDark,
    background = KelyBackground,
    onBackground = KelyInkPrimary,
    surface = KelySurface,
    onSurface = KelyInkPrimary,
    surfaceVariant = KelySurfaceMuted,
    onSurfaceVariant = KelyInkSecondary,
    outline = KelyBorder,
    error = KelyError,
    onError = Color.White
)

private val KelywinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KelywinTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KelywinLightColorScheme,
        typography = KelywinTypography,
        shapes = KelywinShapes,
        content = content
    )
}
