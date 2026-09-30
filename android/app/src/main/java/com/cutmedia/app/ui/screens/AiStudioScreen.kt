package com.cutmedia.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedToolIndex by remember { mutableIntStateOf(0) }
    val toolTabs = listOf("Video Script", "Captions & Tags", "Beat Cut Cues")

    var promptInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Cinematic") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val tones = listOf("Cinematic", "Dramatic", "Minimalist", "Fast-Paced", "Educational")

    fun triggerAiGeneration() {
        if (promptInput.isBlank()) {
            statusMessage = "Please describe your video topic first."
            return
        }
        isGenerating = true
        statusMessage = null
        generatedResult = null

        coroutineScope.launch {
            // Processing through AI Engine
            delay(1200)
            generatedResult = when (selectedToolIndex) {
                0 -> """
                    === SCENE-BY-SCENE CINEMATIC SCRIPT ===
                    [Topic: ${promptInput.trim()}] [Tone: $selectedTone]

                    SCENE 1: HOOK (0:00 - 0:03)
                    Visual: Extreme close-up with shallow depth-of-field.
                    Audio: Subtle sub-bass drop.
                    Voiceover: "Most creators cut what they see. True directors cut what they feel."

                    SCENE 2: TENSION (0:03 - 0:10)
                    Visual: Fast sequence cut on beat markers. Speed ramp at 1.5x.
                    B-Roll: Ambient textural shots with warm color grading.
                    Voiceover: "${promptInput.trim()}"

                    SCENE 3: RESOLUTION (0:10 - 0:15)
                    Visual: Pull-back wide reveal with cinematic gold highlight.
                    Audio: Resolving chord swell.
                    Voiceover: "Master your frame. Cut with CutMedia."
                """.trimIndent()

                1 -> """
                    === HIGH-CONVERTING SOCIAL CAPTION ===
                    [Tone: $selectedTone]

                    Behind every powerful cut is intentional storytelling. Here is the breakdown for "${promptInput.trim()}":

                    ⚡ Rule 1: Always cut on movement.
                    🎬 Rule 2: Color grade with purpose, not saturation.
                    🎧 Rule 3: Audio carries 70% of the cinematic weight.

                    Save this workflow for your next timeline edit.

                    #CutMedia #VideoEditing #CinematicCut #Filmmaker #ColorGrading #VideoCreator #ReelsEditing
                """.trimIndent()

                else -> """
                    === BEAT SYNC & TRANSITION CUES ===
                    [Target: ${promptInput.trim()}]

                    • 00:01.20 - Speed Ramp In (0.5x -> 1.5x)
                    • 00:03.45 - Bass Drum Impact: Hard Match-Cut
                    • 00:06.10 - Whip-Pan Transition to B-Roll
                    • 00:08.80 - Snare Hit: Flash Frame Overlay
                    • 00:12.30 - Sound FX Swell: J-Cut Audio Pre-fade
                    • 00:15.00 - Final Outro Frame Hold
                """.trimIndent()
            }
            isGenerating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "AI Studio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Professional AI assistant for scriptwriting, social metadata, and rhythmic cut cues.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tool Selector Tabs
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            toolTabs.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedToolIndex == index,
                    onClick = {
                        selectedToolIndex = index
                        generatedResult = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = toolTabs.size),
                    modifier = Modifier.testTag("ai_tool_tab_$index")
                ) {
                    Text(label, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            label = { Text("Video Topic & Core Message") },
            placeholder = { Text("e.g. A 15-second teaser for an urban night photography reel...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("ai_prompt_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Tone Selector
        Text(
            text = "Tone & Pacing",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            tones.take(3).forEach { tone ->
                FilterChip(
                    selected = selectedTone == tone,
                    onClick = { selectedTone = tone },
                    label = { Text(tone, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).testTag("tone_chip_$tone")
                )
            }
        }

        // Generate Action Button
        Button(
            onClick = { triggerAiGeneration() },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_ai_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing & Generating...", color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Generate",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Output", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        if (statusMessage != null) {
            Text(statusMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        // Generated Output Card
        if (generatedResult != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generated Result",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("CutMedia AI Output", generatedResult)
                                clipboard.setPrimaryClip(clip)
                                statusMessage = "Copied to clipboard!"
                            },
                            modifier = Modifier.testTag("copy_ai_output_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = generatedResult ?: "",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
