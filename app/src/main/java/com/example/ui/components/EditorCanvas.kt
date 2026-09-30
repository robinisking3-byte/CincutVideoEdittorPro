package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.VideoRenderer
import com.example.core.model.Clip
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.ui.theme.*

@Composable
fun EditorCanvas(
    project: Project,
    tracks: List<Track>,
    playheadMs: Long,
    selectedClip: Clip?,
    showSafeGuides: Boolean,
    onClipTransformChange: ((scale: Float, rotation: Float, dx: Float, dy: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeClips = remember(tracks, playheadMs) {
        VideoRenderer.getActiveClips(tracks, playheadMs)
    }

    val aspectRatioValue = when (project.aspectRatio) {
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        "21:9" -> 21f / 9f
        else -> 16f / 9f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatioValue)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F111A))
                .border(1.dp, CineTimelineRuler, RoundedCornerShape(4.dp))
                .testTag("preview_canvas")
                .pointerInput(selectedClip) {
                    if (selectedClip != null && onClipTransformChange != null) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onClipTransformChange(zoom, rotation, pan.x, pan.y)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Render active video/image clips
                for (clip in activeClips) {
                    if (clip.textOverlay == null) {
                        val clipOffsetMs = (playheadMs - clip.startMs).coerceAtLeast(0L)
                        val interpolatedScale = VideoRenderer.interpolateKeyframe(
                            clip, "scale", clipOffsetMs, clip.scale
                        )
                        val interpolatedOpacity = VideoRenderer.interpolateKeyframe(
                            clip, "opacity", clipOffsetMs, clip.opacity
                        )

                        // Visual rendering representing the video layer with gradient and color grading
                        val filter = VideoRenderer.createColorFilter(clip.colorGrading)
                        val layerRectWidth = canvasWidth * interpolatedScale
                        val layerRectHeight = canvasHeight * interpolatedScale
                        val left = (canvasWidth - layerRectWidth) / 2f + clip.translationX
                        val top = (canvasHeight - layerRectHeight) / 2f + clip.translationY

                        // Draw cinematic video frame background representation
                        val clipColor = when (clip.colorGrading.lutFilter) {
                            "Cinematic Teal & Orange" -> Color(0xFF1E3A4C)
                            "Cyberpunk" -> Color(0xFF38104A)
                            "Noir" -> Color(0xFF262626)
                            "Vintage 16mm" -> Color(0xFF4A3828)
                            "Golden Hour" -> Color(0xFF4D3818)
                            else -> Color(0xFF1A2138)
                        }

                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(clipColor.copy(alpha = interpolatedOpacity), Color(0xFF05060A)),
                                center = Offset(left + layerRectWidth / 2f, top + layerRectHeight / 2f),
                                radius = maxOf(layerRectWidth, layerRectHeight) / 1.2f
                            ),
                            topLeft = Offset(left, top),
                            size = Size(layerRectWidth, layerRectHeight),
                            colorFilter = filter
                        )

                        // Subtle video detail grids/lines to visualize motion and grading
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(left, top + layerRectHeight * 0.33f),
                            end = Offset(left + layerRectWidth, top + layerRectHeight * 0.33f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(left, top + layerRectHeight * 0.66f),
                            end = Offset(left + layerRectWidth, top + layerRectHeight * 0.66f),
                            strokeWidth = 1f
                        )
                    }
                }

                // Render active text layers
                for (clip in activeClips) {
                    val overlay = clip.textOverlay
                    if (overlay != null) {
                        val textX = canvasWidth * overlay.positionX
                        val textY = canvasHeight * overlay.positionY

                        // Background container if present
                        if (overlay.backgroundColor != 0L) {
                            drawRoundRect(
                                color = Color(overlay.backgroundColor),
                                topLeft = Offset(textX - 120f, textY - 25f),
                                size = Size(240f, 50f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                            )
                        }
                    }
                }

                // Selection bounding box with transform handles
                if (selectedClip != null && activeClips.any { it.id == selectedClip.id }) {
                    val boxW = canvasWidth * selectedClip.scale * 0.85f
                    val boxH = canvasHeight * selectedClip.scale * 0.85f
                    val boxLeft = (canvasWidth - boxW) / 2f + selectedClip.translationX
                    val boxTop = (canvasHeight - boxH) / 2f + selectedClip.translationY

                    // Bounding dashed box
                    drawRect(
                        color = CineTertiary,
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(boxW, boxH),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    )

                    // Corner Transform Handles
                    val handleRadius = 7f
                    val corners = listOf(
                        Offset(boxLeft, boxTop),
                        Offset(boxLeft + boxW, boxTop),
                        Offset(boxLeft, boxTop + boxH),
                        Offset(boxLeft + boxW, boxTop + boxH)
                    )
                    for (corner in corners) {
                        drawCircle(color = Color.White, radius = handleRadius, center = corner)
                        drawCircle(color = CineTertiary, radius = handleRadius, center = corner, style = Stroke(width = 2f))
                    }
                }

                // Safe area guidelines (90% Action Safe, 80% Title Safe)
                if (showSafeGuides) {
                    // Action Safe (90%)
                    val actionSafeInsetX = canvasWidth * 0.05f
                    val actionSafeInsetY = canvasHeight * 0.05f
                    drawRect(
                        color = Color(0x6600E5FF),
                        topLeft = Offset(actionSafeInsetX, actionSafeInsetY),
                        size = Size(canvasWidth * 0.9f, canvasHeight * 0.9f),
                        style = Stroke(width = 1f)
                    )
                    // Title Safe (80%)
                    val titleSafeInsetX = canvasWidth * 0.1f
                    val titleSafeInsetY = canvasHeight * 0.1f
                    drawRect(
                        color = Color(0x66FFB703),
                        topLeft = Offset(titleSafeInsetX, titleSafeInsetY),
                        size = Size(canvasWidth * 0.8f, canvasHeight * 0.8f),
                        style = Stroke(width = 1f)
                    )
                }
            }

            // HTML/Compose Text layer overlay for crisp typography
            for (clip in activeClips) {
                val overlay = clip.textOverlay
                if (overlay != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = overlay.text,
                            color = Color(overlay.textColor),
                            fontSize = (overlay.fontSizeSp * 0.75f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Timecode & Aspect indicator tag
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(CinePrimary))
                Text(
                    text = "${formatTimecode(playheadMs)} | ${project.aspectRatio}",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun formatTimecode(ms: Long): String {
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val frames = ((ms % 1000L) / 33.3f).toInt()
    return "%02d:%02d:%02d".format(minutes, seconds, frames)
}
