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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Complete iOS 26/27-inspired Blue Liquid Glass color tokens.
 */
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

/**
 * Complete design-token system for spacing, corner radii, elevation, blur/refraction,
 * animation durations, and icon sizing.
 */
@Immutable
data class LiquidGlassSpacingTokens(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val screenHorizontal: Dp = 16.dp,
    val bottomNavSafeClearance: Dp = 116.dp
)

@Immutable
data class LiquidGlassRadiusTokens(
    val pill: Dp = 999.dp,
    val modal: Dp = 30.dp,
    val cardHero: Dp = 28.dp,
    val cardLarge: Dp = 26.dp,
    val cardMedium: Dp = 24.dp,
    val cardSmall: Dp = 20.dp,
    val innerSubCard: Dp = 16.dp,
    val iconSquircle: Dp = 14.dp
)

@Immutable
data class LiquidGlassElevationTokens(
    val cardDefault: Dp = 8.dp,
    val cardElevated: Dp = 14.dp,
    val floatingCapsule: Dp = 18.dp,
    val modalSheet: Dp = 24.dp,
    val borderThin: Dp = 1.dp,
    val borderHighlighted: Dp = 1.5.dp
)

@Immutable
data class LiquidGlassAnimationTokens(
    val fastMs: Int = 180,
    val mediumMs: Int = 280,
    val slowMs: Int = 450,
    val progressRingSweepMs: Int = 650
)

@Immutable
data class LiquidGlassIconSizeTokens(
    val xs: Dp = 14.dp,
    val sm: Dp = 18.dp,
    val md: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val hero: Dp = 32.dp,
    val minTouchTarget: Dp = 48.dp
)

private val LightLiquidGlassColors = LiquidGlassColors(
    isDark = false,
    bgPrimaryStart = IceBlueBgTop,          // #EAF5FF Pale blue background
    bgPrimaryMid = IceBlueBgMid,            // #DDF3FF Soft ice blue
    bgPrimaryEnd = IceBlueBgBottom,         // #F3F9FF
    ambientOrbCyan = Color(0x4508C5E8),     // #08C5E8 Cyan accent orb
    ambientOrbBlue = Color(0x3B078BFA),     // #078BFA Primary electric blue orb
    glassSurfacePrimary = Color(0xD6FFFFFF),
    glassSurfaceElevated = Color(0xF0FFFFFF),
    glassSurfaceSecondary = Color(0xC4EAF5FF),
    glassHighlightTop = Color(0xFFFFFFFF),
    glassBorderSubtle = Color(0xB3FFFFFF),
    glassBorderStrong = Color(0xFF9ED0FF),
    accentPrimaryBlue = Color(0xFF078BFA),  // #078BFA Primary electric blue
    accentElectricCyan = Color(0xFF08C5E8), // #08C5E8 Cyan accent
    accentDeepBlue = Color(0xFF0058C4),
    accentSoftGlow = Color(0xFFDDF3FF),     // #DDF3FF Soft ice blue
    textPrimary = NavyInkDark,              // #10243A Deep navy
    textSecondary = NavyInkMuted,           // Muted blue-gray
    textMuted = Color(0xFF7692B2),
    progressTrack = Color(0xFFC8E3FC),
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
    ambientOrbCyan = Color(0x3308C5E8),
    ambientOrbBlue = Color(0x38078BFA),
    glassSurfacePrimary = Color(0xC4102544),
    glassSurfaceElevated = Color(0xE6153056),
    glassSurfaceSecondary = Color(0xB8183662),
    glassHighlightTop = Color(0x7582D1FF),
    glassBorderSubtle = Color(0x4D54A8FF),
    glassBorderStrong = Color(0x945EC8FF),
    accentPrimaryBlue = Color(0xFF339DFF),
    accentElectricCyan = Color(0xFF28D8F8),
    accentDeepBlue = Color(0xFF078BFA),
    accentSoftGlow = Color(0xFF173B69),
    textPrimary = IceWhiteText,
    textSecondary = FrostedBlueMutedText,
    textMuted = Color(0xFF6B8CB8),
    progressTrack = Color(0xFF1D3C69),
    successMint = SuccessMintDark,
    successSoftBg = SuccessContainerDark,
    warningAmber = WarningAmberDark,
    warningSoftBg = WarningContainerDark,
    dangerCoral = ErrorCoralDark,
    dangerSoftBg = ErrorContainerDark
)

val LocalLiquidGlassColors = staticCompositionLocalOf { LightLiquidGlassColors }
val LocalLiquidGlassSpacing = staticCompositionLocalOf { LiquidGlassSpacingTokens() }
val LocalLiquidGlassRadii = staticCompositionLocalOf { LiquidGlassRadiusTokens() }
val LocalLiquidGlassElevation = staticCompositionLocalOf { LiquidGlassElevationTokens() }
val LocalLiquidGlassAnimation = staticCompositionLocalOf { LiquidGlassAnimationTokens() }
val LocalLiquidGlassIconSize = staticCompositionLocalOf { LiquidGlassIconSizeTokens() }

object LiquidGlassTokens {
    val colors: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlassColors.current

    val spacing: LiquidGlassSpacingTokens
        @Composable
        get() = LocalLiquidGlassSpacing.current

    val radii: LiquidGlassRadiusTokens
        @Composable
        get() = LocalLiquidGlassRadii.current

    val elevation: LiquidGlassElevationTokens
        @Composable
        get() = LocalLiquidGlassElevation.current

    val animation: LiquidGlassAnimationTokens
        @Composable
        get() = LocalLiquidGlassAnimation.current

    val iconSize: LiquidGlassIconSizeTokens
        @Composable
        get() = LocalLiquidGlassIconSize.current
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
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
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
