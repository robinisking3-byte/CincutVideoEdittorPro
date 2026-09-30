package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ExportJob
import com.example.core.model.ExportResolution
import com.example.core.model.ExportSettings
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    currentJob: ExportJob?,
    onStartExport: (ExportSettings) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRes by remember { mutableStateOf(ExportResolution.FHD_1080P) }
    var selectedFps by remember { mutableStateOf(30) }
    var selectedCodec by remember { mutableStateOf("H.264 / AVC") }
    var bitrateMbps by remember { mutableStateOf(16f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CineSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CineTimelineRuler) },
        modifier = modifier.testTag("export_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Export Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary)
                }
            }

            if (currentJob != null && currentJob.status == "Exporting") {
                // Active export in progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CineSurfaceVariant)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Rendering Timeline & Color Grading...",
                        fontSize = 13.sp,
                        color = CineTextPrimary
                    )
                    LinearProgressIndicator(
                        progress = { currentJob.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CinePrimary,
                        trackColor = CineTimelineRuler
                    )
                    Text(
                        text = "${(currentJob.progress * 100).toInt()}% Complete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineSecondary
                    )

                    OutlinedButton(
                        onClick = onCancelExport,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineError),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Export")
                    }
                }
            } else if (currentJob != null && currentJob.status == "Completed") {
                // Completed state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D2818))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CineSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Export Finished Successfully!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Saved to Gallery • ${currentJob.outputPath.substringAfterLast("/")}",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            } else {
                // Resolution Selector
                Text("RESOLUTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportResolution.values().forEach { res ->
                        val isSelected = selectedRes == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CinePrimary.copy(alpha = 0.2f) else CineSurfaceVariant)
                                .border(1.dp, if (isSelected) CinePrimary else CineTimelineRuler, RoundedCornerShape(8.dp))
                                .clickable { selectedRes = res }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res.label.split(" ").first(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CinePrimary else Color.White
                            )
                        }
                    }
                }

                // Frame rate
                Text("FRAME RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24, 30, 60).forEach { fps ->
                        val isSelected = selectedFps == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CineTertiary.copy(alpha = 0.2f) else CineSurfaceVariant)
                                .border(1.dp, if (isSelected) CineTertiary else CineTimelineRuler, RoundedCornerShape(8.dp))
                                .clickable { selectedFps = fps }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${fps} FPS",
                                fontSize = 12.sp,
                                color = if (isSelected) CineTertiary else Color.White
                            )
                        }
                    }
                }

                // Codec selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Codec: $selectedCodec", fontSize = 12.sp, color = CineTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("H.264 / AVC", "H.265 / HEVC").forEach { c ->
                            FilterChip(
                                selected = selectedCodec == c,
                                onClick = { selectedCodec = c },
                                label = { Text(c.split(" ").first(), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Start Export Button
                Button(
                    onClick = {
                        onStartExport(
                            ExportSettings(
                                resolution = selectedRes,
                                frameRate = selectedFps,
                                bitrateMbps = bitrateMbps,
                                codec = selectedCodec
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_export_button")
                ) {
                    Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Render & Export (${selectedRes.label})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
