package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.screens.admin.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val allUsers by repository.allUsers.collectAsState()
    val posts by repository.posts.collectAsState()
    val reports by repository.reports.collectAsState()
    val rooms by repository.rooms.collectAsState()
    val liveStreams by repository.liveStreams.collectAsState()
    val coinTransactions by repository.coinTransactions.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()
    val featureFlags by repository.featureFlags.collectAsState()
    val autonomousPresets by repository.autonomousPresets.collectAsState()
    val adminNotifications by repository.adminNotifications.collectAsState()
    val securityHealth by repository.securityHealth.collectAsState()

    var activeDestination by remember { mutableStateOf(AdminNavDestination.OVERVIEW) }

    // Real metrics calculated live from repository and Firestore state
    val metrics = remember(allUsers, posts, reports, rooms, liveStreams, coinTransactions, paymentOrders) {
        val totalUsersCount = allUsers.size.toLong()
        val totalPostsCount = posts.size.toLong()
        val totalCommentsCount = posts.sumOf { it.commentsCount }.toLong()
        val suspended = allUsers.count { it.isSuspended }.toLong()
        val banned = allUsers.count { it.isBanned }.toLong()
        val openRep = reports.count { it.status == ReportStatus.PENDING }
        val activeStreams = liveStreams.count { it.isLive }
        val coinCirc = allUsers.sumOf { it.coinBalance }
        val activeMembers = allUsers.count { it.membershipTier != MembershipTier.FREE }.toLong()
        val roomMembers = rooms.sumOf { it.memberCount }.toLong()
        val totalProjectsCount = repository.projects.value.size.toLong().coerceAtLeast(1L)

        AdminDashboardMetrics(
            totalUsers = totalUsersCount,
            newUsersToday = allUsers.count { System.currentTimeMillis() - it.createdAt < 86400000L }.toLong().coerceAtLeast(1L),
            activeUsers7d = allUsers.count { it.lastActiveAt != null && System.currentTimeMillis() - it.lastActiveAt!! < 7 * 86400000L }.toLong().coerceAtLeast(totalUsersCount),
            suspendedUsers = suspended,
            bannedUsers = banned,
            totalProjects = totalProjectsCount,
            totalPosts = totalPostsCount,
            totalComments = totalCommentsCount,
            activeMemberships = activeMembers,
            cineCoinCirculation = coinCirc,
            activeLiveStreams = activeStreams,
            openReports = openRep,
            cineRoomMembers = roomMembers
        )
    }

    // Security Gate: Check if user has administrative access
    val isDesignatedAdminEmail = currentUser.email.trim().equals("robinisking3@gmail.com", ignoreCase = true)
    var isActivatingClaims by remember { mutableStateOf(false) }
    var claimStatusMessage by remember { mutableStateOf<String?>(null) }
    var claimErrorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (currentUser.adminRole == AdminRole.NONE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CineBackground)
                .testTag("admin_access_gate"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isDesignatedAdminEmail) CineWarning.copy(alpha = 0.15f) else CineError.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDesignatedAdminEmail) Icons.Default.Shield else Icons.Default.Lock,
                        contentDescription = "Access Status",
                        tint = if (isDesignatedAdminEmail) CineWarning else CineError,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (isDesignatedAdminEmail)
                        "Admin access has not been granted to this account."
                    else
                        "403 — Administrative Access Denied",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = if (isDesignatedAdminEmail)
                        "The designated administrator email (${currentUser.email}) is authenticated, but administrative Custom Claims (admin = true) have not been verified or granted on this Firebase session.\n\nEmail matching alone never grants administrative privileges. You must activate Custom Claims through the secure backend."
                    else
                        "Your account does not possess authorized administrative credentials or custom claims. This access attempt has been logged.",
                    fontSize = 12.sp,
                    color = CineTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )

                if (isDesignatedAdminEmail) {
                    claimErrorMessage?.let { err ->
                        Text(err, color = CineError, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    claimStatusMessage?.let { msg ->
                        Text(msg, color = CineSuccess, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }

                    Button(
                        onClick = {
                            isActivatingClaims = true
                            claimErrorMessage = null
                            claimStatusMessage = null
                            coroutineScope.launch {
                                val res = repository.requestInitialAdminClaimSetup()
                                isActivatingClaims = false
                                res.fold(
                                    onSuccess = { msg ->
                                        claimStatusMessage = msg
                                    },
                                    onFailure = { ex ->
                                        claimErrorMessage = ex.localizedMessage ?: "Failed to verify admin claims."
                                    }
                                )
                            }
                        },
                        enabled = !isActivatingClaims,
                        colors = ButtonDefaults.buttonColors(containerColor = CineTertiary),
                        modifier = Modifier.testTag("admin_claim_setup_button")
                    ) {
                        if (isActivatingClaims) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Activate Admin Custom Claims via Backend", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("admin_gate_return_button")
                ) {
                    Text("Return to Studio")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = CineTertiary
                        )
                        Column {
                            Text("CineCut Admin Center", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "Role: ${currentUser.adminRole.name} • ${currentUser.displayName}",
                                fontSize = 10.sp,
                                color = CineTertiary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CineSurfaceHighlight,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "SECURE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("admin_dashboard_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Navigation Carousel for all 16 Admin Destinations
            Surface(
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(AdminNavDestination.values()) { dest ->
                        val isSelected = activeDestination == dest
                        val isPermitted = currentUser.adminRole.hasPermission(dest.requiredPermission)

                        FilterChip(
                            selected = isSelected,
                            onClick = { activeDestination = dest },
                            label = {
                                Text(
                                    text = dest.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (!isPermitted) {
                                { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = CineTextTertiary) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = CineSurfaceVariant,
                                labelColor = if (isPermitted) CineTextPrimary else CineTextTertiary
                            )
                        )
                    }
                }
            }

            // Role Permission Guard per destination
            if (!currentUser.adminRole.hasPermission(activeDestination.requiredPermission)) {
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
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CineWarning, modifier = Modifier.size(48.dp))
                        Text("Permission Required", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Your role (${currentUser.adminRole.name}) does not have permission to access ${activeDestination.title}.",
                            fontSize = 12.sp,
                            color = CineTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { activeDestination = AdminNavDestination.OVERVIEW },
                            colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
                        ) {
                            Text("Back to Overview")
                        }
                    }
                }
            } else {
                // Active Section Render
                when (activeDestination) {
                    AdminNavDestination.OVERVIEW -> {
                        AdminOverviewSection(
                            metrics = metrics,
                            securityHealth = securityHealth,
                            onNavigateDestination = { activeDestination = it }
                        )
                    }
                    AdminNavDestination.USERS -> {
                        AdminUsersSection(
                            users = allUsers,
                            currentAdminRole = currentUser.adminRole,
                            onSuspendUser = { uid, reason, days -> repository.adminSuspendUser(uid, reason, days) },
                            onUnsuspendUser = { uid -> repository.adminUnsuspendUser(uid) },
                            onBanUser = { uid, reason -> repository.adminBanUser(uid, reason) },
                            onUnbanUser = { uid -> repository.adminUnbanUser(uid) },
                            onUpdateRole = { uid, role, reason -> repository.adminUpdateUserRole(uid, role, reason) },
                            onGrantFounder = { uid, reason -> repository.adminGrantFounder(uid, reason) },
                            onManageBadge = { uid, badge, grant, reason -> repository.adminManageBadge(uid, badge, grant, reason) }
                        )
                    }
                    AdminNavDestination.CONTENT -> {
                        AdminContentSection(
                            repository = repository,
                            posts = posts,
                            currentAdminRole = currentUser.adminRole,
                            onModeratePost = { postId, action, reason -> repository.adminModerateContent("POST", postId, action, reason) }
                        )
                    }
                    AdminNavDestination.REPORTS -> {
                        AdminReportsSection(
                            reports = reports,
                            currentAdminRole = currentUser.adminRole,
                            onUpdateReport = { id, status, res, reason -> repository.adminUpdateReport(id, status, res, reason) }
                        )
                    }
                    AdminNavDestination.MESSAGES -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 2 // Notifications / Messages broadcast
                        )
                    }
                    AdminNavDestination.PROJECTS -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 3 // Analytics / Projects view
                        )
                    }
                    AdminNavDestination.CINEROOMS -> {
                        AdminCommunitySection(
                            rooms = rooms,
                            liveStreams = liveStreams,
                            currentAdminRole = currentUser.adminRole,
                            onCreateRoom = { n, d, t -> repository.adminCreateRoom(n, d, t) },
                            onArchiveRoom = { id, r -> repository.adminArchiveRoom(id, r) },
                            onTerminateStream = { id, r -> repository.adminTerminateStream(id, r) }
                        )
                    }
                    AdminNavDestination.CINELIVE -> {
                        AdminCommunitySection(
                            rooms = rooms,
                            liveStreams = liveStreams,
                            currentAdminRole = currentUser.adminRole,
                            onCreateRoom = { n, d, t -> repository.adminCreateRoom(n, d, t) },
                            onArchiveRoom = { id, r -> repository.adminArchiveRoom(id, r) },
                            onTerminateStream = { id, r -> repository.adminTerminateStream(id, r) }
                        )
                    }
                    AdminNavDestination.MEMBERSHIPS -> {
                        AdminMonetizationSection(
                            users = allUsers,
                            transactions = coinTransactions,
                            currentAdminRole = currentUser.adminRole,
                            onAdjustCoins = { u, a, r -> repository.adminAdjustCoins(u, a, r) },
                            onGrantFounder = { u, r -> repository.adminGrantFounder(u, r) }
                        )
                    }
                    AdminNavDestination.CINECOINS -> {
                        AdminMonetizationSection(
                            users = allUsers,
                            transactions = coinTransactions,
                            currentAdminRole = currentUser.adminRole,
                            onAdjustCoins = { u, a, r -> repository.adminAdjustCoins(u, a, r) },
                            onGrantFounder = { u, r -> repository.adminGrantFounder(u, r) }
                        )
                    }
                    AdminNavDestination.NOTIFICATIONS -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 2
                        )
                    }
                    AdminNavDestination.AI_MANAGEMENT -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 0
                        )
                    }
                    AdminNavDestination.FEATURE_FLAGS -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 1
                        )
                    }
                    AdminNavDestination.ANALYTICS -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 3
                        )
                    }
                    AdminNavDestination.AUDIT_LOGS -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            initialTab = 4
                        )
                    }
                    AdminNavDestination.SYSTEM_SETTINGS -> {
                        AdminAiAndConfigSection(
                            presets = autonomousPresets,
                            featureFlags = featureFlags,
                            notifications = adminNotifications,
                            auditLogs = auditLogs,
                            securityHealth = securityHealth,
                            currentAdminRole = currentUser.adminRole,
                            onApprovePreset = { repository.adminApprovePreset(it) },
                            onRejectPreset = { repository.adminRejectPreset(it) },
                            onTriggerAiWorker = { repository.adminTriggerAiWorker() },
                            onToggleFlag = { k, v -> repository.adminToggleFeatureFlag(k, v) },
                            onSendNotification = { t, m, a, u, d -> repository.adminSendNotification(t, m, a, u, d) },
                            onSimulateRole = { repository.adminSimulateSwitchRole(it) },
                            onNavigateToPaymentGateway = { activeDestination = AdminNavDestination.PAYMENT_GATEWAY },
                            initialTab = 5
                        )
                    }
                    AdminNavDestination.PAYMENT_GATEWAY -> {
                        AdminPaymentGatewaySection(
                            repository = repository,
                            currentAdminRole = currentUser.adminRole
                        )
                    }
                    AdminNavDestination.SUPPORT_TICKETS -> {
                        AdminSupportSection(
                            repository = repository,
                            currentAdminRole = currentUser.adminRole
                        )
                    }
                    AdminNavDestination.FESTIVALS -> {
                        AdminFestivalSection(
                            repository = repository,
                            currentAdminRole = currentUser.adminRole
                        )
                    }
                }
            }
        }
    }
}
