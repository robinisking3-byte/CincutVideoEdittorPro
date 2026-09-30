package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AdminDashboardMetrics
import com.example.core.model.AdminNavDestination
import com.example.core.model.SecurityHealthStatus
import com.example.ui.theme.*

@Composable
fun AdminOverviewSection(
    metrics: AdminDashboardMetrics,
    securityHealth: SecurityHealthStatus,
    onNavigateDestination: (AdminNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Platform Telemetry Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CineSurface)
                    .border(1.dp, CinePrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CineSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "PLATFORM TELEMETRY & APP CHECK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CineSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HEALTHY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "App Check: ${securityHealth.appCheckStatus} • Auth: ${securityHealth.authHealth}",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )
                    Text(
                        text = "Functions: ${securityHealth.backendFunctionsHealth} • ${securityHealth.databaseStatus}",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )
                }
            }
        }

        // Section Title: Core Metrics
        item {
            Text(
                "CORE PLATFORM AGGREGATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextTertiary
            )
        }

        // Row 1: Users & Active
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricTile(
                    label = "Total Users",
                    value = "%,d".format(metrics.totalUsers),
                    subtext = "+%,d today".format(metrics.newUsersToday),
                    icon = Icons.Default.People,
                    iconTint = CinePrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.USERS) }
                )
                AdminMetricTile(
                    label = "Active 7-Day",
                    value = "%,d".format(metrics.activeUsers7d),
                    subtext = "High engagement",
                    icon = Icons.Default.TrendingUp,
                    iconTint = CineTertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.ANALYTICS) }
                )
            }
        }

        // Row 2: Moderation Sanctions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricTile(
                    label = "Suspended / Banned",
                    value = "${metrics.suspendedUsers} / ${metrics.bannedUsers}",
                    subtext = "Enforced sanctions",
                    icon = Icons.Default.Gavel,
                    iconTint = CineWarning,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.USERS) }
                )
                AdminMetricTile(
                    label = "Open Reports",
                    value = "${metrics.openReports} Pending",
                    subtext = "Requires moderator triage",
                    icon = Icons.Default.ReportProblem,
                    iconTint = CineError,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.REPORTS) }
                )
            }
        }

        // Row 3: Content & Live
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricTile(
                    label = "Rendered Projects",
                    value = "%,d".format(metrics.totalProjects),
                    subtext = "%,d posts".format(metrics.totalPosts),
                    icon = Icons.Default.Movie,
                    iconTint = CinePrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.CONTENT) }
                )
                AdminMetricTile(
                    label = "Live Streams",
                    value = "${metrics.activeLiveStreams} Online",
                    subtext = "HLS Broadcast active",
                    icon = Icons.Default.LiveTv,
                    iconTint = Color(0xFFFF3366),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.CINELIVE) }
                )
            }
        }

        // Row 4: Economy & Subscriptions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricTile(
                    label = "CineCoins Circulation",
                    value = "%,d ¢".format(metrics.cineCoinCirculation),
                    subtext = "Virtual economy ledger",
                    icon = Icons.Default.Paid,
                    iconTint = CineSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.CINECOINS) }
                )
                AdminMetricTile(
                    label = "Active Subscriptions",
                    value = "%,d".format(metrics.activeMemberships),
                    subtext = "%,d in CineRooms".format(metrics.cineRoomMembers),
                    icon = Icons.Default.WorkspacePremium,
                    iconTint = CineTertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateDestination(AdminNavDestination.MEMBERSHIPS) }
                )
            }
        }

        // Section: Quick Administrative Actions
        item {
            Text(
                "QUICK MANAGEMENT ACTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextTertiary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminQuickActionButton(
                    title = "Moderate Pending Reports",
                    subtitle = "${metrics.openReports} reports awaiting decision",
                    icon = Icons.Default.Flag,
                    accentColor = CineError,
                    onClick = { onNavigateDestination(AdminNavDestination.REPORTS) }
                )
                AdminQuickActionButton(
                    title = "Broadcast Push Notification",
                    subtitle = "Send global or targeted announcements",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = CineTertiary,
                    onClick = { onNavigateDestination(AdminNavDestination.NOTIFICATIONS) }
                )
                AdminQuickActionButton(
                    title = "Adjust CineCoins / Ledger",
                    subtitle = "Authoritative coin balance mutation",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = CineSecondary,
                    onClick = { onNavigateDestination(AdminNavDestination.CINECOINS) }
                )
                AdminQuickActionButton(
                    title = "ZapUPI Payment Gateway",
                    subtitle = "Manage API keys, test connection & view orders",
                    icon = Icons.Default.Payments,
                    accentColor = CineSuccess,
                    onClick = { onNavigateDestination(AdminNavDestination.PAYMENT_GATEWAY) }
                )
                AdminQuickActionButton(
                    title = "Customer Support Tickets",
                    subtitle = "Triage tickets, staff assignment & chat replies",
                    icon = Icons.Default.SupportAgent,
                    accentColor = CineTertiary,
                    onClick = { onNavigateDestination(AdminNavDestination.SUPPORT_TICKETS) }
                )
                AdminQuickActionButton(
                    title = "Autonomous AI Presets",
                    subtitle = "Review and approve generated styles",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = CinePrimary,
                    onClick = { onNavigateDestination(AdminNavDestination.AI_MANAGEMENT) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdminMetricTile(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CineTimelineRuler, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CineSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 11.sp, color = CineTextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtext, fontSize = 10.sp, color = CineTextTertiary)
        }
    }
}

@Composable
fun AdminQuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
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
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 11.sp, color = CineTextSecondary)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = CineTextTertiary)
        }
    }
}
