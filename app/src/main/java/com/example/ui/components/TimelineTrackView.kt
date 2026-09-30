package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Clip
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.ui.theme.*

@Composable
fun TimelineTrackView(
    tracks: List<Track>,
    playheadMs: Long,
    totalDurationMs: Long,
    selectedClipId: String?,
    zoomScale: Float,
    onPlayheadChange: (Long) -> Unit,
    onClipSelect: (String?) -> Unit,
    onTrimStart: (clipId: String, newStartMs: Long) -> Unit,
    onTrimEnd: (clipId: String, newDurationMs: Long) -> Unit,
    onToggleTrackMute: (String) -> Unit,
    onToggleTrackLock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val pxPerSec = (60f * zoomScale).coerceAtLeast(30f)
    val totalWidthDp = ((totalDurationMs / 1000f) * pxPerSec).dp.coerceAtLeast(600.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CineBackground)
            .border(1.dp, CineTimelineRuler)
            .testTag("timeline_track_view")
    ) {
        // Top Timeline Ruler with Playhead Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(CineSurface)
                .horizontalScroll(scrollState)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val clickedSec = offset.x / pxPerSec
                        onPlayheadChange((clickedSec * 1000L).toLong())
                    }
                }
        ) {
            Canvas(modifier = Modifier.width(totalWidthDp).height(30.dp)) {
                val totalSeconds = (totalDurationMs / 1000f).toInt()
                for (sec in 0..totalSeconds) {
                    val x = sec * pxPerSec
                    val isMajor = sec % 2 == 0
                    drawLine(
                        color = if (isMajor) CineTextSecondary else CineTimelineRuler,
                        start = Offset(x, if (isMajor) 10f else 18f),
                        end = Offset(x, 30f),
                        strokeWidth = if (isMajor) 2f else 1f
                    )
                }
            }

            // Timecode labels on ruler
            Row(modifier = Modifier.width(totalWidthDp)) {
                val stepSec = if (zoomScale < 1.0f) 5 else 2
                val totalSec = (totalDurationMs / 1000L).toInt()
                for (s in 0..totalSec step stepSec) {
                    Text(
                        text = "%02d:%02d".format(s / 60, s % 60),
                        fontSize = 9.sp,
                        color = CineTextTertiary,
                        modifier = Modifier
                            .offset(x = (s * pxPerSec).dp)
                            .padding(top = 2.dp)
                    )
                }
            }
        }

        Divider(color = CineTimelineRuler, thickness = 1.dp)

        // Tracks List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            // Track rows
            Column(
                modifier = Modifier
                    .width(totalWidthDp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (track in tracks) {
                    TrackRow(
                        track = track,
                        pxPerSec = pxPerSec,
                        selectedClipId = selectedClipId,
                        onClipSelect = onClipSelect,
                        onTrimStart = onTrimStart,
                        onTrimEnd = onTrimEnd
                    )
                }
            }

            // Vertical Playhead Line overlay
            val playheadXDp = ((playheadMs / 1000f) * pxPerSec).dp
            Box(
                modifier = Modifier
                    .offset(x = playheadXDp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(CinePlayhead)
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / pxPerSec) * 1000f).toLong()
                            onPlayheadChange(playheadMs + deltaMs)
                        }
                    }
            ) {
                // Playhead head icon
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .offset(x = (-5).dp, y = (-2).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CinePlayhead)
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    pxPerSec: Float,
    selectedClipId: String?,
    onClipSelect: (String?) -> Unit,
    onTrimStart: (clipId: String, newStartMs: Long) -> Unit,
    onTrimEnd: (clipId: String, newDurationMs: Long) -> Unit
) {
    val trackBg = when (track.type) {
        TrackType.VIDEO -> CineSurfaceVariant
        TrackType.AUDIO -> Color(0xFF0F2620)
        TrackType.TEXT -> Color(0xFF1E1630)
        else -> CineSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(trackBg)
            .border(0.5.dp, CineTimelineRuler.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
    ) {
        // Track Name watermark
        Text(
            text = track.name.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextTertiary.copy(alpha = 0.35f),
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )

        // Clips in this track
        for (clip in track.clips) {
            val clipStartDp = ((clip.startMs / 1000f) * pxPerSec).dp
            val clipWidthDp = ((clip.durationMs / 1000f) * pxPerSec).dp.coerceAtLeast(30.dp)
            val isSelected = clip.id == selectedClipId

            ClipBlock(
                clip = clip,
                trackType = track.type,
                isSelected = isSelected,
                modifier = Modifier
                    .offset(x = clipStartDp)
                    .width(clipWidthDp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                onClick = { onClipSelect(clip.id) },
                onTrimLeft = { deltaPx ->
                    val deltaMs = ((deltaPx / pxPerSec) * 1000f).toLong()
                    onTrimStart(clip.id, clip.startMs + deltaMs)
                },
                onTrimRight = { deltaPx ->
                    val deltaMs = ((deltaPx / pxPerSec) * 1000f).toLong()
                    onTrimEnd(clip.id, clip.durationMs + deltaMs)
                }
            )
        }
    }
}

@Composable
fun ClipBlock(
    clip: Clip,
    trackType: TrackType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onTrimLeft: (Float) -> Unit,
    onTrimRight: (Float) -> Unit
) {
    val blockColor = when (trackType) {
        TrackType.VIDEO -> if (isSelected) Color(0xFF283A5A) else Color(0xFF1C273D)
        TrackType.AUDIO -> if (isSelected) Color(0xFF0F4D3C) else Color(0xFF0B3328)
        TrackType.TEXT -> if (isSelected) Color(0xFF3B275E) else Color(0xFF271A3F)
        else -> CineSurfaceHighlight
    }

    val accentColor = when (trackType) {
        TrackType.VIDEO -> CineTertiary
        TrackType.AUDIO -> CineAudioTrack
        TrackType.TEXT -> CineTextTrack
        else -> CinePrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(blockColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
    ) {
        // Content representation
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (trackType) {
                        TrackType.VIDEO -> Icons.Default.Movie
                        TrackType.AUDIO -> Icons.Default.Audiotrack
                        TrackType.TEXT -> Icons.Default.Title
                        else -> Icons.Default.Layers
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = clip.name,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Duration tag
            Text(
                text = "${clip.durationMs / 1000f}s",
                fontSize = 9.sp,
                color = CineTextTertiary
            )
        }

        // Left & Right Trim Handles when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(accentColor)
                    .pointerInput(clip.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimLeft(dragAmount.x)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(2.dp).height(12.dp).background(Color.Black))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(accentColor)
                    .pointerInput(clip.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimRight(dragAmount.x)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(2.dp).height(12.dp).background(Color.Black))
            }
        }
    }
}
