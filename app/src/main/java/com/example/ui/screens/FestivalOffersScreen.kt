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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.components.FestivalParticlesOverlay
import com.example.ui.components.ZapUpiPaymentDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivalOffersScreen(
    repository: CineCutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val festivalConfig by repository.festivalConfig.collectAsState()
    val allOffers by repository.festivalOffers.collectAsState()
    val coupons by repository.festivalCoupons.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val activePaymentOrder by repository.activePaymentOrder.collectAsState()

    var selectedCategory by remember { mutableStateOf(FestivalOfferItemCategory.ALL) }
    var enteredCouponCode by remember { mutableStateOf("") }
    var appliedCouponCode by remember { mutableStateOf<String?>(null) }
    var couponValidationMessage by remember { mutableStateOf<String?>(null) }
    var selectedOfferForCheckout by remember { mutableStateOf<FestivalOffer?>(null) }
    var checkoutSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Live ticking countdown timer
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val remainingMs = remember(currentTime, festivalConfig.endDate) {
        (festivalConfig.endDate - currentTime).coerceAtLeast(0L)
    }

    val countdownFormatted = remember(remainingMs) {
        val hours = TimeUnit.MILLISECONDS.toHours(remainingMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    val displayedOffers = remember(allOffers, selectedCategory, currentTime) {
        allOffers
            .filter { it.isCurrentlyActive(currentTime) }
            .filter { selectedCategory == FestivalOfferItemCategory.ALL || it.category == selectedCategory }
    }

    val theme = festivalConfig.themeConfig
    val primaryColor = Color(theme.primaryAccentHex)
    val secondaryColor = Color(theme.secondaryAccentHex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "${festivalConfig.activeFestival.displayName} Offers",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Festival Store • Instant Perks",
                                fontSize = 10.sp,
                                color = secondaryColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Quick low-end animation toggle button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineSurfaceHighlight)
                            .clickable {
                                repository.setReduceAnimations(!festivalConfig.animationConfig.reduceAnimations)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (festivalConfig.animationConfig.reduceAnimations) Icons.Default.Speed else Icons.Default.AutoAwesome,
                            contentDescription = "Reduce Animations",
                            tint = if (festivalConfig.animationConfig.reduceAnimations) CineSuccess else secondaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (festivalConfig.animationConfig.reduceAnimations) "Lite FX" else "Rich FX",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineSurface)
            )
        },
        containerColor = CineBackground,
        modifier = modifier.testTag("festival_offers_screen")
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Lightweight ambient festival animation layer
            if (festivalConfig.animationConfig.enabled) {
                FestivalParticlesOverlay(
                    config = festivalConfig.animationConfig,
                    modifier = Modifier.fillMaxSize()
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Festive Event Hero Card with Real-time Countdown
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(theme.surfaceGradientStart),
                                            Color(theme.surfaceGradientEnd)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = primaryColor
                                    ) {
                                        Text(
                                            text = theme.bannerBadgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Real Countdown Timer Pill
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.Black.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, secondaryColor)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Ends in $countdownFormatted",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = theme.bannerHeadline,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Text(
                                    text = theme.bannerSubheadline,
                                    fontSize = 12.sp,
                                    color = CineTextSecondary,
                                    lineHeight = 16.sp
                                )

                                // Bonus CineCoin banner pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33FFD700),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, secondaryColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text(
                                                text = "+${festivalConfig.cineCoinBonusPercent}% CineCoin Bonus On All Festival Packs!",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "First-time buyers receive +${festivalConfig.firstPurchaseBonusPercent}% extra coins automatically",
                                                fontSize = 10.sp,
                                                color = CineTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Coupon Code Entry Box
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CineSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Have a Festival Coupon Code?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = enteredCouponCode,
                                    onValueChange = {
                                        enteredCouponCode = it.uppercase()
                                        couponValidationMessage = null
                                    },
                                    placeholder = { Text("e.g. DIWALI50, FESTIVE20", fontSize = 12.sp, color = CineTextTertiary) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("festival_coupon_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = CineTimelineRuler,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Button(
                                    onClick = {
                                        val code = enteredCouponCode.trim()
                                        if (code.isBlank()) return@Button
                                        val (valid, _) = repository.validateAndApplyCoupon(code, 499.0)
                                        if (valid) {
                                            appliedCouponCode = code
                                            couponValidationMessage = "✓ Coupon '$code' applied successfully!"
                                        } else {
                                            couponValidationMessage = "✕ Invalid or expired coupon code."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier.testTag("apply_coupon_button")
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (couponValidationMessage != null) {
                                Text(
                                    text = couponValidationMessage!!,
                                    fontSize = 11.sp,
                                    color = if (couponValidationMessage!!.startsWith("✓")) CineSuccess else CineError,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Active quick coupons chips
                            Text("Available Festival Coupons:", fontSize = 10.sp, color = CineTextSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(coupons.filter { it.enabled }) { c ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CineSurfaceHighlight,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, secondaryColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable {
                                            enteredCouponCode = c.code
                                            appliedCouponCode = c.code
                                            couponValidationMessage = "✓ Selected '${c.code}' (${c.discountPercent}% Off)"
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${c.code} (${c.discountPercent}%)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Success Message Banner if purchased
                if (checkoutSuccessMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F3D24),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CineSuccess),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CineSuccess)
                                Text(
                                    text = checkoutSuccessMessage!!,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Category Chips Filter
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FestivalOfferItemCategory.values()) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = CineSurface,
                                    labelColor = CineTextPrimary
                                )
                            )
                        }
                    }
                }

                // Empty state if no offers active
                if (displayedOffers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = CineTextTertiary, modifier = Modifier.size(40.dp))
                                Text("No Active Festival Offers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Check back soon or explore other categories.", fontSize = 12.sp, color = CineTextSecondary)
                            }
                        }
                    }
                } else {
                    // List of Active Festival Offers
                    items(displayedOffers) { offer ->
                        FestivalOfferCard(
                            offer = offer,
                            appliedCoupon = appliedCouponCode,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onCheckout = {
                                selectedOfferForCheckout = offer
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Checkout Dialog for Offer
    if (selectedOfferForCheckout != null) {
        val offer = selectedOfferForCheckout!!
        val (_, finalPrice, _) = repository.computeSecureOfferCheckout(offer.offerId, appliedCouponCode)

        AlertDialog(
            onDismissRequest = { selectedOfferForCheckout = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = primaryColor)
                    Text("Confirm Festival Purchase", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(offer.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(offer.description, fontSize = 12.sp, color = CineTextSecondary)

                    Divider(color = CineTimelineRuler)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Original Price:", fontSize = 12.sp, color = CineTextSecondary)
                        Text("₹${offer.originalPrice}", fontSize = 12.sp, textDecoration = TextDecoration.LineThrough, color = CineTextTertiary)
                    }

                    if (appliedCouponCode != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coupon Applied ($appliedCouponCode):", fontSize = 12.sp, color = CineSuccess)
                            Text("- ₹${(offer.finalPrice - finalPrice).toInt()}", fontSize = 12.sp, color = CineSuccess, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Backend Final Price:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("₹$finalPrice", fontSize = 16.sp, fontWeight = FontWeight.Black, color = secondaryColor)
                    }

                    if (offer.bonusCoins > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, primaryColor)
                        ) {
                            Text(
                                text = "★ Instant Perk: +${offer.bonusCoins} Bonus CineCoins directly credited to wallet",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = repository.redeemFestivalOffer(offer.offerId, appliedCouponCode)
                        if (success) {
                            checkoutSuccessMessage = "🎉 Successfully unlocked ${offer.title}!"
                        }
                        selectedOfferForCheckout = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.testTag("confirm_festival_purchase_button")
                ) {
                    Text("Pay ₹$finalPrice", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOfferForCheckout = null }) {
                    Text("Cancel", color = CineTextSecondary)
                }
            },
            containerColor = CineSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun FestivalOfferCard(
    offer: FestivalOffer,
    appliedCoupon: String?,
    primaryColor: Color,
    secondaryColor: Color,
    onCheckout: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CineSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offer_card_${offer.offerId}")
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
                // Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = primaryColor
                ) {
                    Text(
                        text = offer.badgeTag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Uses / Stock counter
                val remainingUses = (offer.maxUses - offer.usedCount).coerceAtLeast(0)
                Text(
                    text = "$remainingUses passes left",
                    fontSize = 10.sp,
                    color = if (remainingUses < 50) CineWarning else CineTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = offer.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = offer.description,
                fontSize = 12.sp,
                color = CineTextSecondary,
                lineHeight = 16.sp
            )

            // Pricing and CTA Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "₹${offer.originalPrice.toInt()}",
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = CineTextTertiary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CineSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = when (offer.discountType) {
                                    FestivalDiscountType.PERCENTAGE -> "${offer.discountValue.toInt()}% OFF"
                                    FestivalDiscountType.FIXED_AMOUNT -> "₹${offer.discountValue.toInt()} OFF"
                                    else -> "SPECIAL"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineSuccess,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "₹${offer.finalPrice.toInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = secondaryColor
                    )
                }

                Button(
                    onClick = onCheckout,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("claim_offer_${offer.offerId}")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unlock Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
