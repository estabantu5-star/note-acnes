package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LiquidGlassColors(
    val isDark: Boolean,
    val bgPrimaryStart: Color,
    val bgPrimaryMid: Color,
    val bgPrimaryEnd: Color,
    val ambientOrbCyan: Color,
    val ambientOrbBlue: Color,
    val glassSurfacePrimary: Color,
    val glassSurfaceElevated: Color,
    val glassSurfaceSecondary: Color,
    val glassHighlightTop: Color,
    val glassBorderSubtle: Color,
    val glassBorderStrong: Color,
    val accentPrimaryBlue: Color,
    val accentElectricCyan: Color,
    val accentDeepBlue: Color,
    val accentSoftGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val progressTrack: Color,
    val successMint: Color,
    val successSoftBg: Color,
    val warningAmber: Color,
    val warningSoftBg: Color,
    val dangerCoral: Color,
    val dangerSoftBg: Color
)

private val LightLiquidGlassColors = LiquidGlassColors(
    isDark = false,
    bgPrimaryStart = IceBlueBgTop,
    bgPrimaryMid = IceBlueBgMid,
    bgPrimaryEnd = IceBlueBgBottom,
    ambientOrbCyan = Color(0x4450D8FF),
    ambientOrbBlue = Color(0x383A9BFF),
    glassSurfacePrimary = Color(0xCCFFFFFF),
    glassSurfaceElevated = Color(0xEBFFFFFF),
    glassSurfaceSecondary = Color(0xB8EAF5FF),
    glassHighlightTop = Color(0xFFFFFFFF),
    glassBorderSubtle = Color(0x99FFFFFF),
    glassBorderStrong = Color(0xFFAAD6FF),
    accentPrimaryBlue = Color(0xFF0A84FF),
    accentElectricCyan = Color(0xFF00C7FF),
    accentDeepBlue = Color(0xFF0055D4),
    accentSoftGlow = Color(0xFFDDF1FF),
    textPrimary = NavyInkDark,
    textSecondary = NavyInkMuted,
    textMuted = Color(0xFF7B96B8),
    progressTrack = Color(0xFFCBE3FC),
    successMint = SuccessMintLight,
    successSoftBg = SuccessContainerLight,
    warningAmber = WarningAmberLight,
    warningSoftBg = WarningContainerLight,
    dangerCoral = ErrorCoralLight,
    dangerSoftBg = ErrorContainerLight
)

private val DarkLiquidGlassColors = LiquidGlassColors(
    isDark = true,
    bgPrimaryStart = MidnightNavyBgTop,
    bgPrimaryMid = MidnightNavyBgMid,
    bgPrimaryEnd = MidnightNavyBgBottom,
    ambientOrbCyan = Color(0x3000D9FF),
    ambientOrbBlue = Color(0x330077FF),
    glassSurfacePrimary = Color(0xBF102544),
    glassSurfaceElevated = Color(0xDF163159),
    glassSurfaceSecondary = Color(0xB3193866),
    glassHighlightTop = Color(0x667CCBFF),
    glassBorderSubtle = Color(0x4555A8FF),
    glassBorderStrong = Color(0x8862C9FF),
    accentPrimaryBlue = Color(0xFF339DFF),
    accentElectricCyan = Color(0xFF33E1FF),
    accentDeepBlue = Color(0xFF0A78E6),
    accentSoftGlow = Color(0xFF193E6E),
    textPrimary = IceWhiteText,
    textSecondary = FrostedBlueMutedText,
    textMuted = Color(0xFF6B8CB8),
    progressTrack = Color(0xFF1E3D6B),
    successMint = SuccessMintDark,
    successSoftBg = SuccessContainerDark,
    warningAmber = WarningAmberDark,
    warningSoftBg = WarningContainerDark,
    dangerCoral = ErrorCoralDark,
    dangerSoftBg = ErrorContainerDark
)

val LocalLiquidGlassColors = staticCompositionLocalOf { LightLiquidGlassColors }

object LiquidGlassTokens {
    val colors: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlassColors.current
}

object LiquidGlassTheme {
    val colors: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlassColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBluePrimaryDark,
    onPrimary = ElectricBlueOnPrimaryDark,
    primaryContainer = ElectricBlueContainerDark,
    onPrimaryContainer = ElectricBlueOnContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = CyanOnSecondaryDark,
    secondaryContainer = CyanSecondaryContainerDark,
    onSecondaryContainer = CyanOnSecondaryContainerDark,
    tertiary = IndigoTertiaryDark,
    onTertiary = IndigoOnTertiaryDark,
    tertiaryContainer = IndigoTertiaryContainerDark,
    onTertiaryContainer = IndigoOnTertiaryContainerDark,
    background = MidnightNavyBgTop,
    onBackground = IceWhiteText,
    surface = DarkGlassSurface,
    onSurface = IceWhiteText,
    surfaceVariant = DarkGlassSurfaceVariant,
    onSurfaceVariant = FrostedBlueMutedText,
    outline = DarkGlassOutline,
    outlineVariant = DarkGlassOutlineVariant,
    error = ErrorCoralDark,
    onError = Color(0xFF3B0608),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBluePrimary,
    onPrimary = ElectricBlueOnPrimary,
    primaryContainer = ElectricBlueContainer,
    onPrimaryContainer = ElectricBlueOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanSecondaryContainer,
    onSecondaryContainer = CyanOnSecondaryContainer,
    tertiary = IndigoTertiary,
    onTertiary = IndigoOnTertiary,
    tertiaryContainer = IndigoTertiaryContainer,
    onTertiaryContainer = IndigoOnTertiaryContainer,
    background = IceBlueBgTop,
    onBackground = NavyInkDark,
    surface = IceSurface,
    onSurface = NavyInkDark,
    surfaceVariant = IceSurfaceVariant,
    onSurfaceVariant = NavyInkMuted,
    outline = IceOutline,
    outlineVariant = IceOutlineVariant,
    error = ErrorCoralLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val glassColors = if (darkTheme) DarkLiquidGlassColors else LightLiquidGlassColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLiquidGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
