package com.example.core.network

import android.content.Context
import android.util.Log
import com.example.core.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Production Firebase Cloud Realtime Synchronization Engine.
 * Provides multi-device real-time listener registrations, offline cache fallback,
 * and server-authoritative mutations across Firestore, Storage, and Cloud Functions.
 */
class FirebaseCloudSyncEngine(private val context: Context? = null) {

    private val tag = "CineCutCloudSync"
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var functions: FirebaseFunctions? = null

    // Active real-time listeners (cleaned up on stopSync to avoid memory leaks)
    private val activeRegistrations = mutableListOf<ListenerRegistration>()

    private fun safeLog(msg: String) {
        try {
            val logClass = Class.forName("android.util.Log")
            val method = logClass.getMethod("d", String::class.java, String::class.java)
            method.invoke(null, tag, msg)
        } catch (_: Throwable) {
            // Safe fallback on standard JVM tests where android.util.Log is not mocked
        }
    }

    init {
        ensureInitialized()
    }

    @Synchronized
    fun ensureInitialized(): Boolean {
        val ctx = context ?: return false
        return try {
            if (com.google.firebase.FirebaseApp.getApps(ctx).isNotEmpty()) {
                if (firestore == null) {
                    val db = FirebaseFirestore.getInstance()
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    db.firestoreSettings = settings
                    firestore = db
                }
                if (storage == null) {
                    storage = FirebaseStorage.getInstance()
                }
                if (functions == null) {
                    functions = FirebaseFunctions.getInstance()
                }
                safeLog("FirebaseCloudSyncEngine successfully verified & initialized.")
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            safeLog("Cloud sync fallback: ${e.message}")
            false
        }
    }

    val db: FirebaseFirestore?
        get() {
            ensureInitialized()
            return firestore
        }

    val cloudStorage: FirebaseStorage?
        get() {
            ensureInitialized()
            return storage
        }

    val cloudFunctions: FirebaseFunctions?
        get() {
            ensureInitialized()
            return functions
        }

    /**
     * Generic Cloud Functions invoker with full type-safe parameter marshalling
     */
    suspend fun callCloudFunction(name: String, parameters: Map<String, Any?> = emptyMap()): Result<Any?> {
        ensureInitialized()
        val fn = functions ?: return Result.failure(IllegalStateException("Firebase Functions instance not available"))
        return try {
            val task = fn.getHttpsCallable(name).call(parameters).await()
            Result.success(task.data)
        } catch (e: Exception) {
            safeLog("Cloud function '$name' call error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * AI Director: Cloud Function call to generate structured edit plan
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun generateAiEditPlanViaCloud(projectId: String, prompt: String, projectSummary: Map<String, Any?> = emptyMap()): Result<Map<String, Any?>> {
        val params = mapOf(
            "projectId" to projectId,
            "prompt" to prompt,
            "projectSummary" to projectSummary
        )
        return callCloudFunction("generateAiEditPlan", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Payments: Cloud Function call to generate UPI payment order
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun createPaymentOrderViaCloud(itemType: String, itemId: String, title: String, amountInr: Long): Result<Map<String, Any?>> {
        val params = mapOf(
            "itemType" to itemType,
            "itemId" to itemId,
            "title" to title,
            "amountInr" to amountInr
        )
        return callCloudFunction("createPaymentOrder", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Payments: Cloud Function call to verify UTR transaction
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun verifyUtrViaCloud(orderId: String, utrNumber: String): Result<Map<String, Any?>> {
        val params = mapOf(
            "orderId" to orderId,
            "utrNumber" to utrNumber
        )
        return callCloudFunction("verifyUTR", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Coins: Cloud Function call to earn or spend CineCoins atomically
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun earnCoinsViaCloud(amount: Long, reason: String): Result<Map<String, Any?>> {
        val params = mapOf("amount" to amount, "reason" to reason)
        return callCloudFunction("earnCoins", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun spendCoinsViaCloud(amount: Long, reason: String): Result<Map<String, Any?>> {
        val params = mapOf("amount" to amount, "reason" to reason)
        return callCloudFunction("spendCoins", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Admin: Cloud Function call to assign user role
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun assignUserRoleViaCloud(targetUid: String, role: String): Result<Map<String, Any?>> {
        val params = mapOf("targetUid" to targetUid, "role" to role)
        return callCloudFunction("assignUserRole", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Support: Cloud Function call to create a ticket
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun createTicketViaCloud(subject: String, category: String, description: String, priority: String): Result<Map<String, Any?>> {
        val params = mapOf(
            "subject" to subject,
            "category" to category,
            "description" to description,
            "priority" to priority
        )
        return callCloudFunction("createTicket", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Festivals: Cloud Function call to redeem a festival offer
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun redeemFestivalOfferViaCloud(offerId: String): Result<Map<String, Any?>> {
        val params = mapOf("offerId" to offerId)
        return callCloudFunction("redeemFestivalOffer", params).mapCatching { data ->
            (data as? Map<String, Any?>) ?: emptyMap()
        }
    }

    /**
     * Stop all active real-time listeners cleanly
     */
    @Synchronized
    fun stopSync() {
        safeLog("Stopping and removing ${activeRegistrations.size} active real-time listeners.")
        for (reg in activeRegistrations) {
            try {
                reg.remove()
            } catch (e: Exception) {
                safeLog("Error removing listener: ${e.message}")
            }
        }
        activeRegistrations.clear()
    }

    /**
     * Start real-time listeners for the authenticated user and sync state to repository callbacks
     */
    @Synchronized
    fun startSync(
        userId: String,
        role: AdminRole,
        onProfileUpdate: (UserProfile) -> Unit,
        onCoinBalanceUpdate: (Long) -> Unit,
        onProjectsUpdate: (List<Project>) -> Unit,
        onRoomsUpdate: (List<CineRoom>) -> Unit,
        onLiveStreamsUpdate: (List<LiveStream>) -> Unit,
        onLiveChatUpdate: (List<LiveChatMessage>) -> Unit,
        onFriendRequestsUpdate: (List<FriendRequest>) -> Unit,
        onFriendshipsUpdate: (List<Friendship>) -> Unit,
        onNotificationsUpdate: (List<InAppNotification>) -> Unit,
        onTicketsUpdate: (List<SupportTicket>) -> Unit,
        onPaymentOrdersUpdate: (List<ZapUpiOrder>) -> Unit,
        onFestivalConfigUpdate: (FestivalConfiguration) -> Unit,
        onFestivalOffersUpdate: (List<FestivalOffer>) -> Unit,
        onAuditLogsUpdate: (List<AuditLog>) -> Unit,
        onAllUsersUpdate: (List<UserProfile>) -> Unit
    ) {
        ensureInitialized()
        val db = firestore ?: return
        stopSync() // Ensure no duplicate listeners

        if (userId.isEmpty()) return

        safeLog("Attaching real-time multi-device listeners for user: $userId with role: $role")

        // 1. User Profile Listener (/users/{userId})
        try {
            val regProfile = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        safeLog("Profile listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val profile = parseUserProfile(snapshot)
                        onProfileUpdate(profile)
                    }
                }
            activeRegistrations.add(regProfile)
        } catch (e: Exception) {
            safeLog("Failed to attach profile listener: ${e.message}")
        }

        // 2. Coin Account Listener (/coinAccounts/{userId})
        try {
            val regCoins = db.collection("coinAccounts").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val balance = snapshot.getLong("balance") ?: 0L
                        onCoinBalanceUpdate(balance)
                    }
                }
            activeRegistrations.add(regCoins)
        } catch (e: Exception) {
            safeLog("Failed to attach coins listener: ${e.message}")
        }

        // 3. User Projects Listener (/projects where ownerId == userId)
        try {
            val regProjects = db.collection("projects")
                .whereEqualTo("ownerId", userId)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val list = snapshots.documents.mapNotNull { parseProject(it) }
                    if (list.isNotEmpty()) {
                        onProjectsUpdate(list)
                    }
                }
            activeRegistrations.add(regProjects)
        } catch (e: Exception) {
            safeLog("Failed to attach projects listener: ${e.message}")
        }

        // 4. CineRooms (/rooms)
        try {
            val regRooms = db.collection("rooms")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val rooms = snapshots.documents.mapNotNull { parseRoom(it) }
                    if (rooms.isNotEmpty()) onRoomsUpdate(rooms)
                }
            activeRegistrations.add(regRooms)
        } catch (e: Exception) {
            safeLog("Rooms listener error: ${e.message}")
        }

        // 5. Live Streams (/liveStreams)
        try {
            val regLive = db.collection("liveStreams")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val streams = snapshots.documents.mapNotNull { parseLiveStream(it) }
                    if (streams.isNotEmpty()) onLiveStreamsUpdate(streams)
                }
            activeRegistrations.add(regLive)
        } catch (e: Exception) {
            safeLog("Live stream listener error: ${e.message}")
        }

        // 6. Live Chat (/liveChat)
        try {
            val regChat = db.collection("liveChat")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val messages = snapshots.documents.mapNotNull { parseLiveChatMessage(it) }.reversed()
                    if (messages.isNotEmpty()) onLiveChatUpdate(messages)
                }
            activeRegistrations.add(regChat)
        } catch (e: Exception) {
            safeLog("Live chat listener error: ${e.message}")
        }

        // 7. Friend Requests (/friendRequests for current user)
        try {
            val regFreq = db.collection("friendRequests")
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val requests = snapshots.documents.mapNotNull { parseFriendRequest(it) }
                    onFriendRequestsUpdate(requests)
                }
            activeRegistrations.add(regFreq)
        } catch (e: Exception) {
            safeLog("Friend requests listener error: ${e.message}")
        }

        // 8. Notifications (/users/{userId}/notifications)
        try {
            val regNotifs = db.collection("users").document(userId).collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val notifs = snapshots.documents.mapNotNull { parseNotification(it) }
                    onNotificationsUpdate(notifs)
                }
            activeRegistrations.add(regNotifs)
        } catch (e: Exception) {
            safeLog("Notifications listener error: ${e.message}")
        }

        // 9. Support Tickets (/supportTickets)
        try {
            val query = if (role != AdminRole.NONE) {
                db.collection("supportTickets").orderBy("createdAt", Query.Direction.DESCENDING)
            } else {
                db.collection("supportTickets").whereEqualTo("userId", userId)
            }
            val regTickets = query.addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val tickets = snapshots.documents.mapNotNull { parseTicket(it) }
                if (tickets.isNotEmpty()) onTicketsUpdate(tickets)
            }
            activeRegistrations.add(regTickets)
        } catch (e: Exception) {
            safeLog("Tickets listener error: ${e.message}")
        }

        // 10. Payment Orders (/paymentOrders)
        try {
            val orderQuery = if (role != AdminRole.NONE) {
                db.collection("paymentOrders").orderBy("createdAt", Query.Direction.DESCENDING)
            } else {
                db.collection("paymentOrders").whereEqualTo("uid", userId)
            }
            val regOrders = orderQuery.addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val orders = snapshots.documents.mapNotNull { parsePaymentOrder(it) }
                if (orders.isNotEmpty()) onPaymentOrdersUpdate(orders)
            }
            activeRegistrations.add(regOrders)
        } catch (e: Exception) {
            safeLog("Payment orders listener error: ${e.message}")
        }

        // 11. Festival Config (/festivalConfig/current) & Offers (/festivalOffers)
        try {
            val regFest = db.collection("festivalConfig").document("current")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val config = parseFestivalConfig(snapshot)
                    onFestivalConfigUpdate(config)
                }
            activeRegistrations.add(regFest)

            val regOffers = db.collection("festivalOffers")
                .whereEqualTo("isEnabled", true)
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val offers = snapshots.documents.mapNotNull { parseFestivalOffer(it) }
                    if (offers.isNotEmpty()) onFestivalOffersUpdate(offers)
                }
            activeRegistrations.add(regOffers)
        } catch (e: Exception) {
            safeLog("Festival listener error: ${e.message}")
        }

        // 12. Admin Only: Audit Logs (/auditLogs) & User Directory (/users)
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.ADMIN) {
            try {
                val regLogs = db.collection("auditLogs")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null || snapshots == null) return@addSnapshotListener
                        val logs = snapshots.documents.mapNotNull { parseAuditLog(it) }
                        if (logs.isNotEmpty()) onAuditLogsUpdate(logs)
                    }
                activeRegistrations.add(regLogs)

                val regAllUsers = db.collection("users").limit(100)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null || snapshots == null) return@addSnapshotListener
                        val users = snapshots.documents.mapNotNull { parseUserProfile(it) }
                        if (users.isNotEmpty()) onAllUsersUpdate(users)
                    }
                activeRegistrations.add(regAllUsers)
            } catch (e: Exception) {
                safeLog("Admin sync error: ${e.message}")
            }
        }
    }

    // ================= REALTIME CLOUD MUTATIONS ================= //

    suspend fun saveProjectToCloud(project: Project): Boolean {
        return try {
            val targetDb = db ?: return false
            val data = hashMapOf(
                "projectId" to project.id,
                "id" to project.id,
                "ownerId" to project.ownerId,
                "title" to project.title,
                "aspectRatio" to project.aspectRatio,
                "resolutionWidth" to project.resolutionWidth,
                "resolutionHeight" to project.resolutionHeight,
                "frameRate" to project.frameRate,
                "durationMs" to project.durationMs,
                "isArchived" to project.isArchived,
                "isCollaborative" to project.isCollaborative,
                "collaboratorsCount" to project.collaboratorsCount,
                "version" to project.version,
                "visibility" to project.visibility,
                "updatedAt" to System.currentTimeMillis()
            )
            targetDb.collection("projects").document(project.id).set(data).await()
            true
        } catch (e: Exception) {
            safeLog("Failed to save project to cloud: ${e.message}")
            false
        }
    }

    suspend fun deleteProjectFromCloud(projectId: String): Boolean {
        return try {
            val targetDb = db ?: return false
            targetDb.collection("projects").document(projectId).delete().await()
            true
        } catch (e: Exception) {
            safeLog("Failed to delete project: ${e.message}")
            false
        }
    }

    suspend fun recordProjectOperationInCloud(projectId: String, operation: ProjectOperation): Boolean {
        return try {
            val targetDb = db ?: return false
            val data = hashMapOf(
                "operationId" to operation.id,
                "projectId" to projectId,
                "userId" to operation.userId,
                "userName" to operation.userName,
                "operationType" to operation.operationType,
                "details" to operation.details,
                "timestamp" to operation.timestamp
            )
            targetDb.collection("projects").document(projectId).collection("operations").document(operation.id).set(data).await()
            true
        } catch (e: Exception) {
            safeLog("Failed to record project operation: ${e.message}")
            false
        }
    }

    suspend fun sendLiveChatMessageToCloud(message: LiveChatMessage): Boolean {
        return try {
            val targetDb = db ?: return false
            val data = hashMapOf(
                "id" to message.id,
                "senderName" to message.senderName,
                "senderTier" to message.senderTier.name,
                "message" to message.message,
                "coinTip" to message.coinTip,
                "timestamp" to message.timestamp
            )
            targetDb.collection("liveChat").document(message.id).set(data).await()
            true
        } catch (e: Exception) {
            safeLog("Failed to send live chat message: ${e.message}")
            false
        }
    }

    suspend fun createSupportTicketInCloud(ticket: SupportTicket): Boolean {
        return try {
            val targetDb = db ?: return false
            val data = hashMapOf(
                "ticketId" to ticket.ticketId,
                "id" to ticket.ticketId,
                "userId" to ticket.userId,
                "userDisplayName" to ticket.userDisplayName,
                "subject" to ticket.subject,
                "category" to ticket.category.name,
                "description" to ticket.description,
                "priority" to ticket.priority.name,
                "status" to ticket.status.name,
                "attachmentUrl" to (ticket.attachments.firstOrNull()?.fileUrl ?: ""),
                "createdAt" to ticket.createdAt,
                "updatedAt" to ticket.updatedAt
            )
            targetDb.collection("supportTickets").document(ticket.ticketId).set(data).await()
            true
        } catch (e: Exception) {
            safeLog("Failed to create support ticket in cloud: ${e.message}")
            false
        }
    }

    suspend fun savePaymentOrderToCloud(order: ZapUpiOrder): Boolean {
        return try {
            val targetDb = db ?: return false
            val data = hashMapOf(
                "orderId" to order.orderId,
                "uid" to order.userId,
                "userDisplayName" to order.userDisplayName,
                "itemType" to order.itemType,
                "itemId" to order.itemId,
                "title" to order.title,
                "amountInr" to order.amountInr,
                "status" to order.status.name,
                "upiIntentUrl" to order.upiIntentUrl,
                "qrPayload" to order.qrPayload,
                "utrNumber" to (order.utrNumber ?: ""),
                "createdAt" to order.createdAt,
                "expiresAt" to order.expiresAt
            )
            targetDb.collection("paymentOrders").document(order.orderId).set(data).await()
            true
        } catch (e: Exception) {
            safeLog("Failed to save payment order in cloud: ${e.message}")
            false
        }
    }

    // ================= PARSING HELPERS ================= //

    private fun parseUserProfile(doc: DocumentSnapshot): UserProfile {
        val uid = doc.getString("uid") ?: doc.id
        val username = doc.getString("username") ?: ""
        val displayName = doc.getString("displayName") ?: username
        val email = doc.getString("email") ?: ""
        val bio = doc.getString("bio") ?: ""
        val avatarUrl = doc.getString("photoUrl") ?: doc.getString("avatarUrl") ?: ""
        val tierStr = doc.getString("membershipTier") ?: "FREE"
        val tier = try { MembershipTier.valueOf(tierStr) } catch (e: Exception) { MembershipTier.FREE }
        val roleStr = doc.getString("role")?.uppercase() ?: "NONE"
        val role = when (roleStr) {
            "SUPER_ADMIN" -> AdminRole.SUPER_ADMIN
            "ADMIN" -> AdminRole.ADMIN
            "MODERATOR" -> AdminRole.MODERATOR
            "SUPPORT" -> AdminRole.SUPPORT
            "CONTENT_ADMIN" -> AdminRole.CONTENT_ADMIN
            else -> AdminRole.NONE
        }
        val coins = doc.getLong("coinBalance") ?: 0L
        @Suppress("UNCHECKED_CAST")
        val badges = (doc.get("badges") as? List<String>) ?: emptyList()
        val isBanned = doc.getBoolean("isBanned") ?: false

        return UserProfile(
            uid = uid,
            username = username,
            displayName = displayName,
            email = email,
            bio = bio,
            avatarUrl = avatarUrl,
            membershipTier = tier,
            adminRole = role,
            coinBalance = coins,
            badges = badges,
            isBanned = isBanned
        )
    }

    private fun parseProject(doc: DocumentSnapshot): Project? {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: "Untitled"
        val ownerId = doc.getString("ownerId") ?: ""
        val aspect = doc.getString("aspectRatio") ?: "16:9"
        val resW = doc.getLong("resolutionWidth")?.toInt() ?: 1920
        val resH = doc.getLong("resolutionHeight")?.toInt() ?: 1080
        val duration = doc.getLong("durationMs") ?: 10000L
        val archived = doc.getBoolean("isArchived") ?: false
        val collab = doc.getBoolean("isCollaborative") ?: false
        val collCount = doc.getLong("collaboratorsCount")?.toInt() ?: 1
        val version = doc.getLong("version")?.toInt() ?: 1
        val vis = doc.getString("visibility") ?: "PRIVATE"

        return Project(
            id = id,
            title = title,
            ownerId = ownerId,
            aspectRatio = aspect,
            resolutionWidth = resW,
            resolutionHeight = resH,
            durationMs = duration,
            isArchived = archived,
            isCollaborative = collab,
            collaboratorsCount = collCount,
            version = version,
            visibility = vis
        )
    }

    private fun parseRoom(doc: DocumentSnapshot): CineRoom? {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: "CineRoom"
        val desc = doc.getString("description") ?: ""
        val tierStr = doc.getString("requiredTier") ?: "FREE"
        val tier = try { MembershipTier.valueOf(tierStr) } catch (e: Exception) { MembershipTier.FREE }
        val memberCount = doc.getLong("memberCount")?.toInt() ?: 100
        val isLive = doc.getBoolean("isLive") ?: false
        val topic = doc.getString("currentTopic") ?: ""

        return CineRoom(
            id = id,
            name = name,
            description = desc,
            requiredTier = tier,
            memberCount = memberCount,
            isLive = isLive,
            currentTopic = topic
        )
    }

    private fun parseLiveStream(doc: DocumentSnapshot): LiveStream? {
        val id = doc.getString("id") ?: doc.id
        val creatorId = doc.getString("creatorId") ?: ""
        val creatorName = doc.getString("creatorName") ?: "Creator"
        val title = doc.getString("title") ?: "Live Stream"
        val url = doc.getString("hlsStreamUrl") ?: ""
        val viewers = doc.getLong("viewerCount")?.toInt() ?: 0
        val isLive = doc.getBoolean("isLive") ?: false
        val category = doc.getString("category") ?: "General"

        return LiveStream(
            id = id,
            creatorId = creatorId,
            creatorName = creatorName,
            title = title,
            hlsStreamUrl = url,
            viewerCount = viewers,
            isLive = isLive,
            category = category
        )
    }

    private fun parseLiveChatMessage(doc: DocumentSnapshot): LiveChatMessage? {
        val id = doc.getString("id") ?: doc.id
        val sender = doc.getString("senderName") ?: "Viewer"
        val tierStr = doc.getString("senderTier") ?: "FREE"
        val tier = try { MembershipTier.valueOf(tierStr) } catch (e: Exception) { MembershipTier.FREE }
        val msg = doc.getString("message") ?: ""
        val tip = doc.getLong("coinTip") ?: 0L
        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

        return LiveChatMessage(
            id = id,
            senderName = sender,
            senderTier = tier,
            message = msg,
            coinTip = tip,
            timestamp = ts
        )
    }

    private fun parseFriendRequest(doc: DocumentSnapshot): FriendRequest? {
        val id = doc.getString("id") ?: doc.id
        val sId = doc.getString("senderId") ?: ""
        val sName = doc.getString("senderName") ?: "Creator"
        val sUname = doc.getString("senderUsername") ?: "creator"
        val rId = doc.getString("receiverId") ?: ""
        val rName = doc.getString("receiverName") ?: ""
        val status = doc.getString("status") ?: "PENDING"

        return FriendRequest(
            id = id,
            senderId = sId,
            senderName = sName,
            senderUsername = sUname,
            receiverId = rId,
            receiverName = rName,
            status = status
        )
    }

    private fun parseNotification(doc: DocumentSnapshot): InAppNotification? {
        val id = doc.getString("id") ?: doc.id
        val uId = doc.getString("userId") ?: ""
        val typeStr = doc.getString("type") ?: "SYSTEM"
        val category = try { NotificationCategory.valueOf(typeStr) } catch (e: Exception) { NotificationCategory.SYSTEM }
        val title = doc.getString("title") ?: "Notification"
        val body = doc.getString("body") ?: doc.getString("message") ?: ""
        val read = doc.getBoolean("read") ?: false
        val ts = doc.getLong("createdAt") ?: System.currentTimeMillis()

        return InAppNotification(
            id = id,
            userId = uId,
            category = category,
            title = title,
            message = body,
            timestamp = ts,
            isRead = read
        )
    }

    private fun parseTicket(doc: DocumentSnapshot): SupportTicket? {
        val tId = doc.getString("ticketId") ?: doc.id
        val uId = doc.getString("userId") ?: ""
        val uName = doc.getString("userDisplayName") ?: doc.getString("userName") ?: "Creator"
        val sub = doc.getString("subject") ?: ""
        val catStr = doc.getString("category") ?: "OTHER"
        val cat = try { SupportTicketCategory.valueOf(catStr) } catch (e: Exception) { SupportTicketCategory.OTHER }
        val desc = doc.getString("description") ?: ""
        val pStr = doc.getString("priority") ?: "MEDIUM"
        val priority = try { SupportTicketPriority.valueOf(pStr) } catch (e: Exception) { SupportTicketPriority.MEDIUM }
        val sStr = doc.getString("status") ?: "OPEN"
        val status = try { SupportTicketStatus.valueOf(sStr) } catch (e: Exception) { SupportTicketStatus.OPEN }
        val att = doc.getString("attachmentUrl") ?: ""
        val cAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        val uAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

        return SupportTicket(
            ticketId = tId,
            userId = uId,
            userDisplayName = uName,
            subject = sub,
            category = cat,
            description = desc,
            priority = priority,
            status = status,
            attachments = if (att.isNotEmpty()) listOf(SupportAttachment(fileName = "Attachment", fileUrl = att)) else emptyList(),
            createdAt = cAt,
            updatedAt = uAt
        )
    }

    private fun parsePaymentOrder(doc: DocumentSnapshot): ZapUpiOrder? {
        val oId = doc.getString("orderId") ?: doc.id
        val uId = doc.getString("uid") ?: doc.getString("userId") ?: ""
        val uName = doc.getString("userDisplayName") ?: "Creator"
        val itemType = doc.getString("itemType") ?: "CINECOINS"
        val itemId = doc.getString("itemId") ?: ""
        val title = doc.getString("title") ?: "Order"
        val amount = doc.getDouble("amountInr") ?: 0.0
        val sStr = doc.getString("status") ?: "PENDING"
        val status = try { PaymentOrderStatus.valueOf(sStr) } catch (e: Exception) { PaymentOrderStatus.PENDING }
        val upi = doc.getString("upiIntentUrl") ?: ""
        val qr = doc.getString("qrPayload") ?: ""
        val utr = doc.getString("utrNumber")
        val cAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        val expAt = doc.getLong("expiresAt") ?: (cAt + 8 * 60 * 1000)

        return ZapUpiOrder(
            orderId = oId,
            userId = uId,
            userDisplayName = uName,
            itemType = itemType,
            itemId = itemId,
            title = title,
            amountInr = amount,
            status = status,
            upiIntentUrl = upi,
            qrPayload = qr,
            utrNumber = utr,
            createdAt = cAt,
            expiresAt = expAt
        )
    }

    private fun parseFestivalConfig(doc: DocumentSnapshot): FestivalConfiguration {
        val typeStr = doc.getString("festivalType") ?: "DIWALI"
        val type = try { FestivalType.valueOf(typeStr) } catch (e: Exception) { FestivalType.DIWALI }
        val enabled = doc.getBoolean("isEnabled") ?: true
        val banner = doc.getString("bannerText") ?: type.defaultHeadline

        return FestivalConfiguration(
            activeFestival = type,
            isFestivalActive = enabled,
            bannerText = banner
        )
    }

    private fun parseFestivalOffer(doc: DocumentSnapshot): FestivalOffer? {
        val id = doc.getString("offerId") ?: doc.id
        val festStr = doc.getString("festivalId") ?: "DIWALI"
        val fest = try { FestivalType.valueOf(festStr) } catch (e: Exception) { FestivalType.DIWALI }
        val title = doc.getString("title") ?: "Festival Offer"
        val orig = doc.getDouble("originalPrice") ?: 999.0
        val disc = doc.getDouble("discountValue") ?: 30.0
        val finalP = doc.getDouble("finalPrice") ?: 699.0
        val bonus = doc.getLong("bonusCoins") ?: 0L
        val coupon = doc.getString("couponCode")
        val enabled = doc.getBoolean("isEnabled") ?: true

        return FestivalOffer(
            offerId = id,
            title = title,
            description = "Special festival exclusive creator offer",
            festival = fest,
            discountValue = disc,
            originalPrice = orig,
            finalPrice = finalP,
            bonusCoins = bonus,
            couponCode = coupon,
            enabled = enabled
        )
    }

    private fun parseAuditLog(doc: DocumentSnapshot): AuditLog? {
        val id = doc.getString("id") ?: doc.id
        val adminUid = doc.getString("actorUid") ?: doc.getString("adminUid") ?: "system"
        val action = doc.getString("action") ?: "UNKNOWN"
        val target = doc.getString("targetId") ?: doc.getString("target") ?: ""
        val reason = doc.getString("reason") ?: "Administrative Action"
        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

        return AuditLog(
            id = id,
            adminUid = adminUid,
            action = action,
            target = target,
            reason = reason,
            timestamp = ts
        )
    }
}
