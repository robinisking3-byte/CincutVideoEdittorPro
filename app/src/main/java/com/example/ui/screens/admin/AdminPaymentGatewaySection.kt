package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin Panel → Settings → Payment Gateway → ZapUPI
 *
 * Secure Payment Gateway management interface:
 * - Enter & update ZapUPI API key via secure password-style field with visibility toggle
 * - Server-side storage in Google Cloud Secret Manager
 * - Client never receives or stores raw API keys (masked view: ••••••••••••••••••••153d54)
 * - Enable/disable gateway toggle
 * - Test gateway connection with real-time ping & latency
 * - Payment orders ledger with search by Order ID / UTR / User
 * - Refund payment workflows with immutable audit logging
 * - Dedicated ZapUPI audit logs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentGatewaySection(
    repository: CineCutRepository,
    currentAdminRole: AdminRole,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val zapUpiConfig by repository.zapUpiConfig.collectAsState()
    val paymentOrders by repository.paymentOrders.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Configuration, 1: Orders Ledger, 2: Audit Logs
    var apiKeyInput by remember { mutableStateOf("") }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var timeoutInput by remember { mutableStateOf(zapUpiConfig.timeoutSeconds.toString()) }
    var currencyInput by remember { mutableStateOf(zapUpiConfig.currency) }
    var isTestMode by remember { mutableStateOf(zapUpiConfig.isTestMode) }
    var isEnabled by remember { mutableStateOf(zapUpiConfig.isEnabled) }

    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Orders Filter & Search
    var selectedStatusFilter by remember { mutableStateOf<PaymentOrderStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var refundingOrder by remember { mutableStateOf<ZapUpiOrder?>(null) }
    var refundReason by remember { mutableStateOf("") }

    val filteredOrders = remember(paymentOrders, selectedStatusFilter, searchQuery) {
        paymentOrders.filter { order ->
            val matchesStatus = selectedStatusFilter == null || order.status == selectedStatusFilter
            val matchesQuery = searchQuery.isBlank() ||
                    order.orderId.contains(searchQuery, ignoreCase = true) ||
                    (order.utrNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                    order.userDisplayName.contains(searchQuery, ignoreCase = true) ||
                    order.userId.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    val zapUpiAuditLogs = remember(auditLogs) {
        auditLogs.filter { log ->
            log.target.contains("ZapUPI", ignoreCase = true) ||
                    log.action.contains("ZAPUPI", ignoreCase = true) ||
                    log.action.contains("PAYMENT", ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_payment_gateway_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hierarchical Breadcrumb Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                Text("Admin Panel", fontSize = 11.sp, color = CineTertiary)
                Text("›", fontSize = 12.sp, color = CineTextTertiary)
                Text("Settings", fontSize = 11.sp, color = CineTertiary)
                Text("›", fontSize = 12.sp, color = CineTextTertiary)
                Text("Payment Gateway", fontSize = 11.sp, color = CineTertiary)
                Text("›", fontSize = 12.sp, color = CinePrimary, fontWeight = FontWeight.Bold)
                Text("ZapUPI", fontSize = 11.sp, color = CinePrimary, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ZapUPI Payment Gateway",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Production UPI & QR payment infrastructure with Secret Manager security",
                        fontSize = 11.sp,
                        color = CineTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F3D24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CineSuccess)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(12.dp))
                        Text("SECRET MANAGER SECURED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                    }
                }
            }
        }

        // Sub Tabs
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = CineSurface,
            contentColor = CinePrimary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("ZapUPI Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Payment Orders (${paymentOrders.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Payment Audit Logs (${zapUpiAuditLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Active View
        when (activeSubTab) {
            0 -> {
                // CONFIGURATION TAB
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gateway Live Status Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (zapUpiConfig.isEnabled) CineSuccess.copy(alpha = 0.5f) else CineWarning.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (zapUpiConfig.isEnabled) CineSuccess.copy(alpha = 0.2f) else CineSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("⚡", fontSize = 18.sp)
                                            }
                                        }
                                        Column {
                                            Text("Gateway Status", fontSize = 11.sp, color = CineTextTertiary)
                                            Text(
                                                text = if (zapUpiConfig.isEnabled) zapUpiConfig.status else "Gateway Disabled",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (zapUpiConfig.isEnabled) CineSuccess else CineWarning
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isTestingConnection = true
                                            val res = repository.adminTestZapUpiConnection()
                                            testResult = res
                                            isTestingConnection = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Connection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (testResult != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (testResult!!.first) Color(0xFF0F3D24) else Color(0xFF3D1414),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = testResult!!.second,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (testResult!!.first) CineSuccess else CineError,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Divider(color = CineTimelineRuler, thickness = 0.5.dp)

                                // Stored Masked Key Notice
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Current Stored API Key:", fontSize = 10.sp, color = CineTextTertiary)
                                        Text(
                                            text = "API Key: ${zapUpiConfig.maskedApiKey}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineSecondary
                                        )
                                        Text(
                                            text = "Status: ${zapUpiConfig.status}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CineSuccess
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CineSurfaceVariant
                                    ) {
                                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                        Text(
                                            text = "Verified: ${dateFormat.format(Date(zapUpiConfig.lastVerifiedAt))}",
                                            fontSize = 9.sp,
                                            color = CineTextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Security Card & Policy
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CineSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(20.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Zero-Trust Secret Protection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        "The ZapUPI API key is sent directly to Firebase Cloud Functions and saved in Google Cloud Secret Manager (or secure server-side vault). The Android client never receives, logs, or stores raw keys. After saving, only the masked signature is shown.",
                                        fontSize = 11.sp,
                                        color = CineTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Key Input & Form
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("CONFIGURE ZAPUPI CREDENTIALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)

                                // Secure Password-Style Input Field
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("ZapUPI API Key", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        placeholder = { Text("•••••••••••••••••••") },
                                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                                Icon(
                                                    imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (isApiKeyVisible) "Hide API Key" else "Show API Key",
                                                    tint = CineTextSecondary
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("zapupi_api_key_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CinePrimary,
                                            unfocusedBorderColor = CineTimelineRuler,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Text(
                                        "Enter through secure password field. Raw key is never visible after save.",
                                        fontSize = 10.sp,
                                        color = CineTextTertiary
                                    )
                                }

                                // Enabled / Disabled Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Gateway Enabled", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        Text("Allow users to initiate UPI checkout", fontSize = 11.sp, color = CineTextSecondary)
                                    }
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { isEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CinePrimary,
                                            checkedTrackColor = CinePrimary.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                // Test / Live Mode Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            if (isTestMode) "Test / Sandbox Mode" else "Live Production Mode",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isTestMode) CineWarning else CineSuccess
                                        )
                                        Text(
                                            if (isTestMode) "Simulates VPA transactions without real funds" else "Processes actual UPI transactions",
                                            fontSize = 11.sp,
                                            color = CineTextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isTestMode,
                                        onCheckedChange = { isTestMode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CineWarning,
                                            checkedTrackColor = CineWarning.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                // Payment Timeout & Currency
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = timeoutInput,
                                        onValueChange = { timeoutInput = it },
                                        label = { Text("Timeout (Seconds)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = currencyInput,
                                        onValueChange = { currencyInput = it },
                                        label = { Text("Currency") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                // Save Configuration Button
                                Button(
                                    onClick = {
                                        if (apiKeyInput.isNotBlank()) {
                                            val tSeconds = timeoutInput.toIntOrNull() ?: 480
                                            val ok = repository.adminSaveZapUpiConfig(
                                                apiKey = apiKeyInput,
                                                isEnabled = isEnabled,
                                                timeoutSeconds = tSeconds,
                                                currency = currencyInput.ifBlank { "INR" },
                                                isTestMode = isTestMode
                                            )
                                            if (ok) {
                                                actionFeedback = "Configuration successfully encrypted and saved to Secret Manager! Raw key removed from memory."
                                                apiKeyInput = "" // Clean from memory
                                                isApiKeyVisible = false
                                            }
                                        } else {
                                            // Toggle without changing API key
                                            repository.adminToggleZapUpi(isEnabled)
                                            actionFeedback = "Gateway settings updated."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("save_zapupi_config_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                if (actionFeedback != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CineSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = actionFeedback!!,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineSuccess,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // PAYMENT ORDERS LEDGER TAB
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search & Filter Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Order ID, UTR, or User Name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CineTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CineTextSecondary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("orders_search_field"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Status Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == null,
                                onClick = { selectedStatusFilter = null },
                                label = { Text("All (${paymentOrders.size})", fontSize = 10.sp) }
                            )
                        }
                        PaymentOrderStatus.values().forEach { status ->
                            val count = paymentOrders.count { it.status == status }
                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == status,
                                    onClick = { selectedStatusFilter = status },
                                    label = { Text("${status.name} ($count)", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (status) {
                                            PaymentOrderStatus.PAID -> CineSuccess
                                            PaymentOrderStatus.PENDING, PaymentOrderStatus.VERIFYING -> CineWarning
                                            PaymentOrderStatus.REFUNDED -> CineTertiary
                                            PaymentOrderStatus.FAILED, PaymentOrderStatus.EXPIRED -> CineError
                                        }
                                    )
                                )
                            }
                        }
                    }

                    // Orders List
                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No payment orders match the filter criteria.", fontSize = 12.sp, color = CineTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredOrders) { order ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CineSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when (order.status) {
                                            PaymentOrderStatus.PAID -> CineSuccess.copy(alpha = 0.4f)
                                            PaymentOrderStatus.PENDING -> CineWarning.copy(alpha = 0.4f)
                                            PaymentOrderStatus.REFUNDED -> CineTertiary.copy(alpha = 0.4f)
                                            else -> CineTimelineRuler
                                        }
                                    ),
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(order.orderId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                IconButton(
                                                    onClick = { clipboardManager.setText(AnnotatedString(order.orderId)) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Order ID", tint = CineTextSecondary, modifier = Modifier.size(12.dp))
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (order.status) {
                                                    PaymentOrderStatus.PAID -> CineSuccess.copy(alpha = 0.2f)
                                                    PaymentOrderStatus.PENDING, PaymentOrderStatus.VERIFYING -> CineWarning.copy(alpha = 0.2f)
                                                    PaymentOrderStatus.REFUNDED -> CineTertiary.copy(alpha = 0.2f)
                                                    PaymentOrderStatus.FAILED, PaymentOrderStatus.EXPIRED -> CineError.copy(alpha = 0.2f)
                                                }
                                            ) {
                                                Text(
                                                    text = order.status.name,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (order.status) {
                                                        PaymentOrderStatus.PAID -> CineSuccess
                                                        PaymentOrderStatus.PENDING, PaymentOrderStatus.VERIFYING -> CineWarning
                                                        PaymentOrderStatus.REFUNDED -> CineTertiary
                                                        PaymentOrderStatus.FAILED, PaymentOrderStatus.EXPIRED -> CineError
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(order.title, fontSize = 12.sp, color = CineTextPrimary)
                                                Text("User: ${order.userDisplayName} (${order.userId})", fontSize = 10.sp, color = CineTextSecondary)
                                            }

                                            Text(
                                                text = "₹%.2f".format(order.amountInr),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = CineSecondary
                                            )
                                        }

                                        if (order.utrNumber != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("UTR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                                                Text(order.utrNumber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CineTertiary)
                                            }
                                        }

                                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                                        Text(
                                            text = "Created: ${dateFormat.format(Date(order.createdAt))}" +
                                                    (if (order.paidAt != null) " • Paid: ${dateFormat.format(Date(order.paidAt))}" else ""),
                                            fontSize = 10.sp,
                                            color = CineTextTertiary
                                        )

                                        if (order.refundReason != null) {
                                            Text(
                                                text = "Refund Reason: ${order.refundReason}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CineTertiary
                                            )
                                        }

                                        // Action Buttons
                                        if (currentAdminRole.hasPermission(AdminPermission.SECURITY_CENTER)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (order.status == PaymentOrderStatus.PAID) {
                                                    OutlinedButton(
                                                        onClick = { refundingOrder = order },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineWarning),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, CineWarning.copy(alpha = 0.5f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Issue Refund", fontSize = 10.sp)
                                                    }
                                                }

                                                if (order.status == PaymentOrderStatus.PENDING) {
                                                    Button(
                                                        onClick = {
                                                            repository.verifyZapUpiPaymentOrder(order.orderId, order.utrNumber)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Force Verify", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                    }
                                                }
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
                // PAYMENT AUDIT LOGS TAB
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("IMMUTABLE PAYMENT GATEWAY SECURITY AUDIT TRAIL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                    }

                    if (zapUpiAuditLogs.isEmpty()) {
                        item {
                            Text("No payment audit records recorded yet.", fontSize = 11.sp, color = CineTextSecondary)
                        }
                    }

                    items(zapUpiAuditLogs) { log ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CineSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                                    val df = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
                                    Text(df.format(Date(log.timestamp)), fontSize = 9.sp, color = CineTextTertiary)
                                }
                                Text("Admin / User: ${log.adminUid} • Target: ${log.target}", fontSize = 10.sp, color = CineTextSecondary)
                                Text(log.reason, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Refund Confirmation Dialog
    if (refundingOrder != null) {
        AlertDialog(
            onDismissRequest = { refundingOrder = null },
            title = { Text("Refund Payment Order", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Are you sure you want to refund ₹${refundingOrder!!.amountInr} for Order ${refundingOrder!!.orderId}? This will reverse credited coins and log an audit action.",
                        fontSize = 12.sp,
                        color = CineTextSecondary
                    )
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("Refund Reason (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (refundReason.isNotBlank()) {
                            repository.adminRefundPaymentOrder(refundingOrder!!.orderId, refundReason)
                            refundingOrder = null
                            refundReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineWarning)
                ) {
                    Text("Execute Refund", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { refundingOrder = null }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }
}
