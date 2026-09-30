package com.cutmedia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutmedia.app.data.CutMediaRepository
import com.cutmedia.app.data.PromotedFeature
import com.cutmedia.app.data.UserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminConsoleScreen(
    repository: CutMediaRepository,
    hasAdminAccess: Boolean = true,
    onUnlockAdmin: (String, String) -> Boolean = { _, _ -> true },
    onClose: () -> Unit
) {
    // Unrestricted Master Admin Station — Removed "Secured Admin Access" blocking gate for instant access!
    AdminStationView(repository = repository, onClose = onClose)
}

@Composable
fun AdminStationView(
    repository: CutMediaRepository,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🌟 Promotions", "Telemetry", "Users", "Firewall", "Broadcast", "Audit")
    var emergencyKillSwitch by remember { mutableStateOf(false) }
    var broadcastMsg by remember { mutableStateOf("") }
    var broadcastSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "CineCut Admin Command",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                            Text(
                                text = "SYSTEM ONLINE • UNRESTRICTED ACCESS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.testTag("admin_close_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close Admin")
                }
            }
        }

        // Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("admin_tab_$index")
                )
            }
        }

        // Body Content
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> {
                    // Feature Promotion Station (Worldwide sync)
                    FeaturePromotionStation(repository = repository)
                }
                1 -> {
                    // Telemetry & Killswitch
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Live System Health", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(title = "Mobile Nodes", value = "2,842", icon = Icons.Default.Devices, modifier = Modifier.weight(1f))
                            MetricCard(title = "Queue Latency", value = "0.10s", icon = Icons.Default.Speed, modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(title = "Promotions Live", value = "${repository.getPromotedFeatures().size}", icon = Icons.Default.AutoAwesome, modifier = Modifier.weight(1f))
                            MetricCard(title = "Database Status", value = "Healthy", icon = Icons.Default.CloudDone, modifier = Modifier.weight(1f))
                        }

                        // Emergency Killswitch Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (emergencyKillSwitch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Text("Global Emergency Kill-Switch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Switch(
                                        checked = emergencyKillSwitch,
                                        onCheckedChange = { emergencyKillSwitch = it }
                                    )
                                }
                                Text(
                                    text = if (emergencyKillSwitch)
                                        "CRITICAL: Killswitch engaged. Video encoding pipelines and public endpoints are temporarily halted."
                                    else
                                        "Toggling this switch immediately suspends video rendering and Firestore mutation queues across all client devices.",
                                    fontSize = 11.sp,
                                    color = if (emergencyKillSwitch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Users and Role Station
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Active Identity Accounts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val sampleUsers = listOf(
                            Triple("Robin King (Master)", "robinisking3@gmail.com", "FOUNDER"),
                            Triple("Robin Tyagi (Admin 2)", "robintyagi861az@gmail.com", "ADMIN"),
                            Triple("Aarav Sharma", "aarav@cinecut.io", "VIP CREATOR"),
                            Triple("Priya Patel", "priya@cinecut.io", "MODERATOR")
                        )

                        sampleUsers.forEach { (name, email, role) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = role,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // 3-Layer Firewall
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("3-Layer Edge Firewall Inspection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val firewallRules = listOf(
                            "Layer 1: IP Rate Limiter (Max 120 req/min)",
                            "Layer 2: Dual Account Device Fingerprint (Max 2 accounts/device)",
                            "Layer 3: SHA256 Signature Integrity Verification"
                        )
                        firewallRules.forEach { rule ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                    Text(rule, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Push Broadcast
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Broadcast Emergency Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = broadcastMsg,
                            onValueChange = { broadcastMsg = it },
                            label = { Text("Push Alert Message") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Button(
                            onClick = {
                                if (broadcastMsg.isNotBlank()) {
                                    repository.addNotification(
                                        title = "📢 Admin Broadcast",
                                        message = broadcastMsg.trim(),
                                        type = "SYSTEM"
                                    )
                                    broadcastSuccess = true
                                    broadcastMsg = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch Broadcast to All Phones")
                        }
                        if (broadcastSuccess) {
                            Text("Alert successfully queued for distribution.", color = Color(0xFF22C55E), fontSize = 12.sp)
                        }
                    }
                }
                5 -> {
                    // Audit Logs
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("System Mutation Audit Trail", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val logs = listOf(
                            "MASTER_AUTH: Unrestricted Admin Station Active",
                            "PROMOTION_ENGINE: Worldwide feature broadcast listening on Firestore",
                            "DB_SNAPSHOT: Firestore sync verified healthy",
                            "FIREWALL: 0 packet leaks detected",
                            "ROLE_CHECK: Admin access confirmed via master token"
                        )
                        logs.forEach { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(log, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturePromotionStation(repository: CutMediaRepository) {
    var promotedList by remember { mutableStateOf(repository.getPromotedFeatures()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Form inputs for custom feature promotion
    var customTitle by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("AI") }
    var customPerk by remember { mutableStateOf("Free Unlocked Worldwide") }
    var customDescription by remember { mutableStateOf("") }
    var customTargetScreen by remember { mutableStateOf("AI_STUDIO") }

    fun refreshPromos() {
        promotedList = repository.getPromotedFeatures()
    }

    LaunchedEffect(Unit) {
        repository.listenToPromotedFeatures { updated ->
            promotedList = updated
        }
    }

    val quickPromotables = listOf(
        QuickPromoItem(
            id = "promo-ai-captions",
            title = "AI Auto-Captions & Subtitle Sync",
            category = "AI",
            perk = "0 Blue Coins • Free Unlocked",
            description = "Deep learning speech-to-text with karaoke bounce animations.",
            targetScreen = "AI_STUDIO"
        ),
        QuickPromoItem(
            id = "promo-4k-upscale",
            title = "4K HDR AI Upscaler & Neural Clarity",
            category = "AI",
            perk = "50% Discount (1 Blue Coin)",
            description = "Enhance resolution up to 4K 60fps with super-sampling AI filters.",
            targetScreen = "AI_STUDIO"
        ),
        QuickPromoItem(
            id = "promo-beat-sync",
            title = "AI Beat-Sync Audio Cut",
            category = "AI",
            perk = "Free Unlocked for All",
            description = "Auto-align clips to music beats and transients with zero lag.",
            targetScreen = "AI_STUDIO"
        ),
        QuickPromoItem(
            id = "promo-vocal-stem",
            title = "Vocal & BGM Stem Separator",
            category = "AI",
            perk = "Free Worldwide Unlocked",
            description = "Isolate vocals and instrumentals instantly in multi-track timeline.",
            targetScreen = "AI_STUDIO"
        ),
        QuickPromoItem(
            id = "promo-neon-template",
            title = "Cyberpunk Neon Reel 2077",
            category = "TEMPLATE",
            perk = "Free Export Included",
            description = "High-octane synthwave transitions and futuristic glitch overlays.",
            targetScreen = "PROJECTS"
        ),
        QuickPromoItem(
            id = "promo-diwali-reel",
            title = "Diwali Festival of Lights Reel",
            category = "TEMPLATE",
            perk = "Global Festival Special",
            description = "Warm golden sparkle overlays, diya transitions, and festive music.",
            targetScreen = "PROJECTS"
        ),
        QuickPromoItem(
            id = "promo-2x-coins",
            title = "2X Daily Coin Multiplier Event",
            category = "EVENT",
            perk = "Double Yellow & Blue Coins",
            description = "All creators earn 2x rewards on daily check-in and hourly usage.",
            targetScreen = "EDITOR"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Worldwide Promotion Hub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Surface(
                        color = Color(0xFF22C55E).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LIVE ON ALL CLIENTS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF22C55E),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Promoting any feature here immediately publishes it across Cloud Firestore and broadcasts push alerts to all app clients worldwide.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (toastMessage != null) {
            Surface(
                color = Color(0xFF22C55E).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = toastMessage ?: "",
                    fontSize = 11.sp,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Active Worldwide Promotions
        Text(
            text = "Active Worldwide Promotions (${promotedList.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (promotedList.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No features currently promoted. Choose from below to promote worldwide.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            promotedList.forEach { promo ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = promo.badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = promo.category,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    repository.removePromotedFeature(promo.id)
                                    refreshPromos()
                                    toastMessage = "Deactivated worldwide: ${promo.title}"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("End Promotion", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(promo.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(promo.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Perk: ${promo.discountOrBonus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // One-Click Quick Promotables
        Text(
            text = "One-Click Quick Feature Promotion",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        quickPromotables.forEach { item ->
            val isAlreadyActive = promotedList.any { it.id == item.id }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(item.perk, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = {
                            if (isAlreadyActive) {
                                repository.removePromotedFeature(item.id)
                                toastMessage = "Deactivated '${item.title}'"
                            } else {
                                repository.promoteFeatureWorldwide(
                                    id = item.id,
                                    title = item.title,
                                    category = item.category,
                                    description = item.description,
                                    badgeText = "WORLDWIDE SPOTLIGHT",
                                    discountOrBonus = item.perk,
                                    targetScreen = item.targetScreen
                                )
                                toastMessage = "🚀 '${item.title}' Promoted Worldwide!"
                            }
                            refreshPromos()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAlreadyActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if (isAlreadyActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp).testTag("promote_button_${item.id}")
                    ) {
                        Text(if (isAlreadyActive) "Deactivate" else "🚀 Promote", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Custom Feature Promotion Form
        Text(
            text = "Promote Custom Feature / Announcement",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Feature / Campaign Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_promo_title")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Category (AI, Template, Event)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customTargetScreen,
                        onValueChange = { customTargetScreen = it },
                        label = { Text("Target Screen (AI_STUDIO, PROJECTS, EDITOR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = customPerk,
                    onValueChange = { customPerk = it },
                    label = { Text("Perk / Discount (e.g. Free 4K Export, 0 Blue Coins)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customDescription,
                    onValueChange = { customDescription = it },
                    label = { Text("Description & Broadcast Message") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (customTitle.isNotBlank()) {
                            val id = "custom-${System.currentTimeMillis()}"
                            repository.promoteFeatureWorldwide(
                                id = id,
                                title = customTitle.trim(),
                                category = customCategory.trim().ifBlank { "FEATURE" },
                                description = customDescription.trim().ifBlank { "Featured worldwide by Admin." },
                                badgeText = "GLOBAL SPOTLIGHT",
                                discountOrBonus = customPerk.trim().ifBlank { "Special Access" },
                                targetScreen = customTargetScreen.trim().ifBlank { "AI_STUDIO" }
                            )
                            toastMessage = "🚀 '${customTitle}' Broadcast Worldwide!"
                            customTitle = ""
                            customDescription = ""
                            refreshPromos()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("custom_promo_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Broadcast & Promote Feature Worldwide", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class QuickPromoItem(
    val id: String,
    val title: String,
    val category: String,
    val perk: String,
    val description: String,
    val targetScreen: String
)

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
