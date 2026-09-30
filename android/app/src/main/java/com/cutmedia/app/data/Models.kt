package com.cutmedia.app.data

data class CutProject(
    val id: String = "",
    val title: String = "",
    val durationSec: Float = 15.0f,
    val aspectRatio: String = "16:9",
    val filterName: String = "None",
    val speed: Float = 1.0f,
    val volumePercent: Int = 100,
    val trimStartSec: Float = 0.0f,
    val trimEndSec: Float = 15.0f,
    val uriString: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

data class UserAccount(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "CREATOR",
    val premiumRole: String = "FOUNDER", // FOUNDER, VIP, SUBSCRIBED, BASIC
    val coins: Int = 150,
    val yellowCoins: Int = 150, // Daily Check-in / Activity coins
    val blueCoins: Int = 25,   // AI Tools / Hourly Usage coins
    val userIdTag: String = "VID-78291",
    val streakDays: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromEmail: String = "",
    val fromName: String = "",
    val fromTag: String = "VID-10492",
    val toUid: String = "",
    val toEmail: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

data class FriendUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "CREATOR",
    val userIdTag: String = "VID-10000",
    val isOnline: Boolean = true,
    val lastMessage: String = ""
)

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderTag: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean = false,
    val projectAttachment: String? = null
)

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "SYSTEM", // SYSTEM, FRIEND, RENDER, COINS, CAMPAIGN
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionData: String? = null
)

data class CoinTransaction(
    val id: String = "",
    val title: String = "",
    val amount: Int = 0,
    val coinType: String = "YELLOW", // YELLOW or BLUE
    val isCredit: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class VideoTemplate(
    val id: String = "",
    val title: String = "",
    val category: String = "Trending",
    val durationSec: Float = 15.0f,
    val aspectRatio: String = "9:16",
    val filterName: String = "Cinematic",
    val downloads: Int = 1240
)

data class AiGenerationResult(
    val id: String = "",
    val toolType: String = "",
    val prompt: String = "",
    val resultText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PromotedFeature(
    val id: String = "",
    val title: String = "",
    val category: String = "AI", // "AI", "TEMPLATE", "EFFECT", "THEME", "EVENT"
    val description: String = "",
    val badgeText: String = "WORLDWIDE SPOTLIGHT",
    val discountOrBonus: String = "Free Unlocked",
    val targetScreen: String = "AI_STUDIO", // "AI_STUDIO", "EDITOR", "PROJECTS", "THEME"
    val promotedBy: String = "Master Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

