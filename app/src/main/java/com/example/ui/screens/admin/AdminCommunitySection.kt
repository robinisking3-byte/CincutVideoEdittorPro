package com.example.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AdminPermission
import com.example.core.model.AdminRole
import com.example.core.model.CineRoom
import com.example.core.model.LiveStream
import com.example.core.model.MembershipTier
import com.example.core.model.hasPermission
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCommunitySection(
    rooms: List<CineRoom>,
    liveStreams: List<LiveStream>,
    currentAdminRole: AdminRole,
    onCreateRoom: (name: String, description: String, tier: MembershipTier) -> Unit,
    onArchiveRoom: (roomId: String, reason: String) -> Unit,
    onTerminateStream: (streamId: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: CineRooms, 1: CineLive
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomDescription by remember { mutableStateOf("") }
    var newRoomTier by remember { mutableStateOf(MembershipTier.FREE) }

    var streamToTerminate by remember { mutableStateOf<LiveStream?>(null) }
    var terminateReason by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = CineSurface,
            contentColor = CinePrimary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("CineRooms (${rooms.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("CineLive Streams (${liveStreams.count { it.isLive }})", fontSize = 12.sp) }
            )
        }

        if (activeSubTab == 0) {
            // CineRooms Management
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CINEROOMS ADMINISTRATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                if (currentAdminRole.hasPermission(AdminPermission.MANAGE_ROOMS)) {
                    Button(
                        onClick = { showCreateRoomDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Room", fontSize = 11.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rooms) { room ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CineSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(room.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(room.requiredTier.badgeColor).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "${room.requiredTier.name} Gate",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(room.requiredTier.badgeColor),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(room.description, fontSize = 11.sp, color = CineTextSecondary)
                            Text("Members: %,d • Topic: %s".format(room.memberCount, room.currentTopic), fontSize = 10.sp, color = CineTertiary)

                            if (currentAdminRole.hasPermission(AdminPermission.MANAGE_ROOMS)) {
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        onClick = { onArchiveRoom(room.id, "Administrative room cleanup") },
                                        colors = ButtonDefaults.textButtonColors(contentColor = CineError)
                                    ) {
                                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Archive Room", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // CineLive Broadcast Monitor
            Text("CINELIVE BROADCAST CONSOLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(liveStreams) { stream ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CineSurface)
                            .border(1.dp, if (stream.isLive) Color(0xFFFF3366).copy(alpha = 0.4f) else CineTimelineRuler, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stream.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (stream.isLive) Color(0xFFFF3366).copy(alpha = 0.2f) else CineSurfaceVariant
                                ) {
                                    Text(
                                        if (stream.isLive) "LIVE" else "ENDED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stream.isLive) Color(0xFFFF3366) else CineTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text("Creator: ${stream.creatorName} • Category: ${stream.category}", fontSize = 11.sp, color = CineTextSecondary)
                            Text("Current Viewers: %,d".format(stream.viewerCount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineSecondary)

                            if (stream.isLive && currentAdminRole.hasPermission(AdminPermission.MANAGE_LIVE)) {
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            streamToTerminate = stream
                                            terminateReason = "Moderation violation / unauthorized content stream"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CineError),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Terminate Stream", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Room Dialog
    if (showCreateRoomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("Create New CineRoom", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRoomDescription,
                        onValueChange = { newRoomDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Access Tier Gate:", fontSize = 11.sp, color = CineTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(MembershipTier.FREE, MembershipTier.BRONZE, MembershipTier.GOLD, MembershipTier.VIP).forEach { tier ->
                            FilterChip(
                                selected = newRoomTier == tier,
                                onClick = { newRoomTier = tier },
                                label = { Text(tier.name.take(4), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRoomName.isNotBlank()) {
                            onCreateRoom(newRoomName, newRoomDescription, newRoomTier)
                            newRoomName = ""
                            newRoomDescription = ""
                            showCreateRoomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Create Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    // Terminate Stream Dialog
    if (streamToTerminate != null) {
        AlertDialog(
            onDismissRequest = { streamToTerminate = null },
            title = { Text("Terminate Live Stream", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Force-terminating stream '${streamToTerminate!!.title}'. An audit log will be written.",
                        fontSize = 12.sp,
                        color = CineTextSecondary
                    )
                    OutlinedTextField(
                        value = terminateReason,
                        onValueChange = { terminateReason = it },
                        label = { Text("Termination Reason (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (terminateReason.isNotBlank()) {
                            onTerminateStream(streamToTerminate!!.id, terminateReason)
                            streamToTerminate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineError)
                ) {
                    Text("Terminate Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { streamToTerminate = null }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }
}
