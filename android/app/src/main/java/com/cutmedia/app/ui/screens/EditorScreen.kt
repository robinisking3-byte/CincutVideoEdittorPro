package com.cutmedia.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutmedia.app.data.CutProject
import com.cutmedia.app.data.CutMediaRepository
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    repository: CutMediaRepository,
    onProjectSaved: () -> Unit
) {
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var projectTitle by remember { mutableStateOf("Cinematic Cut 01") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var selectedFilter by remember { mutableStateOf("Cinematic Gold") }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var volumePercent by remember { mutableIntStateOf(100) }
    var trimStart by remember { mutableFloatStateOf(0.0f) }
    var trimEnd by remember { mutableFloatStateOf(15.0f) }
    var totalDuration by remember { mutableFloatStateOf(30.0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Android Zero-Permission Photo/Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            projectTitle = "Clip_${System.currentTimeMillis() % 10000}"
        }
    }

    val aspectRatios = listOf("16:9", "9:16", "1:1", "4:5")
    val filters = listOf(
        "None",
        "Cinematic Gold",
        "Teal & Orange",
        "Noir B&W",
        "Warm Sunset",
        "Cool Teal",
        "Cyberpunk Neon",
        "Retro VHS",
        "Emerald Film",
        "Tokyo Night",
        "Pastel Dream",
        "Bleach Bypass",
        "Vivid Contrast"
    )
    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val filterOverlayColor = when (selectedFilter) {
            "Cinematic Gold" -> Color(0x33F59E0B)
            "Teal & Orange" -> Color(0x2E06B6D4)
            "Noir B&W" -> Color(0x4D000000)
            "Warm Sunset" -> Color(0x33F97316)
            "Cool Teal" -> Color(0x3314B8A6)
            "Cyberpunk Neon" -> Color(0x33EC4899)
            "Retro VHS" -> Color(0x33A16207)
            "Emerald Film" -> Color(0x3310B981)
            "Tokyo Night" -> Color(0x336366F1)
            "Pastel Dream" -> Color(0x29F472B6)
            "Bleach Bypass" -> Color(0x3B64748B)
            "Vivid Contrast" -> Color(0x2B3B82F6)
            else -> Color.Transparent
        }

        // Video Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (filterOverlayColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(filterOverlayColor)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = "Video Canvas",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )

                Text(
                    text = if (selectedVideoUri != null) "Loaded: ${selectedVideoUri?.lastPathSegment ?: "Media Clip"}" else "No Media Imported",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Ratio: $selectedAspectRatio | Speed: ${selectedSpeed}x | Filter: $selectedFilter",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.testTag("import_clip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Import",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Clip", fontSize = 13.sp)
                    }
                }
            }
        }

        // Project Title
        OutlinedTextField(
            value = projectTitle,
            onValueChange = { projectTitle = it },
            label = { Text("Project Title") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project_title_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Aspect Ratio Selector
        Text(
            text = "Aspect Ratio",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(aspectRatios) { ratio ->
                FilterChip(
                    selected = selectedAspectRatio == ratio,
                    onClick = { selectedAspectRatio = ratio },
                    label = { Text(ratio, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = ratio,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("ratio_chip_$ratio")
                )
            }
        }

        // Real Interactive Trim Sliders
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trim",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trim Range", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Text(
                        text = "${String.format("%.1f", trimStart)}s - ${String.format("%.1f", trimEnd)}s (${String.format("%.1f", trimEnd - trimStart)}s)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text("Start Point (s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = trimStart,
                    onValueChange = { if (it < trimEnd) trimStart = it },
                    valueRange = 0.0f..totalDuration,
                    modifier = Modifier.testTag("trim_start_slider")
                )

                Text("End Point (s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = trimEnd,
                    onValueChange = { if (it > trimStart) trimEnd = it },
                    valueRange = 0.0f..totalDuration,
                    modifier = Modifier.testTag("trim_end_slider")
                )
            }
        }

        // Real Playback Speed Chips
        Text(
            text = "Playback Speed",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            speeds.forEach { speed ->
                FilterChip(
                    selected = selectedSpeed == speed,
                    onClick = { selectedSpeed = speed },
                    label = { Text("${speed}x", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "${speed}x",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("speed_chip_$speed")
                )
            }
        }

        // Real Volume Slider
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio Volume", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Text(
                        text = "$volumePercent%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { volumePercent = it.toInt() },
                    valueRange = 0f..200f,
                    modifier = Modifier.testTag("volume_slider")
                )
            }
        }

        // Real Color Filter Presets
        Text(
            text = "Color Grading Filter",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = filter,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_chip_$filter")
                )
            }
        }

        // Save / Export Project Button
        Button(
            onClick = {
                val proj = CutProject(
                    id = UUID.randomUUID().toString(),
                    title = projectTitle.ifBlank { "Untitled Cut" },
                    durationSec = trimEnd - trimStart,
                    aspectRatio = selectedAspectRatio,
                    filterName = selectedFilter,
                    speed = selectedSpeed,
                    volumePercent = volumePercent,
                    trimStartSec = trimStart,
                    trimEndSec = trimEnd,
                    uriString = selectedVideoUri?.toString() ?: ""
                )
                repository.saveProject(proj)
                saveSuccessMessage = "Project '${proj.title}' saved to Library!"
                onProjectSaved()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_project_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save Project",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Save & Export Project",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (saveSuccessMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = saveSuccessMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
