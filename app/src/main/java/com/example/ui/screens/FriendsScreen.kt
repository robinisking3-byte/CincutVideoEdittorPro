package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.core.model.FriendRequest
import com.example.core.model.FriendState
import com.example.core.model.Friendship
import com.example.core.model.UserProfile
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    onViewProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val friendships by repository.friendships.collectAsState()
    val friendRequests by repository.friendRequests.collectAsState()
    val sentRequests by repository.sentFriendRequests.collectAsState()
    val allUsers by repository.allUsers.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val blockedIds by repository.blockedUserIds.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var userToBlock by remember { mutableStateOf<UserProfile?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val suggestions = remember(allUsers, friendships, currentUser, blockedIds, searchQuery) {
        val friendUids = friendships.map { it.friendUserId }.toSet()
        allUsers.filter { u ->
            u.uid != currentUser.uid &&
                    !friendUids.contains(u.uid) &&
                    !blockedIds.contains(u.uid) &&
                    (searchQuery.isBlank() ||
                            u.displayName.contains(searchQuery, ignoreCase = true) ||
                            u.username.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Friends & Network", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${friendships.size} connected creators", fontSize = 11.sp, color = CineTertiary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("friends_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CineSurface,
                contentColor = CinePrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Friends (${friendships.size})", fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Requests", fontSize = 11.sp)
                            if (friendRequests.isNotEmpty()) {
                                Badge(containerColor = CinePrimary) { Text("${friendRequests.size}", fontSize = 9.sp) }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Sent (${sentRequests.size})", fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Discover & Search", fontSize = 11.sp) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Friends List
                    if (friendships.isEmpty()) {
                        EmptyFriendsState(
                            title = "No Friends Added Yet",
                            message = "Connect with video editors and directors to share projects.",
                            onActionClick = { selectedTabIndex = 3 }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(friendships) { friendship ->
                                FriendItemCard(
                                    user = friendship.friendProfile,
                                    onRemove = {
                                        repository.removeFriend(friendship.friendUserId)
                                        snackbarMessage = "Removed friend."
                                    },
                                    onBlock = { userToBlock = friendship.friendProfile }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Received Friend Requests
                    if (friendRequests.isEmpty()) {
                        EmptyFriendsState(
                            title = "No Pending Requests",
                            message = "When creators send you friend requests, they will appear here.",
                            onActionClick = { selectedTabIndex = 3 }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(friendRequests) { req ->
                                FriendRequestCard(
                                    request = req,
                                    onAccept = {
                                        repository.acceptFriendRequest(req.id)
                                        snackbarMessage = "Accepted friend request from ${req.senderName}!"
                                    },
                                    onReject = {
                                        repository.rejectFriendRequest(req.id)
                                        snackbarMessage = "Request declined."
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Sent Friend Requests
                    if (sentRequests.isEmpty()) {
                        EmptyFriendsState(
                            title = "No Outgoing Requests",
                            message = "You have no pending outgoing friend requests.",
                            onActionClick = { selectedTabIndex = 3 }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sentRequests) { req ->
                                SentRequestCard(
                                    request = req,
                                    onCancel = {
                                        repository.cancelFriendRequest(req.id)
                                        snackbarMessage = "Friend request cancelled."
                                    }
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // Discover & Search Users
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search creators by name or @handle...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CineTertiary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CineTextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CineSurface,
                                unfocusedContainerColor = CineSurface,
                                focusedBorderColor = CinePrimary,
                                unfocusedBorderColor = CineTimelineRuler
                            )
                        )

                        Text(
                            text = if (searchQuery.isBlank()) "CREATOR SUGGESTIONS" else "SEARCH RESULTS (${suggestions.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextTertiary
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(suggestions) { user ->
                                val friendState = repository.getFriendState(user.uid)
                                SuggestionUserCard(
                                    user = user,
                                    state = friendState,
                                    onAddFriend = {
                                        val ok = repository.sendFriendRequest(user.uid)
                                        if (ok) snackbarMessage = "Friend request sent to ${user.displayName}!"
                                    },
                                    onBlock = { userToBlock = user }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Block Confirmation Dialog
    if (userToBlock != null) {
        val target = userToBlock!!
        AlertDialog(
            onDismissRequest = { userToBlock = null },
            title = { Text("Block @${target.username}?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "They will be removed from your friends list and cannot view your shared projects or send you requests.",
                    fontSize = 12.sp,
                    color = CineTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.blockUser(target.uid)
                        snackbarMessage = "Blocked ${target.displayName}."
                        userToBlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineError)
                ) {
                    Text("Block Creator")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBlock = null }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    // Feedback Snackbar Toast
    if (snackbarMessage != null) {
        LaunchedEffect(snackbarMessage) {
            kotlinx.coroutines.delay(2500)
            snackbarMessage = null
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
                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = snackbarMessage!!,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun FriendItemCard(
    user: UserProfile,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CineSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(user.displayName.take(1), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("@${user.username} • Tier: ${user.membershipTier.name}", fontSize = 11.sp, color = CineTextSecondary)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Remove Friend", tint = CineTextSecondary)
            }
            IconButton(onClick = onBlock) {
                Icon(Icons.Default.Block, contentDescription = "Block", tint = CineError)
            }
        }
    }
}

@Composable
fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CineSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(request.senderName.take(1), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CinePrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(request.senderName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("@${request.senderUsername}", fontSize = 11.sp, color = CineTextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Accept", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onReject,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Decline", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SentRequestCard(
    request: FriendRequest,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CineSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(request.receiverName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Pending Request Sent", fontSize = 10.sp, color = CineTertiary)
            }
            OutlinedButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Cancel", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SuggestionUserCard(
    user: UserProfile,
    state: FriendState,
    onAddFriend: () -> Unit,
    onBlock: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CineSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(user.displayName.take(1), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("@${user.username} • Tier: ${user.membershipTier.name}", fontSize = 11.sp, color = CineTextSecondary)
                Text(user.bio.take(48) + if (user.bio.length > 48) "..." else "", fontSize = 10.sp, color = CineTextTertiary)
            }

            when (state) {
                FriendState.NONE -> {
                    Button(
                        onClick = onAddFriend,
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp)
                    }
                }
                FriendState.REQUEST_SENT -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CineSurfaceVariant
                    ) {
                        Text("Sent", fontSize = 10.sp, color = CineTertiary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                FriendState.REQUEST_RECEIVED -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CinePrimary.copy(alpha = 0.2f)
                    ) {
                        Text("Requested", fontSize = 10.sp, color = CinePrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                FriendState.FRIENDS -> {
                    Icon(Icons.Default.Check, contentDescription = "Friends", tint = CineSuccess)
                }
                FriendState.BLOCKED -> {
                    Text("Blocked", fontSize = 10.sp, color = CineError)
                }
            }
        }
    }
}

@Composable
fun EmptyFriendsState(
    title: String,
    message: String,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(48.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(message, fontSize = 12.sp, color = CineTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
            ) {
                Text("Find Creators to Connect")
            }
        }
    }
}
