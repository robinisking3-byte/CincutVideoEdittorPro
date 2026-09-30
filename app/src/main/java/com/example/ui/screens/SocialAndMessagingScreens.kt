package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.core.model.DirectMessage
import com.example.core.model.Post
import com.example.core.repository.CineCutRepository
import com.example.ui.components.MembershipBadge
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    repository: CineCutRepository,
    modifier: Modifier = Modifier
) {
    val posts by repository.posts.collectAsState()
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var newPostText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(
                        onClick = { showCreatePostDialog = true },
                        modifier = Modifier.testTag("create_post_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddComment, contentDescription = "Create Post", tint = CinePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("social_feed_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts) { post ->
                PostCard(
                    post = post,
                    onLike = { repository.toggleLike(post.id) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showCreatePostDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            containerColor = CineSurface,
            title = { Text("Start Creator Discussion", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Video & Live broadcasting is strictly managed by CineCut Admins. Community members can share editing workflows, color grading notes, or tips.",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )
                    OutlinedTextField(
                        value = newPostText,
                        onValueChange = { newPostText = it },
                        placeholder = { Text("Share an editing tip, color grading formula, or workflow question...") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinePrimary,
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPostText.isNotBlank()) {
                            repository.addPost(newPostText, listOf("CineCut", "ColorGrading"))
                            newPostText = ""
                            showCreatePostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Post Topic")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Cancel", color = CineTextSecondary)
                }
            }
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    onLike: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CineSurface)
            .border(1.dp, CineTimelineRuler, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(CineSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.authorName.take(1), fontWeight = FontWeight.Bold, color = CineTertiary)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(post.authorName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            VerifiedBadge()
                        }
                        Text(post.authorHandle, fontSize = 11.sp, color = CineTextTertiary)
                    }
                }

                MembershipBadge(tier = post.authorTier)
            }

            // Post content
            Text(post.content, fontSize = 13.sp, color = CineTextPrimary, lineHeight = 18.sp)

            // Tags
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                post.tags.forEach { tag ->
                    Text("#$tag", fontSize = 11.sp, color = CineTertiary, fontWeight = FontWeight.Medium)
                }
            }

            Divider(color = CineTimelineRuler, thickness = 0.5.dp)

            // Interaction Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button (Duplicate prevented)
                IconButton(onClick = onLike) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) CinePrimary else CineTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${post.likesCount}",
                            fontSize = 11.sp,
                            color = if (post.isLiked) CinePrimary else CineTextSecondary
                        )
                    }
                }

                // Comments
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = CineTextSecondary, modifier = Modifier.size(16.dp))
                    Text("${post.commentsCount}", fontSize = 11.sp, color = CineTextSecondary)
                }

                // Share
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = CineTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by repository.directMessages.collectAsState()
    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collaborative Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("messages_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
                    val bubbleColor = if (msg.isMe) CinePrimary else CineSurfaceVariant

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        if (!msg.isMe) {
                            Text(msg.senderName, fontSize = 10.sp, color = CineTextTertiary, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bubbleColor)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(msg.text, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type message...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CinePrimary,
                        unfocusedBorderColor = CineTimelineRuler,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            repository.sendDirectMessage(input)
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(CinePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}
