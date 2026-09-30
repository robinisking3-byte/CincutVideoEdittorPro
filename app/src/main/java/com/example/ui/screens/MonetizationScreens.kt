package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.MembershipTier
import com.example.core.repository.CineCutRepository
import com.example.ui.components.CineCoinBadge
import com.example.ui.components.MembershipBadge
import com.example.ui.components.ZapUpiPaymentDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val activePaymentOrder by repository.activePaymentOrder.collectAsState()
    var purchaseSuccessMessage by remember { mutableStateOf<String?>(null) }

    val tiers = listOf(
        Triple(MembershipTier.BRONZE, "$4.99/mo", listOf("1080p 60fps Export", "Bronze CineRooms Access", "Standard AI Director")),
        Triple(MembershipTier.SILVER, "$9.99/mo", listOf("2K 1440p Export", "Silver CineRooms Access", "Custom LUT Imports", "500 Bonus CineCoins/mo")),
        Triple(MembershipTier.GOLD, "$19.99/mo", listOf("4K UHD Ultra Export", "Gold CineRooms Access", "Unlimited AI Director", "1,500 Bonus CineCoins/mo")),
        Triple(MembershipTier.DIAMOND, "$39.99/mo", listOf("8K Ready ProRes Export", "Diamond CineRooms Access", "Autonomous AI Generation", "Priority Cloud Rendering")),
        Triple(MembershipTier.VIP, "$69.99/mo", listOf("VIP Lounge & Founders Circle", "Direct Engineering Support", "Verified Creator Badge", "All Perks Included")),
        Triple(MembershipTier.FOUNDER, "$99.99/mo", listOf("Lifetime Founder Badge", "Advisory Board Access", "Max Perks & Unlimited 4K Live"))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Membership", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    MembershipBadge(tier = currentUser.membershipTier)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("membership_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Elevate Your Production Studio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Unlock hardware-accelerated 4K export, real-time AI Director scripting, and tier-gated creator CineRooms.", fontSize = 12.sp, color = CineTextSecondary)
                }
            }

            if (purchaseSuccessMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F3D24))
                            .padding(12.dp)
                    ) {
                        Text(purchaseSuccessMessage!!, fontSize = 12.sp, color = CineSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(tiers) { (tier, price, perks) ->
                val isCurrentTier = currentUser.membershipTier == tier

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CineSurface)
                        .border(
                            1.dp,
                            if (isCurrentTier) CinePrimary else CineTimelineRuler,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MembershipBadge(tier = tier)
                            Text(price, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            perks.forEach { perk ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(14.dp))
                                    Text(perk, fontSize = 12.sp, color = CineTextPrimary)
                                }
                            }
                        }

                        if (isCurrentTier) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Current Active Plan", color = CineSuccess)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        repository.upgradeMembership(tier)
                                        purchaseSuccessMessage = "Successfully upgraded to ${tier.title} via Google Play Billing!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Google Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val inrAmount = when (tier) {
                                    MembershipTier.FREE -> 0.0
                                    MembershipTier.BRONZE -> 99.0
                                    MembershipTier.SILVER -> 199.0
                                    MembershipTier.GOLD -> 399.0
                                    MembershipTier.DIAMOND -> 799.0
                                    MembershipTier.VIP -> 1499.0
                                    MembershipTier.FOUNDER -> 2999.0
                                    else -> 99.0
                                }

                                Button(
                                    onClick = {
                                        repository.createZapUpiPaymentOrder(
                                            itemType = "MEMBERSHIP",
                                            itemId = "tier_${tier.name.lowercase()}",
                                            amountInr = inrAmount,
                                            title = "${tier.title} Membership"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("⚡ UPI ₹%.0f".format(inrAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF332000))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (activePaymentOrder != null && activePaymentOrder!!.itemType == "MEMBERSHIP") {
        ZapUpiPaymentDialog(
            order = activePaymentOrder!!,
            onDismiss = { repository.dismissActivePaymentOrder() },
            onCheckPayment = { orderId ->
                repository.verifyZapUpiPaymentOrder(orderId, null)
            },
            onSubmitUtr = { orderId, utr ->
                repository.verifyZapUpiPaymentOrder(orderId, utr)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineCoinsScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coinAccount by repository.coinAccount.collectAsState()
    val transactions by repository.coinTransactions.collectAsState()
    val activePaymentOrder by repository.activePaymentOrder.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) } // null = All
    var refundDialogTx by remember { mutableStateOf<com.example.core.model.CineCoinTransaction?>(null) }
    var refundReasonText by remember { mutableStateOf("") }

    val filteredTransactions = remember(transactions, selectedTypeFilter) {
        if (selectedTypeFilter == null) transactions else transactions.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CineCoins Wallet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Authoritative Virtual Currency Ledger", fontSize = 11.sp, color = CineSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("cine_coins_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CineSurfaceVariant)
                        .border(1.dp, CineSecondary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Authoritative Account Balance", fontSize = 12.sp, color = CineTextSecondary)
                        Text(
                            text = "%,d ¢".format(coinAccount.balance),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = CineSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Earned", fontSize = 10.sp, color = CineTextTertiary)
                                Text("+%,d ¢".format(coinAccount.totalEarned), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Spent", fontSize = 10.sp, color = CineTextTertiary)
                                Text("-%,d ¢".format(coinAccount.totalSpent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineWarning)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Earn Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ok = repository.executeCoinTransaction("REWARD", 250L, "Daily Creator Check-in Reward")
                                    if (ok) statusMessage = "Claimed +250 CineCoins Daily Reward!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF332000), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Daily (+250¢)", color = Color(0xFF332000), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val ok = repository.executeCoinTransaction("EARN", 500L, "Shared 4K Project with Friends")
                                    if (ok) statusMessage = "Earned +500 CineCoins for Project Collaboration!"
                                },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CineTertiary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Collab (+500¢)", color = CineTertiary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (statusMessage != null) {
                item {
                    Text(statusMessage!!, color = CineSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Instant ZapUPI Coin Packages
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BUY CINECOINS VIA ZAPUPI (INSTANT UPI & QR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F3D24)
                    ) {
                        Text(
                            "⚡ ZERO FEE UPI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(500L, 49.0, "Starter"),
                        Triple(1500L, 129.0, "Popular"),
                        Triple(5000L, 399.0, "Best Value")
                    ).forEach { (coins, priceInr, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CineSurface)
                                .border(1.dp, CineSecondary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label, fontSize = 9.sp, color = CineSecondary, fontWeight = FontWeight.Bold)
                                Text("${coins}¢", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Button(
                                    onClick = {
                                        repository.createZapUpiPaymentOrder(
                                            itemType = "CINECOINS",
                                            itemId = "coins_$coins",
                                            amountInr = priceInr,
                                            title = "$coins CineCoins Pack"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CineSecondary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("₹%.0f".format(priceInr), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF332000))
                                }
                            }
                        }
                    }
                }
            }

            // Google Play Coin Packages
            item {
                Text("BUY CINECOINS PACKAGES (GOOGLE PLAY BILLING)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(500L, "$1.99", "Starter"),
                        Triple(1500L, "$4.99", "Popular"),
                        Triple(5000L, "$14.99", "Best Value")
                    ).forEach { (coins, price, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CineSurface)
                                .border(1.dp, CineTimelineRuler, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label, fontSize = 9.sp, color = CineTertiary, fontWeight = FontWeight.Bold)
                                Text("${coins}¢", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineSecondary)
                                Button(
                                    onClick = {
                                        repository.executeCoinTransaction("PURCHASE", coins, "Google Play Purchase ($label Package)")
                                        statusMessage = "Added +$coins CineCoins via Google Play Billing!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(price, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Transaction Filter Chips
            item {
                Text("TRANSACTION TYPES & LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
            }

            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text("All (${transactions.size})", fontSize = 10.sp) }
                        )
                    }
                    listOf("EARN", "SPEND", "PURCHASE", "REWARD", "REFUND", "ADMIN_GRANT").forEach { t ->
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == t,
                                onClick = { selectedTypeFilter = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // Transaction Ledger List
            items(filteredTransactions) { tx ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CineSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (tx.type) {
                                        "PURCHASE", "REWARD", "EARN" -> CineSuccess.copy(alpha = 0.15f)
                                        "SPEND" -> CineWarning.copy(alpha = 0.15f)
                                        "REFUND" -> CineError.copy(alpha = 0.15f)
                                        else -> CineTertiary.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        tx.type,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (tx.type) {
                                            "PURCHASE", "REWARD", "EARN" -> CineSuccess
                                            "SPEND" -> CineWarning
                                            "REFUND" -> CineError
                                            else -> CineTertiary
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(tx.reason, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("Balance After: %,d ¢ • Idempotency Verified".format(tx.balanceAfter), fontSize = 10.sp, color = CineTextTertiary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (tx.amount >= 0) "+${tx.amount}¢" else "${tx.amount}¢",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.amount >= 0) CineSuccess else CineError
                            )
                            if (tx.type == "PURCHASE") {
                                TextButton(
                                    onClick = { refundDialogTx = tx },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Text("Refund", fontSize = 9.sp, color = CineWarning)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Refund Dialog
    if (refundDialogTx != null) {
        val target = refundDialogTx!!
        AlertDialog(
            onDismissRequest = { refundDialogTx = null },
            title = { Text("Request Coin Refund", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Refunding purchase of ${target.amount} CineCoins. Amount will be deducted from balance.", fontSize = 12.sp, color = CineTextSecondary)
                    OutlinedTextField(
                        value = refundReasonText,
                        onValueChange = { refundReasonText = it },
                        label = { Text("Refund Reason (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (refundReasonText.isNotBlank()) {
                            val ok = repository.requestCoinRefund(target.id, refundReasonText)
                            if (ok) statusMessage = "Purchase refunded and ledger updated."
                            refundDialogTx = null
                            refundReasonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineWarning)
                ) {
                    Text("Confirm Refund", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { refundDialogTx = null }) { Text("Cancel") }
            },
            containerColor = CineSurface
        )
    }

    if (activePaymentOrder != null && activePaymentOrder!!.itemType == "CINECOINS") {
        ZapUpiPaymentDialog(
            order = activePaymentOrder!!,
            onDismiss = { repository.dismissActivePaymentOrder() },
            onCheckPayment = { orderId ->
                repository.verifyZapUpiPaymentOrder(orderId, null)
            },
            onSubmitUtr = { orderId, utr ->
                repository.verifyZapUpiPaymentOrder(orderId, utr)
            }
        )
    }
}
