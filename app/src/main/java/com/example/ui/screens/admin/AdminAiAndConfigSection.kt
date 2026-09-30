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
import com.example.core.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAiAndConfigSection(
    presets: List<AutonomousPreset>,
    featureFlags: FeatureFlags,
    notifications: List<AdminNotification>,
    auditLogs: List<AuditLog>,
    securityHealth: SecurityHealthStatus,
    currentAdminRole: AdminRole,
    onApprovePreset: (String) -> Unit,
    onRejectPreset: (String) -> Unit,
    onTriggerAiWorker: () -> Unit,
    onToggleFlag: (String, Boolean) -> Unit,
    onSendNotification: (String, String, String, String?, String) -> Unit,
    onSimulateRole: (AdminRole) -> Unit,
    onNavigateToPaymentGateway: (() -> Unit)? = null,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(initialTab) } // 0: AI, 1: Flags, 2: Notifs, 3: Analytics, 4: Logs, 5: System

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = CineSurface,
            contentColor = CinePrimary,
            edgePadding = 8.dp
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("AI Studio", fontSize = 11.sp) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Flags", fontSize = 11.sp) })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Notifications", fontSize = 11.sp) })
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("Analytics", fontSize = 11.sp) })
            Tab(selected = activeTab == 4, onClick = { activeTab = 4 }, text = { Text("Audit Logs", fontSize = 11.sp) })
            Tab(selected = activeTab == 5, onClick = { activeTab = 5 }, text = { Text("System & Roles", fontSize = 11.sp) })
        }

        when (activeTab) {
            0 -> AiPresetsTab(presets, currentAdminRole, onApprovePreset, onRejectPreset, onTriggerAiWorker)
            1 -> FeatureFlagsTab(featureFlags, currentAdminRole, onToggleFlag)
            2 -> NotificationsTab(notifications, currentAdminRole, onSendNotification)
            3 -> AnalyticsVisualizationTab()
            4 -> AuditLogsTab(auditLogs)
            5 -> SystemSettingsTab(securityHealth, currentAdminRole, onSimulateRole, onNavigateToPaymentGateway)
        }
    }
}

@Composable
fun AiPresetsTab(
    presets: List<AutonomousPreset>,
    currentRole: AdminRole,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onTriggerWorker: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AUTONOMOUS AI PRESETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                if (currentRole.hasPermission(AdminPermission.MANAGE_AI)) {
                    Button(
                        onClick = onTriggerWorker,
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trigger Generation Batch", fontSize = 10.sp)
                    }
                }
            }
        }

        items(presets) { preset ->
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
                        Text(preset.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (preset.isApproved) CineSuccess.copy(alpha = 0.2f) else CineWarning.copy(alpha = 0.2f)
                        ) {
                            Text(
                                if (preset.isApproved) "APPROVED" else "PENDING REVIEW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (preset.isApproved) CineSuccess else CineWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text("Type: ${preset.category} • v${preset.version} • Rating: ${preset.rating} ★", fontSize = 10.sp, color = CineSecondary)
                    Text(preset.description, fontSize = 11.sp, color = CineTextSecondary)

                    if (currentRole.hasPermission(AdminPermission.MANAGE_AI)) {
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            if (!preset.isApproved) {
                                TextButton(
                                    onClick = { onApprove(preset.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CineSuccess)
                                ) {
                                    Text("Approve Style", fontSize = 11.sp)
                                }
                            } else {
                                TextButton(
                                    onClick = { onReject(preset.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CineError)
                                ) {
                                    Text("Revoke Approval", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureFlagsTab(
    flags: FeatureFlags,
    currentRole: AdminRole,
    onToggleFlag: (String, Boolean) -> Unit
) {
    val flagList = listOf(
        Pair("aiDirectorEnabled", flags.aiDirectorEnabled),
        Pair("autonomousAiEnabled", flags.autonomousAiEnabled),
        Pair("cineLiveEnabled", flags.cineLiveEnabled),
        Pair("cineRoomsEnabled", flags.cineRoomsEnabled),
        Pair("messagingEnabled", flags.messagingEnabled),
        Pair("newEditorEnabled", flags.newEditorEnabled),
        Pair("maintenanceMode", flags.maintenanceMode)
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("REMOTE PLATFORM FEATURE FLAGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        items(flagList) { (key, isEnabled) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CineSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(key, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (key == "maintenanceMode") "Emergency client block" else "Feature activation switch", fontSize = 10.sp, color = CineTextSecondary)
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onToggleFlag(key, it) },
                        enabled = currentRole.hasPermission(AdminPermission.MANAGE_FEATURE_FLAGS)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsTab(
    notifications: List<AdminNotification>,
    currentRole: AdminRole,
    onSendNotification: (String, String, String, String?, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("GLOBAL") }
    var feedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("BROADCAST PUSH ANNOUNCEMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CineSurface)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message Body") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("GLOBAL", "BRONZE_PLUS", "GOLD_PLUS").forEach { aud ->
                            FilterChip(
                                selected = audience == aud,
                                onClick = { audience = aud },
                                label = { Text(aud, fontSize = 10.sp) }
                            )
                        }
                    }

                    if (currentRole.hasPermission(AdminPermission.SEND_NOTIFICATIONS)) {
                        Button(
                            onClick = {
                                if (title.isNotBlank() && message.isNotBlank()) {
                                    onSendNotification(title, message, audience, null, "cinecut://home")
                                    feedback = "Broadcast dispatched successfully!"
                                    title = ""
                                    message = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dispatch Broadcast")
                        }
                    }

                    if (feedback != null) {
                        Text(feedback!!, fontSize = 11.sp, color = CineSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("RECENT DISPATCHED BROADCASTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        items(notifications) { notif ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(notif.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(notif.audience, fontSize = 10.sp, color = CineTertiary)
                    }
                    Text(notif.message, fontSize = 11.sp, color = CineTextSecondary)
                }
            }
        }
    }
}

@Composable
fun AnalyticsVisualizationTab() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("DEEP-DIVE PLATFORM ANALYTICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        // Daily Users Chart Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily New Creators (Last 7 Days)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val days = listOf("Mon" to 840, "Tue" to 950, "Wed" to 1120, "Thu" to 1050, "Fri" to 1420, "Sat" to 1890, "Sun" to 1240)
                    val maxVal = 2000f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEach { (day, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${count}", fontSize = 9.sp, color = CineTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(count / maxVal)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(CinePrimary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day, fontSize = 10.sp, color = CineTextTertiary)
                            }
                        }
                    }
                }
            }
        }

        // Membership Tiers Distribution
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Membership Distribution Breakdown", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    listOf(
                        Triple("Free Creators", 68f, 0xFF64748B),
                        Triple("Bronze & Silver", 18f, 0xFFCD7F32),
                        Triple("Gold Creators", 9f, 0xFFFFD700),
                        Triple("Diamond Directors", 3.5f, 0xFF00E5FF),
                        Triple("VIP Studio & Founders", 1.5f, 0xFFFF5500)
                    ).forEach { (label, percent, colorLong) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 11.sp, color = CineTextSecondary)
                                Text("$percent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(colorLong))
                            }
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                color = Color(colorLong),
                                trackColor = CineSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsTab(auditLogs: List<AuditLog>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("IMMUTABLE ADMINISTRATIVE AUDIT LOG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        items(auditLogs) { log ->
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 9.sp, color = CineTextTertiary)
                    }
                    Text("Target: ${log.target}", fontSize = 10.sp, color = CineTextSecondary)
                    Text("Reason: ${log.reason}", fontSize = 11.sp, color = Color.White)
                    Text("Admin UID: ${log.adminUid}", fontSize = 9.sp, color = CineTextTertiary)
                }
            }
        }
    }
}

@Composable
fun SystemSettingsTab(
    securityHealth: SecurityHealthStatus,
    currentRole: AdminRole,
    onSimulateRole: (AdminRole) -> Unit,
    onNavigateToPaymentGateway: (() -> Unit)? = null
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Payment Gateway Settings Card (ZapUPI)
        item {
            Text("PAYMENT GATEWAY CONFIGURATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚡", fontSize = 20.sp)
                            Column {
                                Text("ZapUPI Gateway", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Admin Panel → Settings → Payment Gateway → ZapUPI", fontSize = 10.sp, color = CineTertiary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0F3D24)
                        ) {
                            Text(
                                "Connected ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        "Manage ZapUPI API credentials securely with Google Cloud Secret Manager. Configure timeouts, currency, test/live mode, orders ledger, and manual UTR verification.",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )

                    Button(
                        onClick = { onNavigateToPaymentGateway?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Payment Gateway Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("FIREBASE & SECURITY POSTURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App Check: ${securityHealth.appCheckStatus}", fontSize = 12.sp, color = Color.White)
                    Text("Auth Service: ${securityHealth.authHealth}", fontSize = 12.sp, color = Color.White)
                    Text("Cloud Functions: ${securityHealth.backendFunctionsHealth}", fontSize = 12.sp, color = Color.White)
                    Text("Database: ${securityHealth.databaseStatus}", fontSize = 12.sp, color = Color.White)
                    Text("Privileged 24h Ops: ${securityHealth.privilegedOperationsCount24h} calls", fontSize = 12.sp, color = CineTertiary)
                }
            }
        }

        item {
            Text("ADMIN ROLE VERIFICATION SIMULATOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
        }

        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Test and verify how the UI and security layers respond to different admin roles.",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )
                    Text("Current Simulated Role: ${currentRole.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineTertiary)

                    listOf(
                        AdminRole.SUPER_ADMIN,
                        AdminRole.ADMIN,
                        AdminRole.MODERATOR,
                        AdminRole.SUPPORT,
                        AdminRole.CONTENT_ADMIN,
                        AdminRole.NONE
                    ).forEach { role ->
                        OutlinedButton(
                            onClick = { onSimulateRole(role) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (currentRole == role) CinePrimary else CineTextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentRole == role) CinePrimary else CineTimelineRuler
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Switch to ${role.name}")
                        }
                    }
                }
            }
        }
    }
}
