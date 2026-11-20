package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = OnTealPrimary,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = SageSecondary,
    onSecondary = OnSageSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = CoralTertiary,
    onTertiary = OnCoralTertiary,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = OnCoralContainer,
    background = WarmAlabaster,
    onBackground = InkCharcoal,
    surface = WarmAlabaster,
    onSurface = InkCharcoal,
    surfaceVariant = SoftCreamSurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PureCardSurface,
    surfaceContainerLow = ColorTokens.surfaceContainerLowLight,
    surfaceContainer = SoftCreamSurface,
    surfaceContainerHigh = ColorTokens.surfaceContainerHighLight,
    outline = HairlineOutline,
    error = ErrorCrimson,
    errorContainer = ErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = OnTealPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = CoralTertiaryDark,
    onTertiary = OnCoralTertiaryDark,
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = OnCoralContainerDark,
    background = DeepSlateBackground,
    onBackground = IvoryInk,
    surface = DeepSlateBackground,
    onSurface = IvoryInk,
    surfaceVariant = DeepSlateSurface,
    onSurfaceVariant = SoftMistText,
    surfaceContainerLowest = DeepSlateSurface,
    surfaceContainerLow = DeepSlateSurface,
    surfaceContainer = DeepSlateElevated,
    surfaceContainerHigh = ColorTokens.surfaceContainerHighDark,
    outline = DarkOutline,
)

private object ColorTokens {
    val surfaceContainerLowLight = androidx.compose.ui.graphics.Color(0xFFF7F4EF)
    val surfaceContainerHighLight = androidx.compose.ui.graphics.Color(0xFFECE6DF)
    val surfaceContainerHighDark = androidx.compose.ui.graphics.Color(0xFF283636)
}

val MedCueShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MedCueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MedCueTypography,
        shapes = MedCueShapes,
        content = content,
    )
}
