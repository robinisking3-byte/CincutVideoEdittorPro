package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.M3u8Video
import com.example.core.model.Project
import com.example.core.model.VideoPublishStatus
import com.example.core.repository.CineCutRepository
import com.example.ui.components.CineCoinBadge
import com.example.ui.components.M3u8PlayerView
import com.example.ui.components.MembershipBadge
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: CineCutRepository,
    onOpenProject: (Project) -> Unit,
    onNavigateToRooms: () -> Unit,
    onNavigateToLive: () -> Unit,
    onNavigateToCoins: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToFestivalOffers: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val projects by repository.projects.collectAsState()
    val liveStreams by repository.liveStreams.collectAsState()
    val rooms by repository.rooms.collectAsState()
    val m3u8Videos by repository.m3u8Videos.collectAsState()
    val festivalConfig by repository.festivalConfig.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("New Film Project") }
    var selectedAspect by remember { mutableStateOf("16:9") }
    var activePlaybackVideo by remember { mutableStateOf<M3u8Video?>(null) }

    // Live countdown timer for festival banner
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val remainingMs = remember(currentTime, festivalConfig.endDate) {
        (festivalConfig.endDate - currentTime).coerceAtLeast(0L)
    }

    val countdownFormatted = remember(remainingMs) {
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remainingMs)
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

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
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CinePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "CINECUT",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    CineCoinBadge(
                        balance = currentUser.coinBalance,
                        onClick = onNavigateToCoins
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (currentUser.adminRole != com.example.core.model.AdminRole.NONE) {
                        IconButton(
                            onClick = onNavigateToAdmin,
                            modifier = Modifier.testTag("admin_center_icon_button")
                        ) {
                            Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admin Center", tint = CineTertiary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewProjectDialog = true },
                containerColor = CinePrimary,
                contentColor = Color.White,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("New Project", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("create_project_fab")
            )
        },
        containerColor = CineBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dynamic Festival Banner (Remote Config driven with real-time countdown)
            if (festivalConfig.isEventRunning()) {
                item {
                    val theme = festivalConfig.themeConfig
                    val primaryColor = Color(theme.primaryAccentHex)
                    val secondaryColor = Color(theme.secondaryAccentHex)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFestivalOffers() }
                            .testTag("festival_home_banner")
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(theme.surfaceGradientStart),
                                            Color(theme.surfaceGradientEnd)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = primaryColor
                                        ) {
                                            Text(
                                                text = "🎉 FESTIVAL EVENT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = festivalConfig.activeFestival.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryColor
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, secondaryColor)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Ends in $countdownFormatted",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = theme.bannerHeadline,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = theme.bannerSubheadline,
                                    fontSize = 11.sp,
                                    color = CineTextSecondary,
                                    maxLines = 2
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+${festivalConfig.cineCoinBonusPercent}% CineCoins Bonus",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryColor
                                    )
                                    Button(
                                        onClick = onNavigateToFestivalOffers,
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("view_festival_offer_button")
                                    ) {
                                        Text("View Offer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Creator Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CineSurfaceHighlight, CineSurfaceVariant)
                            )
                        )
                        .border(1.dp, CineTimelineRuler, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentUser.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                VerifiedBadge()
                            }
                            MembershipBadge(tier = currentUser.membershipTier)
                        }
                        Text(
                            text = currentUser.bio,
                            fontSize = 12.sp,
                            color = CineTextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            // CineLive Section Header & Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(CinePrimary)
                            )
                            Text(
                                text = "CINELIVE BROADCASTS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        TextButton(onClick = onNavigateToLive) {
                            Text("Watch All", fontSize = 11.sp, color = CineTertiary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (stream in liveStreams) {
                            LiveStreamCard(
                                stream = stream,
                                onClick = onNavigateToLive
                            )
                        }
                    }
                }
            }

            // CineRooms Preview Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CINEROOMS (TIER GATED)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TextButton(onClick = onNavigateToRooms) {
                            Text("Explore", fontSize = 11.sp, color = CineTertiary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (room in rooms) {
                            CineRoomChipCard(
                                room = room,
                                onClick = onNavigateToRooms
                            )
                        }
                    }
                }
            }

            // Recent Projects
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT PROJECTS (${projects.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            items(projects) { project ->
                ProjectItemCard(
                    project = project,
                    onOpen = { onOpenProject(project) },
                    onDuplicate = { repository.duplicateProject(project) },
                    onDelete = { repository.deleteProject(project.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // New Project Setup Dialog
    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = CineSurface,
            title = { Text("Create New Project", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProjectTitle,
                        onValueChange = { newProjectTitle = it },
                        label = { Text("Project Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinePrimary,
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Text("Aspect Ratio", fontSize = 12.sp, color = CineTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("16:9", "9:16", "1:1", "21:9").forEach { ratio ->
                            val isSelected = selectedAspect == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CinePrimary else CineSurfaceVariant)
                                    .clickable { selectedAspect = ratio }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val created = repository.addProject(newProjectTitle, selectedAspect)
                        showNewProjectDialog = false
                        onOpenProject(created)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Create & Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel", color = CineTextSecondary)
                }
            }
        )
    }
}

@Composable
fun LiveStreamCard(
    stream: com.example.core.model.LiveStream,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161A28))
            .border(1.dp, CineTimelineRuler, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CinePrimary)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = CineTextSecondary, modifier = Modifier.size(12.dp))
                    Text("${stream.viewerCount}", fontSize = 10.sp, color = CineTextSecondary)
                }
            }

            Column {
                Text(
                    text = stream.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                Text(
                    text = "by ${stream.creatorName}",
                    fontSize = 10.sp,
                    color = CineTertiary
                )
            }
        }
    }
}

@Composable
fun CineRoomChipCard(
    room: com.example.core.model.CineRoom,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CineSurfaceVariant)
            .border(1.dp, CineTimelineRuler, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MembershipBadge(tier = room.requiredTier)
                if (room.isLive) {
                    Text("🔴 Active", fontSize = 9.sp, color = CinePrimary)
                }
            }
            Text(
                text = room.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "${room.memberCount} creators",
                fontSize = 10.sp,
                color = CineTextTertiary
            )
        }
    }
}

@Composable
fun ProjectItemCard(
    project: Project,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CineSurface)
            .border(1.dp, CineTimelineRuler, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(12.dp)
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Aspect Ratio Thumbnail preview
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CineSurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(24.dp))
                }

                Column {
                    Text(
                        text = project.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${project.aspectRatio} • ${project.durationMs / 1000}s • ${project.resolutionWidth}p",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )
                }
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options", tint = CineTextSecondary)
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in Editor") },
                        onClick = {
                            expandedMenu = false
                            onOpen()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            expandedMenu = false
                            onDuplicate()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = CineError) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CineError) }
                    )
                }
            }
        }
    }
}
