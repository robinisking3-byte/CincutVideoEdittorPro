package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.TimelineController
import com.example.core.model.TrackType
import com.example.core.network.AiChatMessage
import com.example.core.network.GeneratedCaption
import com.example.core.network.GroqAiService
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AiTabMode {
    CHAT,
    DIRECTOR,
    CAPTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDirectorChatScreen(
    repository: CineCutRepository,
    timelineController: TimelineController,
    onBack: () -> Unit,
    onNavigateEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groqService = remember { GroqAiService(context) }

    val project by timelineController.projectState.collectAsState()
    val tracks by timelineController.tracks.collectAsState()
    val totalClips = remember(tracks) { tracks.flatMap { it.clips }.size }

    var activeTab by remember { mutableStateOf(AiTabMode.CHAT) }

    // Chat State
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                AiChatMessage(
                    role = "assistant",
                    content = "Hello! I am CineCut AI, your creative director and video editing copilot powered by high-speed Groq AI. Ask me for filmmaking advice, pacing ideas, color grading recipes, or let's brainstorm viral hooks!"
                )
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Director State
    var directorOutput by remember { mutableStateOf<String?>(null) }
    var directorQuestion by remember { mutableStateOf("") }

    // Captions State
    var captionTopic by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Viral / Energetic") }
    var generatedCaptions by remember { mutableStateOf<List<GeneratedCaption>>(emptyList()) }
    var viralTitle by remember { mutableStateOf<String?>(null) }
    var viralHashtags by remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "CineCut AI Copilot",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Groq High-Speed Intelligence",
                                fontSize = 11.sp,
                                color = CineTertiary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            chatMessages = listOf(
                                AiChatMessage(
                                    role = "assistant",
                                    content = "Chat cleared! How can I assist your filmmaking workflow today?"
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = CineTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("ai_director_chat_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Selector Tabs
            PrimaryTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = CineSurface,
                contentColor = CinePrimary
            ) {
                Tab(
                    selected = activeTab == AiTabMode.CHAT,
                    onClick = { activeTab = AiTabMode.CHAT },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("AI Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == AiTabMode.DIRECTOR,
                    onClick = { activeTab = AiTabMode.DIRECTOR },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("AI Director", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == AiTabMode.CAPTIONS,
                    onClick = { activeTab = AiTabMode.CAPTIONS },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Captions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = CineError.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CineError, modifier = Modifier.size(16.dp))
                        Text(errorMessage.orEmpty(), fontSize = 11.sp, color = CineError, modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = CineError, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            when (activeTab) {
                AiTabMode.CHAT -> {
                    // Chat message list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatMessages) { msg ->
                            val isMe = msg.role == "user"
                            val bubbleColor = if (isMe) CinePrimary else CineSurface
                            val alignment = if (isMe) Alignment.End else Alignment.Start

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    if (!isMe) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                                        Text("CineCut AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                    } else {
                                        Text("You", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextSecondary)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 320.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(bubbleColor)
                                        .border(1.dp, if (isMe) CinePrimary else CineTimelineRuler, RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = msg.content,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                }

                                if (!isMe) {
                                    Row(
                                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("CineCut AI Response", msg.content))
                                                Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CineTextSecondary, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy", fontSize = 10.sp, color = CineTextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        if (isGenerating) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CineTertiary, strokeWidth = 2.dp)
                                    Text("Groq AI is thinking...", fontSize = 12.sp, color = CineTertiary)
                                }
                            }
                        }
                    }

                    // Quick Prompt Suggestions Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CineSurface.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prompts = listOf(
                            "Best Teal & Orange settings?",
                            "How to pace a cinematic trailer?",
                            "Give me 5 viral Reel hooks",
                            "Sound effects for tension?",
                            "Match cut vs J-cut explained"
                        )
                        items(prompts) { p ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CineSurfaceVariant,
                                modifier = Modifier.clickable {
                                    inputText = p
                                }
                            ) {
                                Text(
                                    text = p,
                                    fontSize = 11.sp,
                                    color = CineTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CineSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask CineCut AI anything...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    val userMsg = inputText.trim()
                                    inputText = ""
                                    chatMessages = chatMessages + AiChatMessage(role = "user", content = userMsg)
                                    isGenerating = true
                                    errorMessage = null

                                    scope.launch {
                                        val result = groqService.chatWithAi(chatMessages, userMsg)
                                        isGenerating = false
                                        result.fold(
                                            onSuccess = { reply ->
                                                chatMessages = chatMessages + AiChatMessage(role = "assistant", content = reply)
                                                scope.launch {
                                                    listState.animateScrollToItem(chatMessages.size - 1)
                                                }
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.localizedMessage ?: "AI generation failed."
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !isGenerating,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) CinePrimary else CineSurfaceVariant)
                                .testTag("ai_send_message_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }

                AiTabMode.DIRECTOR -> {
                    // AI Director Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Project Overview Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CineSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = CinePrimary)
                                        Text(
                                            "Active Project: ${project.title}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration: ${project.durationMs / 1000}s", fontSize = 12.sp, color = CineTextSecondary)
                                        Text("Timeline Clips: $totalClips", fontSize = 12.sp, color = CineTextSecondary)
                                        Text("Aspect: ${project.aspectRatio}", fontSize = 12.sp, color = CineTertiary)
                                    }
                                }
                            }
                        }

                        // Query Input
                        item {
                            OutlinedTextField(
                                value = directorQuestion,
                                onValueChange = { directorQuestion = it },
                                label = { Text("Specific direction or question (optional)") },
                                placeholder = { Text("e.g. How can I make the opening hook punchier?") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CinePrimary,
                                    unfocusedBorderColor = CineTimelineRuler,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        // Generate Button
                        item {
                            Button(
                                onClick = {
                                    isGenerating = true
                                    errorMessage = null
                                    scope.launch {
                                        val res = groqService.getDirectorAdvice(
                                            projectTitle = project.title,
                                            durationMs = project.durationMs,
                                            clipCount = totalClips,
                                            aspectRatio = project.aspectRatio,
                                            userQuery = directorQuestion
                                        )
                                        isGenerating = false
                                        res.fold(
                                            onSuccess = { directorOutput = it },
                                            onFailure = { errorMessage = it.localizedMessage ?: "Director analysis failed." }
                                        )
                                    }
                                },
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Analyze Project with AI Director", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Analysis Result Card
                        if (directorOutput != null) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CineSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Director's Cut Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI Director Report", directorOutput))
                                                    Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CineTextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(
                                            text = directorOutput.orEmpty(),
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AiTabMode.CAPTIONS -> {
                    // AI Captions & Subtitles Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                "AI Smart Subtitles & Viral Captions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Generate synced timestamped captions and trending hashtags in seconds using Groq AI.",
                                fontSize = 12.sp,
                                color = CineTextSecondary
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = captionTopic,
                                onValueChange = { captionTopic = it },
                                label = { Text("Video Topic or Spoken Script") },
                                placeholder = { Text("e.g. 3 secret camera tricks every smartphone filmmaker should know") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CinePrimary,
                                    unfocusedBorderColor = CineTimelineRuler,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        item {
                            Text("Select Tone / Style:", fontSize = 12.sp, color = CineTextTertiary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Viral / Energetic", "Cinematic / Drama", "Educational", "Minimalist").forEach { tone ->
                                    val isSelected = selectedTone == tone
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CinePrimary else CineSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CinePrimary else CineTimelineRuler),
                                        modifier = Modifier.clickable { selectedTone = tone }
                                    ) {
                                        Text(
                                            text = tone,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    if (captionTopic.isNotBlank()) {
                                        isGenerating = true
                                        errorMessage = null
                                        scope.launch {
                                            val captionsRes = groqService.generateCaptions(captionTopic, selectedTone)
                                            val titleRes = groqService.generateTitleAndHashtags(captionTopic)
                                            isGenerating = false

                                            captionsRes.fold(
                                                onSuccess = { generatedCaptions = it },
                                                onFailure = { errorMessage = it.localizedMessage ?: "Captions generation failed." }
                                            )
                                            titleRes.onSuccess { (t, tags) ->
                                                viralTitle = t
                                                viralHashtags = tags
                                            }
                                        }
                                    }
                                },
                                enabled = captionTopic.isNotBlank() && !isGenerating,
                                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate Synced Captions & Tags", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Generated Title & Tags
                        if (viralTitle != null) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CineSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Generated Viral Title:", fontSize = 11.sp, color = CineTertiary, fontWeight = FontWeight.Bold)
                                        Text(viralTitle.orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                            viralHashtags.forEach { tag ->
                                                Text(tag, fontSize = 11.sp, color = CineTertiary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Generated Subtitle Lines
                        if (generatedCaptions.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Generated Timed Subtitles (${generatedCaptions.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    TextButton(
                                        onClick = {
                                            // Apply first caption directly as a text clip on timeline!
                                            val first = generatedCaptions.firstOrNull()
                                            if (first != null) {
                                                timelineController.addClipToTrack(
                                                    trackType = TrackType.TEXT,
                                                    name = first.text.take(15),
                                                    durationMs = 3000L
                                                )
                                                Toast.makeText(context, "Added caption to Timeline Text track!", Toast.LENGTH_SHORT).show()
                                                onNavigateEditor()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.AddToPhotos, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add to Timeline", fontSize = 11.sp, color = CineTertiary)
                                    }
                                }
                            }

                            items(generatedCaptions) { cap ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CineSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CinePrimary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = cap.timestampFormatted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CinePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(cap.text, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
