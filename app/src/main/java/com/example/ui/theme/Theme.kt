package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val VerdantLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = SlateOnContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = TealOnTertiaryLight,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = TealOnContainerLight,
    background = AlabasterBackground,
    onBackground = SlateInk,
    surface = CrispWhiteSurface,
    onSurface = SlateInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant,
    error = RoseErrorLight,
    onError = EmeraldOnPrimaryLight,
    errorContainer = RoseErrorContainerLight,
    onErrorContainer = RoseOnErrorContainerLight
)

private val VerdantDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = SlateOnContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = TealOnContainerDark,
    background = ObsidianBackground,
    onBackground = CrispOffWhiteInk,
    surface = ObsidianSurface,
    onSurface = CrispOffWhiteInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMutedDark,
    outline = SlateOutlineDark,
    outlineVariant = SlateOutlineVariantDark,
    error = RoseErrorDark,
    onError = EmeraldOnPrimaryDark,
    errorContainer = RoseErrorContainerDark,
    onErrorContainer = RoseOnErrorContainerDark
)

val VerdantShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object VerdantThemeTokens {
    val colors: VerdantExtendedColors
        @Composable
        get() = LocalVerdantColors.current
}

@Composable
fun VerdantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VerdantDarkColorScheme else VerdantLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalVerdantColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = VerdantShapes,
            content = content
        )
    }
}
