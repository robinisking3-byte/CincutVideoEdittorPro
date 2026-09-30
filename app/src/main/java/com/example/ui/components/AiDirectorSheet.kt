package com.example.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AiDirectorCommand
import com.example.core.model.AiEditPlan
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDirectorSheet(
    onGeneratePlan: (String) -> AiEditPlan,
    onExecutePlan: (AiEditPlan) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    var currentPlan by remember { mutableStateOf<AiEditPlan?>(null) }
    var isThinking by remember { mutableStateOf(false) }

    val promptSuggestions = listOf(
        "Make this video cinematic",
        "Add dramatic transition on cuts",
        "Trim selected clip by 2 seconds",
        "Add dynamic subtitle captions",
        "Apply Cyberpunk neon color grade",
        "Set slow motion 0.5x on action shot",
        "Optimize audio levels with speech ducking"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CineSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CineTimelineRuler) },
        modifier = modifier.testTag("ai_director_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CinePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("AI Director", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary)
                }
            }

            Text(
                text = "Describe how you want to edit your video in natural language. The AI Director will compile and execute structured timeline edits.",
                fontSize = 12.sp,
                color = CineTextSecondary
            )

            // Suggestions Chips
            Text("QUICK PROMPTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (s in promptSuggestions) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(CineSurfaceVariant)
                            .border(1.dp, CineTimelineRuler, RoundedCornerShape(16.dp))
                            .clickable {
                                promptInput = s
                                val plan = onGeneratePlan(s)
                                currentPlan = plan
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(s, fontSize = 11.sp, color = CineTextPrimary)
                    }
                }
            }

            // Input field
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("e.g. Add 35mm film grain and cinematic title...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_director_input"),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                val plan = onGeneratePlan(promptInput)
                                currentPlan = plan
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Generate Plan", tint = CinePrimary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinePrimary,
                    unfocusedBorderColor = CineTimelineRuler,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Structured Plan Review
            if (currentPlan != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CineSurfaceVariant)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Structured Edit Plan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
                        Text("${currentPlan?.commands?.size ?: 0} commands", fontSize = 11.sp, color = CineTextTertiary)
                    }

                    Text(
                        text = currentPlan!!.summary,
                        fontSize = 12.sp,
                        color = CineTextPrimary
                    )

                    Divider(color = CineTimelineRuler, thickness = 0.5.dp)

                    // Individual command steps
                    currentPlan!!.commands.forEachIndexed { index, cmd ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CinePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CinePrimary)
                            }
                            Text(
                                text = "[${cmd.type.name}] ${cmd.description}",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Execute Plan Button
                    Button(
                        onClick = {
                            onExecutePlan(currentPlan!!)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_ai_plan_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Edit Plan to Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
