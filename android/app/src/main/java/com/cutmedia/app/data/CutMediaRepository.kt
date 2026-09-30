package com.cutmedia.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CutMediaRepository(private val context: Context) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("cutmedia_prefs", Context.MODE_PRIVATE)

    // Current User
    fun getCurrentUser(): UserAccount {
        val fbUser = auth.currentUser
        val yellow = prefs.getInt("yellow_coins", 150)
        val blue = prefs.getInt("blue_coins", 25)
        val streak = prefs.getInt("streak_days", 5)
        val tag = prefs.getString("user_id_tag", "VID-78291") ?: "VID-78291"
        val pRole = prefs.getString("premium_role", "FOUNDER") ?: "FOUNDER"

        if (fbUser != null) {
            return UserAccount(
                uid = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: (fbUser.email?.substringBefore("@") ?: "Creator"),
                role = prefs.getString("user_role", "CREATOR") ?: "CREATOR",
                premiumRole = pRole,
                coins = yellow,
                yellowCoins = yellow,
                blueCoins = blue,
                userIdTag = tag,
                streakDays = streak
            )
        }
        return UserAccount(
            uid = "creator_local",
            email = "robinisking3@gmail.com",
            displayName = prefs.getString("user_display_name", "Robin") ?: "Robin",
            role = "CREATOR",
            premiumRole = pRole,
            coins = yellow,
            yellowCoins = yellow,
            blueCoins = blue,
            userIdTag = tag,
            streakDays = streak
        )
    }

    // Coin Economy
    fun getYellowCoins(): Int = prefs.getInt("yellow_coins", 150)
    fun getBlueCoins(): Int = prefs.getInt("blue_coins", 25)

    fun claimDailyCheckIn(): Pair<Boolean, String> {
        val lastClaim = prefs.getLong("last_checkin_timestamp", 0L)
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        if (now - lastClaim < oneDay && lastClaim != 0L) {
            return Pair(false, "Daily check-in already claimed today. Next reward in 24 hours!")
        }
        val currentYellow = getYellowCoins()
        val currentStreak = prefs.getInt("streak_days", 5)
        val newStreak = currentStreak + 1
        val reward = 10 + (newStreak % 7) * 2

        prefs.edit()
            .putInt("yellow_coins", currentYellow + reward)
            .putInt("streak_days", newStreak)
            .putLong("last_checkin_timestamp", now)
            .apply()

        addNotification(
            title = "Daily Streak Maintained! (Day $newStreak)",
            message = "You earned +$reward Yellow Coins. Keep checking in for bonus multipliers!",
            type = "COINS"
        )
        recordCoinTx("Daily Check-in Streak (Day $newStreak)", reward, "YELLOW", true)
        return Pair(true, "+$reward Yellow Coins added to your wallet! Streak: Day $newStreak")
    }

    fun claimHourlyBonus(): Pair<Boolean, String> {
        val lastClaim = prefs.getLong("last_hourly_timestamp", 0L)
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L
        if (now - lastClaim < oneHour && lastClaim != 0L) {
            return Pair(false, "Hourly AI bonus already claimed. Next reward in 60 minutes.")
        }
        val currentBlue = getBlueCoins()
        prefs.edit()
            .putInt("blue_coins", currentBlue + 2)
            .putLong("last_hourly_timestamp", now)
            .apply()

        addNotification(
            title = "Hourly AI Bonus Claimed!",
            message = "Received +2 Blue Coins for active timeline video editing.",
            type = "COINS"
        )
        recordCoinTx("Hourly Editing Bonus", 2, "BLUE", true)
        return Pair(true, "+2 Blue Coins claimed! Ready for AI Auto-Cut & Captions.")
    }

    fun spendBlueCoins(amount: Int, reason: String): Boolean {
        val current = getBlueCoins()
        if (current < amount) return false
        prefs.edit().putInt("blue_coins", current - amount).apply()
        recordCoinTx(reason, amount, "BLUE", false)
        return true
    }

    fun redeemPromoCode(code: String): Pair<Boolean, String> {
        val c = code.trim().uppercase()
        val redeemed = prefs.getStringSet("redeemed_codes", mutableSetOf()) ?: mutableSetOf()
        if (redeemed.contains(c)) {
            return Pair(false, "Promo code '$c' has already been redeemed on this device.")
        }

        when (c) {
            "DIWALI50" -> {
                val y = getYellowCoins() + 50
                val b = getBlueCoins() + 5
                prefs.edit()
                    .putInt("yellow_coins", y)
                    .putInt("blue_coins", b)
                    .putStringSet("redeemed_codes", redeemed.toMutableSet().apply { add(c) })
                    .apply()
                addNotification("Diwali Festive Gift Unlocked", "Redeemed DIWALI50 for +50 Yellow & +5 Blue Coins!", "CAMPAIGN")
                recordCoinTx("Promo Code DIWALI50", 50, "YELLOW", true)
                return Pair(true, "Diwali Gift Applied! +50 Yellow Coins & +5 Blue Coins awarded.")
            }
            "CREATORPRO", "CINECUTVIP" -> {
                val b = getBlueCoins() + 20
                prefs.edit()
                    .putInt("blue_coins", b)
                    .putString("premium_role", "VIP")
                    .putStringSet("redeemed_codes", redeemed.toMutableSet().apply { add(c) })
                    .apply()
                addNotification("VIP Creator Unlocked", "Promo code applied: VIP role unlocked with +20 AI Blue Coins!", "CAMPAIGN")
                recordCoinTx("Promo Code $c (VIP)", 20, "BLUE", true)
                return Pair(true, "VIP Creator Activated! +20 Blue Coins & VIP Badge unlocked.")
            }
            else -> return Pair(false, "Invalid promo code. Try DIWALI50 or CREATORPRO.")
        }
    }

    fun processZapUpiSubscription(
        planName: String,
        amountInr: Int,
        role: String,
        blueCoinsReward: Int,
        orderId: String,
        txnId: String
    ): Boolean {
        val b = getBlueCoins() + blueCoinsReward
        prefs.edit()
            .putInt("blue_coins", b)
            .putString("premium_role", role)
            .apply()

        addNotification(
            title = "Subscription Active: $planName",
            message = "Payment of ₹$amountInr verified via ZapUPI Gateway ($txnId). Unlocked $role role & +$blueCoinsReward Blue Coins!",
            type = "SUBSCRIPTION"
        )
        recordCoinTx("ZapUPI Subscription: $planName", blueCoinsReward, "BLUE", true)

        try {
            val fbUser = auth.currentUser
            if (fbUser != null) {
                firestore.collection("users").document(fbUser.uid).update(
                    mapOf(
                        "premiumRole" to role,
                        "blueCoins" to b,
                        "subscriptionPlanId" to planName.lowercase().replace(" ", "-"),
                        "subscriptionExpiresAt" to (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                    )
                )
            }
        } catch (_: Exception) {}
        return true
    }

    private fun recordCoinTx(title: String, amount: Int, coinType: String, isCredit: Boolean) {
        val raw = prefs.getString("coin_transactions", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            val obj = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("title", title)
                put("amount", amount)
                put("coinType", coinType)
                put("isCredit", isCredit)
                put("timestamp", System.currentTimeMillis())
            }
            arr.put(0, obj)
            prefs.edit().putString("coin_transactions", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getCoinTransactions(): List<CoinTransaction> {
        val raw = prefs.getString("coin_transactions", null)
        if (raw == null) {
            // Seed initial transactions
            val initial = listOf(
                CoinTransaction(UUID.randomUUID().toString(), "Welcome Creator Grant", 150, "YELLOW", true, System.currentTimeMillis() - 86400000L),
                CoinTransaction(UUID.randomUUID().toString(), "Starter AI Credit Pack", 25, "BLUE", true, System.currentTimeMillis() - 86400000L),
                CoinTransaction(UUID.randomUUID().toString(), "Daily Check-in Streak", 10, "YELLOW", true, System.currentTimeMillis() - 3600000L)
            )
            return initial
        }
        val list = mutableListOf<CoinTransaction>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CoinTransaction(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Reward"),
                        amount = obj.optInt("amount", 0),
                        coinType = obj.optString("coinType", "YELLOW"),
                        isCredit = obj.optBoolean("isCredit", true),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    // Notifications Center
    fun getNotifications(): List<AppNotification> {
        val raw = prefs.getString("app_notifications", null)
        if (raw == null) {
            val initial = listOf(
                AppNotification(
                    id = "notif_1",
                    title = "Diwali Festive Creator Bonus",
                    message = "Celebrate with us! Use code DIWALI50 in wallet to claim +50 Yellow Coins & +5 Blue Coins.",
                    type = "CAMPAIGN",
                    timestamp = System.currentTimeMillis() - 3600000L,
                    isRead = false
                ),
                AppNotification(
                    id = "notif_2",
                    title = "Collaboration Request",
                    message = "Aarav Sharma (VID-10492) invited you to collaborate on 'Mumbai_Night_Reel'.",
                    type = "FRIEND",
                    timestamp = System.currentTimeMillis() - 7200000L,
                    isRead = false
                ),
                AppNotification(
                    id = "notif_3",
                    title = "Render Engine v2.4 Ready",
                    message = "Hardware acceleration active: 4K 60fps export and multi-track audio mixing is enabled.",
                    type = "RENDER",
                    timestamp = System.currentTimeMillis() - 14400000L,
                    isRead = true
                )
            )
            return initial
        }
        val list = mutableListOf<AppNotification>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Alert"),
                        message = obj.optString("message", ""),
                        type = obj.optString("type", "SYSTEM"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        actionData = obj.optString("actionData", null)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun addNotification(title: String, message: String, type: String = "SYSTEM", actionData: String? = null) {
        val current = getNotifications().toMutableList()
        current.add(0, AppNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionData = actionData
        ))
        val arr = JSONArray()
        current.take(30).forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("message", n.message)
                put("type", n.type)
                put("timestamp", n.timestamp)
                put("isRead", n.isRead)
                put("actionData", n.actionData ?: "")
            })
        }
        prefs.edit().putString("app_notifications", arr.toString()).apply()
    }

    fun markNotificationRead(id: String) {
        val current = getNotifications().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(isRead = true)
            val arr = JSONArray()
            current.forEach { n ->
                arr.put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("message", n.message)
                    put("type", n.type)
                    put("timestamp", n.timestamp)
                    put("isRead", n.isRead)
                })
            }
            prefs.edit().putString("app_notifications", arr.toString()).apply()
        }
    }

    fun clearNotifications() {
        prefs.edit().putString("app_notifications", "[]").apply()
    }

    // Direct Messaging / Chat System
    fun getChatMessages(friendUid: String): List<ChatMessage> {
        val key = "chat_$friendUid"
        val raw = prefs.getString(key, null)
        if (raw == null) {
            // Seed initial friendly conversation
            val seed = when (friendUid) {
                "user_aarav" -> listOf(
                    ChatMessage(
                        id = "m1",
                        chatId = key,
                        senderId = "user_aarav",
                        senderName = "Aarav Sharma",
                        senderTag = "VID-10492",
                        text = "Hey Robin! Did you see the new Cinematic LUT in CineCut?",
                        timestamp = System.currentTimeMillis() - 3600000L,
                        isMe = false
                    ),
                    ChatMessage(
                        id = "m2",
                        chatId = key,
                        senderId = "me",
                        senderName = "Robin",
                        senderTag = "VID-78291",
                        text = "Yes! I used it on my sunset reel at 4K 60fps. Looks incredible!",
                        timestamp = System.currentTimeMillis() - 3000000L,
                        isMe = true
                    ),
                    ChatMessage(
                        id = "m3",
                        chatId = key,
                        senderId = "user_aarav",
                        senderName = "Aarav Sharma",
                        senderTag = "VID-10492",
                        text = "Can you send the project? Let me tweak the audio fade curves on the beat drop.",
                        timestamp = System.currentTimeMillis() - 1200000L,
                        isMe = false
                    )
                )
                "user_priya" -> listOf(
                    ChatMessage(
                        id = "m4",
                        chatId = key,
                        senderId = "user_priya",
                        senderName = "Priya Patel",
                        senderTag = "VID-20914",
                        text = "Hi Robin! I'm editing a Mumbai travel documentary. Do you want to collaborate?",
                        timestamp = System.currentTimeMillis() - 7200000L,
                        isMe = false
                    )
                )
                else -> emptyList()
            }
            return seed
        }
        val list = mutableListOf<ChatMessage>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        chatId = key,
                        senderId = obj.optString("senderId", ""),
                        senderName = obj.optString("senderName", ""),
                        senderTag = obj.optString("senderTag", ""),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isMe = obj.optBoolean("isMe", false),
                        projectAttachment = obj.optString("projectAttachment", null)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun sendChatMessage(friendUid: String, text: String, projectAttachment: String? = null): ChatMessage {
        val key = "chat_$friendUid"
        val current = getChatMessages(friendUid).toMutableList()
        val user = getCurrentUser()
        val msg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            chatId = key,
            senderId = user.uid,
            senderName = user.displayName,
            senderTag = user.userIdTag,
            text = text,
            timestamp = System.currentTimeMillis(),
            isMe = true,
            projectAttachment = projectAttachment
        )
        current.add(msg)
        val arr = JSONArray()
        current.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("senderId", m.senderId)
                put("senderName", m.senderName)
                put("senderTag", m.senderTag)
                put("text", m.text)
                put("timestamp", m.timestamp)
                put("isMe", m.isMe)
                put("projectAttachment", m.projectAttachment ?: "")
            })
        }
        prefs.edit().putString(key, arr.toString()).apply()

        // Also record simulated reply if friend is active
        if (text.isNotBlank()) {
            addNotification(
                title = "Message Sent",
                message = "Delivered to $friendUid via CineCut Direct Chat.",
                type = "FRIEND"
            )
        }
        return msg
    }

    // Default pre-seeded creator network for zero-friction collaboration
    fun getSuggestedCreators(): List<FriendUser> {
        return listOf(
            FriendUser("user_aarav", "aarav@cinecut.io", "Aarav Sharma", "VIP Creator", "VID-10492", true, "Active 2m ago"),
            FriendUser("user_priya", "priya@cinecut.io", "Priya Patel", "Cinematographer", "VID-20914", true, "Editing 4K Reel"),
            FriendUser("user_devon", "devon@cinecut.io", "Devon King", "Sound Designer", "VID-90214", false, "Offline"),
            FriendUser("user_rohan", "rohan@cinecut.io", "Rohan Verma", "Motion Designer", "VID-30192", true, "Online"),
            FriendUser("user_ananya", "ananya@cinecut.io", "Ananya Roy", "Colorist", "VID-40182", true, "Active 10m ago")
        )
    }

    fun getLocalFriends(): List<FriendUser> {
        val raw = prefs.getString("local_friends", null)
        if (raw == null) {
            val initial = listOf(
                FriendUser("user_aarav", "aarav@cinecut.io", "Aarav Sharma", "VIP Creator", "VID-10492", true, "Can you send the project?"),
                FriendUser("user_priya", "priya@cinecut.io", "Priya Patel", "Cinematographer", "VID-20914", true, "Hi Robin! Want to collaborate?")
            )
            return initial
        }
        val list = mutableListOf<FriendUser>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FriendUser(
                        uid = obj.optString("uid"),
                        email = obj.optString("email"),
                        displayName = obj.optString("displayName"),
                        role = obj.optString("role", "CREATOR"),
                        userIdTag = obj.optString("userIdTag", "VID-10000"),
                        isOnline = obj.optBoolean("isOnline", true),
                        lastMessage = obj.optString("lastMessage", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun addLocalFriend(friend: FriendUser) {
        val current = getLocalFriends().toMutableList()
        if (current.none { it.uid == friend.uid }) {
            current.add(friend)
            val arr = JSONArray()
            current.forEach { f ->
                arr.put(JSONObject().apply {
                    put("uid", f.uid)
                    put("email", f.email)
                    put("displayName", f.displayName)
                    put("role", f.role)
                    put("userIdTag", f.userIdTag)
                    put("isOnline", f.isOnline)
                    put("lastMessage", f.lastMessage)
                })
            }
            prefs.edit().putString("local_friends", arr.toString()).apply()
            addNotification(
                title = "New Friend Added",
                message = "${friend.displayName} (${friend.userIdTag}) is now in your collaborative team.",
                type = "FRIEND"
            )
        }
    }

    fun getLocalPendingRequests(): List<FriendRequest> {
        val raw = prefs.getString("local_pending_requests", null)
        if (raw == null) {
            return listOf(
                FriendRequest(
                    id = "req_rohan",
                    fromUid = "user_rohan",
                    fromEmail = "rohan@cinecut.io",
                    fromName = "Rohan Verma",
                    fromTag = "VID-30192",
                    toUid = "creator_local",
                    toEmail = "robinisking3@gmail.com",
                    status = "PENDING",
                    timestamp = System.currentTimeMillis() - 1800000L
                )
            )
        }
        val list = mutableListOf<FriendRequest>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FriendRequest(
                        id = obj.optString("id"),
                        fromUid = obj.optString("fromUid"),
                        fromEmail = obj.optString("fromEmail"),
                        fromName = obj.optString("fromName"),
                        fromTag = obj.optString("fromTag", "VID-30000"),
                        toUid = obj.optString("toUid"),
                        toEmail = obj.optString("toEmail"),
                        status = obj.optString("status", "PENDING"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun removeLocalPendingRequest(id: String) {
        val current = getLocalPendingRequests().filterNot { it.id == id }
        val arr = JSONArray()
        current.forEach { req ->
            arr.put(JSONObject().apply {
                put("id", req.id)
                put("fromUid", req.fromUid)
                put("fromEmail", req.fromEmail)
                put("fromName", req.fromName)
                put("fromTag", req.fromTag)
                put("toUid", req.toUid)
                put("toEmail", req.toEmail)
                put("status", req.status)
                put("timestamp", req.timestamp)
            })
        }
        prefs.edit().putString("local_pending_requests", arr.toString()).apply()
    }


    suspend fun signIn(email: String, pass: String): Result<UserAccount> {
        return try {
            val res = auth.signInWithEmailAndPassword(email, pass).await()
            val u = res.user ?: throw Exception("User is null")
            val account = UserAccount(
                uid = u.uid,
                email = u.email ?: "",
                displayName = u.displayName ?: (u.email?.substringBefore("@") ?: "Creator")
            )
            // Sync to Firestore users collection
            try {
                firestore.collection("users").document(u.uid).set(
                    mapOf(
                        "uid" to u.uid,
                        "email" to (u.email ?: ""),
                        "displayName" to account.displayName,
                        "lastActive" to System.currentTimeMillis()
                    )
                ).await()
            } catch (_: Exception) {}
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String, name: String): Result<UserAccount> {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            val u = res.user ?: throw Exception("User is null")
            val account = UserAccount(
                uid = u.uid,
                email = u.email ?: "",
                displayName = name.ifBlank { u.email?.substringBefore("@") ?: "Creator" }
            )
            try {
                firestore.collection("users").document(u.uid).set(
                    mapOf(
                        "uid" to u.uid,
                        "email" to (u.email ?: ""),
                        "displayName" to account.displayName,
                        "role" to "CREATOR",
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            } catch (_: Exception) {}
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Projects Persistence
    fun getSavedProjects(): List<CutProject> {
        val jsonStr = prefs.getString("saved_projects", "[]") ?: "[]"
        val list = mutableListOf<CutProject>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CutProject(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Untitled Project"),
                        durationSec = obj.optDouble("durationSec", 15.0).toFloat(),
                        aspectRatio = obj.optString("aspectRatio", "16:9"),
                        filterName = obj.optString("filterName", "None"),
                        speed = obj.optDouble("speed", 1.0).toFloat(),
                        volumePercent = obj.optInt("volumePercent", 100),
                        trimStartSec = obj.optDouble("trimStartSec", 0.0).toFloat(),
                        trimEndSec = obj.optDouble("trimEndSec", 15.0).toFloat(),
                        uriString = obj.optString("uriString", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveProject(project: CutProject) {
        val existing = getSavedProjects().toMutableList()
        val index = existing.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            existing[index] = project.copy(lastModified = System.currentTimeMillis())
        } else {
            existing.add(0, project.copy(id = if (project.id.isBlank()) UUID.randomUUID().toString() else project.id))
        }

        val arr = JSONArray()
        for (p in existing) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("durationSec", p.durationSec.toDouble())
            obj.put("aspectRatio", p.aspectRatio)
            obj.put("filterName", p.filterName)
            obj.put("speed", p.speed.toDouble())
            obj.put("volumePercent", p.volumePercent)
            obj.put("trimStartSec", p.trimStartSec.toDouble())
            obj.put("trimEndSec", p.trimEndSec.toDouble())
            obj.put("uriString", p.uriString)
            obj.put("createdAt", p.createdAt)
            obj.put("lastModified", p.lastModified)
            arr.put(obj)
        }
        prefs.edit().putString("saved_projects", arr.toString()).apply()

        // Sync to Firestore if signed in
        val user = auth.currentUser
        if (user != null) {
            try {
                firestore.collection("users").document(user.uid)
                    .collection("projects").document(project.id)
                    .set(
                        mapOf(
                            "id" to project.id,
                            "title" to project.title,
                            "durationSec" to project.durationSec,
                            "aspectRatio" to project.aspectRatio,
                            "filterName" to project.filterName,
                            "speed" to project.speed,
                            "volumePercent" to project.volumePercent,
                            "lastModified" to System.currentTimeMillis()
                        )
                    )
            } catch (_: Exception) {}
        }
    }

    fun deleteProject(projectId: String) {
        val existing = getSavedProjects().filterNot { it.id == projectId }
        val arr = JSONArray()
        for (p in existing) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("durationSec", p.durationSec.toDouble())
            obj.put("aspectRatio", p.aspectRatio)
            obj.put("filterName", p.filterName)
            obj.put("speed", p.speed.toDouble())
            obj.put("volumePercent", p.volumePercent)
            obj.put("trimStartSec", p.trimStartSec.toDouble())
            obj.put("trimEndSec", p.trimEndSec.toDouble())
            obj.put("uriString", p.uriString)
            obj.put("createdAt", p.createdAt)
            obj.put("lastModified", p.lastModified)
            arr.put(obj)
        }
        prefs.edit().putString("saved_projects", arr.toString()).apply()

        val user = auth.currentUser
        if (user != null) {
            try {
                firestore.collection("users").document(user.uid)
                    .collection("projects").document(projectId).delete()
            } catch (_: Exception) {}
        }
    }

    // Real Social & Friend System via Firestore
    suspend fun searchUsers(query: String): List<FriendUser> {
        if (query.isBlank()) return emptyList()
        val currentUid = auth.currentUser?.uid ?: ""
        val remoteResults = try {
            val snapshot = firestore.collection("users")
                .limit(20)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                if (uid == currentUid) return@mapNotNull null
                val email = doc.getString("email") ?: ""
                val name = doc.getString("displayName") ?: email.substringBefore("@")
                if (email.contains(query, ignoreCase = true) || name.contains(query, ignoreCase = true) || uid.contains(query, ignoreCase = true)) {
                    FriendUser(
                        uid = uid,
                        email = email,
                        displayName = name,
                        role = doc.getString("role") ?: "CREATOR"
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }

        // If firestore results are empty, search local suggested creators
        if (remoteResults.isEmpty()) {
            val q = query.trim().lowercase()
            return getSuggestedCreators().filter { creator ->
                creator.displayName.lowercase().contains(q) ||
                creator.email.lowercase().contains(q) ||
                creator.userIdTag.lowercase().contains(q)
            }
        }
        return remoteResults
    }

    suspend fun sendFriendRequest(targetUser: FriendUser): Result<String> {
        val user = getCurrentUser()
        val reqId = "req_${user.uid}_${targetUser.uid}"
        
        // Always record locally so friend system works instantly
        addLocalFriend(targetUser)
        addNotification(
            title = "Friend Request Sent",
            message = "Connection established with ${targetUser.displayName} (${targetUser.userIdTag}). You can now start chatting!",
            type = "FRIEND"
        )

        // Try syncing to Firestore
        try {
            firestore.collection("friend_requests").document(reqId).set(
                mapOf(
                    "id" to reqId,
                    "fromUid" to user.uid,
                    "fromEmail" to user.email,
                    "fromName" to user.displayName,
                    "fromTag" to user.userIdTag,
                    "toUid" to targetUser.uid,
                    "toEmail" to targetUser.email,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {}

        return Result.success("Connected with ${targetUser.displayName}! You can now open Direct Chat.")
    }

    suspend fun acceptFriendRequest(requestId: String, otherUid: String, otherName: String, otherEmail: String): Result<String> {
        val user = getCurrentUser()
        val friend = FriendUser(
            uid = otherUid,
            email = otherEmail,
            displayName = otherName,
            role = "CREATOR",
            userIdTag = "VID-${otherUid.takeLast(5).uppercase()}",
            isOnline = true
        )
        addLocalFriend(friend)
        removeLocalPendingRequest(requestId)

        addNotification(
            title = "Friend Request Accepted",
            message = "You and $otherName are now connected in CineCut Studio.",
            type = "FRIEND"
        )

        try {
            firestore.collection("friend_requests").document(requestId)
                .update("status", "ACCEPTED")
                .await()

            firestore.collection("users").document(user.uid)
                .collection("friends").document(otherUid)
                .set(
                    mapOf(
                        "uid" to otherUid,
                        "displayName" to otherName,
                        "email" to otherEmail,
                        "addedAt" to System.currentTimeMillis()
                    )
                ).await()

            firestore.collection("users").document(otherUid)
                .collection("friends").document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "displayName" to user.displayName,
                        "email" to user.email,
                        "addedAt" to System.currentTimeMillis()
                    )
                ).await()
        } catch (_: Exception) {}

        return Result.success("Accepted $otherName's friend request!")
    }

    suspend fun declineFriendRequest(requestId: String): Result<String> {
        removeLocalPendingRequest(requestId)
        try {
            firestore.collection("friend_requests").document(requestId)
                .update("status", "REJECTED")
                .await()
        } catch (_: Exception) {}
        return Result.success("Request dismissed")
    }

    fun listenToIncomingRequests(onUpdate: (List<FriendRequest>) -> Unit): ListenerRegistration? {
        // Emit local requests first so user sees incoming requests immediately
        onUpdate(getLocalPendingRequests())
        val user = auth.currentUser ?: return null
        return try {
            firestore.collection("friend_requests")
                .whereEqualTo("toUid", user.uid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            FriendRequest(
                                id = doc.getString("id") ?: doc.id,
                                fromUid = doc.getString("fromUid") ?: "",
                                fromEmail = doc.getString("fromEmail") ?: "",
                                fromName = doc.getString("fromName") ?: "",
                                fromTag = doc.getString("fromTag") ?: "VID-10492",
                                toUid = doc.getString("toUid") ?: "",
                                toEmail = doc.getString("toEmail") ?: "",
                                status = doc.getString("status") ?: "PENDING",
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        }
                        val combined = (remoteList + getLocalPendingRequests()).distinctBy { it.id }
                        onUpdate(combined)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }

    fun listenToFriends(onUpdate: (List<FriendUser>) -> Unit): ListenerRegistration? {
        // Emit local friends immediately
        onUpdate(getLocalFriends())
        val user = auth.currentUser ?: return null
        return try {
            firestore.collection("users").document(user.uid)
                .collection("friends")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            FriendUser(
                                uid = doc.getString("uid") ?: doc.id,
                                email = doc.getString("email") ?: "",
                                displayName = doc.getString("displayName") ?: "Friend",
                                userIdTag = doc.getString("userIdTag") ?: "VID-10492",
                                isOnline = true
                            )
                        }
                        val combined = (remoteList + getLocalFriends()).distinctBy { it.uid }
                        onUpdate(combined)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }

    // Promoted Features Worldwide System
    fun getPromotedFeatures(): List<PromotedFeature> {
        val raw = prefs.getString("promoted_features_json", null)
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<PromotedFeature>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PromotedFeature(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Featured Feature"),
                            category = obj.optString("category", "AI"),
                            description = obj.optString("description", ""),
                            badgeText = obj.optString("badgeText", "WORLDWIDE SPOTLIGHT"),
                            discountOrBonus = obj.optString("discountOrBonus", "Free Unlocked"),
                            targetScreen = obj.optString("targetScreen", "AI_STUDIO"),
                            promotedBy = obj.optString("promotedBy", "Master Admin"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
                if (list.isNotEmpty()) return list.filter { it.isActive }
            } catch (_: Exception) {}
        }
        val defaultPromotions = listOf(
            PromotedFeature(
                id = "promo-ai-captions",
                title = "AI Auto-Captions & Subtitle Sync",
                category = "AI",
                description = "Deep learning speech-to-text with karaoke bounce animations and multi-language styling.",
                badgeText = "WORLDWIDE SPOTLIGHT",
                discountOrBonus = "0 Blue Coins • Free Unlocked Worldwide",
                targetScreen = "AI_STUDIO",
                promotedBy = "Master Admin",
                isActive = true
            ),
            PromotedFeature(
                id = "promo-4k-upscale",
                title = "4K HDR AI Upscaler & Neural Clarity",
                category = "AI",
                description = "Enhance resolution up to 4K 60fps with super-sampling AI filters.",
                badgeText = "HOT PROMOTION",
                discountOrBonus = "50% Discount (1 Blue Coin)",
                targetScreen = "AI_STUDIO",
                promotedBy = "Master Admin",
                isActive = true
            ),
            PromotedFeature(
                id = "promo-neon-template",
                title = "Cyberpunk Neon Reel 2077",
                category = "TEMPLATE",
                description = "High-octane synthwave transitions, beat-matched glow cuts, and futuristic glitch overlays.",
                badgeText = "FEATURED TEMPLATE",
                discountOrBonus = "Free Export Included",
                targetScreen = "PROJECTS",
                promotedBy = "Master Admin",
                isActive = true
            )
        )
        savePromotedFeaturesLocally(defaultPromotions)
        return defaultPromotions
    }

    private fun savePromotedFeaturesLocally(list: List<PromotedFeature>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("category", item.category)
                obj.put("description", item.description)
                obj.put("badgeText", item.badgeText)
                obj.put("discountOrBonus", item.discountOrBonus)
                obj.put("targetScreen", item.targetScreen)
                obj.put("promotedBy", item.promotedBy)
                obj.put("timestamp", item.timestamp)
                obj.put("isActive", item.isActive)
                array.put(obj)
            }
            prefs.edit().putString("promoted_features_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun promoteFeatureWorldwide(
        id: String,
        title: String,
        category: String,
        description: String,
        badgeText: String = "WORLDWIDE SPOTLIGHT",
        discountOrBonus: String = "Free Unlocked Worldwide",
        targetScreen: String = "AI_STUDIO"
    ): Result<String> {
        val newPromo = PromotedFeature(
            id = id,
            title = title,
            category = category,
            description = description,
            badgeText = badgeText,
            discountOrBonus = discountOrBonus,
            targetScreen = targetScreen,
            promotedBy = getCurrentUser().displayName,
            timestamp = System.currentTimeMillis(),
            isActive = true
        )

        val current = getPromotedFeatures().filter { it.id != id }.toMutableList()
        current.add(0, newPromo)
        savePromotedFeaturesLocally(current)

        addNotification(
            title = "🌟 Worldwide Promotion: $title",
            message = "$description — $discountOrBonus (Promoted by Admin worldwide)",
            type = "CAMPAIGN"
        )

        try {
            val map = hashMapOf(
                "id" to newPromo.id,
                "title" to newPromo.title,
                "category" to newPromo.category,
                "description" to newPromo.description,
                "badgeText" to newPromo.badgeText,
                "discountOrBonus" to newPromo.discountOrBonus,
                "targetScreen" to newPromo.targetScreen,
                "promotedBy" to newPromo.promotedBy,
                "timestamp" to newPromo.timestamp,
                "isActive" to true
            )
            firestore.collection("promoted_features").document(id).set(map)

            firestore.collection("system_broadcasts").add(
                mapOf(
                    "type" to "FEATURE_PROMOTED",
                    "featureId" to id,
                    "title" to "🌟 Worldwide Feature Spotlight: $title",
                    "message" to "$title is now highlighted worldwide with $discountOrBonus!",
                    "targetScreen" to targetScreen,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}

        return Result.success("Feature '$title' successfully promoted worldwide!")
    }

    fun removePromotedFeature(id: String): Result<String> {
        val updated = getPromotedFeatures().map {
            if (it.id == id) it.copy(isActive = false) else it
        }.filter { it.isActive }
        savePromotedFeaturesLocally(updated)

        try {
            firestore.collection("promoted_features").document(id).update("isActive", false)
        } catch (_: Exception) {}

        return Result.success("Promotion deactivated.")
    }

    fun listenToPromotedFeatures(onUpdate: (List<PromotedFeature>) -> Unit): ListenerRegistration? {
        onUpdate(getPromotedFeatures())
        return try {
            firestore.collection("promoted_features")
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            PromotedFeature(
                                id = doc.getString("id") ?: doc.id,
                                title = doc.getString("title") ?: "Featured",
                                category = doc.getString("category") ?: "AI",
                                description = doc.getString("description") ?: "",
                                badgeText = doc.getString("badgeText") ?: "WORLDWIDE SPOTLIGHT",
                                discountOrBonus = doc.getString("discountOrBonus") ?: "Free",
                                targetScreen = doc.getString("targetScreen") ?: "AI_STUDIO",
                                promotedBy = doc.getString("promotedBy") ?: "Master Admin",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                isActive = doc.getBoolean("isActive") ?: true
                            )
                        }
                        val combined = (remoteList + getPromotedFeatures()).distinctBy { it.id }.filter { it.isActive }
                        savePromotedFeaturesLocally(combined)
                        onUpdate(combined)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }
}
