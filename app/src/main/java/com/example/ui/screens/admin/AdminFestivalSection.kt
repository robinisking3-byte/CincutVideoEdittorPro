package com.example.ui.screens.admin

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
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import com.example.ui.components.FestivalParticlesOverlay
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFestivalSection(
    repository: CineCutRepository,
    currentAdminRole: AdminRole,
    modifier: Modifier = Modifier
) {
    val festivalConfig by repository.festivalConfig.collectAsState()
    val offers by repository.festivalOffers.collectAsState()
    val coupons by repository.festivalCoupons.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Manager, 1: Theme & Preview, 2: Animations, 3: Offers, 4: Coupons
    var showCreateOfferDialog by remember { mutableStateOf(false) }
    var showCreateCouponDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Editable draft configuration for live tweaking before publishing
    var draftConfig by remember(festivalConfig) { mutableStateOf(festivalConfig) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_festival_section")
    ) {
        // Sub-Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = CineSurface,
            contentColor = CinePrimary,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Manager", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Theme & Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Animations Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeSubTab == 3,
                onClick = { activeSubTab = 3 },
                text = { Text("Festival Offers (${offers.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeSubTab == 4,
                onClick = { activeSubTab = 4 },
                text = { Text("Coupons (${coupons.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        if (feedbackMessage != null) {
            Surface(
                color = Color(0xFF0F3D24),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(feedbackMessage!!, fontSize = 11.sp, color = CineSuccess, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { feedbackMessage = null }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        when (activeSubTab) {
            0 -> FestivalManagerTab(
                draft = draftConfig,
                onDraftChange = { draftConfig = it },
                onPublish = {
                    val success = repository.adminUpdateFestivalConfig(draftConfig)
                    feedbackMessage = if (success) "✓ Published Festival Configuration to Firestore & Remote Config (v${draftConfig.remoteConfigVersion + 1})" else "Failed to publish"
                },
                onQuickSelectFestival = { fest ->
                    repository.adminSelectFestival(fest)
                    draftConfig = repository.festivalConfig.value
                    feedbackMessage = "✓ Switched active festival to ${fest.displayName}"
                }
            )
            1 -> FestivalThemeTab(
                draft = draftConfig,
                onDraftChange = { draftConfig = it },
                onPublish = {
                    val success = repository.adminUpdateFestivalConfig(draftConfig)
                    feedbackMessage = if (success) "✓ Festival Theme Published Successfully!" else "Theme update failed"
                }
            )
            2 -> FestivalAnimationsTab(
                draft = draftConfig,
                onDraftChange = { draftConfig = it },
                onPublish = {
                    val success = repository.adminUpdateFestivalConfig(draftConfig)
                    feedbackMessage = if (success) "✓ Festival Animation Configuration Saved" else "Failed to update animations"
                }
            )
            3 -> FestivalOffersTab(
                offers = offers,
                onAddOfferClick = { showCreateOfferDialog = true },
                onToggleOffer = { id, enabled ->
                    repository.adminToggleFestivalOffer(id, enabled)
                    feedbackMessage = "Offer state updated"
                },
                onDeleteOffer = { id ->
                    repository.adminDeleteFestivalOffer(id)
                    feedbackMessage = "Deleted offer $id"
                }
            )
            4 -> FestivalCouponsTab(
                coupons = coupons,
                onAddCouponClick = { showCreateCouponDialog = true },
                onToggleCoupon = { code, enabled ->
                    repository.adminToggleCoupon(code, enabled)
                    feedbackMessage = "Coupon $code toggled"
                }
            )
        }
    }

    // Modal Create Offer Dialog
    if (showCreateOfferDialog) {
        CreateFestivalOfferDialog(
            activeFestival = festivalConfig.activeFestival,
            onDismiss = { showCreateOfferDialog = false },
            onCreate = { offer ->
                repository.adminCreateFestivalOffer(offer)
                feedbackMessage = "✓ Successfully created offer '${offer.title}'"
                showCreateOfferDialog = false
            }
        )
    }

    // Modal Create Coupon Dialog
    if (showCreateCouponDialog) {
        CreateFestivalCouponDialog(
            onDismiss = { showCreateCouponDialog = false },
            onCreate = { coupon ->
                repository.adminCreateCoupon(coupon)
                feedbackMessage = "✓ Created coupon code '${coupon.code}'"
                showCreateCouponDialog = false
            }
        )
    }
}

// ================= TAB 0: FESTIVAL MANAGER ================= //

@Composable
fun FestivalManagerTab(
    draft: FestivalConfiguration,
    onDraftChange: (FestivalConfiguration) -> Unit,
    onPublish: () -> Unit,
    onQuickSelectFestival: (FestivalType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Enable / Disable Toggle Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (draft.isFestivalActive) Color(draft.themeConfig.secondaryAccentHex) else CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (draft.isFestivalActive) "Festival Mode: ACTIVE" else "Festival Mode: INACTIVE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (draft.isFestivalActive) CineSuccess else CineTextSecondary
                        )
                        Text(
                            text = "When active, dynamic festival theme, promotional banners, bonus coins & animations apply across CineCut.",
                            fontSize = 11.sp,
                            color = CineTextTertiary
                        )
                    }
                    Switch(
                        checked = draft.isFestivalActive,
                        onCheckedChange = { onDraftChange(draft.copy(isFestivalActive = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(draft.themeConfig.primaryAccentHex)
                        ),
                        modifier = Modifier.testTag("festival_active_switch")
                    )
                }
            }
        }

        // Festival Selection Carousel (9 Festivals)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Active Festival (Remote Config)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Switching presets automatically adapts color palettes, particle animations, and default headlines.", fontSize = 11.sp, color = CineTextSecondary)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FestivalType.values()) { fest ->
                        val isSelected = draft.activeFestival == fest
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(fest.defaultPrimaryHex).copy(alpha = 0.2f) else CineSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) Color(fest.defaultPrimaryHex) else CineTimelineRuler
                            ),
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onQuickSelectFestival(fest) }
                                .testTag("select_festival_${fest.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(fest.defaultPrimaryHex))
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(
                                    text = fest.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = fest.celebrationTitle,
                                    fontSize = 9.sp,
                                    color = CineTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Event Schedule & CineCoins Bonus Configuration
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Festival Schedule & CineCoin Incentives", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    // Headline Editor
                    OutlinedTextField(
                        value = draft.themeConfig.bannerHeadline,
                        onValueChange = {
                            onDraftChange(draft.copy(themeConfig = draft.themeConfig.copy(bannerHeadline = it)))
                        },
                        label = { Text("Banner Headline") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(draft.themeConfig.primaryAccentHex),
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Subheadline Editor
                    OutlinedTextField(
                        value = draft.themeConfig.bannerSubheadline,
                        onValueChange = {
                            onDraftChange(draft.copy(themeConfig = draft.themeConfig.copy(bannerSubheadline = it)))
                        },
                        label = { Text("Banner Subheadline / Offer Hook") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(draft.themeConfig.primaryAccentHex),
                            unfocusedBorderColor = CineTimelineRuler,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // CineCoin Bonus Sliders
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Universal CineCoin Bonus:", fontSize = 11.sp, color = CineTextSecondary)
                            Text("+${draft.cineCoinBonusPercent}% Bonus Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(draft.themeConfig.secondaryAccentHex))
                        }
                        Slider(
                            value = draft.cineCoinBonusPercent.toFloat(),
                            onValueChange = { onDraftChange(draft.copy(cineCoinBonusPercent = it.toInt())) },
                            valueRange = 0f..100f,
                            steps = 19,
                            colors = SliderDefaults.colors(thumbColor = Color(draft.themeConfig.primaryAccentHex), activeTrackColor = Color(draft.themeConfig.primaryAccentHex))
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("First-Purchase Extra Bonus:", fontSize = 11.sp, color = CineTextSecondary)
                            Text("+${draft.firstPurchaseBonusPercent}% Extra Bonus", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                        }
                        Slider(
                            value = draft.firstPurchaseBonusPercent.toFloat(),
                            onValueChange = { onDraftChange(draft.copy(firstPurchaseBonusPercent = it.toInt())) },
                            valueRange = 10f..150f,
                            steps = 13,
                            colors = SliderDefaults.colors(thumbColor = CineSuccess, activeTrackColor = CineSuccess)
                        )
                    }

                    // Save / Publish Button
                    Button(
                        onClick = onPublish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(draft.themeConfig.primaryAccentHex)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_festival_config_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish Festival Remote Config", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= TAB 1: THEME & LIVE PREVIEW ================= //

@Composable
fun FestivalThemeTab(
    draft: FestivalConfiguration,
    onDraftChange: (FestivalConfiguration) -> Unit,
    onPublish: () -> Unit
) {
    val theme = draft.themeConfig
    val primaryColor = Color(theme.primaryAccentHex)
    val secondaryColor = Color(theme.secondaryAccentHex)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Festival Theme Configuration & Live Preview", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Preview real-time UI components before applying to user devices.", fontSize = 11.sp, color = CineTextSecondary)
        }

        // LIVE PREVIEW CARD
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(theme.surfaceGradientStart), Color(theme.surfaceGradientEnd))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(6.dp), color = primaryColor) {
                                Text(theme.bannerBadgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                            Text("LIVE PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                        }

                        Text(theme.bannerHeadline, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(theme.bannerSubheadline, fontSize = 11.sp, color = CineTextSecondary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Themed Button", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Secondary", fontSize = 11.sp, color = secondaryColor)
                            }
                        }
                    }
                }
            }
        }

        // Toggles for Theme Scopes
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Application Scopes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    ThemeScopeToggle("Home Screen Hero & Banner", theme.enableHomeHeroStyling) {
                        onDraftChange(draft.copy(themeConfig = theme.copy(enableHomeHeroStyling = it)))
                    }
                    ThemeScopeToggle("Buttons & CTA Accents", theme.enableButtonStyling) {
                        onDraftChange(draft.copy(themeConfig = theme.copy(enableButtonStyling = it)))
                    }
                    ThemeScopeToggle("Cards & Container Borders", theme.enableCardAccents) {
                        onDraftChange(draft.copy(themeConfig = theme.copy(enableCardAccents = it)))
                    }
                    ThemeScopeToggle("CineCoin Store & Vault", theme.enableStoreStyling) {
                        onDraftChange(draft.copy(themeConfig = theme.copy(enableStoreStyling = it)))
                    }
                    ThemeScopeToggle("Video Editor Timeline Accents", theme.enableEditorAccents) {
                        onDraftChange(draft.copy(themeConfig = theme.copy(enableEditorAccents = it)))
                    }
                }
            }
        }

        item {
            Button(
                onClick = onPublish,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Theme Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThemeScopeToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 11.sp, color = CineTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CinePrimary)
        )
    }
}

// ================= TAB 2: ANIMATIONS STUDIO ================= //

@Composable
fun FestivalAnimationsTab(
    draft: FestivalConfiguration,
    onDraftChange: (FestivalConfiguration) -> Unit,
    onPublish: () -> Unit
) {
    val anim = draft.animationConfig

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Festival Animations Studio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Hardware-accelerated, lightweight canvas particle systems with battery and low-end CPU safeguards.", fontSize = 11.sp, color = CineTextSecondary)
        }

        // Live Animation Canvas Preview
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(draft.themeConfig.secondaryAccentHex)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FestivalParticlesOverlay(
                        config = anim,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color.Black.copy(alpha = 0.7f)) {
                            Text(
                                text = "${anim.animationType.title} • ${if (anim.reduceAnimations) "Lite Mode" else "Full Density"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Animation Settings Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Particle Animations", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Switch(
                            checked = anim.enabled,
                            onCheckedChange = { onDraftChange(draft.copy(animationConfig = anim.copy(enabled = it))) }
                        )
                    }

                    Divider(color = CineTimelineRuler)

                    // Reduce Animations Toggle (Low-end device safeguard)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reduce Animations (Low-end Optimization)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Limits particles to 6 gentle motes with minimal GPU draw calls.", fontSize = 10.sp, color = CineTextSecondary)
                        }
                        Switch(
                            checked = anim.reduceAnimations,
                            onCheckedChange = { onDraftChange(draft.copy(animationConfig = anim.copy(reduceAnimations = it))) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CineSuccess)
                        )
                    }

                    Divider(color = CineTimelineRuler)

                    // Density Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Particle Density:", fontSize = 11.sp, color = CineTextSecondary)
                            Text("${(anim.particleDensity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Slider(
                            value = anim.particleDensity,
                            onValueChange = { onDraftChange(draft.copy(animationConfig = anim.copy(particleDensity = it))) },
                            valueRange = 0.2f..1.0f
                        )
                    }

                    // Animation Style Selector
                    Text("Select Animation Style:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FestivalAnimationType.values().forEach { style ->
                            val isSelected = anim.animationType == style
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CinePrimary.copy(alpha = 0.15f) else CineSurfaceHighlight,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isSelected) CinePrimary else CineTimelineRuler),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDraftChange(draft.copy(animationConfig = anim.copy(animationType = style)))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onDraftChange(draft.copy(animationConfig = anim.copy(animationType = style))) }
                                    )
                                    Column {
                                        Text(style.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(style.description, fontSize = 9.sp, color = CineTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onPublish,
                colors = ButtonDefaults.buttonColors(containerColor = Color(draft.themeConfig.primaryAccentHex)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Animations Setup", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ================= TAB 3: FESTIVAL OFFERS ================= //

@Composable
fun FestivalOffersTab(
    offers: List<FestivalOffer>,
    onAddOfferClick: () -> Unit,
    onToggleOffer: (String, Boolean) -> Unit,
    onDeleteOffer: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Promotional Festival Offers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Discounts, bonus coin packs & exclusive templates", fontSize = 11.sp, color = CineTextSecondary)
                }
                Button(
                    onClick = onAddOfferClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_add_offer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Offer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(offers) { offer ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = CinePrimary) {
                            Text(offer.offerId, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Switch(
                                checked = offer.enabled,
                                onCheckedChange = { onToggleOffer(offer.offerId, it) },
                                modifier = Modifier.size(24.dp)
                            )
                            IconButton(onClick = { onDeleteOffer(offer.offerId) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CineError, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(offer.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(offer.description, fontSize = 11.sp, color = CineTextSecondary)

                    Divider(color = CineTimelineRuler)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Category: ${offer.category.title}", fontSize = 10.sp, color = CineTextTertiary)
                        Text("Uses: ${offer.usedCount} / ${offer.maxUses}", fontSize = 10.sp, color = CineSuccess)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Original: ₹${offer.originalPrice.toInt()}", fontSize = 11.sp, color = CineTextSecondary)
                        Text("Backend Price: ₹${offer.finalPrice.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineTertiary)
                    }
                }
            }
        }
    }
}

// ================= TAB 4: COUPONS ================= //

@Composable
fun FestivalCouponsTab(
    coupons: List<FestivalCoupon>,
    onAddCouponClick: () -> Unit,
    onToggleCoupon: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Festival Promotional Coupons", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Backend-verified discount promo codes", fontSize = 11.sp, color = CineTextSecondary)
                }
                Button(
                    onClick = onAddCouponClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CinePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_add_coupon_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Coupon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(coupons) { coupon ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CineSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CineTimelineRuler),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = CineSecondary) {
                                Text(coupon.code, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text("${coupon.discountPercent}% OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CineSuccess)
                        }
                        Text("Max Discount: ₹${coupon.maxDiscountInr.toInt()} • Min Order: ₹${coupon.minOrderAmount.toInt()}", fontSize = 10.sp, color = CineTextSecondary)
                        Text("Redemptions: ${coupon.usageCount} times", fontSize = 10.sp, color = CineTextTertiary)
                    }

                    Switch(
                        checked = coupon.enabled,
                        onCheckedChange = { onToggleCoupon(coupon.code, it) }
                    )
                }
            }
        }
    }
}

// ================= DIALOGS ================= //

@Composable
fun CreateFestivalOfferDialog(
    activeFestival: FestivalType,
    onDismiss: () -> Unit,
    onCreate: (FestivalOffer) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FestivalOfferItemCategory.CINECOIN_PACK) }
    var discountType by remember { mutableStateOf(FestivalDiscountType.PERCENTAGE) }
    var originalPriceStr by remember { mutableStateOf("999") }
    var discountValueStr by remember { mutableStateOf("30") }
    var bonusCoinsStr by remember { mutableStateOf("500") }
    var badgeTag by remember { mutableStateOf("FESTIVAL SPECIAL") }

    val origPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
    val discVal = discountValueStr.toDoubleOrNull() ?: 0.0

    // Live preview backend calculation
    val previewPrice = when (discountType) {
        FestivalDiscountType.PERCENTAGE -> origPrice * (1.0 - (discVal / 100.0))
        FestivalDiscountType.FIXED_AMOUNT -> (origPrice - discVal).coerceAtLeast(0.0)
        else -> origPrice
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Festival Offer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Offer Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = originalPriceStr,
                        onValueChange = { originalPriceStr = it },
                        label = { Text("Original Price (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = discountValueStr,
                        onValueChange = { discountValueStr = it },
                        label = { Text("Discount Value") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = bonusCoinsStr,
                    onValueChange = { bonusCoinsStr = it },
                    label = { Text("Bonus CineCoins") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CineSurfaceHighlight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Backend Final Price: ₹${kotlin.math.round(previewPrice).toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTertiary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            FestivalOffer(
                                title = title,
                                description = description,
                                festival = activeFestival,
                                category = category,
                                discountType = discountType,
                                discountValue = discVal,
                                originalPrice = origPrice,
                                finalPrice = previewPrice,
                                bonusCoins = bonusCoinsStr.toLongOrNull() ?: 0L,
                                badgeTag = badgeTag
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
            ) {
                Text("Create Offer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CineTextSecondary) }
        },
        containerColor = CineSurface,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun CreateFestivalCouponDialog(
    onDismiss: () -> Unit,
    onCreate: (FestivalCoupon) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var percentStr by remember { mutableStateOf("25") }
    var maxDiscountStr by remember { mutableStateOf("500") }
    var minOrderStr by remember { mutableStateOf("299") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Festival Coupon Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Coupon Code (e.g. DIWALI25)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = percentStr,
                        onValueChange = { percentStr = it },
                        label = { Text("Discount %") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxDiscountStr,
                        onValueChange = { maxDiscountStr = it },
                        label = { Text("Max Cap (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = minOrderStr,
                    onValueChange = { minOrderStr = it },
                    label = { Text("Min Order (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        onCreate(
                            FestivalCoupon(
                                code = code.trim().uppercase(),
                                discountPercent = percentStr.toIntOrNull() ?: 20,
                                maxDiscountInr = maxDiscountStr.toDoubleOrNull() ?: 500.0,
                                minOrderAmount = minOrderStr.toDoubleOrNull() ?: 199.0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CinePrimary)
            ) {
                Text("Create Coupon")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CineTextSecondary) }
        },
        containerColor = CineSurface,
        shape = RoundedCornerShape(14.dp)
    )
}
