package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.components.M3u8PlayerView
import com.example.ui.components.isValidM3u8Url
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentSection(
    repository: CineCutRepository,
    posts: List<Post>,
    currentAdminRole: AdminRole,
    onModeratePost: (postId: String, action: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val m3u8Videos by repository.m3u8Videos.collectAsState()
    val m3u8LiveStreams by repository.m3u8LiveStreams.collectAsState()
    val categories by repository.contentCategories.collectAsState()

    var primaryTab by remember { mutableIntStateOf(0) } // 0: Videos, 1: Live Streams, 2: Categories, 3: User Post Queue
    var videoSubFilter by remember { mutableStateOf("PUBLISHED") } // "ADD", "PUBLISHED", "DRAFTS", "DELETED"
    var liveSubFilter by remember { mutableStateOf("SCHEDULED") } // "ADD", "SCHEDULED", "LIVE", "ENDED"

    // Edit Modals State
    var editingVideo by remember { mutableStateOf<M3u8Video?>(null) }
    var editingLiveStream by remember { mutableStateOf<M3u8LiveStream?>(null) }

    // Preview Player Modal State
    var previewM3u8Url by remember { mutableStateOf<String?>(null) }
    var previewTitle by remember { mutableStateOf("") }
    var previewIsLive by remember { mutableStateOf(false) }

    // Feedback Toast
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hierarchical Admin Breadcrumb Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(16.dp))
                Text("Admin Panel → Content", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CineTertiary)
            }
            Text("Content Management", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Authoritative HLS / M3U8 video ingestion, live broadcasts, and taxonomy control.",
                fontSize = 12.sp,
                color = CineTextSecondary
            )
        }

        // Top Primary Navigation Tabs (Videos, Live Streams, Categories / Tags)
        TabRow(
            selectedTabIndex = primaryTab,
            containerColor = CineSurface,
            contentColor = CinePrimary
        ) {
            Tab(
                selected = primaryTab == 0,
                onClick = { primaryTab = 0 },
                text = { Text("Videos (${m3u8Videos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = primaryTab == 1,
                onClick = { primaryTab = 1 },
                text = { Text("Live Streams (${m3u8LiveStreams.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = primaryTab == 2,
                onClick = { primaryTab = 2 },
                text = { Text("Categories / Tags (${categories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = primaryTab == 3,
                onClick = { primaryTab = 3 },
                text = { Text("Posts Queue", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Security Policy Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CineSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Admin-Only M3U8 Authority: Normal user video/stream uploads are completely disabled on client & backend.",
                    fontSize = 10.sp,
                    color = CineTextSecondary
                )
            }
        }

        // ================= TAB 0: M3U8 VIDEOS ================= //
        if (primaryTab == 0) {
            // Sub-filter Carousel
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = videoSubFilter == "ADD",
                        onClick = { videoSubFilter = "ADD" },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Add M3U8 Video", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
                item {
                    val count = m3u8Videos.count { it.status == VideoPublishStatus.PUBLISHED }
                    FilterChip(
                        selected = videoSubFilter == "PUBLISHED",
                        onClick = { videoSubFilter = "PUBLISHED" },
                        label = { Text("Published ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
                item {
                    val count = m3u8Videos.count { it.status == VideoPublishStatus.DRAFT }
                    FilterChip(
                        selected = videoSubFilter == "DRAFTS",
                        onClick = { videoSubFilter = "DRAFTS" },
                        label = { Text("Drafts ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
                item {
                    val count = m3u8Videos.count { it.status == VideoPublishStatus.DELETED }
                    FilterChip(
                        selected = videoSubFilter == "DELETED",
                        onClick = { videoSubFilter = "DELETED" },
                        label = { Text("Deleted ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
            }

            if (videoSubFilter == "ADD") {
                AddM3u8VideoForm(
                    categories = categories,
                    onPublish = { title, desc, url, thumb, cat, tags, vis, feat, status ->
                        val success = repository.adminAddM3u8Video(title, desc, url, thumb, cat, tags, vis, feat, status)
                        if (success) {
                            feedbackMessage = "Video published successfully via backend authoritative M3U8 pipeline!"
                            videoSubFilter = "PUBLISHED"
                        }
                    },
                    onPreviewStream = { url, title ->
                        previewM3u8Url = url
                        previewTitle = title
                        previewIsLive = false
                    }
                )
            } else {
                val statusTarget = when (videoSubFilter) {
                    "DRAFTS" -> VideoPublishStatus.DRAFT
                    "DELETED" -> VideoPublishStatus.DELETED
                    else -> VideoPublishStatus.PUBLISHED
                }
                val filtered = m3u8Videos.filter { it.status == statusTarget }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No videos in $videoSubFilter state.", color = CineTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { video ->
                            M3u8VideoAdminCard(
                                video = video,
                                onPreview = {
                                    previewM3u8Url = video.m3u8Url
                                    previewTitle = video.title
                                    previewIsLive = false
                                },
                                onEdit = {
                                    editingVideo = video
                                },
                                onToggleStatus = { nextStatus ->
                                    repository.adminSetVideoStatus(video.id, nextStatus)
                                    feedbackMessage = "Updated video status to ${nextStatus.name}"
                                },
                                onDelete = { permanent ->
                                    repository.adminDeleteM3u8Video(video.id, permanent)
                                    feedbackMessage = if (permanent) "Video permanently purged" else "Video moved to Deleted"
                                },
                                onRestore = {
                                    repository.adminSetVideoStatus(video.id, VideoPublishStatus.DRAFT)
                                    feedbackMessage = "Video restored to Drafts"
                                },
                                onToggleFeatured = {
                                    repository.adminUpdateM3u8Video(video.copy(isFeatured = !video.isFeatured))
                                    feedbackMessage = "Featured status updated"
                                }
                            )
                        }
                    }
                }
            }
        }

        // ================= TAB 1: M3U8 LIVE STREAMS ================= //
        if (primaryTab == 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = liveSubFilter == "ADD",
                        onClick = { liveSubFilter = "ADD" },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Add M3U8 Live Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
                item {
                    val count = m3u8LiveStreams.count { it.status == LiveStreamStatus.SCHEDULED }
                    FilterChip(
                        selected = liveSubFilter == "SCHEDULED",
                        onClick = { liveSubFilter = "SCHEDULED" },
                        label = { Text("Scheduled ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
                item {
                    val count = m3u8LiveStreams.count { it.status == LiveStreamStatus.LIVE }
                    FilterChip(
                        selected = liveSubFilter == "LIVE",
                        onClick = { liveSubFilter = "LIVE" },
                        label = { Text("Live Now ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
                item {
                    val count = m3u8LiveStreams.count { it.status == LiveStreamStatus.ENDED }
                    FilterChip(
                        selected = liveSubFilter == "ENDED",
                        onClick = { liveSubFilter = "ENDED" },
                        label = { Text("Ended ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
            }

            if (liveSubFilter == "ADD") {
                AddM3u8LiveStreamForm(
                    categories = categories,
                    onPublishStream = { title, desc, url, thumb, cat, tags, sched, status, feat ->
                        val success = repository.adminAddM3u8LiveStream(title, desc, url, thumb, cat, tags, sched, status, feat)
                        if (success) {
                            feedbackMessage = "M3U8 Live Broadcast configured and saved to registry!"
                            liveSubFilter = if (status == LiveStreamStatus.LIVE) "LIVE" else "SCHEDULED"
                        }
                    },
                    onPreviewStream = { url, title ->
                        previewM3u8Url = url
                        previewTitle = title
                        previewIsLive = true
                    }
                )
            } else {
                val targetStatus = when (liveSubFilter) {
                    "SCHEDULED" -> LiveStreamStatus.SCHEDULED
                    "ENDED" -> LiveStreamStatus.ENDED
                    else -> LiveStreamStatus.LIVE
                }
                val filtered = m3u8LiveStreams.filter { it.status == targetStatus }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No live streams in $liveSubFilter state.", color = CineTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { stream ->
                            M3u8LiveStreamAdminCard(
                                stream = stream,
                                onPreview = {
                                    previewM3u8Url = stream.m3u8Url
                                    previewTitle = stream.title
                                    previewIsLive = true
                                },
                                onEdit = {
                                    editingLiveStream = stream
                                },
                                onUpdateStatus = { next ->
                                    repository.adminUpdateLiveStreamStatus(stream.id, next)
                                    feedbackMessage = "Stream transitioned to ${next.name}"
                                },
                                onUnpublish = {
                                    repository.adminUnpublishLiveStream(stream.id)
                                    feedbackMessage = "Live stream unpublished"
                                },
                                onDelete = {
                                    repository.adminDeleteLiveStream(stream.id)
                                    feedbackMessage = "Live stream removed"
                                }
                            )
                        }
                    }
                }
            }
        }

        // ================= TAB 2: CATEGORIES & TAGS ================= //
        if (primaryTab == 2) {
            AdminCategoriesSection(
                categories = categories,
                onAddCategory = { name, desc ->
                    repository.adminAddCategory(name, desc)
                    feedbackMessage = "Category '$name' created."
                },
                onDeleteCategory = { id ->
                    repository.adminDeleteCategory(id)
                    feedbackMessage = "Category removed."
                }
            )
        }

        // ================= TAB 3: USER POSTS QUEUE ================= //
        if (primaryTab == 3) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("COMMUNITY DISCUSSION & COMMENT MODERATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                }
                items(posts) { post ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CineSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${post.authorName} (${post.authorHandle})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Status: ${post.moderationStatus}", fontSize = 10.sp, color = CineTertiary)
                            }
                            Text(post.content, fontSize = 12.sp, color = CineTextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onModeratePost(post.id, "RESTRICT", "Content restricted by admin") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineWarning),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Restrict", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { onModeratePost(post.id, "REMOVE", "Content removed by admin") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineError),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Remove", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Player Preview Dialog
    if (previewM3u8Url != null) {
        Dialog(onDismissRequest = { previewM3u8Url = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ADMIN M3U8 STREAM PREVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                            Text(previewTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }
                        IconButton(onClick = { previewM3u8Url = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary)
                        }
                    }

                    // Media3 Player Component
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        M3u8PlayerView(
                            m3u8Url = previewM3u8Url!!,
                            isLiveStream = previewIsLive,
                            autoPlay = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "Source: ${previewM3u8Url!!}",
                        fontSize = 10.sp,
                        color = CineTertiary,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Modal Edit M3U8 Video Dialog
    if (editingVideo != null) {
        EditM3u8VideoDialog(
            video = editingVideo!!,
            categories = categories,
            onDismiss = { editingVideo = null },
            onSave = { updated ->
                repository.adminUpdateM3u8Video(updated)
                feedbackMessage = "Video '${updated.title}' updated successfully."
                editingVideo = null
            },
            onPreviewStream = { url, title ->
                previewM3u8Url = url
                previewTitle = title
                previewIsLive = false
            }
        )
    }

    // Modal Edit M3U8 Live Stream Dialog
    if (editingLiveStream != null) {
        EditM3u8LiveStreamDialog(
            stream = editingLiveStream!!,
            categories = categories,
            onDismiss = { editingLiveStream = null },
            onSave = { updated ->
                repository.adminUpdateM3u8LiveStream(updated)
                feedbackMessage = "Live stream '${updated.title}' updated successfully."
                editingLiveStream = null
            },
            onPreviewStream = { url, title ->
                previewM3u8Url = url
                previewTitle = title
                previewIsLive = true
            }
        )
    }

    // Feedback Toast
    if (feedbackMessage != null) {
        LaunchedEffect(feedbackMessage) {
            kotlinx.coroutines.delay(2500)
            feedbackMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CineSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary)
            ) {
                Text(feedbackMessage!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

/**
 * Form to configure and publish an M3U8 video with live stream preview.
 */
@Composable
fun AddM3u8VideoForm(
    categories: List<ContentCategory>,
    onPublish: (title: String, desc: String, url: String, thumb: String, cat: String, tags: List<String>, vis: String, feat: Boolean, status: VideoPublishStatus) -> Unit,
    onPreviewStream: (url: String, title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var m3u8Url by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Cinematography") }
    var tagsInput by remember { mutableStateOf("4K, Cinematic, Master") }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var isFeatured by remember { mutableStateOf(false) }

    val isValid = isValidM3u8Url(m3u8Url)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("M3U8 VIDEO INGESTION & PUBLISHING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        item {
            OutlinedTextField(
                value = m3u8Url,
                onValueChange = { m3u8Url = it },
                label = { Text("HLS Stream URL (.m3u8)") },
                placeholder = { Text("https://example.com/hls/master.m3u8") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (m3u8Url.isNotBlank()) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isValid) CineSuccess else CineError
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValid) CinePrimary else CineError,
                    unfocusedBorderColor = CineTimelineRuler,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            if (m3u8Url.isNotBlank() && !isValid) {
                Text(
                    text = "URL must start with http(s):// and contain '.m3u8'",
                    fontSize = 10.sp,
                    color = CineError,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        m3u8Url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                        if (title.isBlank()) title = "Tears of Steel 4K HLS Master"
                        if (description.isBlank()) description = "ACES Color graded open-source master film."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Sample 4K Stream", fontSize = 10.sp)
                }

                Button(
                    onClick = {
                        m3u8Url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                        if (title.isBlank()) title = "Big Buck Bunny 1080p60 HLS"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Sample Multi-Bitrate", fontSize = 10.sp)
                }

                if (isValid) {
                    Button(
                        onClick = { onPreviewStream(m3u8Url, if (title.isNotBlank()) title else "Stream Preview") },
                        colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview Stream", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinePrimary, unfocusedBorderColor = CineTimelineRuler, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Production Notes") },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinePrimary, unfocusedBorderColor = CineTimelineRuler, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            OutlinedTextField(
                value = thumbnailUrl,
                onValueChange = { thumbnailUrl = it },
                label = { Text("Poster Image / Thumbnail URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinePrimary, unfocusedBorderColor = CineTimelineRuler, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            Text("Category:", fontSize = 11.sp, color = CineTextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name },
                        label = { Text(cat.name, fontSize = 10.sp) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinePrimary, unfocusedBorderColor = CineTimelineRuler, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Featured Content Status", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Pin this video to the top of creator home recommendations", fontSize = 10.sp, color = CineTextSecondary)
                }
                Switch(
                    checked = isFeatured,
                    onCheckedChange = { isFeatured = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CinePrimary)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onPublish(title, description, m3u8Url, thumbnailUrl, selectedCategory, tags, visibility, isFeatured, VideoPublishStatus.PUBLISHED)
                    },
                    enabled = isValid && title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Publish, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish M3U8 Video")
                }

                OutlinedButton(
                    onClick = {
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onPublish(title, description, m3u8Url, thumbnailUrl, selectedCategory, tags, visibility, isFeatured, VideoPublishStatus.DRAFT)
                    },
                    enabled = isValid && title.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Draft")
                }
            }
        }
    }
}

/**
 * Form to configure and start/schedule an M3U8 live stream.
 */
@Composable
fun AddM3u8LiveStreamForm(
    categories: List<ContentCategory>,
    onPublishStream: (title: String, desc: String, url: String, thumb: String, cat: String, tags: List<String>, sched: Long, status: LiveStreamStatus, feat: Boolean) -> Unit,
    onPreviewStream: (url: String, title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var m3u8Url by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Masterclass") }
    var streamStatus by remember { mutableStateOf(LiveStreamStatus.LIVE) }
    var isFeatured by remember { mutableStateOf(true) }

    val isValid = isValidM3u8Url(m3u8Url)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("CONFIGURE M3U8 LIVE STREAM BROADCAST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        item {
            OutlinedTextField(
                value = m3u8Url,
                onValueChange = { m3u8Url = it },
                label = { Text("Live HLS URL (.m3u8)") },
                placeholder = { Text("https://live.example.com/hls/broadcast.m3u8") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (m3u8Url.isNotBlank()) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isValid) CineSuccess else CineError
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValid) CinePrimary else CineError,
                    unfocusedBorderColor = CineTimelineRuler,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        m3u8Url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                        if (title.isBlank()) title = "🔴 Live Masterclass: 4K Color Grading Workflows"
                        if (description.isBlank()) description = "Broadcast director session with live chat and viewer interaction."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Sample Live Stream", fontSize = 10.sp)
                }

                if (isValid) {
                    Button(
                        onClick = { onPreviewStream(m3u8Url, if (title.isNotBlank()) title else "Live Preview") },
                        colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview Stream", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Stream Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinePrimary, unfocusedBorderColor = CineTimelineRuler, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Stream Description") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinePrimary, unfocusedBorderColor = CineTimelineRuler, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            Text("Broadcast Mode / Initial Status:", fontSize = 11.sp, color = CineTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(LiveStreamStatus.LIVE to "Go Live Immediately", LiveStreamStatus.SCHEDULED to "Schedule for Later").forEach { (st, label) ->
                    FilterChip(
                        selected = streamStatus == st,
                        onClick = { streamStatus = st },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    onPublishStream(title, description, m3u8Url, thumbnailUrl, selectedCategory, listOf("Live", "Masterclass"), System.currentTimeMillis(), streamStatus, isFeatured)
                },
                enabled = isValid && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (streamStatus == LiveStreamStatus.LIVE) CinePrimary else CineTertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (streamStatus == LiveStreamStatus.LIVE) Icons.Default.Sensors else Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (streamStatus == LiveStreamStatus.LIVE) "Launch Live Stream Now" else "Save Scheduled Stream")
            }
        }
    }
}

/**
 * Card displaying an M3U8 video with administrative actions.
 */
@Composable
fun M3u8VideoAdminCard(
    video: M3u8Video,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: (VideoPublishStatus) -> Unit,
    onDelete: (permanent: Boolean) -> Unit,
    onRestore: (() -> Unit)? = null,
    onToggleFeatured: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (video.isFeatured) CineSecondary.copy(alpha = 0.6f) else CineTimelineRuler
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (video.status) {
                            VideoPublishStatus.PUBLISHED -> CineSuccess.copy(alpha = 0.2f)
                            VideoPublishStatus.DRAFT -> CineWarning.copy(alpha = 0.2f)
                            VideoPublishStatus.DELETED -> CineError.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = video.status.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (video.status) {
                                VideoPublishStatus.PUBLISHED -> CineSuccess
                                VideoPublishStatus.DRAFT -> CineWarning
                                VideoPublishStatus.DELETED -> CineError
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (video.isFeatured) {
                        Surface(shape = RoundedCornerShape(4.dp), color = CineSecondary.copy(alpha = 0.2f)) {
                            Text("★ FEATURED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CineSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(video.resolutionLabel, fontSize = 10.sp, color = CineTextTertiary)
                }

                Text(video.category, fontSize = 11.sp, color = CineTertiary, fontWeight = FontWeight.Bold)
            }

            Text(video.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(video.description, fontSize = 11.sp, color = CineTextSecondary, maxLines = 2)

            Text(
                text = "HLS: ${video.m3u8Url}",
                fontSize = 10.sp,
                color = CineTextTertiary,
                maxLines = 1
            )

            Divider(color = CineTimelineRuler, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👁 %,d views • 👍 %,d likes".format(video.viewsCount, video.likesCount),
                    fontSize = 10.sp,
                    color = CineTextTertiary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Preview", tint = CineSecondary)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Video", tint = Color.White)
                    }

                    IconButton(onClick = onToggleFeatured, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (video.isFeatured) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Feature",
                            tint = if (video.isFeatured) CineSecondary else CineTextSecondary
                        )
                    }

                    if (video.status == VideoPublishStatus.PUBLISHED) {
                        Button(
                            onClick = { onToggleStatus(VideoPublishStatus.DRAFT) },
                            colors = ButtonDefaults.buttonColors(containerColor = CineSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Unpublish", fontSize = 10.sp)
                        }
                    } else if (video.status == VideoPublishStatus.DRAFT) {
                        Button(
                            onClick = { onToggleStatus(VideoPublishStatus.PUBLISHED) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Publish", fontSize = 10.sp)
                        }
                    } else if (video.status == VideoPublishStatus.DELETED && onRestore != null) {
                        Button(
                            onClick = onRestore,
                            colors = ButtonDefaults.buttonColors(containerColor = CineTertiary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Restore", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { onDelete(video.status == VideoPublishStatus.DELETED) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (video.status == VideoPublishStatus.DELETED) Icons.Default.DeleteForever else Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = CineError
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying an M3U8 Live Stream with broadcast controls.
 */
@Composable
fun M3u8LiveStreamAdminCard(
    stream: M3u8LiveStream,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onUpdateStatus: (LiveStreamStatus) -> Unit,
    onUnpublish: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (stream.status == LiveStreamStatus.LIVE) CinePrimary else CineTimelineRuler
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (stream.status) {
                            LiveStreamStatus.LIVE -> CinePrimary
                            LiveStreamStatus.SCHEDULED -> CineTertiary
                            LiveStreamStatus.ENDED -> CineTextTertiary
                        }
                    ) {
                        Text(
                            text = stream.status.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (stream.status == LiveStreamStatus.LIVE) {
                        Text("%,d watching".format(stream.viewerCount), fontSize = 10.sp, color = CineTertiary, fontWeight = FontWeight.Bold)
                    }
                }

                Text(stream.category, fontSize = 11.sp, color = CineTextSecondary)
            }

            Text(stream.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(stream.description, fontSize = 11.sp, color = CineTextSecondary, maxLines = 2)

            Text(
                text = "Live HLS: ${stream.m3u8Url}",
                fontSize = 10.sp,
                color = CineTextTertiary,
                maxLines = 1
            )

            Divider(color = CineTimelineRuler, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operator: ${stream.publishedByAdminName}",
                    fontSize = 10.sp,
                    color = CineTextTertiary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Preview", tint = CineSecondary)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Live Stream", tint = Color.White)
                    }

                    if (stream.status == LiveStreamStatus.SCHEDULED) {
                        Button(
                            onClick = { onUpdateStatus(LiveStreamStatus.LIVE) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Go Live", fontSize = 10.sp)
                        }
                    } else if (stream.status == LiveStreamStatus.LIVE) {
                        Button(
                            onClick = { onUpdateStatus(LiveStreamStatus.ENDED) },
                            colors = ButtonDefaults.buttonColors(containerColor = CineError),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Stop Stream", fontSize = 10.sp)
                        }
                    }

                    if (stream.isPublished && stream.status != LiveStreamStatus.ENDED) {
                        Button(
                            onClick = onUnpublish,
                            colors = ButtonDefaults.buttonColors(containerColor = CineSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Unpublish", fontSize = 10.sp)
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CineError)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to edit existing M3U8 Video metadata with instant stream preview.
 */
@Composable
fun EditM3u8VideoDialog(
    video: M3u8Video,
    categories: List<ContentCategory>,
    onDismiss: () -> Unit,
    onSave: (updated: M3u8Video) -> Unit,
    onPreviewStream: (url: String, title: String) -> Unit
) {
    var title by remember { mutableStateOf(video.title) }
    var description by remember { mutableStateOf(video.description) }
    var m3u8Url by remember { mutableStateOf(video.m3u8Url) }
    var thumbnailUrl by remember { mutableStateOf(video.thumbnailUrl) }
    var selectedCategory by remember { mutableStateOf(video.category) }
    var tagsInput by remember { mutableStateOf(video.tags.joinToString(", ")) }
    var visibility by remember { mutableStateOf(video.visibility) }
    var isFeatured by remember { mutableStateOf(video.isFeatured) }
    var isRecommended by remember { mutableStateOf(video.isRecommended) }
    var status by remember { mutableStateOf(video.status) }

    val isValid = isValidM3u8Url(m3u8Url)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CineSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("EDIT M3U8 VIDEO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                        Text(video.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = m3u8Url,
                            onValueChange = { m3u8Url = it },
                            label = { Text("HLS Stream URL (.m3u8)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (isValid) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CineSuccess)
                                } else {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = CineError)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isValid) CinePrimary else CineError,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        if (isValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onPreviewStream(m3u8Url, title.ifBlank { "Stream Preview" }) },
                                colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview Stream", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = thumbnailUrl,
                            onValueChange = { thumbnailUrl = it },
                            label = { Text("Poster / Thumbnail URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        Text("Category:", fontSize = 11.sp, color = CineTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.name,
                                    onClick = { selectedCategory = cat.name },
                                    label = { Text(cat.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        Text("Publication Status:", fontSize = 11.sp, color = CineTextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(VideoPublishStatus.PUBLISHED, VideoPublishStatus.DRAFT, VideoPublishStatus.DELETED).forEach { st ->
                                FilterChip(
                                    selected = status == st,
                                    onClick = { status = st },
                                    label = { Text(st.name, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Featured Video", fontSize = 12.sp, color = Color.White)
                            Switch(checked = isFeatured, onCheckedChange = { isFeatured = it }, colors = SwitchDefaults.colors(checkedThumbColor = CinePrimary))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recommended Content", fontSize = 12.sp, color = Color.White)
                            Switch(checked = isRecommended, onCheckedChange = { isRecommended = it }, colors = SwitchDefaults.colors(checkedThumbColor = CineSecondary))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onSave(
                                video.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    m3u8Url = m3u8Url.trim(),
                                    thumbnailUrl = thumbnailUrl.trim(),
                                    category = selectedCategory,
                                    tags = tags,
                                    visibility = visibility,
                                    isFeatured = isFeatured,
                                    isRecommended = isRecommended,
                                    status = status
                                )
                            )
                        },
                        enabled = isValid && title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

/**
 * Dialog to edit existing M3U8 Live Stream metadata with instant stream preview.
 */
@Composable
fun EditM3u8LiveStreamDialog(
    stream: M3u8LiveStream,
    categories: List<ContentCategory>,
    onDismiss: () -> Unit,
    onSave: (updated: M3u8LiveStream) -> Unit,
    onPreviewStream: (url: String, title: String) -> Unit
) {
    var title by remember { mutableStateOf(stream.title) }
    var description by remember { mutableStateOf(stream.description) }
    var m3u8Url by remember { mutableStateOf(stream.m3u8Url) }
    var thumbnailUrl by remember { mutableStateOf(stream.thumbnailUrl) }
    var selectedCategory by remember { mutableStateOf(stream.category) }
    var status by remember { mutableStateOf(stream.status) }
    var isFeatured by remember { mutableStateOf(stream.isFeatured) }
    var isPublished by remember { mutableStateOf(stream.isPublished) }

    val isValid = isValidM3u8Url(m3u8Url)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CineSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("EDIT LIVE STREAM BROADCAST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                        Text(stream.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Stream Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = m3u8Url,
                            onValueChange = { m3u8Url = it },
                            label = { Text("Live HLS URL (.m3u8)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (isValid) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CineSuccess)
                                } else {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = CineError)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isValid) CinePrimary else CineError,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        if (isValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onPreviewStream(m3u8Url, title.ifBlank { "Live Preview" }) },
                                colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview Stream", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Stream Description") },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = thumbnailUrl,
                            onValueChange = { thumbnailUrl = it },
                            label = { Text("Thumbnail URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    item {
                        Text("Category:", fontSize = 11.sp, color = CineTextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.name,
                                    onClick = { selectedCategory = cat.name },
                                    label = { Text(cat.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Broadcast Status:", fontSize = 11.sp, color = CineTextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(LiveStreamStatus.SCHEDULED, LiveStreamStatus.LIVE, LiveStreamStatus.ENDED).forEach { st ->
                                FilterChip(
                                    selected = status == st,
                                    onClick = { status = st },
                                    label = { Text(st.name, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CinePrimary, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Featured Broadcast", fontSize = 12.sp, color = Color.White)
                            Switch(checked = isFeatured, onCheckedChange = { isFeatured = it }, colors = SwitchDefaults.colors(checkedThumbColor = CinePrimary))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Publicly Published", fontSize = 12.sp, color = Color.White)
                            Switch(checked = isPublished, onCheckedChange = { isPublished = it }, colors = SwitchDefaults.colors(checkedThumbColor = CineSecondary))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                stream.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    m3u8Url = m3u8Url.trim(),
                                    thumbnailUrl = thumbnailUrl.trim(),
                                    category = selectedCategory,
                                    status = status,
                                    isFeatured = isFeatured,
                                    isPublished = isPublished
                                )
                            )
                        },
                        enabled = isValid && title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

/**
 * Management Section for Content Categories & Tags.
 */
@Composable
fun AdminCategoriesSection(
    categories: List<ContentCategory>,
    onAddCategory: (name: String, desc: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryDesc by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONTENT CATEGORIES & TAXONOMY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Category", fontSize = 10.sp)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { cat ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CineSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Surface(shape = RoundedCornerShape(4.dp), color = CineSurfaceVariant) {
                                    Text("/${cat.slug}", fontSize = 9.sp, color = CineTertiary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text(cat.description, fontSize = 11.sp, color = CineTextSecondary)
                        }

                        if (!cat.isSystemDefault) {
                            IconButton(onClick = { onDeleteCategory(cat.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CineError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CineSurface,
            title = { Text("New Content Category", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategoryDesc,
                        onValueChange = { newCategoryDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName, newCategoryDesc)
                            newCategoryName = ""
                            newCategoryDesc = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
