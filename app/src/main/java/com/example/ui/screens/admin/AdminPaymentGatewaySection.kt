package com.example.ui.screens.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin Panel → Payment & UTR Approvals Section
 * Real payment verification dashboard for UPI ID: robintyagi@fam
 * - Pending Approvals Queue: review user-submitted 12-digit UTR numbers
 * - Approve action: instantly activates Pro membership and credits CineCoins
 * - Reject action: rejects with audit note and user notification
 * - Approved history ledger
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentGatewaySection(
    repository: CineCutRepository,
    currentAdminRole: AdminRole,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val paymentOrders by repository.paymentOrders.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pending Approvals, 1: Approved History, 2: UPI Config
    var rejectingOrder by remember { mutableStateOf<ZapUpiOrder?>(null) }
    var rejectReasonInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val pendingOrders = remember(paymentOrders) {
        paymentOrders.filter { it.status == PaymentOrderStatus.PENDING_APPROVAL }
    }

    val approvedOrders = remember(paymentOrders, searchQuery) {
        paymentOrders.filter {
            (it.status == PaymentOrderStatus.APPROVED || it.status == PaymentOrderStatus.PAID) &&
            (searchQuery.isBlank() ||
             it.orderId.contains(searchQuery, ignoreCase = true) ||
             (it.utrNumber?.contains(searchQuery, ignoreCase = true) == true) ||
             it.userDisplayName.contains(searchQuery, ignoreCase = true) ||
             it.userEmail.contains(searchQuery, ignoreCase = true))
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Payment Approvals & UPI Ledger",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Receiving UPI ID: robintyagi@fam",
                    fontSize = 12.sp,
                    color = CineTertiary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (pendingOrders.isNotEmpty()) Color(0xFFFF9900).copy(alpha = 0.2f) else CineSurface
            ) {
                Text(
                    text = "${pendingOrders.size} PENDING APPROVAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingOrders.isNotEmpty()) Color(0xFFFF9900) else CineTextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = CineSurface,
            contentColor = CinePrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Pending Approvals", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (pendingOrders.isNotEmpty()) {
                            Badge(containerColor = Color(0xFFFF9900)) {
                                Text("${pendingOrders.size}", color = Color.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Approved History", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("UPI Config", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Pending Approvals Queue
                if (pendingOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(48.dp))
                            Text("All Caught Up!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("No pending UTR payments waiting for review.", fontSize = 12.sp, color = CineTextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingOrders) { order ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CineSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(order.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                "Order: ${order.orderId} • Submitted: ${dateFormatter.format(Date(order.createdAt))}",
                                                fontSize = 11.sp,
                                                color = CineTextTertiary
                                            )
                                        }
                                        Text(
                                            "₹%.0f".format(order.amountInr),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CineSuccess
                                        )
                                    }

                                    Divider(color = CineTimelineRuler, thickness = 0.5.dp)

                                    // User Details & UTR
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("User: ${order.userDisplayName}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            if (order.userEmail.isNotBlank()) {
                                                Text(order.userEmail, fontSize = 11.sp, color = CineTextSecondary)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CineSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("UTR: ${order.utrNumber.orEmpty()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("UTR", order.utrNumber.orEmpty()))
                                                        Toast.makeText(context, "Copied UTR: ${order.utrNumber}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CineTextSecondary, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Action Buttons: Approve or Reject
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val success = repository.adminApproveUpiPayment(order.orderId)
                                                if (success) {
                                                    Toast.makeText(context, "Payment Approved! User Pro activated.", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Approve Subscription", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                rejectingOrder = order
                                                rejectReasonInput = "UTR not verified in bank account"
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CineError),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CineError),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(0.6f).height(44.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Approved History Ledger
                Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Order ID, UTR, or User...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CineTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinePrimary,
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (approvedOrders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No approved transactions match your search.", color = CineTextSecondary)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(approvedOrders) { order ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CineSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(order.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("User: ${order.userDisplayName} • UTR: ${order.utrNumber ?: "N/A"}", fontSize = 11.sp, color = CineTextSecondary)
                                            Text("Approved: ${dateFormatter.format(Date(order.paidAt ?: order.createdAt))}", fontSize = 10.sp, color = CineTertiary)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹%.0f".format(order.amountInr), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CineSuccess.copy(alpha = 0.15f)
                                            ) {
                                                Text("APPROVED ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineSuccess, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // UPI Configuration
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CineSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Active UPI Receiver Configuration", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Official UPI ID", fontSize = 11.sp, color = CineTextSecondary)
                                    Text("robintyagi@fam", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Payee Display Name", fontSize = 11.sp, color = CineTextSecondary)
                                    Text("Robin Tyagi (CineCut Pro)", fontSize = 14.sp, color = Color.White)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Verification Workflow", fontSize = 11.sp, color = CineTextSecondary)
                                    Text("Direct P2P UPI Transfer with 12-Digit UTR Admin Review & Grant", fontSize = 13.sp, color = CineSuccess)
                                }

                                Divider(color = CineTimelineRuler)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status:", fontSize = 12.sp, color = CineTextSecondary)
                                    Text("LIVE & ACCEPTING PAYMENTS ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection Dialog
    if (rejectingOrder != null) {
        val targetOrder = rejectingOrder!!
        AlertDialog(
            onDismissRequest = { rejectingOrder = null },
            containerColor = CineSurface,
            title = { Text("Reject Payment Request", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Are you sure you want to reject the payment of ₹%.0f for '${targetOrder.title}' from user ${targetOrder.userDisplayName}?".format(targetOrder.amountInr),
                        fontSize = 12.sp,
                        color = CineTextSecondary
                    )
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinePrimary,
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.adminRejectUpiPayment(targetOrder.orderId, rejectReasonInput)
                        rejectingOrder = null
                        Toast.makeText(context, "Payment rejected.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineError)
                ) {
                    Text("Confirm Rejection", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingOrder = null }) {
                    Text("Cancel", color = CineTextSecondary)
                }
            }
        )
    }
}
