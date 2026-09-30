package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.core.model.InAppNotification
import com.example.core.model.NotificationCategory
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    onNavigateToFriends: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifications by repository.inAppNotifications.collectAsState()
    var selectedCategory by remember { mutableStateOf<NotificationCategory?>(null) } // null = ALL
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val filteredNotifications = remember(notifications, selectedCategory) {
        if (selectedCategory == null) notifications else notifications.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val unreadCount = notifications.count { !it.isRead }
                        if (unreadCount > 0) {
                            Badge(containerColor = CinePrimary) {
                                Text("$unreadCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        repository.markAllNotificationsAsRead()
                        toastMessage = "All notifications marked as read."
                    }) {
                        Text("Mark all read", fontSize = 11.sp, color = CineTertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("notifications_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${notifications.size})", fontSize = 11.sp) }
                    )
                }
                NotificationCategory.values().forEach { cat ->
                    item {
                        val count = notifications.count { it.category == cat }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.name} ($count)", fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(48.dp))
                        Text("All Caught Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("You don't have any notifications right now.", fontSize = 12.sp, color = CineTextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotifications) { notif ->
                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        Surface(
                            onClick = { repository.markNotificationAsRead(notif.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (notif.isRead) CineSurface else CineSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (notif.isRead) CineTimelineRuler else CinePrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (notif.category) {
                                                        NotificationCategory.FRIEND -> CinePrimary.copy(alpha = 0.2f)
                                                        NotificationCategory.PROJECT -> CineTertiary.copy(alpha = 0.2f)
                                                        NotificationCategory.COIN -> CineSecondary.copy(alpha = 0.2f)
                                                        else -> CineSurfaceVariant
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (notif.category) {
                                                    NotificationCategory.FRIEND -> Icons.Default.PersonAdd
                                                    NotificationCategory.PROJECT -> Icons.Default.FolderShared
                                                    NotificationCategory.COIN -> Icons.Default.Paid
                                                    NotificationCategory.COMMENT -> Icons.Default.Comment
                                                    else -> Icons.Default.Notifications
                                                },
                                                contentDescription = null,
                                                tint = when (notif.category) {
                                                    NotificationCategory.FRIEND -> CinePrimary
                                                    NotificationCategory.PROJECT -> CineTertiary
                                                    NotificationCategory.COIN -> CineSecondary
                                                    else -> Color.White
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(notif.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(dateFormat.format(Date(notif.timestamp)), fontSize = 10.sp, color = CineTextTertiary)
                                        }
                                    }

                                    IconButton(
                                        onClick = { repository.clearNotification(notif.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = CineTextTertiary, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Text(notif.message, fontSize = 11.sp, color = CineTextSecondary)

                                // Actionable Inline Controls
                                if (notif.actionable && notif.actionType != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (notif.actionType == "FRIEND_REQUEST") {
                                            Button(
                                                onClick = {
                                                    if (notif.relatedEntityId != null) {
                                                        repository.acceptFriendRequest(notif.relatedEntityId)
                                                    }
                                                    repository.clearNotification(notif.id)
                                                    toastMessage = "Friend request accepted!"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Accept", fontSize = 10.sp)
                                            }
                                        } else if (notif.actionType == "PROJECT_INVITE") {
                                            Button(
                                                onClick = {
                                                    if (notif.relatedEntityId != null) {
                                                        repository.respondToProjectInvite(notif.relatedEntityId, true)
                                                    }
                                                    repository.clearNotification(notif.id)
                                                    toastMessage = "Joined project collaboration team!"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CineTertiary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Join Team", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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
    }

    if (toastMessage != null) {
        LaunchedEffect(toastMessage) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
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
                Text(toastMessage!!, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}
