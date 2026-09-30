package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.core.model.FestivalAnimationConfig
import com.example.core.model.FestivalAnimationType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var colorIndex: Int,
    var pulsePhase: Float
)

/**
 * Lightweight, hardware-accelerated particle animation canvas for festivals.
 * Engineered specifically with battery and low-end Android device optimizations.
 */
@Composable
fun FestivalParticlesOverlay(
    config: FestivalAnimationConfig,
    modifier: Modifier = Modifier
) {
    if (!config.enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "festival_particles")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (4000 / config.speedMultiplier).toInt().coerceAtLeast(1000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )

    // Low-end optimization: reduce particle count significantly
    val particleCount = remember(config.particleDensity, config.reduceAnimations) {
        if (config.reduceAnimations) 6
        else (12 + (config.particleDensity * 24).toInt()).coerceIn(8, 36)
    }

    val particles = remember(config.animationType, particleCount) {
        val random = Random(42)
        List(particleCount) {
            Particle(
                x = random.nextFloat(),
                y = random.nextFloat(),
                vx = (random.nextFloat() - 0.5f) * 0.002f,
                vy = -0.002f - (random.nextFloat() * 0.003f),
                size = 4f + random.nextFloat() * 10f,
                alpha = 0.3f + random.nextFloat() * 0.6f,
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 4f,
                colorIndex = random.nextInt(4),
                pulsePhase = random.nextFloat() * 6.28f
            )
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            when (config.animationType) {
                FestivalAnimationType.DIYA_LIGHTS -> {
                    drawDiyaLights(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.COLOR_SPLASH -> {
                    drawColorSplash(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.PEACOCK_FEATHER -> {
                    drawPeacockFeatherParticles(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.RAKHI_MOTIF -> {
                    drawRakhiMotifs(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.FIREWORKS_LIGHTS -> {
                    drawFireworksLights(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.PUMPKINS_BATS -> {
                    drawSpookyParticles(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.SNOWFALL -> {
                    drawSnowfall(particles, animProgress, width, height, config.reduceAnimations)
                }
                FestivalAnimationType.TRICOLOR_CELEBRATION -> {
                    drawTricolorCelebration(particles, animProgress, width, height, config.reduceAnimations)
                }
            }
        }
    }
}

// ----------------- 1. DIWALI: DIYAS & GOLDEN SPARKLES -----------------
private fun DrawScope.drawDiyaLights(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val goldColor = Color(0xFFFFD700)
    val amberColor = Color(0xFFFF9100)
    val flameColor = Color(0xFFFF3D00)

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y - progress * (0.15f + i * 0.01f)) % 1.0f + 1.0f) % 1.0f * height
        val xDrift = sin(progress * 6.28f + p.pulsePhase) * (if (reduceAnimations) 8f else 18f)
        val xPos = ((p.x * width) + xDrift) % width
        val pulse = 0.7f + 0.3f * sin(progress * 12.56f + p.pulsePhase)
        val curAlpha = (p.alpha * pulse).coerceIn(0.15f, 0.95f)

        if (i % 5 == 0 && !reduceAnimations) {
            // Draw a subtle, delicate diya base with warm flame on selected particles
            val baseWidth = p.size * 2.8f
            val baseHeight = p.size * 1.2f
            val flameHeight = p.size * 2.2f

            // Golden bowl
            drawArc(
                color = amberColor.copy(alpha = curAlpha * 0.8f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(xPos - baseWidth / 2, yPos),
                size = Size(baseWidth, baseHeight)
            )
            // Little warm flame teardrop
            val flamePath = Path().apply {
                moveTo(xPos, yPos - flameHeight)
                quadraticBezierTo(xPos + baseWidth * 0.35f, yPos - flameHeight * 0.3f, xPos, yPos)
                quadraticBezierTo(xPos - baseWidth * 0.35f, yPos - flameHeight * 0.3f, xPos, yPos - flameHeight)
                close()
            }
            drawPath(flamePath, color = flameColor.copy(alpha = curAlpha))
            drawCircle(
                color = goldColor.copy(alpha = curAlpha * 0.9f),
                radius = p.size * 0.6f,
                center = Offset(xPos, yPos - flameHeight * 0.4f)
            )
        } else {
            // Golden rising sparkle / star mote
            val radius = p.size * (if (reduceAnimations) 0.6f else 0.85f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(goldColor.copy(alpha = curAlpha), amberColor.copy(alpha = 0f)),
                    center = Offset(xPos, yPos),
                    radius = radius * 2.5f
                ),
                radius = radius * 2.5f,
                center = Offset(xPos, yPos)
            )
            drawCircle(color = Color.White.copy(alpha = curAlpha), radius = radius * 0.5f, center = Offset(xPos, yPos))
        }
    }
}

// ----------------- 2. HOLI: COLOR PARTICLES & SPLASH -----------------
private fun DrawScope.drawColorSplash(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val holiPalette = listOf(
        Color(0xFFFF007F), // Vivid Gulal Pink
        Color(0xFF00E5FF), // Turquoise Cyan
        Color(0xFFFFD600), // Vibrant Yellow
        Color(0xFF76FF03), // Spring Green
        Color(0xFF9C27B0)  // Regal Violet
    )

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y + progress * (0.12f + i * 0.008f)) % 1.0f) * height
        val xPos = ((p.x * width) + cos(progress * 6.28f + p.pulsePhase) * 20f) % width
        val color = holiPalette[p.colorIndex % holiPalette.size]
        val alpha = (p.alpha * (0.6f + 0.4f * sin(progress * 6.28f + p.pulsePhase))).coerceIn(0.2f, 0.85f)

        val radius = p.size * (if (reduceAnimations) 0.8f else 1.2f)

        // Soft pigment powder cloud
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                center = Offset(xPos, yPos),
                radius = radius * 2.2f
            ),
            radius = radius * 2.2f,
            center = Offset(xPos, yPos)
        )
        drawCircle(color = color.copy(alpha = alpha), radius = radius * 0.6f, center = Offset(xPos, yPos))
    }
}

// ----------------- 3. JANMASHTAMI: PEACOCK FEATHER & KRISHNA MOTES -----------------
private fun DrawScope.drawPeacockFeatherParticles(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val royalBlue = Color(0xFF1E88E5)
    val peacockTeal = Color(0xFF00B0FF)
    val sacredGold = Color(0xFFFFD700)
    val emerald = Color(0xFF00E676)

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y + progress * (0.08f + i * 0.005f)) % 1.0f) * height
        val xPos = ((p.x * width) + sin(progress * 6.28f + p.pulsePhase) * 16f) % width
        val alpha = (p.alpha * 0.85f).coerceIn(0.2f, 0.9f)

        if (i % 4 == 0 && !reduceAnimations) {
            // Stylized peacock-feather eyelet mote
            val r = p.size * 1.5f
            drawOval(
                color = emerald.copy(alpha = alpha * 0.6f),
                topLeft = Offset(xPos - r, yPos - r * 1.3f),
                size = Size(r * 2f, r * 2.6f)
            )
            drawOval(
                color = royalBlue.copy(alpha = alpha * 0.8f),
                topLeft = Offset(xPos - r * 0.7f, yPos - r * 0.9f),
                size = Size(r * 1.4f, r * 1.8f)
            )
            drawCircle(
                color = sacredGold.copy(alpha = alpha),
                radius = r * 0.35f,
                center = Offset(xPos, yPos)
            )
        } else {
            // Celestial golden sparkle
            drawCircle(
                color = sacredGold.copy(alpha = alpha * 0.8f),
                radius = p.size * 0.6f,
                center = Offset(xPos, yPos)
            )
            drawCircle(
                color = peacockTeal.copy(alpha = alpha * 0.4f),
                radius = p.size * 1.2f,
                center = Offset(xPos, yPos)
            )
        }
    }
}

// ----------------- 4. RAKSHA BANDHAN: RAKHI THREADS & SACRED MOTIFS -----------------
private fun DrawScope.drawRakhiMotifs(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val crimson = Color(0xFFE91E63)
    val saffron = Color(0xFFFF9800)
    val gold = Color(0xFFFFD700)

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y - progress * (0.09f + i * 0.006f)) % 1.0f + 1.0f) % 1.0f * height
        val xPos = ((p.x * width) + cos(progress * 6.28f + p.pulsePhase) * 14f) % width
        val alpha = (p.alpha * 0.8f).coerceIn(0.2f, 0.85f)

        val r = p.size * (if (reduceAnimations) 0.7f else 1.0f)

        // Floral petal motif
        if (i % 3 == 0 && !reduceAnimations) {
            val petalCount = 6
            for (petal in 0 until petalCount) {
                val angle = (petal * 360f / petalCount + progress * 90f) * (3.14159f / 180f)
                val px = xPos + cos(angle) * (r * 1.2f)
                val py = yPos + sin(angle) * (r * 1.2f)
                drawCircle(color = crimson.copy(alpha = alpha * 0.7f), radius = r * 0.5f, center = Offset(px, py))
            }
            drawCircle(color = gold.copy(alpha = alpha), radius = r * 0.6f, center = Offset(xPos, yPos))
        } else {
            drawCircle(color = saffron.copy(alpha = alpha), radius = r * 0.6f, center = Offset(xPos, yPos))
        }
    }
}

// ----------------- 5. DUSSEHRA: FIREWORKS & VICTORY LIGHTS -----------------
private fun DrawScope.drawFireworksLights(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val fireworkGold = Color(0xFFFFD54F)
    val victoryOrange = Color(0xFFFF6F00)
    val ruby = Color(0xFFE53935)

    particles.forEachIndexed { i, p ->
        val burstProgress = ((progress * 1.5f + p.pulsePhase * 0.2f) % 1.0f)
        val center = Offset(p.x * width, p.y * height)
        val alpha = ((1.0f - burstProgress) * p.alpha).coerceIn(0f, 0.9f)

        val sparkCount = if (reduceAnimations) 4 else 8
        val sparkDist = burstProgress * (p.size * 4f)

        for (s in 0 until sparkCount) {
            val angle = (s * 360f / sparkCount + p.rotation) * (3.14159f / 180f)
            val sx = center.x + cos(angle) * sparkDist
            val sy = center.y + sin(angle) * sparkDist

            val sparkColor = if (s % 2 == 0) fireworkGold else if (s % 3 == 0) victoryOrange else ruby
            drawCircle(
                color = sparkColor.copy(alpha = alpha),
                radius = (p.size * 0.35f) * (1f - burstProgress * 0.5f),
                center = Offset(sx, sy)
            )
        }
    }
}

// ----------------- 6. HALLOWEEN: SPICY FOG & BATS -----------------
private fun DrawScope.drawSpookyParticles(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val pumpkinOrange = Color(0xFFFF6D00)
    val mysticPurple = Color(0xFF7C4DFF)
    val ectoGreen = Color(0xFF76FF03)

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y + progress * (0.07f + i * 0.004f)) % 1.0f) * height
        val xPos = ((p.x * width) + sin(progress * 4.71f + p.pulsePhase) * 22f) % width
        val alpha = (p.alpha * 0.7f).coerceIn(0.2f, 0.75f)

        if (i % 4 == 0 && !reduceAnimations) {
            // Bat silhouette
            val batWidth = p.size * 1.8f
            val batHeight = p.size * 0.7f
            val wingFlap = sin(progress * 18.84f + p.pulsePhase) * (batHeight * 0.4f)

            val batPath = Path().apply {
                moveTo(xPos, yPos)
                quadraticBezierTo(xPos - batWidth * 0.6f, yPos - batHeight + wingFlap, xPos - batWidth, yPos)
                quadraticBezierTo(xPos - batWidth * 0.4f, yPos + batHeight * 0.4f, xPos, yPos + batHeight * 0.3f)
                quadraticBezierTo(xPos + batWidth * 0.4f, yPos + batHeight * 0.4f, xPos + batWidth, yPos)
                quadraticBezierTo(xPos + batWidth * 0.6f, yPos - batHeight + wingFlap, xPos, yPos)
                close()
            }
            drawPath(batPath, color = mysticPurple.copy(alpha = alpha * 0.9f))
        } else {
            // Glowing spooky fog ember
            val emberColor = if (i % 2 == 0) pumpkinOrange else ectoGreen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(emberColor.copy(alpha = alpha * 0.8f), emberColor.copy(alpha = 0f)),
                    center = Offset(xPos, yPos),
                    radius = p.size * 2f
                ),
                radius = p.size * 2f,
                center = Offset(xPos, yPos)
            )
        }
    }
}

// ----------------- 7. CHRISTMAS: SNOWFALL & TWINKLE LIGHTS -----------------
private fun DrawScope.drawSnowfall(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val snowWhite = Color(0xFFFFFFFF)
    val holidayRed = Color(0xFFE53935)
    val holidayGreen = Color(0xFF43A047)
    val warmGold = Color(0xFFFFD54F)

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y + progress * (0.18f + i * 0.008f)) % 1.0f) * height
        val sway = sin(progress * 6.28f + p.pulsePhase) * (if (reduceAnimations) 6f else 16f)
        val xPos = ((p.x * width) + sway) % width
        val alpha = (p.alpha * 0.85f).coerceIn(0.2f, 0.95f)

        if (i % 6 == 0 && !reduceAnimations) {
            // 6-point snowflake crystal
            val r = p.size * 1.1f
            for (arm in 0 until 3) {
                val angle = (arm * 60f + p.rotation) * (3.14159f / 180f)
                val dx = cos(angle) * r
                val dy = sin(angle) * r
                drawLine(
                    color = snowWhite.copy(alpha = alpha),
                    start = Offset(xPos - dx, yPos - dy),
                    end = Offset(xPos + dx, yPos + dy),
                    strokeWidth = 1.8f
                )
            }
        } else if (i % 5 == 0 && !reduceAnimations) {
            // Festive twinkle bulb
            val bulbColor = when (i % 3) {
                0 -> holidayRed
                1 -> holidayGreen
                else -> warmGold
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bulbColor.copy(alpha = alpha * 0.9f), bulbColor.copy(alpha = 0f)),
                    center = Offset(xPos, yPos),
                    radius = p.size * 2f
                ),
                radius = p.size * 2f,
                center = Offset(xPos, yPos)
            )
            drawCircle(color = snowWhite.copy(alpha = alpha), radius = p.size * 0.4f, center = Offset(xPos, yPos))
        } else {
            // Soft falling snow mote
            drawCircle(
                color = snowWhite.copy(alpha = alpha * 0.75f),
                radius = p.size * (if (reduceAnimations) 0.6f else 0.8f),
                center = Offset(xPos, yPos)
            )
        }
    }
}

// ----------------- 8. INDEPENDENCE & REPUBLIC DAY: TRICOLOR CELEBRATION -----------------
private fun DrawScope.drawTricolorCelebration(
    particles: List<Particle>,
    progress: Float,
    width: Float,
    height: Float,
    reduceAnimations: Boolean
) {
    val saffron = Color(0xFFFF6F00)
    val white = Color(0xFFFFFFFF)
    val indiaGreen = Color(0xFF138808)
    val chakraNavy = Color(0xFF000080)

    particles.forEachIndexed { i, p ->
        val yPos = ((p.y + progress * (0.14f + i * 0.007f)) % 1.0f) * height
        val sway = sin(progress * 6.28f + p.pulsePhase) * 14f
        val xPos = ((p.x * width) + sway) % width
        val alpha = (p.alpha * 0.85f).coerceIn(0.25f, 0.9f)

        val color = when (i % 3) {
            0 -> saffron
            1 -> white
            else -> indiaGreen
        }

        if (i % 4 == 0 && !reduceAnimations) {
            // Fluttering patriotic ribbon confetti
            val ribbonLen = p.size * 1.8f
            val ribbonAngle = (progress * 180f + p.rotation) * (3.14159f / 180f)
            val dx = cos(ribbonAngle) * ribbonLen
            val dy = sin(ribbonAngle) * (ribbonLen * 0.6f)

            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(xPos - dx, yPos - dy),
                end = Offset(xPos + dx, yPos + dy),
                strokeWidth = p.size * 0.5f
            )
        } else {
            drawCircle(color = color.copy(alpha = alpha), radius = p.size * 0.65f, center = Offset(xPos, yPos))
        }
    }
}
