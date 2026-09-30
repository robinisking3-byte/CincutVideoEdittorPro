package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AdminRole
import com.example.core.repository.CineCutRepository
import com.example.ui.components.CineCoinBadge
import com.example.ui.components.MembershipBadge
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: CineCutRepository,
    onNavigateToMembership: () -> Unit,
    onNavigateToCoins: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToFriends: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val allProjects by repository.projects.collectAsState()
    val allPosts by repository.posts.collectAsState()
    val friendships by repository.friendships.collectAsState()
    val inAppNotifications by repository.inAppNotifications.collectAsState()
    val supportTickets by repository.supportTickets.collectAsState()

    val unreadSupportCount = remember(supportTickets, currentUser.uid) {
        supportTickets.filter { it.userId == currentUser.uid }.sumOf { it.unreadUserCount }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Posts, 1: Projects, 2: Videos, 3: About, 4: Friends, 5: Badges
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    var editDisplayName by remember { mutableStateOf(currentUser.displayName) }
    var editUsername by remember { mutableStateOf(currentUser.username) }
    var editBio by remember { mutableStateOf(currentUser.bio) }
    var editUsernameError by remember { mutableStateOf<String?>(null) }

    var privacyPublicCoins by remember { mutableStateOf(currentUser.isCoinBalancePublic) }
    var privacyInvites by remember { mutableStateOf(currentUser.allowProjectInvites) }
    var privacyTagging by remember { mutableStateOf(currentUser.allowTagging) }

    val userPosts = remember(allPosts, currentUser) {
        allPosts.filter { it.authorId == currentUser.uid || it.authorHandle.contains(currentUser.username) }
    }

    val userProjects = remember(allProjects, currentUser) {
        allProjects.filter { it.ownerId == currentUser.uid || it.ownerId == "local_creator" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    // Friends shortcut
                    IconButton(onClick = onNavigateToFriends) {
                        Icon(Icons.Default.People, contentDescription = "Friends", tint = CineTertiary)
                    }
                    // Notifications with badge
                    val unread = inAppNotifications.count { !it.isRead }
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (unread > 0) {
                                    Badge(containerColor = CinePrimary) { Text("$unread") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                    }
                    CineCoinBadge(balance = currentUser.coinBalance, onClick = onNavigateToCoins)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("profile_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Banner & Header Card
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CineSurface)
                        .border(1.dp, CineTimelineRuler, RoundedCornerShape(16.dp))
                ) {
                    Column {
                        // Cinematic Banner Art
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF2A0845), Color(0xFF6441A5), Color(0xFFFF416C))
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        "CINECUT PRO CREATOR",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Avatar & Profile Info Overlay
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = (-40).dp)
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(CineSurfaceHighlight)
                                    .border(3.dp, CineSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.displayName.take(1),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CineTertiary
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(y = (-32).dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(currentUser.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    VerifiedBadge()
                                }
                                Text("@${currentUser.username} • ${currentUser.email}", fontSize = 12.sp, color = CineTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                MembershipBadge(tier = currentUser.membershipTier)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentUser.bio,
                                    fontSize = 12.sp,
                                    color = CineTextPrimary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Stats Row with Friends & Privacy-Gated Coin Balance
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CineSurfaceVariant)
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("%,d".format(currentUser.followersCount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Followers", fontSize = 10.sp, color = CineTextTertiary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${currentUser.followingCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Following", fontSize = 10.sp, color = CineTextTertiary)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onNavigateToFriends() }
                                    ) {
                                        Text("${currentUser.friendsCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                        Text("Friends ➔", fontSize = 10.sp, color = CineTertiary)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onNavigateToCoins() }
                                    ) {
                                        Text(
                                            if (currentUser.isCoinBalancePublic) "%,d ¢".format(currentUser.coinBalance) else "Private 🔒",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineSecondary
                                        )
                                        Text("CineCoins", fontSize = 10.sp, color = CineTextTertiary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Profile Quick Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editDisplayName = currentUser.displayName
                                            editUsername = currentUser.username
                                            editBio = currentUser.bio
                                            showEditProfileDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit Profile", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            privacyPublicCoins = currentUser.isCoinBalancePublic
                                            privacyInvites = currentUser.allowProjectInvites
                                            privacyTagging = currentUser.allowTagging
                                            showPrivacyDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Privacy", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Profile Tabs: Posts, Projects, Videos, About, Friends, Badges
            item {
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = CineSurface,
                    contentColor = CinePrimary,
                    edgePadding = 8.dp
                ) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Posts (${userPosts.size})", fontSize = 11.sp) })
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Projects (${userProjects.size})", fontSize = 11.sp) })
                    Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Videos (3)", fontSize = 11.sp) })
                    Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("About", fontSize = 11.sp) })
                    Tab(selected = activeTab == 4, onClick = { activeTab = 4 }, text = { Text("Friends (${friendships.size})", fontSize = 11.sp) })
                    Tab(selected = activeTab == 5, onClick = { activeTab = 5 }, text = { Text("Badges", fontSize = 11.sp) })
                }
            }

            // Tab Content
            when (activeTab) {
                0 -> {
                    // Posts
                    if (userPosts.isEmpty()) {
                        item {
                            Text("No posts shared yet.", fontSize = 12.sp, color = CineTextSecondary, modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(userPosts) { post ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CineSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(post.content, fontSize = 12.sp, color = Color.White)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        post.tags.forEach { tag ->
                                            Text("#$tag", fontSize = 10.sp, color = CineTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Projects
                    items(userProjects) { proj ->
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
                                Icon(Icons.Default.Movie, contentDescription = null, tint = CinePrimary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(proj.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${proj.aspectRatio} • ${proj.resolutionWidth}x${proj.resolutionHeight} • ${proj.durationMs / 1000}s", fontSize = 10.sp, color = CineTextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CinePrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(proj.visibility, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CinePrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Videos
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Tokyo Neon Drift — 4K 60fps HDR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Exported with H.265 codec • 1.4 GB • Public", fontSize = 11.sp, color = CineTextSecondary)
                            }
                        }
                    }
                }
                3 -> {
                    // About
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ABOUT CREATOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                                Text(currentUser.bio, fontSize = 12.sp, color = Color.White)
                                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                Text("Joined CineCut in ${dateFormat.format(Date(currentUser.createdAt))}", fontSize = 11.sp, color = CineTextSecondary)
                                Text("Device: ${currentUser.deviceModel}", fontSize = 11.sp, color = CineTextSecondary)
                                Text("Membership Rank: ${currentUser.membershipTier.name} (Tier ${currentUser.membershipTier.rank})", fontSize = 11.sp, color = CineTertiary)
                            }
                        }
                    }
                }
                4 -> {
                    // Friends Tab
                    items(friendships) { f ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CineSurfaceHighlight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(f.friendProfile.displayName.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.friendProfile.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("@${f.friendProfile.username}", fontSize = 10.sp, color = CineTextSecondary)
                                }
                                Text("Friend", fontSize = 10.sp, color = CineSuccess)
                            }
                        }
                    }
                }
                5 -> {
                    // Badges Tab
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentUser.badges.forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CineSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = CineTertiary)
                                        Column {
                                            Text(badge, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Authenticated Creator Milestone Badge", fontSize = 10.sp, color = CineTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Settings & Administration Section
            item {
                Text("SETTINGS & ADMINISTRATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNavigateToMembership,
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Membership & Subscription", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToCoins,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineSecondary)
                    ) {
                        Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = CineSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CineCoins Wallet & Purchases", color = CineSecondary)
                    }

                    // Help & Support Customer Care Portal
                    Surface(
                        onClick = onNavigateToSupport,
                        shape = RoundedCornerShape(10.dp),
                        color = CineSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (unreadSupportCount > 0) CineTertiary else CineTimelineRuler
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_help_support_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "Help & Support",
                                    tint = CineTertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Help & Support",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (unreadSupportCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = CinePrimary
                                        ) {
                                            Text(
                                                text = "$unreadSupportCount new",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Open tickets, track inquiries, payment assistance & FAQ",
                                    fontSize = 11.sp,
                                    color = CineTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CineTertiary
                            )
                        }
                    }

                    // Administrative Portal: Gated strictly by adminRole != NONE
                    if (currentUser.adminRole != AdminRole.NONE) {
                        Surface(
                            onClick = onNavigateToAdmin,
                            shape = RoundedCornerShape(10.dp),
                            color = CineSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CineTertiary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin Center",
                                        tint = CineTertiary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "CineCut Admin Center",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CineTertiary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                currentUser.adminRole.name,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CineTertiary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Manage platform users, moderation, telemetry & economy",
                                        fontSize = 11.sp,
                                        color = CineTextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = CineTertiary
                                )
                            }
                        }
                    }

                    // Authenticated Session Security & Sign Out
                    Surface(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp),
                        color = CineSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineError.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_sign_out_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineError.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out",
                                    tint = CineError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sign Out of CineCut",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineError
                                )
                                Text(
                                    "End authenticated Firebase session and lock studio",
                                    fontSize = 11.sp,
                                    color = CineTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CineError
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Edit Profile Dialog with Unique Username Validation
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Creator Profile", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = {
                            editUsername = it.trim().lowercase()
                            editUsernameError = if (it.isBlank()) "Username cannot be empty" else null
                        },
                        label = { Text("Unique Username (@handle)") },
                        isError = editUsernameError != null,
                        supportingText = {
                            if (editUsernameError != null) Text(editUsernameError!!, color = CineError)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDisplayName.isNotBlank() && editUsername.isNotBlank()) {
                            repository.updateUserProfile(
                                displayName = editDisplayName,
                                username = editUsername,
                                bio = editBio
                            )
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    // Privacy Settings Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Controls", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Public CineCoin Balance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Show balance publicly on your profile card", fontSize = 10.sp, color = CineTextSecondary)
                        }
                        Switch(checked = privacyPublicCoins, onCheckedChange = { privacyPublicCoins = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Project Invites", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Allow friends to invite you as collaborator", fontSize = 10.sp, color = CineTextSecondary)
                        }
                        Switch(checked = privacyInvites, onCheckedChange = { privacyInvites = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mentions & Tagging", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Allow comments to @mention your handle", fontSize = 10.sp, color = CineTextSecondary)
                        }
                        Switch(checked = privacyTagging, onCheckedChange = { privacyTagging = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updatePrivacySettings(
                            isCoinBalancePublic = privacyPublicCoins,
                            allowProjectInvites = privacyInvites,
                            allowTagging = privacyTagging
                        )
                        showPrivacyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                ) {
                    Text("Save Privacy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }
}
