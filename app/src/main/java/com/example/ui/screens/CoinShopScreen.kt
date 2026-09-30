package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CoinType
import com.example.core.model.LimitedCoinSupply
import com.example.core.model.hasPermission
import com.example.core.repository.CineCutRepository
import com.example.ui.components.CineCoinStatusBadge
import com.example.ui.components.CoinStatusInspectDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinShopScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    onNavigateToMembership: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "MEMBERSHIP", "LIMITED", "CREATOR_STAFF"
    var inspectCoin by remember { mutableStateOf<CoinType?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val limitedSupplies = remember {
        listOf(
            LimitedCoinSupply(CoinType.FOUNDER, totalMinted = 27, maxSupply = 100),
            LimitedCoinSupply(CoinType.EARLY_SUPPORTER, totalMinted = 104, maxSupply = 500)
        )
    }

    val displayedCoins = remember(selectedCategory) {
        when (selectedCategory) {
            "MEMBERSHIP" -> listOf(CoinType.BRONZE, CoinType.SILVER, CoinType.GOLD, CoinType.DIAMOND, CoinType.VIP)
            "LIMITED" -> listOf(CoinType.FOUNDER, CoinType.EARLY_SUPPORTER)
            "CREATOR_STAFF" -> listOf(CoinType.VERIFIED_CREATOR, CoinType.MODERATOR, CoinType.ADMIN)
            else -> CoinType.values().toList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Coin Vault & Status Shop", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Creator Identity & Community Badges", fontSize = 11.sp, color = CineTertiary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("coin_shop_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Equipped Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CineSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(currentUser.coinType.accentColor).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CURRENTLY EQUIPPED BADGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CineSuccess.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineSuccess)
                            ) {
                                Text(
                                    "ACTIVE IDENTITY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CineCoinStatusBadge(
                                coinType = currentUser.coinType,
                                editionNumber = currentUser.coinEditionNumber,
                                maxSupply = currentUser.coinMaxSupply,
                                size = 56.dp,
                                showInspectDialogOnClick = true
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.coinType.displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                if (currentUser.isCoinLimited && currentUser.coinEditionNumber != null) {
                                    Text(
                                        text = "Edition #${currentUser.coinEditionNumber} of ${currentUser.coinMaxSupply}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(currentUser.coinType.accentColor)
                                    )
                                } else {
                                    Text(
                                        text = "Rank Level ${currentUser.coinLevel} • ${currentUser.coinAnimation.label}",
                                        fontSize = 12.sp,
                                        color = CineTextSecondary
                                    )
                                }
                                Text(
                                    text = "Assigned by: ${currentUser.coinAssignedBy ?: "CineCut Protocol"}",
                                    fontSize = 11.sp,
                                    color = CineTextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // Limited Edition Supply Progress Card
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CineSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CineTimelineRuler),
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
                            Text("LIMITED COIN SCARCITY LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineTextTertiary)
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CineTertiary, modifier = Modifier.size(14.dp))
                        }

                        limitedSupplies.forEach { supply ->
                            val progress = supply.totalMinted.toFloat() / supply.maxSupply.toFloat()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CineCoinStatusBadge(coinType = supply.coinType, size = 16.dp, showInspectDialogOnClick = false)
                                        Text(supply.coinType.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Text(
                                        "${supply.totalMinted} / ${supply.maxSupply} Claimed (${supply.remainingSupply} left)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(supply.coinType.accentColor)
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(supply.coinType.baseColor),
                                    trackColor = CineTimelineRuler
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Carousel
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf(
                        "ALL" to "All Badges (${CoinType.values().size})",
                        "MEMBERSHIP" to "Tier Coins",
                        "LIMITED" to "Limited Editions",
                        "CREATOR_STAFF" to "Staff & Creator"
                    )
                    items(categories) { (id, label) ->
                        FilterChip(
                            selected = selectedCategory == id,
                            onClick = { selectedCategory = id },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Coin Catalog Items
            items(displayedCoins) { coin ->
                val isEquipped = currentUser.coinType == coin
                val isEligible = when (coin) {
                    CoinType.DEFAULT -> true
                    CoinType.BRONZE -> currentUser.membershipTier.rank >= 1
                    CoinType.SILVER -> currentUser.membershipTier.rank >= 2
                    CoinType.GOLD -> currentUser.membershipTier.rank >= 3
                    CoinType.DIAMOND -> currentUser.membershipTier.rank >= 4
                    CoinType.VIP -> currentUser.membershipTier.rank >= 5
                    CoinType.FOUNDER -> currentUser.membershipTier.rank >= 6
                    CoinType.VERIFIED_CREATOR -> currentUser.badges.contains("Verified Creator")
                    CoinType.ADMIN -> currentUser.adminRole.hasPermission(com.example.core.model.AdminPermission.SECURITY_CENTER)
                    CoinType.MODERATOR -> currentUser.adminRole.hasPermission(com.example.core.model.AdminPermission.MODERATE_CONTENT)
                    else -> false
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CineSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isEquipped) Color(coin.accentColor) else CineTimelineRuler
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CineCoinStatusBadge(
                            coinType = coin,
                            editionNumber = if (coin.isLimited) 27 else null,
                            size = 44.dp,
                            showInspectDialogOnClick = true
                        )

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(coin.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (coin.isLimited) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(coin.baseColor).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(coin.accentColor))
                                    ) {
                                        Text(
                                            "LIMITED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(coin.accentColor),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = coin.description,
                                fontSize = 11.sp,
                                color = CineTextSecondary,
                                maxLines = 2
                            )

                            Text(
                                text = "Animation: ${coin.defaultAnimation.label}",
                                fontSize = 10.sp,
                                color = CineTertiary
                            )
                        }

                        // Action / Acquisition Status Button
                        Column(horizontalAlignment = Alignment.End) {
                            if (isEquipped) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CinePrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "EQUIPPED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CinePrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (isEligible) {
                                Button(
                                    onClick = {
                                        inspectCoin = coin
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Inspect", fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        if (coin == CoinType.VIP || coin == CoinType.FOUNDER || coin == CoinType.DIAMOND || coin == CoinType.GOLD) {
                                            onNavigateToMembership()
                                        } else {
                                            inspectCoin = coin
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        if (coin.isStaffOnly) "Staff Only" else "Unlock",
                                        fontSize = 10.sp,
                                        color = CineTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Inspect Dialog if opened
    if (inspectCoin != null) {
        CoinStatusInspectDialog(
            coinType = inspectCoin!!,
            editionNumber = if (inspectCoin!!.isLimited) 27 else null,
            onDismiss = { inspectCoin = null }
        )
    }

    if (toastMessage != null) {
        LaunchedEffect(toastMessage) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CineSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinePrimary)
            ) {
                Text(toastMessage!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}
