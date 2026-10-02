package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.example.core.model.ChromaKey
import com.example.core.model.ColorGrading
import com.example.ui.theme.*

@Composable
fun ColorGradingPanel(
    colorGrading: ColorGrading,
    chromaKey: ChromaKey,
    onColorGradingChange: (ColorGrading) -> Unit,
    onChromaKeyChange: (ChromaKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val lutPresets = listOf(
        "Normal",
        "Cinematic Gold",
        "Teal & Orange",
        "Noir B&W",
        "Cyberpunk Neon",
        "Warm Sunset",
        "Retro VHS",
        "Emerald Film",
        "Tokyo Night",
        "Pastel Dream",
        "Bleach Bypass",
        "Lomo Chrome",
        "Cold Ice",
        "Vintage Sepia",
        "Cinematic Teal & Orange",
        "Cyberpunk",
        "Noir",
        "Vintage 16mm",
        "Golden Hour"
    )

    var activeTab by remember { mutableStateOf(0) } // 0: LUT & Grading, 1: Chroma Key

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CineSurface)
            .padding(12.dp)
            .testTag("color_grading_panel")
    ) {
        // Tab selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = CinePrimary,
                modifier = Modifier.width(260.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Color Grade & LUT", fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Chroma Key", fontSize = 12.sp) }
                )
            }

            IconButton(onClick = { onColorGradingChange(ColorGrading()) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = CineTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == 0) {
            // LUT Presets Horizontal Carousel
            Text("LUT PRESETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (lut in lutPresets) {
                    val isSelected = colorGrading.lutFilter == lut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CinePrimary.copy(alpha = 0.25f) else CineSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) CinePrimary else CineTimelineRuler,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onColorGradingChange(colorGrading.copy(lutFilter = lut)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = lut,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CinePrimary else Color.White
                        )
                    }
                }
            }

            // Fine tuning sliders
            SliderControl(
                label = "Exposure",
                value = colorGrading.exposure,
                range = -2f..2f,
                onValueChange = { onColorGradingChange(colorGrading.copy(exposure = it)) }
            )
            SliderControl(
                label = "Contrast",
                value = colorGrading.contrast,
                range = 0.2f..2.0f,
                onValueChange = { onColorGradingChange(colorGrading.copy(contrast = it)) }
            )
            SliderControl(
                label = "Saturation",
                value = colorGrading.saturation,
                range = 0.0f..2.0f,
                onValueChange = { onColorGradingChange(colorGrading.copy(saturation = it)) }
            )
            SliderControl(
                label = "Temperature",
                value = colorGrading.temperature,
                range = -1.0f..1.0f,
                onValueChange = { onColorGradingChange(colorGrading.copy(temperature = it)) }
            )
        } else {
            // Chroma Key (Green Screen) Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Chroma Key", fontSize = 13.sp, color = Color.White)
                Switch(
                    checked = chromaKey.enabled,
                    onCheckedChange = { onChromaKeyChange(chromaKey.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = CineTertiary, checkedTrackColor = CineTertiary.copy(alpha = 0.3f))
                )
            }

            if (chromaKey.enabled) {
                SliderControl(
                    label = "Key Tolerance",
                    value = chromaKey.tolerance,
                    range = 0.05f..0.8f,
                    onValueChange = { onChromaKeyChange(chromaKey.copy(tolerance = it)) }
                )
                SliderControl(
                    label = "Edge Feather",
                    value = chromaKey.edgeFeather,
                    range = 0.01f..0.5f,
                    onValueChange = { onChromaKeyChange(chromaKey.copy(edgeFeather = it)) }
                )
            }
        }
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CineTextSecondary,
            modifier = Modifier.width(80.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = CineTertiary,
                activeTrackColor = CineTertiary
            )
        )
        Text(
            text = "%.2f".format(value),
            fontSize = 10.sp,
            color = CineTextTertiary,
            modifier = Modifier.width(36.dp)
        )
    }
}
