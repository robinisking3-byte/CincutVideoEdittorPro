package com.example.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AdminPermission
import com.example.core.model.AdminRole
import com.example.core.model.MembershipTier
import com.example.core.model.UserProfile
import com.example.core.model.hasPermission
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersSection(
    users: List<UserProfile>,
    currentAdminRole: AdminRole,
    onSuspendUser: (String, String, Int) -> Unit,
    onUnsuspendUser: (String) -> Unit,
    onBanUser: (String, String) -> Unit,
    onUnbanUser: (String) -> Unit,
    onUpdateRole: (String, AdminRole, String) -> Unit,
    onGrantFounder: (String, String) -> Unit,
    onManageBadge: (String, String, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "SUSPENDED", "BANNED"
    var selectedTierFilter by remember { mutableStateOf("ALL") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var selectedUserForDetails by remember { mutableStateOf<UserProfile?>(null) }
    var actionDialogType by remember { mutableStateOf<String?>(null) } // "BAN", "SUSPEND", "ROLE", "FOUNDER", "BADGE"
    var actionReason by remember { mutableStateOf("") }
    var actionDurationDays by remember { mutableIntStateOf(7) }
    var selectedRoleToAssign by remember { mutableStateOf(AdminRole.NONE) }
    var badgeToManage by remember { mutableStateOf("Verified Creator") }
    var isBadgeGranting by remember { mutableStateOf(true) }

    val filteredUsers = remember(users, searchQuery, selectedStatusFilter, selectedTierFilter, selectedRoleFilter) {
        users.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                    user.username.contains(searchQuery, ignoreCase = true) ||
                    user.displayName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.uid.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "ACTIVE" -> !user.isBanned && !user.isSuspended
                "SUSPENDED" -> user.isSuspended
                "BANNED" -> user.isBanned
                else -> true
            }

            val matchesTier = when (selectedTierFilter) {
                "FOUNDER" -> user.membershipTier == MembershipTier.FOUNDER
                "VIP" -> user.membershipTier == MembershipTier.VIP
                "GOLD_PLUS" -> user.membershipTier in listOf(MembershipTier.GOLD, MembershipTier.DIAMOND, MembershipTier.VIP, MembershipTier.FOUNDER)
                else -> true
            }

            val matchesRole = when (selectedRoleFilter) {
                "ADMINS" -> user.adminRole != AdminRole.NONE
                "MODERATORS" -> user.adminRole == AdminRole.MODERATOR
                else -> true
            }

            matchesQuery && matchesStatus && matchesTier && matchesRole
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name, @handle, UID, or email") },
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
                focusedBorderColor = CineTertiary,
                unfocusedBorderColor = CineTimelineRuler
            )
        )

        // Filter Chips Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { selectedStatusFilter = "ALL" },
                    label = { Text("All (${users.size})", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatusFilter == "ACTIVE",
                    onClick = { selectedStatusFilter = "ACTIVE" },
                    label = { Text("Active", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatusFilter == "SUSPENDED",
                    onClick = { selectedStatusFilter = "SUSPENDED" },
                    label = { Text("Suspended", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatusFilter == "BANNED",
                    onClick = { selectedStatusFilter = "BANNED" },
                    label = { Text("Banned", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedTierFilter == "FOUNDER",
                    onClick = { selectedTierFilter = if (selectedTierFilter == "FOUNDER") "ALL" else "FOUNDER" },
                    label = { Text("Founders", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedRoleFilter == "ADMINS",
                    onClick = { selectedRoleFilter = if (selectedRoleFilter == "ADMINS") "ALL" else "ADMINS" },
                    label = { Text("Staff / Admins", fontSize = 11.sp) }
                )
            }
        }

        Text(
            text = "SHOWING ${filteredUsers.size} CREATORS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextTertiary
        )

        // User Cards List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredUsers) { user ->
                AdminUserCard(
                    user = user,
                    onClick = { selectedUserForDetails = user }
                )
            }
        }
    }

    // Modal Bottom Sheet: User Details & Administration Console
    if (selectedUserForDetails != null) {
        val user = selectedUserForDetails!!
        ModalBottomSheet(
            onDismissRequest = { selectedUserForDetails = null },
            containerColor = CineSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Avatar and Basic Info
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CineSurfaceHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTertiary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(user.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (user.adminRole != AdminRole.NONE) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CineTertiary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            user.adminRole.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineTertiary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text("@${user.username} • UID: ${user.uid}", fontSize = 11.sp, color = CineTextSecondary)
                            Text(user.email, fontSize = 11.sp, color = CineTextTertiary)
                        }
                    }
                }

                // Status Badge & Tier
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                user.isBanned -> CineError.copy(alpha = 0.2f)
                                user.isSuspended -> CineWarning.copy(alpha = 0.2f)
                                else -> CineSuccess.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when {
                                    user.isBanned -> "PERMANENTLY BANNED"
                                    user.isSuspended -> "CURRENTLY SUSPENDED"
                                    else -> "ACTIVE ACCOUNT"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    user.isBanned -> CineError
                                    user.isSuspended -> CineWarning
                                    else -> CineSuccess
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Tier: ${user.membershipTier.name} • ${user.coinBalance} ¢",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineSecondary
                        )
                    }
                }

                // Sanction Details if Suspended or Banned
                if (user.isSuspended && user.suspensionReason != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineWarning.copy(alpha = 0.1f))
                                .padding(10.dp)
                        ) {
                            Text("Suspension note: ${user.suspensionReason}", fontSize = 11.sp, color = CineWarning)
                        }
                    }
                }
                if (user.isBanned && user.banReason != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineError.copy(alpha = 0.1f))
                                .padding(10.dp)
                        ) {
                            Text("Ban justification: ${user.banReason}", fontSize = 11.sp, color = CineError)
                        }
                    }
                }

                // Account Telemetry
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineSurfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        Text("Created: ${dateFormat.format(Date(user.createdAt))}", fontSize = 11.sp, color = CineTextSecondary)
                        Text("Last Active: ${dateFormat.format(Date(user.lastActiveAt))}", fontSize = 11.sp, color = CineTextSecondary)
                        Text("Device: ${user.deviceModel}", fontSize = 11.sp, color = CineTextSecondary)
                        Text("Reports Received: ${user.reportsReceivedCount}", fontSize = 11.sp, color = if (user.reportsReceivedCount > 0) CineWarning else CineTextSecondary)
                        Text("Badges: ${user.badges.joinToString(", ").ifEmpty { "None" }}", fontSize = 11.sp, color = CineTertiary)
                    }
                }

                // Management Action Buttons
                item {
                    Text("AUTHORIZED MODERATION & ACCOUNT ACTIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Suspend / Unsuspend
                        if (currentAdminRole.hasPermission(AdminPermission.BAN_USERS) || currentAdminRole == AdminRole.MODERATOR) {
                            if (!user.isSuspended) {
                                OutlinedButton(
                                    onClick = {
                                        actionDialogType = "SUSPEND"
                                        actionReason = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineWarning),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Suspend Creator Account")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onUnsuspendUser(user.uid)
                                        selectedUserForDetails = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unsuspend & Restore Account")
                                }
                            }
                        }

                        // Ban / Unban
                        if (currentAdminRole.hasPermission(AdminPermission.BAN_USERS)) {
                            if (!user.isBanned) {
                                Button(
                                    onClick = {
                                        actionDialogType = "BAN"
                                        actionReason = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineError),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Permanently Ban User (Requires Reason)")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onUnbanUser(user.uid)
                                        selectedUserForDetails = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Revoke Ban & Restore Access")
                                }
                            }
                        }

                        // Grant Founder Status
                        if (currentAdminRole.hasPermission(AdminPermission.MANAGE_MEMBERSHIPS) && user.membershipTier != MembershipTier.FOUNDER) {
                            OutlinedButton(
                                onClick = {
                                    actionDialogType = "FOUNDER"
                                    actionReason = ""
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5500)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Founder Status (Authoritative)")
                            }
                        }

                        // Manage Badges
                        if (currentAdminRole.hasPermission(AdminPermission.MANAGE_BADGES)) {
                            OutlinedButton(
                                onClick = {
                                    actionDialogType = "BADGE"
                                    actionReason = ""
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTertiary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Manage Achievement Badges")
                            }
                        }

                        // Role Assignment (Super Admin Only)
                        if (currentAdminRole == AdminRole.SUPER_ADMIN) {
                            OutlinedButton(
                                onClick = {
                                    actionDialogType = "ROLE"
                                    selectedRoleToAssign = user.adminRole
                                    actionReason = ""
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CinePrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Assign Administrative Role")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Modal Confirmation Dialogs for Privileged Actions
    if (actionDialogType != null && selectedUserForDetails != null) {
        val target = selectedUserForDetails!!
        AlertDialog(
            onDismissRequest = { actionDialogType = null },
            title = {
                Text(
                    text = when (actionDialogType) {
                        "BAN" -> "Confirm Permanent Ban"
                        "SUSPEND" -> "Suspend Account"
                        "FOUNDER" -> "Grant Founder Status"
                        "BADGE" -> "Manage User Badge"
                        "ROLE" -> "Assign Role: ${target.displayName}"
                        else -> "Administrative Action"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = when (actionDialogType) {
                            "BAN" -> "You are permanently banning @${target.username}. This will revoke platform access and write an immutable audit log."
                            "SUSPEND" -> "Specify the reason and duration for suspending @${target.username}."
                            "FOUNDER" -> "You are authoritatively awarding Founder status to @${target.username}. Provide justification for the audit trail."
                            "BADGE" -> "Select badge to grant or revoke for @${target.username}."
                            "ROLE" -> "Select administrative privilege tier for @${target.username}."
                            else -> ""
                        },
                        fontSize = 12.sp,
                        color = CineTextSecondary
                    )

                    if (actionDialogType == "SUSPEND") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 7, 30).forEach { days ->
                                FilterChip(
                                    selected = actionDurationDays == days,
                                    onClick = { actionDurationDays = days },
                                    label = { Text("${days}d") }
                                )
                            }
                        }
                    }

                    if (actionDialogType == "ROLE") {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(AdminRole.NONE, AdminRole.SUPPORT, AdminRole.CONTENT_ADMIN, AdminRole.MODERATOR, AdminRole.ADMIN).forEach { r ->
                                FilterChip(
                                    selected = selectedRoleToAssign == r,
                                    onClick = { selectedRoleToAssign = r },
                                    label = { Text(r.name.take(4), fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    if (actionDialogType == "BADGE") {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Verified Creator", "Pro Editor", "Master Colorist", "Staff Moderator").forEach { b ->
                                FilterChip(
                                    selected = badgeToManage == b,
                                    onClick = { badgeToManage = b },
                                    label = { Text(b.take(10), fontSize = 10.sp) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isBadgeGranting, onClick = { isBadgeGranting = true })
                            Text("Grant Badge", fontSize = 12.sp, color = Color.White)
                            RadioButton(selected = !isBadgeGranting, onClick = { isBadgeGranting = false })
                            Text("Revoke", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Audit Justification / Reason (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionReason.isNotBlank()) {
                            when (actionDialogType) {
                                "BAN" -> onBanUser(target.uid, actionReason)
                                "SUSPEND" -> onSuspendUser(target.uid, actionReason, actionDurationDays)
                                "FOUNDER" -> onGrantFounder(target.uid, actionReason)
                                "ROLE" -> onUpdateRole(target.uid, selectedRoleToAssign, actionReason)
                                "BADGE" -> onManageBadge(target.uid, badgeToManage, isBadgeGranting, actionReason)
                            }
                            actionDialogType = null
                            selectedUserForDetails = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionDialogType == "BAN") CineError else CinePrimary
                    )
                ) {
                    Text("Execute Action")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionDialogType = null }) {
                    Text("Cancel")
                }
            },
            containerColor = CineSurface
        )
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CineSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.take(1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTertiary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(user.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (user.isBanned) {
                        Text("[BANNED]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineError)
                    } else if (user.isSuspended) {
                        Text("[SUSPENDED]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineWarning)
                    }
                }
                Text("@${user.username} • Tier: ${user.membershipTier.name}", fontSize = 11.sp, color = CineTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%,d ¢".format(user.coinBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
                Text(user.adminRole.name, fontSize = 9.sp, color = if (user.adminRole != AdminRole.NONE) CineTertiary else CineTextTertiary)
            }
        }
    }
}
