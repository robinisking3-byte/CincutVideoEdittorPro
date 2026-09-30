package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.core.model.CineRoom
import com.example.core.model.MembershipTier
import com.example.core.repository.CineCutRepository
import com.example.ui.components.MembershipBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineRoomsScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    onNavigateToMembership: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val rooms by repository.rooms.collectAsState()
    val roomMessages by repository.roomMessages.collectAsState()

    var activeRoom by remember { mutableStateOf<CineRoom?>(null) }
    var messageInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeRoom?.name ?: "CineRooms",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (activeRoom != null) "${activeRoom!!.memberCount} creators in room" else "Tier-Gated Creator Communities",
                            fontSize = 11.sp,
                            color = CineTextTertiary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeRoom != null) activeRoom = null else onBack()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    MembershipBadge(tier = currentUser.membershipTier)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("cine_rooms_screen")
    ) { innerPadding ->
        if (activeRoom == null) {
            // Rooms List View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Access exclusive filmmaker hubs, live audio masterclasses, and collaborative channels based on your CineCut membership tier.",
                        fontSize = 12.sp,
                        color = CineTextSecondary
                    )
                }

                items(rooms) { room ->
                    val isEligible = currentUser.membershipTier.rank >= room.requiredTier.rank

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CineSurface)
                            .border(
                                1.dp,
                                if (isEligible) CineTimelineRuler else Color(0xFF3B1D25),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (isEligible) {
                                    activeRoom = room
                                } else {
                                    onNavigateToMembership()
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MembershipBadge(tier = room.requiredTier)

                                if (isEligible) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(14.dp))
                                        Text("UNLOCKED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CinePrimary, modifier = Modifier.size(14.dp))
                                        Text("UPGRADE TO ACCESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CinePrimary)
                                    }
                                }
                            }

                            Text(
                                text = room.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = room.description,
                                fontSize = 12.sp,
                                color = CineTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Topic: ${room.currentTopic}",
                                    fontSize = 11.sp,
                                    color = CineTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${room.memberCount} members",
                                    fontSize = 11.sp,
                                    color = CineTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Active Room Chat View
            val messages = roomMessages[activeRoom!!.id] ?: emptyList()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineSurface)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(msg.senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                MembershipBadge(tier = msg.senderTier)
                            }
                            Text(msg.message, fontSize = 13.sp, color = CineTextPrimary)
                        }
                    }
                }

                // Chat Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CineSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Message room...", fontSize = 13.sp) },
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
                            if (messageInput.isNotBlank()) {
                                repository.sendRoomMessage(activeRoom!!.id, messageInput)
                                messageInput = ""
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
}
