package com.cutmedia.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CutMediaAdminTheme {
                AdminConsoleScaffold()
            }
        }
    }
}

@Composable
fun CutMediaAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD4AF37), // Restrained Premium Gold
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF2C240E),
            onPrimaryContainer = Color(0xFFF3E5AB),
            secondary = Color(0xFF38BDF8),
            background = Color(0xFF090A0F),
            surface = Color(0xFF13151F),
            surfaceVariant = Color(0xFF1B1E2B),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF25293A)
        ),
        content = content
    )
}

data class ManagedUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "USER",
    val isBanned: Boolean = false
)

data class AuditLogEntry(
    val id: String = "",
    val action: String = "",
    val adminEmail: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CouponEntry(
    val code: String = "",
    val discountPercent: Int = 20,
    val maxUses: Int = 100,
    val active: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScaffold() {
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🌟 Promotions", "Telemetry", "Users", "Audit Logs", "System Lock", "Broadcasts", "Coupons")

    var userCount by remember { mutableIntStateOf(0) }
    var auditCount by remember { mutableIntStateOf(0) }
    var broadcastCount by remember { mutableIntStateOf(0) }
    var isEmergencyLocked by remember { mutableStateOf(false) }

    var usersList by remember { mutableStateOf<List<ManagedUser>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLogEntry>>(emptyList()) }
    var couponsList by remember { mutableStateOf<List<CouponEntry>>(emptyList()) }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun recordAudit(action: String, details: String) {
        val email = auth.currentUser?.email ?: "master-admin"
        firestore.collection("audit_logs").add(
            mapOf(
                "action" to action,
                "adminEmail" to email,
                "details" to details,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    fun refreshData() {
        coroutineScope.launch {
            try {
                // Fetch real users from Firestore
                val userSnap = firestore.collection("users").limit(50).get().await()
                userCount = userSnap.size()
                usersList = userSnap.documents.map { doc ->
                    ManagedUser(
                        uid = doc.getString("uid") ?: doc.id,
                        email = doc.getString("email") ?: "no-email",
                        displayName = doc.getString("displayName") ?: "User",
                        role = doc.getString("role") ?: "USER",
                        isBanned = doc.getBoolean("isBanned") ?: false
                    )
                }

                // Fetch real audit logs
                val auditSnap = firestore.collection("audit_logs").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(30).get().await()
                auditCount = auditSnap.size()
                auditLogs = auditSnap.documents.map { doc ->
                    AuditLogEntry(
                        id = doc.id,
                        action = doc.getString("action") ?: "Action",
                        adminEmail = doc.getString("adminEmail") ?: "Admin",
                        details = doc.getString("details") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }

                // Fetch system maintenance state
                val configDoc = firestore.collection("app_config").document("maintenance").get().await()
                isEmergencyLocked = configDoc.getBoolean("isMaintenanceMode") ?: false

                // Fetch coupons
                val couponSnap = firestore.collection("coupons").limit(20).get().await()
                couponsList = couponSnap.documents.map { doc ->
                    CouponEntry(
                        code = doc.getString("code") ?: doc.id,
                        discountPercent = (doc.getLong("discountPercent") ?: 20L).toInt(),
                        maxUses = (doc.getLong("maxUses") ?: 100L).toInt(),
                        active = doc.getBoolean("active") ?: true
                    )
                }

                val broadcastSnap = firestore.collection("system_broadcasts").get().await()
                broadcastCount = broadcastSnap.size()
            } catch (e: Exception) {
                statusMessage = "Sync notice: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Admin Shield",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "CutMedia Admin",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        IconButton(onClick = { refreshData() }, modifier = Modifier.testTag("admin_refresh_button")) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp
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

            if (statusMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        statusMessage ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> PromotionsTab { id, title, perk, category ->
                    coroutineScope.launch {
                        firestore.collection("promoted_features").document(id).set(
                            mapOf(
                                "id" to id,
                                "title" to title,
                                "category" to category,
                                "description" to "Promoted worldwide by Master Admin",
                                "badgeText" to "WORLDWIDE SPOTLIGHT",
                                "discountOrBonus" to perk,
                                "targetScreen" to (if (category == "AI") "AI_STUDIO" else "PROJECTS"),
                                "isActive" to true,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                        firestore.collection("system_broadcasts").add(
                            mapOf(
                                "type" to "FEATURE_PROMOTED",
                                "title" to "🌟 Worldwide Feature Spotlight: $title",
                                "message" to "$title is now promoted worldwide with $perk!",
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                        recordAudit("FEATURE_PROMOTED_WORLDWIDE", "$title ($perk)")
                        statusMessage = "🚀 '$title' promoted worldwide!"
                    }
                }
                1 -> TelemetryTab(userCount, auditCount, broadcastCount, isEmergencyLocked)
                2 -> UsersManagementTab(usersList) { user, newRole ->
                    coroutineScope.launch {
                        firestore.collection("users").document(user.uid).update("role", newRole)
                        recordAudit("USER_ROLE_CHANGE", "Updated ${user.email} to $newRole")
                        statusMessage = "Updated role to $newRole"
                        refreshData()
                    }
                }
                3 -> AuditLogsTab(auditLogs)
                4 -> SystemLockTab(isEmergencyLocked) { locked ->
                    coroutineScope.launch {
                        firestore.collection("app_config").document("maintenance")
                            .set(mapOf("isMaintenanceMode" to locked, "updatedAt" to System.currentTimeMillis()))
                        recordAudit("MAINTENANCE_TOGGLE", "Set maintenance mode to $locked")
                        isEmergencyLocked = locked
                        statusMessage = if (locked) "Emergency Kill-Switch Engaged!" else "System Unlocked"
                    }
                }
                5 -> BroadcastTab { title, body ->
                    coroutineScope.launch {
                        firestore.collection("system_broadcasts").add(
                            mapOf("title" to title, "body" to body, "timestamp" to System.currentTimeMillis())
                        )
                        recordAudit("PUSH_BROADCAST", "Title: $title")
                        statusMessage = "Push broadcast published to cloud!"
                        refreshData()
                    }
                }
                6 -> CouponsTab(couponsList) { code, discount ->
                    coroutineScope.launch {
                        firestore.collection("coupons").document(code).set(
                            mapOf("code" to code, "discountPercent" to discount, "active" to true)
                        )
                        recordAudit("COUPON_CREATED", "Code: $code ($discount%)")
                        statusMessage = "Coupon $code activated!"
                        refreshData()
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryTab(users: Int, audits: Int, broadcasts: Int, isLocked: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Real-Time Telemetry & Operations", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Registered Users", "$users", Icons.Default.People, Modifier.weight(1f))
            MetricCard("Audit Events", "$audits", Icons.Default.ListAlt, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Broadcast Notices", "$broadcasts", Icons.Default.Campaign, Modifier.weight(1f))
            MetricCard("System State", if (isLocked) "LOCKED" else "ONLINE", Icons.Default.Security, Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun UsersManagementTab(users: List<ManagedUser>, onRoleChange: (ManagedUser, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("User Directory (${users.size} Accounts)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (users.isEmpty()) {
            Text("No registered users found in Firestore yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { u ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(u.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(u.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Role: ${u.role}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onRoleChange(u, "VIP") }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Make VIP", fontSize = 10.sp)
                                }
                                OutlinedButton(onClick = { onRoleChange(u, "ADMIN") }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Admin", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsTab(logs: List<AuditLogEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Immutable Audit Trail (${logs.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (logs.isEmpty()) {
            Text("No audit records recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { log ->
                    val sdf = remember(log.timestamp) { SimpleDateFormat("HH:mm:ss, MMM dd", Locale.getDefault()) }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(sdf.format(Date(log.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("By: ${log.adminEmail}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemLockTab(isLocked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Global Maintenance & App Kill-Switch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                "When activated, the CutMedia client app shows a maintenance banner and blocks timeline mutations.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onToggle(!isLocked) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().testTag("toggle_maintenance_button")
            ) {
                Text(if (isLocked) "Deactivate Maintenance Mode" else "Engage Emergency App Lock")
            }
        }
    }
}

@Composable
fun BroadcastTab(onSend: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Push Notification Broadcast", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Announcement Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input")
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Message Body") },
                modifier = Modifier.fillMaxWidth().height(90.dp).testTag("broadcast_body_input")
            )
            Button(
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSend(title.trim(), body.trim())
                        title = ""
                        body = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("send_broadcast_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Broadcast to All Users")
            }
        }
    }
}

@Composable
fun CouponsTab(coupons: List<CouponEntry>, onAdd: (String, Int) -> Unit) {
    var code by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("25") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create Promo Voucher", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Coupon Code") },
                        modifier = Modifier.weight(1f).testTag("coupon_code_input")
                    )
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Discount %") },
                        modifier = Modifier.width(100.dp).testTag("coupon_discount_input")
                    )
                }
                Button(
                    onClick = {
                        val d = discount.toIntOrNull() ?: 20
                        if (code.isNotBlank()) {
                            onAdd(code.trim(), d)
                            code = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_coupon_button")
                ) {
                    Text("Issue Coupon")
                }
            }
        }

        Text("Active Coupons (${coupons.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(coupons) { c ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c.code, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text("${c.discountPercent}% OFF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionsTab(onPromote: (String, String, String, String) -> Unit) {
    val promotables = listOf(
        Triple("promo-ai-captions", "AI Auto-Captions & Subtitle Sync", "0 Blue Coins • Free Unlocked"),
        Triple("promo-4k-upscale", "4K HDR AI Upscaler & Neural Clarity", "50% Discount (1 Blue Coin)"),
        Triple("promo-beat-sync", "AI Beat-Sync Audio Cut", "Free Unlocked Worldwide"),
        Triple("promo-neon-template", "Cyberpunk Neon Reel 2077", "Free Export Included"),
        Triple("promo-diwali-reel", "Diwali Festival Lights Reel", "Global Festival Special"),
        Triple("promo-2x-coins", "2X Daily Coin Multiplier Event", "Double Coins for All Creators")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🌟 Worldwide Feature Promotion Hub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Promoting any feature here immediately updates Firestore and broadcasts to all CutMedia creator apps worldwide.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("One-Tap Live Feature Promotions", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        promotables.forEach { (id, title, perk) ->
            val cat = if (id.contains("template") || id.contains("reel")) "TEMPLATE" else "AI"
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(perk, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = { onPromote(id, title, perk, cat) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Promote 🚀", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
