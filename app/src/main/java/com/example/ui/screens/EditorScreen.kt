package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.AiDirectorEngine
import com.example.core.engine.ExportEngine
import com.example.core.engine.TimelineController
import com.example.core.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    timelineController: TimelineController,
    onBack: () -> Unit,
    onNavigateAiCopilot: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportEngine = remember { ExportEngine(context) }
    val aiDirectorEngine = remember { AiDirectorEngine(timelineController) }

    val project by timelineController.projectState.collectAsState()
    val tracks by timelineController.tracks.collectAsState()
    val playheadMs by timelineController.playheadMs.collectAsState()
    val isPlaying by timelineController.isPlaying.collectAsState()
    val selectedClipId by timelineController.selectedClipId.collectAsState()
    val zoomScale by timelineController.zoomScale.collectAsState()
    val snappingEnabled by timelineController.snappingEnabled.collectAsState()

    val currentExportJob by exportEngine.currentJob.collectAsState()

    // Real Android Zero-Permission Photo/Video Picker with metadata extraction
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val retriever = android.media.MediaMetadataRetriever()
            var realDurationMs = 5000L
            var detectedName = "Clip_${System.currentTimeMillis() % 10000}.mp4"
            try {
                retriever.setDataSource(context, uri)
                val dur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (dur != null && dur > 0) realDurationMs = dur
                val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                if (!title.isNullOrBlank()) detectedName = title
            } catch (_: Exception) {} finally {
                try { retriever.release() } catch (_: Exception) {}
            }
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val cName = cursor.getString(nameIndex)
                            if (!cName.isNullOrBlank()) detectedName = cName
                        }
                    }
                }
            } catch (_: Exception) {}
            timelineController.addClipToTrack(TrackType.VIDEO, detectedName, durationMs = realDurationMs, mediaUri = uri.toString())
        }
    }

    // Real Audio / Music Picker from device storage
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val retriever = android.media.MediaMetadataRetriever()
            var realDurationMs = 15000L
            var audioName = "Audio_${System.currentTimeMillis() % 10000}.mp3"
            try {
                retriever.setDataSource(context, uri)
                val dur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (dur != null && dur > 0) realDurationMs = dur
                val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                if (!title.isNullOrBlank()) audioName = title
            } catch (_: Exception) {} finally {
                try { retriever.release() } catch (_: Exception) {}
            }
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val cName = cursor.getString(nameIndex)
                            if (!cName.isNullOrBlank()) audioName = cName
                        }
                    }
                }
            } catch (_: Exception) {}
            timelineController.addClipToTrack(TrackType.AUDIO, audioName, durationMs = realDurationMs, mediaUri = uri.toString())
        }
    }

    var showSafeGuides by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showAiDirectorSheet by remember { mutableStateOf(false) }
    var activeSubTool by remember { mutableStateOf("timeline") } // "timeline", "color", "keyframes", "audio", "text", "transitions", "speed"

    val selectedClip = remember(tracks, selectedClipId) {
        timelineController.getSelectedClip()
    }

    // Playback loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(33L) // ~30 fps
            val nextTime = playheadMs + 33L
            if (nextTime >= project.durationMs) {
                timelineController.setPlayhead(0L)
                timelineController.setPlaying(false)
            } else {
                timelineController.setPlayhead(nextTime)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        Text(
                            text = "${project.aspectRatio} • ${project.resolutionWidth}x${project.resolutionHeight} • ${project.frameRate}fps",
                            fontSize = 10.sp,
                            color = CineTextTertiary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Safe guides toggle
                    IconButton(onClick = { showSafeGuides = !showSafeGuides }) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Safe Guides",
                            tint = if (showSafeGuides) CineTertiary else CineTextSecondary
                        )
                    }
                    // AI Director trigger button
                    IconButton(
                        onClick = { showAiDirectorSheet = true },
                        modifier = Modifier.testTag("ai_director_toolbar_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Director", tint = CinePrimary)
                    }
                    // Export button
                    Button(
                        onClick = { showExportSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(end = 8.dp)
                            .testTag("export_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Preview Canvas
            EditorCanvas(
                project = project,
                tracks = tracks,
                playheadMs = playheadMs,
                selectedClip = selectedClip,
                showSafeGuides = showSafeGuides,
                onClipTransformChange = { zoom, rotation, dx, dy ->
                    if (selectedClip != null) {
                        timelineController.updateClip(
                            selectedClip.copy(
                                scale = (selectedClip.scale * zoom).coerceIn(0.2f, 3.0f),
                                rotation = selectedClip.rotation + rotation,
                                translationX = selectedClip.translationX + dx,
                                translationY = selectedClip.translationY + dy
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
            )

            // Timeline Transport & Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timecode
                Text(
                    text = "${formatTimecode(playheadMs)} / ${formatTimecode(project.durationMs)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Playback Transport Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { timelineController.stepFrame(false) }) {
                        Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Step Back", tint = CineTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { timelineController.togglePlayPause() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(CinePrimary)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { timelineController.stepFrame(true) }) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = "Step Forward", tint = CineTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Undo / Redo & Snapping
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { timelineController.undo() },
                        enabled = timelineController.canUndo()
                    ) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = if (timelineController.canUndo()) Color.White else CineTextTertiary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { timelineController.redo() },
                        enabled = timelineController.canRedo()
                    ) {
                        Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = if (timelineController.canRedo()) Color.White else CineTextTertiary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { timelineController.toggleSnapping() }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Snapping",
                            tint = if (snappingEnabled) CineTertiary else CineTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Timeline Tracks Area
            TimelineTrackView(
                tracks = tracks,
                playheadMs = playheadMs,
                totalDurationMs = project.durationMs,
                selectedClipId = selectedClipId,
                zoomScale = zoomScale,
                onPlayheadChange = { timelineController.setPlayhead(it) },
                onClipSelect = { timelineController.selectClip(it) },
                onTrimStart = { id, start -> timelineController.trimClipStart(id, start) },
                onTrimEnd = { id, dur -> timelineController.trimClipEnd(id, dur) },
                onToggleTrackMute = { timelineController.toggleTrackMute(it) },
                onToggleTrackLock = { timelineController.toggleTrackLock(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.36f)
            )

            // Contextual Tool Panels or Bottom Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.22f)
                    .background(CineSurface)
            ) {
                when (activeSubTool) {
                    "color" -> {
                        if (selectedClip != null) {
                            ColorGradingPanel(
                                colorGrading = selectedClip.colorGrading,
                                chromaKey = selectedClip.chromaKey,
                                onColorGradingChange = { timelineController.updateColorGrading(it) },
                                onChromaKeyChange = { timelineController.updateChromaKey(it) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a video clip to adjust colors", color = CineTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    "keyframes" -> {
                        KeyframeEditorPanel(
                            selectedClip = selectedClip,
                            playheadMs = playheadMs,
                            onAddKeyframe = { prop, v -> timelineController.addKeyframe(prop, v) },
                            onRemoveKeyframe = { id -> timelineController.removeKeyframe(id) }
                        )
                    }
                    "speed" -> {
                        SpeedEditorPanel(
                            selectedClip = selectedClip,
                            onSpeedChange = { timelineController.changeClipSpeed(it) }
                        )
                    }
                    "text" -> {
                        TextLayerPanel(
                            onAddTitle = { text -> timelineController.addTextLayer(text) }
                        )
                    }
                    "transitions" -> {
                        TransitionPanel(
                            selectedClip = selectedClip,
                            onSetTransition = { trans -> timelineController.setTransition(trans) }
                        )
                    }
                    else -> {
                        // Standard Timeline Operations Bar
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EditorActionChip(
                                icon = Icons.Default.ContentCut,
                                label = "Split",
                                enabled = selectedClip != null,
                                onClick = { timelineController.splitClipAtPlayhead() }
                            )
                            EditorActionChip(
                                icon = Icons.Default.Delete,
                                label = "Delete",
                                enabled = selectedClip != null,
                                onClick = { timelineController.deleteSelectedClip() }
                            )
                            EditorActionChip(
                                icon = Icons.Default.DeleteSweep,
                                label = "Ripple Del",
                                enabled = selectedClip != null,
                                onClick = { timelineController.rippleDeleteSelectedClip() }
                            )
                            EditorActionChip(
                                icon = Icons.Default.ContentCopy,
                                label = "Duplicate",
                                enabled = selectedClip != null,
                                onClick = { timelineController.duplicateSelectedClip() }
                            )
                            EditorActionChip(
                                icon = Icons.Default.Palette,
                                label = "Color & LUT",
                                enabled = selectedClip != null,
                                onClick = { activeSubTool = "color" }
                            )
                            EditorActionChip(
                                icon = Icons.Default.Timeline,
                                label = "Keyframes",
                                enabled = selectedClip != null,
                                onClick = { activeSubTool = "keyframes" }
                            )
                            EditorActionChip(
                                icon = Icons.Default.Speed,
                                label = "Speed",
                                enabled = selectedClip != null,
                                onClick = { activeSubTool = "speed" }
                            )
                            EditorActionChip(
                                icon = Icons.Default.Transform,
                                label = "Transition",
                                enabled = selectedClip != null,
                                onClick = { activeSubTool = "transitions" }
                            )
                            EditorActionChip(
                                icon = Icons.Default.Title,
                                label = "Add Text",
                                onClick = { activeSubTool = "text" }
                            )
                            EditorActionChip(
                                icon = Icons.Default.AddPhotoAlternate,
                                label = "Add Media",
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                            )
                            EditorActionChip(
                                icon = Icons.Default.LibraryMusic,
                                label = "Add Music",
                                onClick = {
                                    audioPickerLauncher.launch("audio/*")
                                }
                            )
                            EditorActionChip(
                                icon = Icons.Default.AutoAwesome,
                                label = "AI Copilot",
                                onClick = onNavigateAiCopilot
                            )
                        }
                    }
                }

                // Close sub-tool button if open
                if (activeSubTool != "timeline") {
                    IconButton(
                        onClick = { activeSubTool = "timeline" },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = CineTertiary)
                    }
                }
            }
        }
    }

    // Export Bottom Sheet
    if (showExportSheet) {
        ExportBottomSheet(
            currentJob = currentExportJob,
            onStartExport = { settings ->
                exportEngine.startExport(project, settings, tracks, coroutineScope)
            },
            onCancelExport = { exportEngine.cancelExport() },
            onDismiss = {
                exportEngine.clearCurrentJob()
                showExportSheet = false
            },
            onOpenGallery = {
                currentExportJob?.let { exportEngine.openInGallery(it) }
            },
            onShareVideo = {
                currentExportJob?.let { exportEngine.shareExportedVideo(it) }
            }
        )
    }

    // AI Director Sheet
    if (showAiDirectorSheet) {
        AiDirectorSheet(
            onGeneratePlan = { prompt -> aiDirectorEngine.generateEditPlan(prompt) },
            onExecutePlan = { plan -> aiDirectorEngine.executePlan(plan) },
            onDismiss = { showAiDirectorSheet = false }
        )
    }
}

@Composable
fun EditorActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) CineSurfaceVariant else CineSurfaceVariant.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) CinePrimary else CineTextTertiary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (enabled) Color.White else CineTextTertiary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun KeyframeEditorPanel(
    selectedClip: Clip?,
    playheadMs: Long,
    onAddKeyframe: (property: String, value: Float) -> Unit,
    onRemoveKeyframe: (keyframeId: String) -> Unit
) {
    if (selectedClip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a clip to animate keyframes", color = CineTextSecondary, fontSize = 12.sp)
        }
        return
    }

    var selectedProp by remember { mutableStateOf("scale") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("KEYFRAME ANIMATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
            Button(
                onClick = {
                    val value = when (selectedProp) {
                        "scale" -> selectedClip.scale
                        "opacity" -> selectedClip.opacity
                        "rotation" -> selectedClip.rotation
                        else -> 1.0f
                    }
                    onAddKeyframe(selectedProp, value)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CineTertiary),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Keyframe", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Property chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("scale", "opacity", "rotation", "position_x").forEach { prop ->
                FilterChip(
                    selected = selectedProp == prop,
                    onClick = { selectedProp = prop },
                    label = { Text(prop.replace("_", " ").uppercase(), fontSize = 10.sp) }
                )
            }
        }

        // List active keyframes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedClip.keyframes.forEach { kf ->
                InputChip(
                    selected = true,
                    onClick = { onRemoveKeyframe(kf.id) },
                    label = { Text("${kf.property}: ${"%.2f".format(kf.value)} @ ${kf.timeMs}ms", fontSize = 10.sp) },
                    trailingIcon = { Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(12.dp)) }
                )
            }
        }
    }
}

@Composable
fun SpeedEditorPanel(
    selectedClip: Clip?,
    onSpeedChange: (Float) -> Unit
) {
    if (selectedClip == null) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("SPEED & DURATION: ${selectedClip.speed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 4.0f).forEach { spd ->
                val isSelected = Math.abs(selectedClip.speed - spd) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CineSecondary else CineSurfaceVariant)
                        .clickable { onSpeedChange(spd) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${spd}x",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TextLayerPanel(
    onAddTitle: (String) -> Unit
) {
    var titleText by remember { mutableStateOf("CINECUT TITLE") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ADD TEXT OVERLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTrack)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { onAddTitle(titleText) },
                colors = ButtonDefaults.buttonColors(containerColor = CineTextTrack)
            ) {
                Text("Insert")
            }
        }
    }
}

@Composable
fun TransitionPanel(
    selectedClip: Clip?,
    onSetTransition: (TransitionType) -> Unit
) {
    if (selectedClip == null) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("TRANSITIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransitionType.values().forEach { trans ->
                val isSelected = selectedClip.transitionIn == trans
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CineTertiary else CineSurfaceVariant)
                        .clickable { onSetTransition(trans) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = trans.name.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
