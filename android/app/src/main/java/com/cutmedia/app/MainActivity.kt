package com.cutmedia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutmedia.app.data.AppNotification
import com.cutmedia.app.data.CutMediaRepository
import com.cutmedia.app.theme.CutMediaTheme
import com.cutmedia.app.theme.CutThemeMode
import com.cutmedia.app.ui.screens.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var repository: CutMediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CutMediaRepository(applicationContext)

        setContent {
            var currentTheme by remember { mutableStateOf(CutThemeMode.CINEMATIC_GOLD) }

            CutMediaTheme(themeMode = currentTheme) {
                CutMediaApp(
                    repository = repository,
                    currentTheme = currentTheme,
                    onThemeSelected = { currentTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutMediaApp(
    repository: CutMediaRepository,
    currentTheme: CutThemeMode,
    onThemeSelected: (CutThemeMode) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAuthScreen by remember { mutableStateOf(false) }
    var showAdminConsole by remember { mutableStateOf(false) }
    var isAdminUnlocked by remember { mutableStateOf(true) }

    var showCoinsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // Dynamic state synced with repository
    var yellowCoins by remember { mutableIntStateOf(repository.getYellowCoins()) }
    var blueCoins by remember { mutableIntStateOf(repository.getBlueCoins()) }
    var notifications by remember { mutableStateOf(repository.getNotifications()) }
    var promotedFeatures by remember { mutableStateOf(repository.getPromotedFeatures()) }

    LaunchedEffect(Unit) {
        repository.listenToPromotedFeatures { updated ->
            promotedFeatures = updated
        }
    }

    val activePromotion = promotedFeatures.firstOrNull { it.isActive }

    fun refreshWalletAndNotifs() {
        yellowCoins = repository.getYellowCoins()
        blueCoins = repository.getBlueCoins()
        notifications = repository.getNotifications()
        promotedFeatures = repository.getPromotedFeatures()
    }

    val unreadNotifsCount = notifications.count { !it.isRead }
    val currentUser = repository.getCurrentUser()
    val isAdminUser = currentUser.role.equals("admin", ignoreCase = true) ||
                      currentUser.premiumRole.equals("founder", ignoreCase = true) ||
                      currentUser.email.equals("robinisking3@gmail.com", ignoreCase = true) ||
                      currentUser.email.equals("robintyagi861az@gmail.com", ignoreCase = true)

    val hasAdminAccess = isAdminUnlocked || isAdminUser

    val navItems = listOf(
        NavigationItem("Studio", Icons.Default.Movie),
        NavigationItem("Projects", Icons.Default.VideoLibrary),
        NavigationItem("Friends", Icons.Default.Group),
        NavigationItem("AI Studio", Icons.Default.AutoAwesome),
        NavigationItem("Profile", Icons.Default.AccountCircle)
    )

    BackHandler(enabled = showAdminConsole || showAuthScreen || showCoinsDialog || showNotificationsDialog || selectedTab != 0) {
        if (showAdminConsole) {
            showAdminConsole = false
        } else if (showAuthScreen) {
            showAuthScreen = false
        } else if (showCoinsDialog) {
            showCoinsDialog = false
        } else if (showNotificationsDialog) {
            showNotificationsDialog = false
        } else {
            selectedTab = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Branding
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CineCut",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "VIDEO EDITOR",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Right TopBar Controls: COIN PILL + NOTIFICATIONS BELL + ADMIN SHIELD
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Interactive Coin Badges Pill
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showCoinsDialog = true }
                                    .testTag("header_coins_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Yellow Coins
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("🪙", fontSize = 12.sp)
                                        Text(
                                            text = "$yellowCoins",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(10.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    // Blue AI Coins
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("⚡", fontSize = 12.sp)
                                        Text(
                                            text = "$blueCoins",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color(0xFF06B6D4)
                                        )
                                    }
                                }
                            }

                            // 2. Notifications Center Bell with Unread Badge
                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("header_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifsCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Text("$unreadNotifsCount", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = if (unreadNotifsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 3. Admin Command Access Icon
                            IconButton(
                                onClick = { showAdminConsole = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("admin_header_icon_button")
                            ) {
                                Surface(
                                    color = if (hasAdminAccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Admin Console",
                                            tint = if (hasAdminAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!showAuthScreen && !showAdminConsole) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_$index")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showAdminConsole) {
                AdminConsoleScreen(
                    repository = repository,
                    hasAdminAccess = true,
                    onUnlockAdmin = { _, _ -> true },
                    onClose = {
                        showAdminConsole = false
                        refreshWalletAndNotifs()
                    }
                )
            } else if (showAuthScreen) {
                AuthScreen(
                    repository = repository,
                    onAuthSuccess = {
                        showAuthScreen = false
                        refreshWalletAndNotifs()
                    },
                    onBack = { showAuthScreen = false }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // WORLDWIDE PROMOTED FEATURE BANNER (Live from Admin Panel & Cloud)
                    if (activePromotion != null) {
                        WorldwidePromotionBanner(
                            promotion = activePromotion,
                            onClick = {
                                when (activePromotion.targetScreen) {
                                    "AI_STUDIO" -> selectedTab = 3
                                    "PROJECTS" -> selectedTab = 1
                                    "FRIENDS" -> selectedTab = 2
                                    "PROFILE" -> selectedTab = 4
                                    else -> selectedTab = 0
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> EditorScreen(
                                repository = repository,
                                onProjectSaved = {
                                    selectedTab = 1
                                    refreshWalletAndNotifs()
                                }
                            )
                            1 -> ProjectsScreen(
                                repository = repository,
                                onNavigateToEditor = { selectedTab = 0 }
                            )
                            2 -> SocialFriendsScreen(repository = repository)
                            3 -> AiStudioScreen()
                            4 -> ProfileSettingsScreen(
                                repository = repository,
                                currentTheme = currentTheme,
                                onThemeSelected = onThemeSelected,
                                onNavigateToAuth = { showAuthScreen = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Coins & Economy Dialog
    if (showCoinsDialog) {
        CoinsEconomyDialog(
            repository = repository,
            onDismiss = {
                showCoinsDialog = false
                refreshWalletAndNotifs()
            }
        )
    }

    // Interactive Notifications Center Dialog
    if (showNotificationsDialog) {
        NotificationsCenterDialog(
            repository = repository,
            onDismiss = {
                showNotificationsDialog = false
                refreshWalletAndNotifs()
            },
            onNavigateToFriends = {
                showNotificationsDialog = false
                selectedTab = 2 // Friends tab
                refreshWalletAndNotifs()
            }
        )
    }
}

@Composable
fun CoinsEconomyDialog(
    repository: CutMediaRepository,
    onDismiss: () -> Unit
) {
    var promoCodeInput by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var yellow by remember { mutableIntStateOf(repository.getYellowCoins()) }
    var blue by remember { mutableIntStateOf(repository.getBlueCoins()) }
    val transactions = remember { repository.getCoinTransactions() }
    val user = repository.getCurrentUser()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Coin Economy & Wallet", fontSize = 16.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yellow Coins
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Yellow Coins", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$yellow", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                                Text("Check-ins & Activity", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Blue Coins
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF06B6D4).copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Blue AI Coins", fontSize = 10.sp, color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$blue", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF06B6D4))
                                Text("AI Captions & Cuts", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Reward Claims Actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daily & Hourly Rewards", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        // 1. Daily Check-in
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Daily Check-In", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Day ${user.streakDays} Streak • +10 to +20 Yellow Coins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        val res = repository.claimDailyCheckIn()
                                        actionMessage = res.second
                                        yellow = repository.getYellowCoins()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("claim_daily_checkin_button")
                                ) {
                                    Text("Claim", fontSize = 11.sp)
                                }
                            }
                        }

                        // 2. Hourly AI Bonus
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Hourly AI Usage Bonus", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("+2 Blue Coins for active editor usage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val res = repository.claimHourlyBonus()
                                        actionMessage = res.second
                                        blue = repository.getBlueCoins()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("claim_hourly_bonus_button")
                                ) {
                                    Text("Claim", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Promo Code Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Redeem Promo Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoCodeInput,
                                onValueChange = { promoCodeInput = it.uppercase() },
                                placeholder = { Text("e.g. DIWALI50 or CREATORPRO", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("promo_code_input")
                            )
                            Button(
                                onClick = {
                                    if (promoCodeInput.isNotBlank()) {
                                        val res = repository.redeemPromoCode(promoCodeInput)
                                        actionMessage = res.second
                                        yellow = repository.getYellowCoins()
                                        blue = repository.getBlueCoins()
                                        promoCodeInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("redeem_promo_button")
                            ) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Action Feedback Message
                if (actionMessage != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = actionMessage!!,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // Recent Transactions
                item {
                    Text("Recent Transactions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                items(transactions.take(5)) { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tx.title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (tx.coinType == "YELLOW") "Yellow Coin" else "Blue AI Coin",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${if (tx.isCredit) "+" else "-"}${tx.amount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.isCredit) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun NotificationsCenterDialog(
    repository: CutMediaRepository,
    onDismiss: () -> Unit,
    onNavigateToFriends: () -> Unit
) {
    var notifications by remember { mutableStateOf(repository.getNotifications()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notifications", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Badge { Text("${notifications.size}") }
                }
                Row {
                    TextButton(onClick = {
                        notifications.forEach { repository.markNotificationRead(it.id) }
                        notifications = repository.getNotifications()
                    }) {
                        Text("Read all", fontSize = 10.sp)
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = "No notifications",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No Notifications Yet", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("You're all caught up with exports, friend requests, and rewards.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notif ->
                        val icon = when (notif.type) {
                            "FRIEND" -> Icons.Default.Person
                            "RENDER" -> Icons.Default.Movie
                            "COINS" -> Icons.Default.MonetizationOn
                            "CAMPAIGN" -> Icons.Default.Celebration
                            else -> Icons.Default.Notifications
                        }
                        val tint = when (notif.type) {
                            "FRIEND" -> Color(0xFF3B82F6)
                            "RENDER" -> Color(0xFF10B981)
                            "COINS" -> Color(0xFFF59E0B)
                            "CAMPAIGN" -> Color(0xFFEC4899)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (!notif.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.markNotificationRead(notif.id)
                                    notifications = repository.getNotifications()
                                    if (notif.type == "FRIEND") {
                                        onNavigateToFriends()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(tint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = notif.type, tint = tint, modifier = Modifier.size(16.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = timeFormat.format(Date(notif.timestamp)),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notif.message,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (notif.type == "FRIEND") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap to open Friends & Chat →",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = {
                        repository.clearNotifications()
                        notifications = emptyList()
                    }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun WorldwidePromotionBanner(
    promotion: com.cutmedia.app.data.PromotedFeature,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .testTag("worldwide_promotion_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🌟 ${promotion.badgeText}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• ${promotion.discountOrBonus}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E)
                        )
                    }
                    Text(
                        text = promotion.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Open →",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
