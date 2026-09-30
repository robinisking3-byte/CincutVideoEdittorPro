package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.model.CoinAnimation
import com.example.core.model.CoinType
import com.example.core.model.MembershipTier
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * CineCut Status & Identity Coin Badge.
 * High-performance, vector Canvas-rendered metallic coin badge with real-time Compose animations.
 * NO emojis - pure professional metallic craftsmanship.
 */
@Composable
fun CineCoinStatusBadge(
    coinType: CoinType,
    modifier: Modifier = Modifier,
    editionNumber: Int? = null,
    maxSupply: Int? = coinType.maxSupply,
    size: Dp = 20.dp,
    showAnimation: Boolean = true,
    showEditionInline: Boolean = false,
    showInspectDialogOnClick: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    // Animations setup
    val infiniteTransition = rememberInfiniteTransition(label = "coin_animations")

    // 1. Shine Sweep phase (-1f to 2f)
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (coinType == CoinType.FOUNDER) 1800 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_progress"
    )

    // 2. 3D Coin Flip Angle (0 to 360 degrees)
    val flipAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (coinType) {
                    CoinType.FOUNDER -> 3200
                    CoinType.VIP -> 4000
                    CoinType.ADMIN -> 3600
                    else -> 4800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "flip_angle"
    )

    // 3. Glow Pulse Alpha (0.25f to 0.85f)
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // 4. Sparkle Particle Phase (0f to 1f)
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_phase"
    )

    // 5. Angular light reflection rotation (0 to 360)
    val reflectionRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reflection_rotation"
    )

    // Effective active flip angle depending on coin animation type
    val effectiveRotationY = if (showAnimation && (coinType.defaultAnimation == CoinAnimation.COIN_FLIP || coinType == CoinType.FOUNDER)) {
        flipAngle
    } else if (showAnimation && (coinType.defaultAnimation == CoinAnimation.ROTATION || coinType == CoinType.VIP || coinType == CoinType.ADMIN)) {
        flipAngle / 2f
    } else {
        0f
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .testTag("status_coin_badge_${coinType.id}")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (onClick != null) {
                    onClick()
                } else if (showInspectDialogOnClick) {
                    showDialog = true
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    if (effectiveRotationY != 0f) {
                        rotationY = effectiveRotationY
                        cameraDistance = 12f * density
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                val radius = canvasSize.minDimension / 2f * 0.9f

                drawCoinGraphic(
                    coinType = coinType,
                    center = center,
                    radius = radius,
                    sweepProgress = if (showAnimation) sweepProgress else -1f,
                    glowPulse = if (showAnimation) glowPulse else 0.5f,
                    sparklePhase = if (showAnimation) sparklePhase else 0f,
                    reflectionRotation = if (showAnimation) reflectionRotation else 45f,
                    rotationY = effectiveRotationY
                )
            }
        }

        if (showEditionInline && coinType.isLimited && editionNumber != null) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(coinType.baseColor).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(coinType.accentColor).copy(alpha = 0.8f))
            ) {
                Text(
                    text = "#$editionNumber${if (maxSupply != null) "/$maxSupply" else ""}",
                    fontSize = (size.value * 0.45f).coerceIn(8f, 11f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(coinType.accentColor),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }

    if (showDialog) {
        CoinStatusInspectDialog(
            coinType = coinType,
            editionNumber = editionNumber,
            maxSupply = maxSupply,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Universal User Tag helper that prepends the animated coin badge before the username.
 */
@Composable
fun UserCoinTag(
    username: String,
    displayName: String? = null,
    coinType: CoinType = CoinType.DEFAULT,
    editionNumber: Int? = null,
    maxSupply: Int? = coinType.maxSupply,
    membershipTier: MembershipTier? = null,
    isVerified: Boolean = false,
    badgeSize: Dp = 18.dp,
    nameFontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    nameColor: Color = Color.White,
    showEdition: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        // 🪙 The Special Status Coin Badge before username
        CineCoinStatusBadge(
            coinType = coinType,
            editionNumber = editionNumber,
            maxSupply = maxSupply,
            size = badgeSize,
            showEditionInline = false
        )

        // Display Name or Username
        Text(
            text = displayName ?: username,
            fontSize = nameFontSize,
            fontWeight = FontWeight.Bold,
            color = nameColor
        )

        // Numbered Edition Pill if Limited
        if (showEdition && coinType.isLimited && editionNumber != null) {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = Color(coinType.baseColor).copy(alpha = 0.22f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(coinType.accentColor).copy(alpha = 0.7f))
            ) {
                Text(
                    text = "#$editionNumber",
                    fontSize = (nameFontSize.value * 0.72f).coerceIn(8f, 10f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(coinType.accentColor),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                )
            }
        }

        // Verified check
        if (isVerified || coinType == CoinType.VERIFIED_CREATOR) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified Creator",
                tint = CineTertiary,
                modifier = Modifier.size((nameFontSize.value + 1f).dp)
            )
        }

        // Optional Membership pill
        if (membershipTier != null && membershipTier != MembershipTier.FREE) {
            MembershipBadge(tier = membershipTier)
        }
    }
}

/**
 * Pure Compose Canvas drawing routines for all 12 Coin Badges
 */
private fun DrawScope.drawCoinGraphic(
    coinType: CoinType,
    center: Offset,
    radius: Float,
    sweepProgress: Float,
    glowPulse: Float,
    sparklePhase: Float,
    reflectionRotation: Float,
    rotationY: Float
) {
    val base = Color(coinType.baseColor)
    val secondary = Color(coinType.secondaryColor)
    val accent = Color(coinType.accentColor)

    // Depth shade factor when coin is flipped edge-on (0 to 1)
    val normalFactor = abs(cos(rotationY * PI / 180.0)).toFloat().coerceIn(0.2f, 1f)

    // 1. Ambient Glow Aura
    val glowColor = when (coinType) {
        CoinType.FOUNDER -> Color(0xFFFFB300)
        CoinType.VIP -> Color(0xFF536DFE)
        CoinType.ADMIN -> Color(0xFFFF1744)
        CoinType.DIAMOND -> Color(0xFF00E5FF)
        CoinType.VERIFIED_CREATOR -> Color(0xFF00E676)
        CoinType.EARLY_SUPPORTER -> Color(0xFF7C4DFF)
        CoinType.GOLD -> Color(0xFFFFD700)
        CoinType.BANNED -> Color(0xFFD50000)
        else -> accent
    }

    if (coinType != CoinType.DEFAULT) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = (0.45f * glowPulse * normalFactor).coerceIn(0f, 0.7f)),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.55f
            ),
            radius = radius * 1.55f,
            center = center
        )
    }

    // 2. Beveled Metallic Coin Rim
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.95f * normalFactor),
            secondary.copy(alpha = 0.85f * normalFactor),
            base.copy(alpha = 0.95f * normalFactor)
        ),
        start = Offset(center.x - radius, center.y - radius),
        end = Offset(center.x + radius, center.y + radius)
    )

    drawCircle(
        brush = rimBrush,
        radius = radius,
        center = center
    )

    // 3. Inner Metallic Core Surface
    val innerRadius = radius * 0.82f
    val innerCoreBrush = when (coinType) {
        CoinType.DEFAULT, CoinType.SILVER -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), base, secondary),
                center = center.copy(y = center.y - innerRadius * 0.2f),
                radius = innerRadius * 1.3f
            )
        }
        CoinType.BRONZE -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFD1A4), base, secondary, Color(0xFF4A2500)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.GOLD -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFF5A4100)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.DIAMOND -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFF80DEEA), Color(0xFF00B0FF), Color(0xFF01579B)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.VIP -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFFFFD700), Color(0xFF283593), Color(0xFF1A237E), Color(0xFFFFD700)),
                start = Offset(center.x - innerRadius, center.y - innerRadius),
                end = Offset(center.x + innerRadius, center.y + innerRadius)
            )
        }
        CoinType.FOUNDER -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFD700), Color(0xFFFF6D00), Color(0xFFB71C1C)),
                center = center.copy(y = center.y - innerRadius * 0.15f),
                radius = innerRadius * 1.3f
            )
        }
        CoinType.ADMIN -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFF8A80), Color(0xFFD50000), Color(0xFF880E4F), Color(0xFF212121)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.MODERATOR -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFF82B1FF), Color(0xFF2979FF), Color(0xFF0D47A1)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.VERIFIED_CREATOR -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFB9F6CA), Color(0xFF00E676), Color(0xFF00897B), Color(0xFF004D40)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.EARLY_SUPPORTER -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFFE1BEE7), Color(0xFF7C4DFF), Color(0xFF4A148C)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
        CoinType.BANNED -> {
            Brush.radialGradient(
                colors = listOf(Color(0xFF37474F), Color(0xFF212121), Color(0xFF000000)),
                center = center,
                radius = innerRadius * 1.2f
            )
        }
    }

    drawCircle(
        brush = innerCoreBrush,
        radius = innerRadius,
        center = center
    )

    // Inner embossed ring border
    drawCircle(
        color = accent.copy(alpha = 0.55f * normalFactor),
        radius = innerRadius * 0.88f,
        center = center,
        style = Stroke(width = (radius * 0.08f).coerceAtLeast(1f))
    )

    // 4. Custom Architectural Coin Emblems
    when (coinType) {
        CoinType.FOUNDER -> {
            // Regal Engraved CineCut Crown Emblem
            drawFounderInsignia(center, innerRadius * 0.65f, accent)
        }
        CoinType.DIAMOND -> {
            // Prismatic Diamond Gemstone Facets
            drawDiamondGem(center, innerRadius * 0.65f, Color.White)
        }
        CoinType.ADMIN -> {
            // Imperial Shield & Crossed Sword Seal
            drawShieldEmblem(center, innerRadius * 0.65f, Color(0xFFFFD700), isCrossed = true)
        }
        CoinType.MODERATOR -> {
            // Guardian Security Shield
            drawShieldEmblem(center, innerRadius * 0.65f, Color.White, isCrossed = false)
        }
        CoinType.VERIFIED_CREATOR -> {
            // Camera Aperture Iris
            drawCameraAperture(center, innerRadius * 0.65f, Color(0xFFFFFFFF))
        }
        CoinType.VIP -> {
            // Regal Starburst & CineCut Monogram
            drawVipStarburst(center, innerRadius * 0.65f, Color(0xFFFFD700))
        }
        CoinType.EARLY_SUPPORTER -> {
            // Four-point Celestial Star
            drawCelestialStar(center, innerRadius * 0.65f, Color.White)
        }
        CoinType.BANNED -> {
            // Broken Fault Line Fissures & Punitive Slash
            drawFractureCracks(center, innerRadius * 0.85f, Color(0xFFFF1744))
        }
        else -> {
            // Standard / Silver / Bronze / Gold CineCut "C" Crest
            drawStandardCrest(center, innerRadius * 0.65f, accent, coinType.badgeSymbol)
        }
    }

    // 5. Specular Shine Sweep Highlight Effect
    if (sweepProgress > -0.4f && sweepProgress < 1.7f && coinType != CoinType.BANNED) {
        val sweepStart = center.x - innerRadius * 1.5f + (innerRadius * 3f * sweepProgress)
        val sweepBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = (0.55f * normalFactor).coerceIn(0f, 0.7f)),
                Color.Transparent
            ),
            start = Offset(sweepStart - innerRadius * 0.5f, center.y - innerRadius),
            end = Offset(sweepStart + innerRadius * 0.5f, center.y + innerRadius)
        )
        drawCircle(
            brush = sweepBrush,
            radius = innerRadius,
            center = center
        )
    }

    // 6. Sparkle Particles around Diamond and Founder
    if ((coinType == CoinType.DIAMOND || coinType == CoinType.FOUNDER || coinType == CoinType.VIP) && sparklePhase > 0f) {
        drawSparkleParticles(center, radius * 1.15f, sparklePhase, accent)
    }
}

private fun DrawScope.drawStandardCrest(center: Offset, size: Float, color: Color, symbol: String) {
    val path = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center.x - size * 0.55f, center.y - size * 0.55f, center.x + size * 0.55f, center.y + size * 0.55f))
    }
    drawCircle(
        color = color.copy(alpha = 0.9f),
        radius = size * 0.35f,
        center = center,
        style = Stroke(width = size * 0.25f)
    )
    // CineCut film tick mark
    drawLine(
        color = color,
        start = Offset(center.x + size * 0.25f, center.y),
        end = Offset(center.x + size * 0.65f, center.y),
        strokeWidth = size * 0.18f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawFounderInsignia(center: Offset, size: Float, gold: Color) {
    // 3-Point Sovereign Crown
    val path = Path().apply {
        moveTo(center.x - size * 0.65f, center.y + size * 0.35f)
        lineTo(center.x - size * 0.7f, center.y - size * 0.25f)
        lineTo(center.x - size * 0.3f, center.y + size * 0.05f)
        lineTo(center.x, center.y - size * 0.55f)
        lineTo(center.x + size * 0.3f, center.y + size * 0.05f)
        lineTo(center.x + size * 0.7f, center.y - size * 0.25f)
        lineTo(center.x + size * 0.65f, center.y + size * 0.35f)
        close()
    }
    drawPath(path = path, color = gold)

    // Crown Jewels / Pearls
    drawCircle(color = Color.White, radius = size * 0.1f, center = Offset(center.x, center.y - size * 0.55f))
    drawCircle(color = Color.White, radius = size * 0.08f, center = Offset(center.x - size * 0.7f, center.y - size * 0.25f))
    drawCircle(color = Color.White, radius = size * 0.08f, center = Offset(center.x + size * 0.7f, center.y - size * 0.25f))
}

private fun DrawScope.drawDiamondGem(center: Offset, size: Float, color: Color) {
    val topY = center.y - size * 0.5f
    val midY = center.y - size * 0.15f
    val botY = center.y + size * 0.55f
    val leftX = center.x - size * 0.6f
    val rightX = center.x + size * 0.6f
    val innerLeft = center.x - size * 0.3f
    val innerRight = center.x + size * 0.3f

    val path = Path().apply {
        moveTo(innerLeft, topY)
        lineTo(innerRight, topY)
        lineTo(rightX, midY)
        lineTo(center.x, botY)
        lineTo(leftX, midY)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.85f), style = Stroke(width = size * 0.12f))

    // Facet lines
    drawLine(color = color.copy(alpha = 0.7f), start = Offset(innerLeft, topY), end = Offset(center.x, botY), strokeWidth = size * 0.08f)
    drawLine(color = color.copy(alpha = 0.7f), start = Offset(innerRight, topY), end = Offset(center.x, botY), strokeWidth = size * 0.08f)
    drawLine(color = color.copy(alpha = 0.7f), start = Offset(leftX, midY), end = Offset(rightX, midY), strokeWidth = size * 0.08f)
}

private fun DrawScope.drawShieldEmblem(center: Offset, size: Float, color: Color, isCrossed: Boolean) {
    val shieldPath = Path().apply {
        moveTo(center.x - size * 0.5f, center.y - size * 0.45f)
        lineTo(center.x + size * 0.5f, center.y - size * 0.45f)
        lineTo(center.x + size * 0.45f, center.y + size * 0.15f)
        quadraticBezierTo(center.x + size * 0.3f, center.y + size * 0.55f, center.x, center.y + size * 0.65f)
        quadraticBezierTo(center.x - size * 0.3f, center.y + size * 0.55f, center.x - size * 0.45f, center.y + size * 0.15f)
        close()
    }
    drawPath(path = shieldPath, color = color.copy(alpha = 0.9f), style = Stroke(width = size * 0.14f))

    if (isCrossed) {
        // Crossed sword or star
        drawLine(color = color, start = Offset(center.x - size * 0.28f, center.y - size * 0.25f), end = Offset(center.x + size * 0.28f, center.y + size * 0.35f), strokeWidth = size * 0.1f)
        drawLine(color = color, start = Offset(center.x + size * 0.28f, center.y - size * 0.25f), end = Offset(center.x - size * 0.28f, center.y + size * 0.35f), strokeWidth = size * 0.1f)
    } else {
        // Security check
        drawCircle(color = color, radius = size * 0.15f, center = Offset(center.x, center.y + size * 0.05f))
    }
}

private fun DrawScope.drawCameraAperture(center: Offset, size: Float, color: Color) {
    // 6 Aperture Blades
    val bladeCount = 6
    val bladeRadius = size * 0.5f
    for (i in 0 until bladeCount) {
        val angle = (i * 60) * (PI / 180.0)
        val x1 = center.x + cos(angle).toFloat() * bladeRadius
        val y1 = center.y + sin(angle).toFloat() * bladeRadius
        val x2 = center.x + cos(angle + 1.2).toFloat() * (bladeRadius * 0.4f)
        val y2 = center.y + sin(angle + 1.2).toFloat() * (bladeRadius * 0.4f)
        drawLine(color = color.copy(alpha = 0.85f), start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = size * 0.12f, cap = StrokeCap.Round)
    }
    drawCircle(color = color, radius = size * 0.18f, center = center)
}

private fun DrawScope.drawVipStarburst(center: Offset, size: Float, gold: Color) {
    for (i in 0 until 8) {
        val angle = (i * 45) * (PI / 180.0)
        val len = if (i % 2 == 0) size * 0.6f else size * 0.35f
        val x = center.x + cos(angle).toFloat() * len
        val y = center.y + sin(angle).toFloat() * len
        drawLine(color = gold, start = center, end = Offset(x, y), strokeWidth = size * 0.1f, cap = StrokeCap.Round)
    }
    drawCircle(color = Color.White, radius = size * 0.16f, center = center)
}

private fun DrawScope.drawCelestialStar(center: Offset, size: Float, color: Color) {
    val starPath = Path().apply {
        moveTo(center.x, center.y - size * 0.6f)
        quadraticBezierTo(center.x, center.y, center.x + size * 0.6f, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y + size * 0.6f)
        quadraticBezierTo(center.x, center.y, center.x - size * 0.6f, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y - size * 0.6f)
        close()
    }
    drawPath(path = starPath, color = color)
}

private fun DrawScope.drawFractureCracks(center: Offset, size: Float, red: Color) {
    // Jagged punitive crack lines
    drawLine(color = red, start = Offset(center.x - size * 0.4f, center.y - size * 0.7f), end = Offset(center.x - size * 0.1f, center.y - size * 0.2f), strokeWidth = size * 0.14f)
    drawLine(color = red, start = Offset(center.x - size * 0.1f, center.y - size * 0.2f), end = Offset(center.x + size * 0.2f, center.y + size * 0.1f), strokeWidth = size * 0.12f)
    drawLine(color = red, start = Offset(center.x + size * 0.2f, center.y + size * 0.1f), end = Offset(center.x + size * 0.5f, center.y + size * 0.65f), strokeWidth = size * 0.14f)
    drawLine(color = red, start = Offset(center.x - size * 0.1f, center.y - size * 0.2f), end = Offset(center.x - size * 0.45f, center.y + size * 0.3f), strokeWidth = size * 0.08f)
}

private fun DrawScope.drawSparkleParticles(center: Offset, radius: Float, phase: Float, color: Color) {
    val angles = listOf(45f, 135f, 225f, 315f)
    angles.forEachIndexed { idx, ang ->
        val rad = (ang + phase * 90f) * (PI / 180.0)
        val dist = radius * (0.85f + 0.35f * sin(phase * 2 * PI + idx).toFloat())
        val alpha = (0.2f + 0.8f * cos(phase * 2 * PI + idx).toFloat().coerceIn(0f, 1f)).coerceIn(0f, 1f)
        val sparkCenter = Offset(center.x + cos(rad).toFloat() * dist, center.y + sin(rad).toFloat() * dist)

        drawCircle(color = color.copy(alpha = alpha), radius = radius * 0.08f, center = sparkCenter)
    }
}

/**
 * Detailed Interactive Coin Inspector Modal Dialog.
 * Shows high-resolution 3D rotating preview, edition scarcity, privileges, and verification certificate.
 */
@Composable
fun CoinStatusInspectDialog(
    coinType: CoinType,
    editionNumber: Int? = null,
    maxSupply: Int? = coinType.maxSupply,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CineSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(coinType.accentColor).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("coin_inspect_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS COIN BADGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextTertiary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary)
                    }
                }

                // Hero 3D Spinning Coin
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CineCoinStatusBadge(
                        coinType = coinType,
                        editionNumber = editionNumber,
                        size = 80.dp,
                        showInspectDialogOnClick = false
                    )
                }

                // Title & Edition Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = coinType.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    if (coinType.isLimited) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(coinType.baseColor).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(coinType.accentColor))
                        ) {
                            Text(
                                text = "EXCLUSIVE LIMITED EDITION #${editionNumber ?: "27"}${if (maxSupply != null) " / $maxSupply" else ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(coinType.accentColor),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Level ${coinType.level} • ${coinType.defaultAnimation.label}",
                            fontSize = 12.sp,
                            color = CineTertiary
                        )
                    }
                }

                // Description
                Text(
                    text = coinType.description,
                    fontSize = 12.sp,
                    color = CineTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Divider(color = CineTimelineRuler, thickness = 0.5.dp)

                // Community Privileges & Rights
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CineSurfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "COMMUNITY PRIVILEGES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextTertiary
                    )

                    val privileges = when (coinType) {
                        CoinType.FOUNDER -> listOf(
                            "Access to VIP & Founder CineRooms",
                            "Golden crown particle aura on all comments",
                            "Priority rendering on CineCut Cloud GPU",
                            "Vote on future AI Director tools & features"
                        )
                        CoinType.VIP -> listOf(
                            "Access to VIP Masterclass CineRooms",
                            "Exclusive royal blue & gold emblem in live chat",
                            "Zero export limits and 4K 60fps unlocked",
                            "Monthly 2,500 CineCoins bonus stipend"
                        )
                        CoinType.ADMIN, CoinType.MODERATOR -> listOf(
                            "Staff administrative badge across all channels",
                            "Authoritative content moderation tools",
                            "Priority support queue management",
                            "Access to CineCut Administrative Center"
                        )
                        CoinType.DIAMOND -> listOf(
                            "Diamond Director CineRoom entry",
                            "Sparkle starlight animation on feed posts",
                            "2K/4K Pro Color LUTs unlocked"
                        )
                        CoinType.VERIFIED_CREATOR -> listOf(
                            "Accredited Creator mark in collaborator invites",
                            "Monetized CineLive tipping enabled",
                            "Featured spot on Creator Discovery Feed"
                        )
                        CoinType.BANNED -> listOf(
                            "Account restrictions active",
                            "Posting and live chatting suspended",
                            "Contact CineCut Support for dispute review"
                        )
                        else -> listOf(
                            "Official CineCut creator identity",
                            "Project collaboration enabled",
                            "Earn coins via daily check-ins & exports"
                        )
                    }

                    privileges.forEach { priv ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (coinType == CoinType.BANNED) Icons.Default.Cancel else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (coinType == CoinType.BANNED) CineError else CineSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(text = priv, fontSize = 11.sp, color = CineTextSecondary)
                        }
                    }
                }

                // Security & Authentication Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = CineTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Cryptographically attested & backend-authoritative. Coins cannot be self-assigned.",
                        fontSize = 10.sp,
                        color = CineTextTertiary
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Inspector")
                }
            }
        }
    }
}
