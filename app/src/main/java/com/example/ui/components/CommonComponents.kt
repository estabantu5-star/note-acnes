package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.ProductCategory
import com.example.domain.RoutineScheduleHelper
import com.example.ui.StepTimerState
import com.example.ui.TaskItemUiModel
import com.example.ui.theme.LiquidGlassTokens
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// 1. iOS 26/27 LIQUID GLASS DESIGN PRIMITIVES (ANDROID COMPOSE NATIVE)
// ============================================================================

/**
 * Full-screen iOS 26/27 spatial ambient background with pale blue (#EAF5FF) to
 * soft ice blue (#DDF3FF) gradient, electric cyan (#08C5E8) & electric blue (#078BFA)
 * luminous light orbs, and a diagonal optical glass sheen.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glass.bgPrimaryStart,
                        glass.bgPrimaryMid,
                        glass.bgPrimaryEnd
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right Electric Cyan (#08C5E8) refraction orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.ambientOrbCyan,
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.10f),
                    radius = w * 0.72f
                ),
                center = Offset(w * 0.88f, h * 0.10f),
                radius = w * 0.72f
            )

            // Mid-left Primary Electric Blue (#078BFA) refraction orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.ambientOrbBlue,
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * 0.45f),
                    radius = w * 0.76f
                ),
                center = Offset(w * 0.12f, h * 0.45f),
                radius = w * 0.76f
            )

            // Bottom-right soft crystalline cyan bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.ambientOrbCyan.copy(alpha = glass.ambientOrbCyan.alpha * 0.75f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.82f, h * 0.88f),
                    radius = w * 0.62f
                ),
                center = Offset(w * 0.82f, h * 0.88f),
                radius = w * 0.62f
            )

            // Subtle diagonal top-left optical glass reflection wash
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glass.glassHighlightTop.copy(alpha = if (glass.isDark) 0.05f else 0.35f),
                        Color.Transparent,
                        glass.accentElectricCyan.copy(alpha = if (glass.isDark) 0.03f else 0.06f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h * 0.65f)
                )
            )
        }
        content()
    }
}

/**
 * Adaptive iOS 26/27 Frosted Liquid Glass Card with:
 * - Tinted ambient & spot elevation shadow
 * - Multi-stop translucent frosted fill
 * - Top-edge specular light reflection bar
 * - Diagonal refractive glass border
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LiquidGlassTokens.radii.cardMedium,
    isElevated: Boolean = false,
    isHighlighted: Boolean = false,
    tintColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val shape = RoundedCornerShape(cornerRadius)
    val baseSurface = when {
        tintColor != null -> tintColor
        isElevated -> glass.glassSurfaceElevated
        else -> glass.glassSurfacePrimary
    }
    val borderBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                glass.accentElectricCyan.copy(alpha = 0.92f),
                glass.glassHighlightTop,
                glass.accentPrimaryBlue.copy(alpha = 0.65f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                glass.glassHighlightTop,
                glass.glassBorderSubtle,
                glass.glassBorderStrong.copy(alpha = 0.52f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isElevated || isHighlighted) {
                    LiquidGlassTokens.elevation.cardElevated
                } else {
                    LiquidGlassTokens.elevation.cardDefault
                },
                shape = shape,
                ambientColor = glass.accentDeepBlue.copy(alpha = 0.14f),
                spotColor = glass.accentPrimaryBlue.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(baseSurface)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glass.glassHighlightTop.copy(alpha = if (glass.isDark) 0.14f else 0.55f),
                        Color.Transparent,
                        glass.accentSoftGlow.copy(alpha = if (glass.isDark) 0.08f else 0.22f)
                    )
                )
            )
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        // Subtle top-edge specular refraction line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            glass.glassHighlightTop.copy(alpha = if (glass.isDark) 0.55f else 0.90f),
                            glass.accentElectricCyan.copy(alpha = if (isHighlighted) 0.70f else 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Floating circular iOS 26/27 Liquid Glass icon button (minimum 48.dp touch target)
 * with frosted glass fill, specular top highlight, and electric blue/cyan icon tint.
 */
@Composable
fun LiquidCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LiquidGlassTokens.colors.accentPrimaryBlue,
    isHighlighted: Boolean = false
) {
    val glass = LiquidGlassTokens.colors
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = glass.accentDeepBlue.copy(alpha = 0.12f),
                spotColor = glass.accentPrimaryBlue.copy(alpha = 0.18f)
            )
            .clip(CircleShape)
            .background(
                brush = if (isHighlighted) {
                    Brush.linearGradient(
                        listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(glass.glassHighlightTop.copy(alpha = if (glass.isDark) 0.22f else 0.85f), glass.glassSurfaceElevated)
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glass.glassHighlightTop,
                        glass.glassBorderStrong
                    )
                ),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isHighlighted) Color.White else tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Reusable iOS 26/27 Frosted Liquid Glass Modal Dialog replacing generic flat AlertDialogs.
 */
@Composable
fun LiquidGlassModalDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    val glass = LiquidGlassTokens.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 18.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassSurface(
                modifier = modifier.fillMaxWidth(),
                cornerRadius = 30.dp,
                isElevated = true,
                isHighlighted = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = content
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}

/**
 * Circular iOS 26/27 Liquid Glass Progress Ring with frosted inner optical plate,
 * subtle radial dial ticks, outer cyan/blue glow halo, and animated gradient sweep.
 */
@Composable
fun LiquidProgressRing(
    percentage: Int,
    modifier: Modifier = Modifier,
    size: Dp = 108.dp,
    strokeWidth: Dp = 11.dp,
    subtitle: String? = null
) {
    val glass = LiquidGlassTokens.colors
    val clamped = percentage.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(
            durationMillis = LiquidGlassTokens.animation.progressRingSweepMs,
            easing = FastOutSlowInEasing
        ),
        label = "liquidRingProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val centerPt = Offset(this.size.width / 2f, this.size.height / 2f)
            val innerPlateRadius = (this.size.minDimension / 2f) - strokePx - 2.dp.toPx()

            // Frosted inner optical circle plate
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.glassSurfaceElevated,
                        glass.glassSurfaceSecondary
                    ),
                    center = centerPt,
                    radius = innerPlateRadius.coerceAtLeast(1f)
                ),
                radius = innerPlateRadius.coerceAtLeast(1f)
            )

            // Subtle dial tick markers inside the ring
            val tickRadiusOuter = innerPlateRadius - 3.dp.toPx()
            val tickRadiusInner = innerPlateRadius - 6.dp.toPx()
            if (tickRadiusInner > 4f) {
                for (i in 0 until 12) {
                    val angleRad = Math.toRadians((i * 30 - 90).toDouble())
                    val startX = centerPt.x + (tickRadiusInner * cos(angleRad)).toFloat()
                    val startY = centerPt.y + (tickRadiusInner * sin(angleRad)).toFloat()
                    val endX = centerPt.x + (tickRadiusOuter * cos(angleRad)).toFloat()
                    val endY = centerPt.y + (tickRadiusOuter * sin(angleRad)).toFloat()
                    drawLine(
                        color = glass.accentPrimaryBlue.copy(alpha = 0.22f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Track ring
            drawArc(
                color = glass.progressTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Soft outer glow arc + main active gradient arc
            if (animatedProgress > 0.01f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            glass.accentPrimaryBlue.copy(alpha = 0.35f),
                            glass.accentElectricCyan.copy(alpha = 0.50f),
                            glass.accentPrimaryBlue.copy(alpha = 0.35f)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glass.accentPrimaryBlue,
                            glass.accentElectricCyan
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = glass.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glass.textSecondary
                )
            }
        }
    }
}

/**
 * iOS 26/27 Liquid Segmented Control with frosted glass capsule and glowing
 * electric blue/cyan active pill with specular border.
 */
@Composable
fun LiquidSegmentedBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    optionTestTags: List<String>? = null
) {
    val glass = LiquidGlassTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = glass.accentDeepBlue.copy(alpha = 0.10f),
                spotColor = glass.accentPrimaryBlue.copy(alpha = 0.14f)
            )
            .clip(CircleShape)
            .background(glass.glassSurfaceSecondary)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(glass.glassHighlightTop, glass.glassBorderStrong)
                ),
                shape = CircleShape
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val animatedTextColor by animateColorAsState(
                targetValue = if (selected) Color.White else glass.textSecondary,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "segTextColor"
            )
            val tagMod = if (optionTestTags != null && index in optionTestTags.indices) {
                Modifier.testTag(optionTestTags[index])
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(tagMod)
                    .clip(CircleShape)
                    .background(
                        brush = if (selected) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    glass.accentPrimaryBlue,
                                    glass.accentElectricCyan
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
                    .then(
                        if (selected) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = animatedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Frosted glass pill badge for category, schedule, or status tags.
 */
@Composable
fun LiquidGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LiquidGlassTokens.colors.accentSoftGlow,
    contentColor: Color = LiquidGlassTokens.colors.accentPrimaryBlue,
    borderColor: Color = LiquidGlassTokens.colors.glassBorderStrong,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

/**
 * Primary glowing gradient action pill button (#078BFA -> #08C5E8) with top specular border.
 */
@Composable
fun LiquidPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    val glass = LiquidGlassTokens.colors
    val shape = CircleShape
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = glass.accentPrimaryBlue.copy(alpha = 0.28f),
                spotColor = glass.accentElectricCyan.copy(alpha = 0.38f)
            )
            .clip(shape)
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            glass.accentPrimaryBlue,
                            glass.accentElectricCyan
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            glass.textMuted.copy(alpha = 0.3f),
                            glass.textMuted.copy(alpha = 0.3f)
                        )
                    )
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Secondary translucent glass pill button with refractive border.
 */
@Composable
fun LiquidSecondaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    contentColor: Color = LiquidGlassTokens.colors.accentPrimaryBlue
) {
    val glass = LiquidGlassTokens.colors
    val shape = CircleShape
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = glass.accentDeepBlue.copy(alpha = 0.08f),
                spotColor = glass.accentPrimaryBlue.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        glass.glassHighlightTop.copy(alpha = if (glass.isDark) 0.16f else 0.75f),
                        glass.glassSurfaceElevated
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(glass.glassHighlightTop, glass.glassBorderStrong)
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3D-inspired translucent product bottle / tube squircle badge with inner specular arc.
 */
@Composable
fun ProductBottleGlyph(
    productCategory: String,
    isActiveHighlight: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val glass = LiquidGlassTokens.colors
    val cat = ProductCategory.fromId(productCategory)
    val icon = when (cat) {
        ProductCategory.PRESCRIPTION_TOPICAL -> Icons.Outlined.MedicalServices
        ProductCategory.SUNSCREEN -> Icons.Outlined.WbSunny
        ProductCategory.PRESCRIPTION_ORAL -> Icons.Outlined.MedicalServices
        ProductCategory.SUPPORTIVE_SKINCARE -> Icons.Outlined.Spa
    }
    val tint = when {
        isActiveHighlight -> Color.White
        cat == ProductCategory.PRESCRIPTION_TOPICAL -> glass.accentPrimaryBlue
        cat == ProductCategory.SUNSCREEN -> glass.accentElectricCyan
        else -> glass.accentPrimaryBlue
    }
    val squircleShape = RoundedCornerShape(size * 0.32f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isActiveHighlight) 8.dp else 4.dp,
                shape = squircleShape,
                ambientColor = glass.accentPrimaryBlue.copy(alpha = 0.22f),
                spotColor = glass.accentElectricCyan.copy(alpha = 0.28f)
            )
            .clip(squircleShape)
            .background(
                brush = if (isActiveHighlight) {
                    Brush.linearGradient(
                        colors = listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            glass.accentSoftGlow,
                            glass.glassSurfaceElevated
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isActiveHighlight) {
                        listOf(Color.White.copy(alpha = 0.8f), glass.accentElectricCyan)
                    } else {
                        listOf(glass.glassHighlightTop, glass.glassBorderStrong)
                    }
                ),
                shape = squircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner specular glass highlight arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isActiveHighlight) 0.28f else 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width * 0.28f, this.size.height * 0.22f),
                    radius = this.size.width * 0.55f
                ),
                center = Offset(this.size.width * 0.28f, this.size.height * 0.22f),
                radius = this.size.width * 0.55f
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// ============================================================================
// 2. NON-BLOCKING 3-MINUTE STEP INTERVAL TIMER CAPSULE + FULL-SCREEN MODAL
// ============================================================================

@Composable
fun NonBlockingStepTimerBar(
    timerState: StepTimerState,
    onTogglePauseResume: () -> Unit,
    onResetAndDismiss: () -> Unit,
    onRestartThreeMinutes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = timerState.isActiveOrPaused
    var showFullScreenModal by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val glass = LiquidGlassTokens.colors
        val total = timerState.totalSeconds.coerceAtLeast(1)
        val progress = (total - timerState.remainingSeconds).toFloat() / total.toFloat()

        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { showFullScreenModal = true }
                .testTag("step_timer_bar"),
            cornerRadius = 26.dp,
            isElevated = true,
            isHighlighted = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Timer Jeda Langkah",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = timerState.contextLabel.ifBlank { "Jeda Antar Langkah (~3 menit)" },
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = timerState.formattedMmSs,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = glass.accentPrimaryBlue,
                                modifier = Modifier.testTag("step_timer_countdown_text")
                            )
                            LiquidGlassPill(
                                text = if (timerState.isRunning) "Berjalan" else "Dijeda",
                                containerColor = glass.accentSoftGlow,
                                contentColor = glass.accentPrimaryBlue
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFullScreenModal = true },
                        modifier = Modifier.testTag("step_timer_expand_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInFull,
                            contentDescription = "Perbesar Timer",
                            tint = glass.accentPrimaryBlue
                        )
                    }
                    IconButton(
                        onClick = onTogglePauseResume,
                        modifier = Modifier.testTag("step_timer_pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (timerState.isRunning) "Jeda Timer" else "Lanjutkan Timer",
                            tint = glass.accentPrimaryBlue
                        )
                    }
                    IconButton(
                        onClick = onRestartThreeMinutes,
                        modifier = Modifier.testTag("step_timer_restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Ulangi 3 Menit",
                            tint = glass.accentPrimaryBlue
                        )
                    }
                    IconButton(
                        onClick = {
                            showFullScreenModal = false
                            onResetAndDismiss()
                        },
                        modifier = Modifier.testTag("step_timer_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup Timer",
                            tint = glass.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = glass.accentElectricCyan,
                trackColor = glass.progressTrack
            )
        }
    }

    if (showFullScreenModal && isVisible) {
        LiquidStepTimerModalDialog(
            timerState = timerState,
            onTogglePauseResume = onTogglePauseResume,
            onRestartThreeMinutes = onRestartThreeMinutes,
            onResetAndDismiss = {
                showFullScreenModal = false
                onResetAndDismiss()
            },
            onMinimize = { showFullScreenModal = false }
        )
    }
}

@Composable
private fun LiquidStepTimerModalDialog(
    timerState: StepTimerState,
    onTogglePauseResume: () -> Unit,
    onRestartThreeMinutes: () -> Unit,
    onResetAndDismiss: () -> Unit,
    onMinimize: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val total = timerState.totalSeconds.coerceAtLeast(1)
    val fractionRemaining = (timerState.remainingSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = fractionRemaining,
        animationSpec = tween(durationMillis = 350),
        label = "timerRingSweep"
    )

    Dialog(
        onDismissRequest = onMinimize,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidCircleIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onMinimize,
                        tint = glass.textPrimary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Timer Jeda Langkah",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Biarkan produk meresap sebelum langkah berikutnya",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    LiquidCircleIconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = "Batalkan Timer",
                        onClick = onResetAndDismiss,
                        tint = glass.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Step Pill Context Card
                LiquidGlassSurface(
                    cornerRadius = 24.dp,
                    isElevated = true,
                    isHighlighted = true,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProductBottleGlyph(
                            productCategory = ProductCategory.SUPPORTIVE_SKINCARE.id,
                            isActiveHighlight = true,
                            size = 46.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SEDANG MERESAP • JEDA RESEP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentPrimaryBlue
                            )
                            Text(
                                text = timerState.contextLabel.ifBlank { "Jeda Antar Langkah (~3 menit)" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Central Circular Liquid Countdown Ring
                Box(
                    modifier = Modifier.size(256.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 16.dp.toPx()
                        val inset = strokePx / 2f + 10.dp.toPx()
                        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                        val topLeft = Offset(inset, inset)

                        drawCircle(
                            color = glass.glassSurfacePrimary,
                            radius = (this.size.minDimension / 2f) - strokePx
                        )

                        drawArc(
                            color = glass.progressTrack,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    glass.accentPrimaryBlue.copy(alpha = 0.35f),
                                    glass.accentElectricCyan.copy(alpha = 0.55f),
                                    glass.accentPrimaryBlue.copy(alpha = 0.35f)
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx + 8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawArc(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glass.accentPrimaryBlue,
                                    glass.accentElectricCyan
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timerState.formattedMmSs,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LiquidGlassPill(
                            text = if (timerState.isRunning) "Waktu Tersisa" else "Timer Dijeda",
                            containerColor = glass.accentSoftGlow,
                            contentColor = glass.accentPrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer Control Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidPrimaryActionButton(
                            text = if (timerState.isRunning) "Jeda Timer" else "Lanjutkan",
                            leadingIcon = if (timerState.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            onClick = onTogglePauseResume,
                            modifier = Modifier.weight(1f)
                        )
                        LiquidSecondaryGlassButton(
                            text = "Ulangi 3 Menit",
                            leadingIcon = Icons.Outlined.Refresh,
                            onClick = onRestartThreeMinutes,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LiquidSecondaryGlassButton(
                        text = "Selesai & Lanjut Langkah Berikutnya",
                        leadingIcon = Icons.Outlined.Check,
                        onClick = onResetAndDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. STEP DETAIL SHEET / DIALOG (DETAIL LANGKAH — LIQUID GLASS MODAL)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailDialog(
    item: TaskItemUiModel,
    onDismiss: () -> Unit,
    onStartTimer: (() -> Unit)? = null,
    onToggleComplete: (() -> Unit)? = null
) {
    val glass = LiquidGlassTokens.colors
    val task = item.task
    val categoryEnum = item.resolvedCategory
    val scheduleText = item.scheduleBadge ?: RoutineScheduleHelper.formatScheduledDaysLabel(
        scheduledDaysCsv = task.scheduledDaysCsv,
        period = task.period
    )
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_detail_dialog"),
                cornerRadius = 30.dp,
                isElevated = true,
                isHighlighted = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Bar: Back + Title + Category Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(glass.glassSurfaceSecondary)
                                .border(1.dp, glass.glassBorderStrong, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Tutup",
                                tint = glass.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Detail Langkah",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                        LiquidGlassPill(
                            text = categoryEnum.labelIndonesian,
                            containerColor = glass.accentSoftGlow,
                            contentColor = glass.accentPrimaryBlue
                        )
                    }

                    // Translucent Product Hero Showcase Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        glass.accentSoftGlow,
                                        glass.glassSurfaceSecondary
                                    )
                                )
                            )
                            .border(1.dp, glass.glassBorderStrong, RoundedCornerShape(22.dp))
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProductBottleGlyph(
                                productCategory = task.category,
                                isActiveHighlight = true,
                                size = 64.dp
                            )
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = glass.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.resolvedProductName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.accentPrimaryBlue,
                                textAlign = TextAlign.Center
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LiquidGlassPill(
                                    text = if (task.period == "MORNING") "Rutinitas Pagi" else "Rutinitas Malam",
                                    containerColor = glass.glassSurfaceElevated,
                                    contentColor = glass.textPrimary
                                )
                                LiquidGlassPill(
                                    text = scheduleText,
                                    containerColor = glass.accentSoftGlow,
                                    contentColor = glass.accentPrimaryBlue
                                )
                                if (task.estimatedMinutes > 0) {
                                    LiquidGlassPill(
                                        text = "Jeda ~${task.estimatedMinutes} mnt",
                                        containerColor = glass.glassSurfaceElevated,
                                        contentColor = glass.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Segmented Tabs: Instruksi Resep | Jadwal & Area | Catatan Medis
                    LiquidSegmentedBar(
                        options = listOf("Instruksi Resep", "Jadwal & Area", "Catatan Medis"),
                        selectedIndex = selectedTabIndex,
                        onSelect = { selectedTabIndex = it }
                    )

                    when (selectedTabIndex) {
                        0 -> {
                            Surface(
                                color = glass.glassSurfaceSecondary,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, glass.glassBorderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Petunjuk Pemakaian Sesuai Resep:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = glass.accentPrimaryBlue
                                    )
                                    Text(
                                        text = item.resolvedInstruction,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.textPrimary
                                    )
                                }
                            }
                        }
                        1 -> {
                            Surface(
                                color = glass.glassSurfaceSecondary,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, glass.glassBorderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DetailMetaRow(
                                        label = "Jumlah Pemakaian",
                                        value = item.resolvedUsageAmount
                                    )
                                    HorizontalDivider(color = glass.glassBorderSubtle)
                                    DetailMetaRow(
                                        label = "Area Aplikasi",
                                        value = item.resolvedApplicationArea
                                    )
                                    HorizontalDivider(color = glass.glassBorderSubtle)
                                    DetailMetaRow(
                                        label = "Durasi / Frekuensi",
                                        value = item.resolvedDurationNote
                                    )
                                    HorizontalDivider(color = glass.glassBorderSubtle)
                                    DetailMetaRow(
                                        label = "Jadwal Hari",
                                        value = scheduleText
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                color = glass.glassSurfaceSecondary,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, glass.glassBorderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.VerifiedUser,
                                            contentDescription = null,
                                            tint = glass.accentPrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Kepatuhan Resep Dokter Kulit",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glass.accentPrimaryBlue
                                        )
                                    }
                                    Text(
                                        text = RoutineScheduleHelper.CHEMICAL_PEELING_WARNING,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textPrimary
                                    )
                                    Text(
                                        text = "Aplikasi ini hanya membantu mencatat jadwal resep dokter kulit dan tidak menjamin bahwa produk pasti menyembuhkan jerawat atau bekas jerawat.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (item.requiresDoctorConfirmation || item.resolvedConfirmationNote.isNotBlank()) {
                        Surface(
                            color = glass.warningSoftBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, glass.warningAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = "Perlu konfirmasi ke dokter",
                                    tint = glass.warningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = glass.warningAmber
                                    )
                                    if (item.resolvedConfirmationNote.isNotBlank()) {
                                        Text(
                                            text = item.resolvedConfirmationNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onStartTimer != null) {
                            LiquidSecondaryGlassButton(
                                text = "Mulai Timer Jeda",
                                leadingIcon = Icons.Outlined.Timer,
                                onClick = {
                                    onStartTimer()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_start_timer_button")
                            )
                        }
                        if (onToggleComplete != null) {
                            LiquidPrimaryActionButton(
                                text = if (item.isCompleted) "Batalkan Selesai" else "Tandai Selesai",
                                leadingIcon = Icons.Outlined.Check,
                                onClick = {
                                    onToggleComplete()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_toggle_complete_button")
                            )
                        } else {
                            LiquidPrimaryActionButton(
                                text = "Tutup",
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("close_task_detail_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetaRow(
    label: String,
    value: String
) {
    val glass = LiquidGlassTokens.colors
    val isNeedsConfirm = value.contains(RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION, ignoreCase = true)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isNeedsConfirm) glass.warningAmber else glass.textPrimary
        )
    }
}

// ============================================================================
// 4. TIME PICKER MODAL DIALOG & FIRST-RUN ONBOARDING DIALOG (LIQUID GLASS)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModalDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val pickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    LiquidGlassModalDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = Icons.Outlined.AccessTime,
        content = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            LiquidPrimaryActionButton(
                text = "Simpan",
                onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                modifier = Modifier.testTag("time_picker_confirm_button")
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("time_picker_cancel_button")
            ) {
                Text("Batal", color = glass.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun FirstRunNotificationSetupDialog(
    onConfirmSetup: (morningEnabled: Boolean, eveningEnabled: Boolean, requestSystemPermission: Boolean) -> Unit,
    onDismissLater: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    var morningChecked by rememberSaveable { mutableStateOf(true) }
    var eveningChecked by rememberSaveable { mutableStateOf(true) }

    LiquidGlassModalDialog(
        onDismissRequest = onDismissLater,
        title = "Aktifkan Pengingat Rutinitas?",
        icon = Icons.Outlined.NotificationsActive,
        content = {
            Text(
                text = "SkinCare Daily membantu mengingatkan jadwal rutinitas pagi & malam sesuai resep dokter kulit Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
            Surface(
                color = glass.glassSurfaceSecondary,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, glass.glassBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = morningChecked,
                            onCheckedChange = { morningChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = glass.accentPrimaryBlue),
                            modifier = Modifier.testTag("onboarding_morning_checkbox")
                        )
                        Text(
                            text = "Pengingat Rutinitas Pagi (07:00)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = glass.textPrimary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = eveningChecked,
                            onCheckedChange = { eveningChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = glass.accentPrimaryBlue),
                            modifier = Modifier.testTag("onboarding_evening_checkbox")
                        )
                        Text(
                            text = "Pengingat Rutinitas Malam (21:00)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = glass.textPrimary
                        )
                    }
                }
            }
            Text(
                text = "Waktu pengingat dapat diubah kapan saja di tab Pengingat.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
        },
        confirmButton = {
            LiquidPrimaryActionButton(
                text = "Aktifkan Sekarang",
                onClick = {
                    onConfirmSetup(morningChecked, eveningChecked, morningChecked || eveningChecked)
                },
                modifier = Modifier.testTag("onboarding_confirm_button")
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissLater,
                modifier = Modifier.testTag("onboarding_later_button")
            ) {
                Text("Nanti Saja", color = glass.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
