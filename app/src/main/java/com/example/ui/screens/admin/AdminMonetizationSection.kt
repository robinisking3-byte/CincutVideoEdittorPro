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
import com.example.core.model.CineCoinTransaction
import com.example.core.model.UserProfile
import com.example.core.model.hasPermission
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonetizationSection(
    users: List<UserProfile>,
    transactions: List<CineCoinTransaction>,
    currentAdminRole: AdminRole,
    onAdjustCoins: (targetUid: String, amount: Long, reason: String) -> Unit,
    onGrantFounder: (targetUid: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: CineCoins, 1: Memberships & Founder
    var targetUserId by remember { mutableStateOf("usr_creator_8921") }
    var adjustAmountText by remember { mutableStateOf("1000") }
    var adjustReasonText by remember { mutableStateOf("Creator Bonus Incentive Grant") }
    var executionFeedback by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = CineSurface,
            contentColor = CineSecondary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("CineCoins Ledger", fontSize = 12.sp) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Memberships & Founder", fontSize = 12.sp) }
            )
        }

        if (activeSubTab == 0) {
            // CineCoins View
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("AUTHORITATIVE COIN ADJUSTMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CineSurface)
                            .border(1.dp, CineSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Administrative transactions are validated and logged to backend audit records.",
                                fontSize = 11.sp,
                                color = CineTextSecondary
                            )

                            OutlinedTextField(
                                value = targetUserId,
                                onValueChange = { targetUserId = it },
                                label = { Text("Target User ID (e.g. usr_creator_8921)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adjustAmountText,
                                onValueChange = { adjustAmountText = it },
                                label = { Text("Amount (+ for grant, - for deduction)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adjustReasonText,
                                onValueChange = { adjustReasonText = it },
                                label = { Text("Audit Justification / Reason (Required)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (currentAdminRole.hasPermission(AdminPermission.ADJUST_COINS)) {
                                Button(
                                    onClick = {
                                        val amt = adjustAmountText.toLongOrNull()
                                        if (amt != null && amt != 0L && adjustReasonText.isNotBlank()) {
                                            showConfirmDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Color(0xFF332000))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Execute Transaction", color = Color(0xFF332000), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Unauthorized: Admin or Super Admin role required to adjust coins.", fontSize = 11.sp, color = CineError)
                            }

                            if (executionFeedback != null) {
                                Text(executionFeedback!!, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                            }
                        }
                    }
                }

                item {
                    Text("RECENT IMMUTABLE TRANSACTIONS LEDGER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                }

                items(transactions) { tx ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CineSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (tx.amount >= 0) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                                contentDescription = null,
                                tint = if (tx.amount >= 0) CineSuccess else CineWarning
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.reason, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("User: ${tx.userId} • Type: ${tx.type}", fontSize = 10.sp, color = CineTextSecondary)
                            }
                            Text(
                                text = "${if (tx.amount >= 0) "+" else ""}${tx.amount} ¢",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.amount >= 0) CineSuccess else CineWarning
                            )
                        }
                    }
                }
            }
        } else {
            // Memberships & Founder View
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("SUBSCRIPTION TIERS & FOUNDER GRANTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                }

                items(users) { u ->
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
                                Column {
                                    Text(u.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("@${u.username} • UID: ${u.uid}", fontSize = 10.sp, color = CineTextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(u.membershipTier.badgeColor).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        u.membershipTier.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(u.membershipTier.badgeColor),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Text(
                                text = "Member since: ${dateFormat.format(Date(u.createdAt))} • Entitlement: Google Play Verified",
                                fontSize = 11.sp,
                                color = CineTextSecondary
                            )

                            if (currentAdminRole.hasPermission(AdminPermission.MANAGE_MEMBERSHIPS) && u.membershipTier != com.example.core.model.MembershipTier.FOUNDER) {
                                OutlinedButton(
                                    onClick = {
                                        onGrantFounder(u.uid, "Founder Status awarded via Admin Console")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5500)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Award Founder Status", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Coin Adjustment", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "You are adjusting $adjustAmountText CineCoins for user $targetUserId with reason: '$adjustReasonText'. This transaction is immutable.",
                    fontSize = 12.sp,
                    color = CineTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = adjustAmountText.toLongOrNull() ?: 0L
                        onAdjustCoins(targetUserId, amt, adjustReasonText)
                        executionFeedback = "Successfully adjusted $amt CineCoins for $targetUserId!"
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineSecondary)
                ) {
                    Text("Confirm Execution", color = Color(0xFF332000), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }
}
