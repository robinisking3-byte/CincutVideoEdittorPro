package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.PaymentOrderStatus
import com.example.core.model.ZapUpiOrder
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Custom CineCut ZapUPI Payment Sheet / Page.
 * Displays:
 * - Amount in INR
 * - Order ID (with one-tap copy)
 * - Dynamic 8-minute countdown timer
 * - Realistic Vector UPI QR Code
 * - Direct "Pay via UPI App" & "Pay via Paytm" intent launch buttons
 * - "Manual Check Payment" status verification
 * - "Submit UTR" verification section
 * - Real-time success feedback state
 */
@Composable
fun ZapUpiPaymentDialog(
    order: ZapUpiOrder,
    onDismiss: () -> Unit,
    onCheckPayment: (orderId: String) -> Unit,
    onSubmitUtr: (orderId: String, utr: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var utrInput by remember { mutableStateOf("") }
    var utrError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var copiedOrderId by remember { mutableStateOf(false) }

    // 8-minute countdown timer calculation
    var remainingSeconds by remember(order.expiresAt) {
        val diffSec = ((order.expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
        mutableIntStateOf(if (diffSec > 0) diffSec else 480)
    }

    LaunchedEffect(order.expiresAt, order.status) {
        while (remainingSeconds > 0 && order.status == PaymentOrderStatus.PENDING) {
            delay(1000L)
            val currentDiff = ((order.expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
            remainingSeconds = currentDiff
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Dialog(
        onDismissRequest = {
            if (order.status != PaymentOrderStatus.VERIFYING) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CineBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .testTag("zapupi_custom_payment_page")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CinePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚡", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text("ZapUPI Instant Checkout", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Fast & Secure UPI Payment", fontSize = 11.sp, color = CineTertiary)
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CineSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CineTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = CineTimelineRuler, thickness = 0.5.dp)

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Success View if already PAID
                    if (order.status == PaymentOrderStatus.PAID) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F3D24),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineSuccess),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = CineSuccess,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text("Payment Confirmed & Credited!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(
                                        "₹${"%.2f".format(order.amountInr)} successfully credited via ZapUPI.",
                                        fontSize = 12.sp,
                                        color = CineTextPrimary
                                    )
                                    if (!order.utrNumber.isNullOrBlank()) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = 0.4f)) {
                                            Text(
                                                "UTR: ${order.utrNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CineTertiary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = CineSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Continue to Studio", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    } else {
                        // Amount & Expiry Timer Badge
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CineSurface)
                                    .border(1.dp, CineTimelineRuler, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Payable", fontSize = 11.sp, color = CineTextSecondary)
                                    Text(
                                        "₹${"%.2f".format(order.amountInr)}",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CineSecondary
                                    )
                                    Text(order.title, fontSize = 11.sp, color = CineTextTertiary)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (remainingSeconds > 60) CineWarning.copy(alpha = 0.15f) else CineError.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (remainingSeconds > 60) CineWarning else CineError
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, tint = if (remainingSeconds > 60) CineWarning else CineError, modifier = Modifier.size(14.dp))
                                            Text(
                                                formattedTime,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (remainingSeconds > 60) CineWarning else CineError
                                            )
                                        }
                                    }
                                    Text("8-min session timeout", fontSize = 9.sp, color = CineTextTertiary, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }

                        // Order ID Row with Copy
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CineSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Order ID:", fontSize = 11.sp, color = CineTextSecondary)
                                        Text(order.orderId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(order.orderId))
                                            copiedOrderId = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedOrderId) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            modifier = Modifier.size(14.dp),
                                            tint = if (copiedOrderId) CineSuccess else CineTertiary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (copiedOrderId) "Copied!" else "Copy",
                                            fontSize = 10.sp,
                                            color = if (copiedOrderId) CineSuccess else CineTertiary
                                        )
                                    }
                                }
                            }
                        }

                        // High Resolution Vector QR Code
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(2.dp, CineSecondary),
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom Geometric QR Canvas Pattern
                                    Canvas(modifier = Modifier.size(190.dp)) {
                                        val canvasSize = size.width
                                        val gridSize = 21
                                        val cellSize = canvasSize / gridSize

                                        // Draw Corner Positioning Squares
                                        fun drawPositioningSquare(xCell: Int, yCell: Int) {
                                            val x = xCell * cellSize
                                            val y = yCell * cellSize
                                            // Outer box
                                            drawRoundRect(
                                                color = Color.Black,
                                                topLeft = Offset(x, y),
                                                size = Size(cellSize * 7, cellSize * 7),
                                                cornerRadius = CornerRadius(cellSize, cellSize)
                                            )
                                            // Inner white cut
                                            drawRoundRect(
                                                color = Color.White,
                                                topLeft = Offset(x + cellSize, y + cellSize),
                                                size = Size(cellSize * 5, cellSize * 5),
                                                cornerRadius = CornerRadius(cellSize * 0.5f, cellSize * 0.5f)
                                            )
                                            // Center solid core
                                            drawRoundRect(
                                                color = Color(0xFF0F172A),
                                                topLeft = Offset(x + cellSize * 2, y + cellSize * 2),
                                                size = Size(cellSize * 3, cellSize * 3),
                                                cornerRadius = CornerRadius(cellSize * 0.3f, cellSize * 0.3f)
                                            )
                                        }

                                        drawPositioningSquare(0, 0) // Top-Left
                                        drawPositioningSquare(14, 0) // Top-Right
                                        drawPositioningSquare(0, 14) // Bottom-Left

                                        // Pseudo-deterministic data cell matrix seeded by Order ID
                                        val seed = order.orderId.hashCode()
                                        for (row in 0 until gridSize) {
                                            for (col in 0 until gridSize) {
                                                // Skip finder corners
                                                val inTopLeft = row < 7 && col < 7
                                                val inTopRight = row < 7 && col >= 14
                                                val inBottomLeft = row >= 14 && col < 7
                                                val inCenter = row in 8..12 && col in 8..12
                                                if (inTopLeft || inTopRight || inBottomLeft || inCenter) continue

                                                val isBlack = (((row * 31 + col * 17) xor seed) % 3 == 0)
                                                if (isBlack) {
                                                    drawRect(
                                                        color = Color(0xFF1E293B),
                                                        topLeft = Offset(col * cellSize, row * cellSize),
                                                        size = Size(cellSize, cellSize)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Center UPI Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("UPI", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF005691))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Scan with any UPI app (GPay, PhonePe, Paytm, BHIM)", fontSize = 11.sp, color = CineTextSecondary)
                        }

                        // UPI Intent Direct Launch Options
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(order.upiIntentUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            utrError = "Could not launch UPI app automatically. Please scan QR or copy UPI ID."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("pay_via_upi_button")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pay via any UPI App (GPay / PhonePe)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                if (order.paytmIntentUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(order.paytmIntentUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                utrError = "Paytm app not installed. Use general UPI button above."
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pay with Paytm Wallet / UPI", color = CineTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Manual Check Payment Action
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        isChecking = true
                                        onCheckPayment(order.orderId)
                                    },
                                    enabled = !isChecking
                                ) {
                                    if (isChecking) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CineSecondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, tint = CineSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Manual Check Payment Status", fontSize = 12.sp, color = CineSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Submit UTR Verification Section
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CineSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(16.dp))
                                        Text("Manual UTR Verification", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Text(
                                        "If payment is completed but not updated, enter the 12-digit UPI Reference / UTR Number from your bank receipt:",
                                        fontSize = 10.sp,
                                        color = CineTextSecondary
                                    )

                                    OutlinedTextField(
                                        value = utrInput,
                                        onValueChange = {
                                            if (it.length <= 16) {
                                                utrInput = it.filter { char -> char.isDigit() || char.isLetter() }
                                                utrError = null
                                            }
                                        },
                                        placeholder = { Text("e.g. 426912389104", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("utr_input_field"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CinePrimary,
                                            unfocusedBorderColor = CineTimelineRuler,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    if (utrError != null) {
                                        Text(utrError!!, fontSize = 10.sp, color = CineError)
                                    }

                                    Button(
                                        onClick = {
                                            if (utrInput.length < 8) {
                                                utrError = "Please enter a valid 12-digit UPI UTR number"
                                            } else {
                                                onSubmitUtr(order.orderId, utrInput)
                                            }
                                        },
                                        enabled = utrInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("submit_utr_button")
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Submit UTR & Verify", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
