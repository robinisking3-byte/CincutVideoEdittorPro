package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// ================= VIDEO EDITOR MODELS ================= //

enum class TrackType {
    VIDEO, AUDIO, TEXT, EFFECT, ADJUSTMENT
}

enum class TransitionType {
    NONE, CROSS_DISSOLVE, FADE_BLACK, FADE_WHITE, WIPE_LEFT, WIPE_RIGHT, ZOOM_IN, GLITCH, SLIDE_UP
}

enum class InterpolationType {
    LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, BEZIER
}

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val property: String, // "position_x", "position_y", "scale", "rotation", "opacity", "volume"
    val timeMs: Long,
    val value: Float,
    val interpolation: InterpolationType = InterpolationType.EASE_IN_OUT
)

data class ColorGrading(
    val exposure: Float = 0f,       // -2.0f .. +2.0f
    val contrast: Float = 1.0f,     // 0.0f .. 2.0f
    val saturation: Float = 1.0f,   // 0.0f .. 2.0f
    val vibrance: Float = 1.0f,     // 0.0f .. 2.0f
    val temperature: Float = 0f,    // -1.0f .. +1.0f (warm/cool)
    val tint: Float = 0f,           // -1.0f .. +1.0f (green/magenta)
    val highlights: Float = 0f,     // -1.0f .. +1.0f
    val shadows: Float = 0f,        // -1.0f .. +1.0f
    val lutFilter: String = "Normal" // "Normal", "Cinematic Teal & Orange", "Noir", "Vintage 16mm", "Cyberpunk", "Golden Hour"
)

data class ChromaKey(
    val enabled: Boolean = false,
    val targetColor: Long = 0xFF00FF00, // Green
    val tolerance: Float = 0.35f,
    val edgeFeather: Float = 0.15f
)

data class ClipEffect(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val intensity: Float = 1.0f,
    val isEnabled: Boolean = true
)

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "CineCut Title",
    val fontName: String = "SansSerif",
    val fontSizeSp: Float = 28f,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0x00000000,
    val strokeColor: Long = 0xFF000000,
    val strokeWidth: Float = 0f,
    val positionX: Float = 0.5f, // 0.0 .. 1.0 relative
    val positionY: Float = 0.5f,
    val opacity: Float = 1.0f,
    val animation: String = "Fade In" // "Fade In", "Slide Up", "Typewriter", "Zoom"
)

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val mediaUri: String = "",
    val name: String = "Clip",
    val startMs: Long = 0L,
    val durationMs: Long = 4000L,
    val trimInMs: Long = 0L,
    val trimOutMs: Long = 4000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val opacity: Float = 1.0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val isMuted: Boolean = false,
    val isReversed: Boolean = false,
    val transitionIn: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 500L,
    val colorGrading: ColorGrading = ColorGrading(),
    val chromaKey: ChromaKey = ChromaKey(),
    val keyframes: List<Keyframe> = emptyList(),
    val effects: List<ClipEffect> = emptyList(),
    val textOverlay: TextOverlay? = null
)

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Track",
    val type: TrackType = TrackType.VIDEO,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val isVisible: Boolean = true,
    val isSolo: Boolean = false,
    val clips: List<Clip> = emptyList()
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "New Cinematic Project",
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1", "4:5", "21:9"
    val resolutionWidth: Int = 1920,
    val resolutionHeight: Int = 1080,
    val frameRate: Int = 30,
    val durationMs: Long = 12000L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailUri: String = "",
    val ownerId: String = "local_creator",
    val isCollaborative: Boolean = false,
    val version: Int = 1,
    val isArchived: Boolean = false,
    val visibility: String = "PRIVATE", // "PRIVATE", "FRIENDS", "INVITE_ONLY", "COLLABORATORS", "PUBLIC"
    val collaboratorsCount: Int = 1,
    val description: String = ""
)

enum class ProjectRole(val label: String, val level: Int) {
    OWNER("Owner", 6),
    ADMIN("Admin", 5),
    EDITOR("Editor", 4),
    CONTRIBUTOR("Contributor", 3),
    REVIEWER("Reviewer", 2),
    VIEWER("Viewer", 1);

    fun canEdit(): Boolean = level >= 3
    fun canManageMembers(): Boolean = level >= 5
    fun canComment(): Boolean = level >= 2
}

data class Collaborator(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val role: ProjectRole = ProjectRole.EDITOR,
    val coinType: CoinType = CoinType.DEFAULT,
    val coinEditionNumber: Int? = null,
    val joinedAt: Long = System.currentTimeMillis()
)

data class ProjectInvitation(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectTitle: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val role: ProjectRole = ProjectRole.EDITOR,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "DECLINED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis()
)

data class ProjectOperation(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val userId: String,
    val userName: String,
    val operationType: String, // "SPLIT_CLIP", "TRIM_CLIP", "ADD_TRACK", "COLOR_GRADE", "CHANGE_AUDIO", "ADD_TEXT", "KEYFRAME"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProjectVersion(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val versionNumber: Int,
    val name: String,
    val authorId: String,
    val authorName: String,
    val changeSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProjectComment(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String = "",
    val authorCoinType: CoinType = CoinType.DEFAULT,
    val authorCoinEdition: Int? = null,
    val text: String,
    val timecodeMs: Long = 0L,
    val mentions: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class TimelineProject(
    val project: Project,
    val tracks: List<Track>,
    val playheadMs: Long = 0L,
    val selectedClipId: String? = null,
    val selectedTrackId: String? = null
)

enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    HD_720P(1280, 720, "720p HD"),
    FHD_1080P(1920, 1080, "1080p Full HD"),
    QHD_1440P(2560, 1440, "1440p 2K"),
    UHD_4K(3840, 2160, "4K Ultra HD")
}

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val frameRate: Int = 30,
    val bitrateMbps: Float = 12.0f,
    val codec: String = "H.264 / AVC", // "H.264 / AVC", "H.265 / HEVC"
    val audioBitrateKbps: Int = 192
)

data class ExportJob(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectTitle: String,
    val outputPath: String = "",
    val progress: Float = 0f,
    val status: String = "Pending", // "Pending", "Exporting", "Completed", "Cancelled", "Failed"
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
)

// ================= AI DIRECTOR & AUTONOMOUS AI ================= //

enum class AiCommandType {
    ADD_CLIP, MOVE_CLIP, TRIM_CLIP, SPLIT_CLIP, DELETE_CLIP,
    CHANGE_SPEED, ADD_TEXT, SET_EFFECT, ADD_TRANSITION,
    CHANGE_AUDIO, ADD_CAPTIONS, CHANGE_COLOR, APPLY_CINEMATIC_GRADE
}

data class AiDirectorCommand(
    val id: String = UUID.randomUUID().toString(),
    val type: AiCommandType,
    val description: String,
    val targetClipId: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val isApplied: Boolean = false
)

data class AiEditPlan(
    val id: String = UUID.randomUUID().toString(),
    val userPrompt: String,
    val summary: String,
    val commands: List<AiDirectorCommand>,
    val estimatedDurationChangeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class AutonomousPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // "LUT", "Transition", "Text Animation", "Effect"
    val version: String = "1.0",
    val description: String,
    val isApproved: Boolean = true,
    val rating: Float = 4.9f
)

// ================= SOCIAL, COMMUNITY & AUTH ================= //

enum class CoinAnimation(val label: String) {
    SHINE_SWEEP("Shine Sweep"),
    COIN_FLIP("3D Coin Flip"),
    GLOW_PULSE("Glow Pulse"),
    SPARKLE_PARTICLES("Sparkle Particles"),
    ROTATION("Slow Rotation"),
    LIGHT_REFLECTION("Light Reflection"),
    NONE("Static")
}

enum class CoinType(
    val id: String,
    val displayName: String,
    val description: String,
    val baseColor: Long,
    val secondaryColor: Long,
    val accentColor: Long,
    val defaultAnimation: CoinAnimation,
    val level: Int,
    val isLimited: Boolean = false,
    val maxSupply: Int? = null,
    val isStaffOnly: Boolean = false,
    val badgeSymbol: String = "C"
) {
    DEFAULT(
        id = "default",
        displayName = "Default Coin",
        description = "Standard silver creator coin given to all registered members. Features a subtle specular shine.",
        baseColor = 0xFFB0BEC5,
        secondaryColor = 0xFF78909C,
        accentColor = 0xFFECEFF1,
        defaultAnimation = CoinAnimation.SHINE_SWEEP,
        level = 1,
        badgeSymbol = "C"
    ),
    BRONZE(
        id = "bronze",
        displayName = "Bronze Coin",
        description = "Handcrafted bronze metallic texture with a warm ambient pulse.",
        baseColor = 0xFFCD7F32,
        secondaryColor = 0xFF8D5524,
        accentColor = 0xFFFFD1A4,
        defaultAnimation = CoinAnimation.GLOW_PULSE,
        level = 2,
        badgeSymbol = "B"
    ),
    SILVER(
        id = "silver",
        displayName = "Silver Coin",
        description = "Polished sterling silver with continuous light beam reflection.",
        baseColor = 0xFFCFD8DC,
        secondaryColor = 0xFF90A4AE,
        accentColor = 0xFFFFFFFF,
        defaultAnimation = CoinAnimation.LIGHT_REFLECTION,
        level = 3,
        badgeSymbol = "S"
    ),
    GOLD(
        id = "gold",
        displayName = "Gold Coin",
        description = "24K cinematic gold with rotating radial shine sweep.",
        baseColor = 0xFFFFD700,
        secondaryColor = 0xFFDAA520,
        accentColor = 0xFFFFF8DC,
        defaultAnimation = CoinAnimation.ROTATION,
        level = 4,
        badgeSymbol = "G"
    ),
    DIAMOND(
        id = "diamond",
        displayName = "Diamond Coin",
        description = "Prismatic crystalline core radiating starlight sparkle particles.",
        baseColor = 0xFF00E5FF,
        secondaryColor = 0xFF00B0FF,
        accentColor = 0xFFE0F7FA,
        defaultAnimation = CoinAnimation.SPARKLE_PARTICLES,
        level = 5,
        badgeSymbol = "D"
    ),
    VIP(
        id = "vip",
        displayName = "VIP Coin",
        description = "Prestige dual-tone royal cobalt & imperial gold aura with slow 3D rotation.",
        baseColor = 0xFF3D5AFE,
        secondaryColor = 0xFFFFD700,
        accentColor = 0xFF8C9EFF,
        defaultAnimation = CoinAnimation.ROTATION,
        level = 6,
        badgeSymbol = "V"
    ),
    FOUNDER(
        id = "founder",
        displayName = "Founder Coin",
        description = "Extremely limited edition (#1/100). Engraved CineCut insignia with golden crown halo and rhythmic 3D flip.",
        baseColor = 0xFFFF9100,
        secondaryColor = 0xFFFF3D00,
        accentColor = 0xFFFFEA00,
        defaultAnimation = CoinAnimation.COIN_FLIP,
        level = 7,
        isLimited = true,
        maxSupply = 100,
        badgeSymbol = "👑"
    ),
    ADMIN(
        id = "admin",
        displayName = "Admin Coin",
        description = "Imperial red and gold heraldic seal of executive command. Visible only to system administrators.",
        baseColor = 0xFFD50000,
        secondaryColor = 0xFFFFD700,
        accentColor = 0xFFFF5252,
        defaultAnimation = CoinAnimation.ROTATION,
        level = 9,
        isStaffOnly = true,
        badgeSymbol = "⚔"
    ),
    MODERATOR(
        id = "moderator",
        displayName = "Moderator Coin",
        description = "Cobalt blue guardian shield of community integrity and moderation.",
        baseColor = 0xFF2979FF,
        secondaryColor = 0xFF1565C0,
        accentColor = 0xFF82B1FF,
        defaultAnimation = CoinAnimation.GLOW_PULSE,
        level = 8,
        isStaffOnly = true,
        badgeSymbol = "🛡"
    ),
    VERIFIED_CREATOR(
        id = "verified_creator",
        displayName = "Verified Creator Coin",
        description = "Accredited creator emblem featuring a calibrated camera aperture iris.",
        baseColor = 0xFF00E676,
        secondaryColor = 0xFF00B0FF,
        accentColor = 0xFFB9F6CA,
        defaultAnimation = CoinAnimation.LIGHT_REFLECTION,
        level = 5,
        badgeSymbol = "🎬"
    ),
    EARLY_SUPPORTER(
        id = "early_supporter",
        displayName = "Early Supporter Coin",
        description = "Numbered limited-run badge (#1/500) awarded to the foundational CineCut community.",
        baseColor = 0xFF7C4DFF,
        secondaryColor = 0xFF536DFE,
        accentColor = 0xFFB388FF,
        defaultAnimation = CoinAnimation.SHINE_SWEEP,
        level = 4,
        isLimited = true,
        maxSupply = 500,
        badgeSymbol = "✦"
    ),
    BANNED(
        id = "banned",
        displayName = "Banned Coin",
        description = "Dark fractured obsidian coin with punitive crimson cracks indicating revoked community standing.",
        baseColor = 0xFF212121,
        secondaryColor = 0xFFB71C1C,
        accentColor = 0xFFFF1744,
        defaultAnimation = CoinAnimation.NONE,
        level = 0,
        badgeSymbol = "✖"
    )
}

data class LimitedCoinSupply(
    val coinType: CoinType,
    val totalMinted: Int,
    val maxSupply: Int,
    val remainingSupply: Int = maxSupply - totalMinted
)

enum class MembershipTier(val rank: Int, val title: String, val badgeColor: Long) {
    FREE(0, "Creator", 0xFF64748B),
    BRONZE(1, "Bronze Creator", 0xFFCD7F32),
    SILVER(2, "Silver Creator", 0xFFC0C0C0),
    GOLD(3, "Gold Creator", 0xFFFFD700),
    DIAMOND(4, "Diamond Director", 0xFF00E5FF),
    VIP(5, "VIP Studio", 0xFFFF007F),
    FOUNDER(6, "Founder", 0xFFFF5500)
}

enum class AdminRole {
    NONE, SUPPORT, CONTENT_ADMIN, MODERATOR, ADMIN, SUPER_ADMIN
}

enum class AdminPermission {
    VIEW_ANALYTICS,
    MANAGE_USERS,
    BAN_USERS,
    MODERATE_CONTENT,
    ADJUST_COINS,
    MANAGE_MEMBERSHIPS,
    MANAGE_BADGES,
    MANAGE_ROOMS,
    MANAGE_LIVE,
    SEND_NOTIFICATIONS,
    MANAGE_AI,
    MANAGE_FEATURE_FLAGS,
    VIEW_AUDIT_LOGS,
    MANAGE_ADMIN_ROLES,
    SECURITY_CENTER,
    MANAGE_SUPPORT_TICKETS,
    MANAGE_FESTIVALS
}

fun AdminRole.hasPermission(permission: AdminPermission): Boolean {
    return when (this) {
        AdminRole.SUPER_ADMIN -> true
        AdminRole.ADMIN -> when (permission) {
            AdminPermission.MANAGE_ADMIN_ROLES -> false
            else -> true
        }
        AdminRole.MODERATOR -> when (permission) {
            AdminPermission.VIEW_ANALYTICS,
            AdminPermission.MANAGE_USERS,
            AdminPermission.BAN_USERS,
            AdminPermission.MODERATE_CONTENT,
            AdminPermission.MANAGE_ROOMS,
            AdminPermission.MANAGE_LIVE,
            AdminPermission.MANAGE_SUPPORT_TICKETS,
            AdminPermission.VIEW_AUDIT_LOGS -> true
            else -> false
        }
        AdminRole.SUPPORT -> when (permission) {
            AdminPermission.VIEW_ANALYTICS,
            AdminPermission.MANAGE_USERS,
            AdminPermission.MANAGE_MEMBERSHIPS,
            AdminPermission.MANAGE_SUPPORT_TICKETS,
            AdminPermission.VIEW_AUDIT_LOGS -> true
            else -> false
        }
        AdminRole.CONTENT_ADMIN -> when (permission) {
            AdminPermission.VIEW_ANALYTICS,
            AdminPermission.MODERATE_CONTENT,
            AdminPermission.MANAGE_BADGES,
            AdminPermission.MANAGE_ROOMS,
            AdminPermission.SEND_NOTIFICATIONS,
            AdminPermission.MANAGE_FESTIVALS,
            AdminPermission.MANAGE_AI -> true
            else -> false
        }
        AdminRole.NONE -> false
    }
}

enum class ReportStatus {
    PENDING, REVIEWING, RESOLVED, REJECTED, ESCALATED
}

data class AdminNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val audience: String, // "GLOBAL", "BRONZE_PLUS", "GOLD_PLUS", "TARGETED"
    val targetUid: String? = null,
    val category: String = "Announcement",
    val deepLink: String = "cinecut://home",
    val sentByAdminUid: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val title: String,
    val message: String,
    val category: String,
    val deepLink: String
)

data class SystemAnnouncement(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val targetAudience: String = "ALL_USERS",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class AdminBadge(
    val badgeId: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val type: String = "CREATOR", // "CREATOR", "EDITOR", "COMMUNITY", "FOUNDER"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class SecurityHealthStatus(
    val appCheckStatus: String = "Enforced & Attested",
    val authHealth: String = "Operational (0.01% error rate)",
    val backendFunctionsHealth: String = "Healthy (24ms latency)",
    val databaseStatus: String = "Firestore Healthy (99.99% uptime)",
    val privilegedOperationsCount24h: Int = 142
)

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val membershipTier: MembershipTier = MembershipTier.FREE,
    val adminRole: AdminRole = AdminRole.NONE,
    val coinBalance: Long = 0L,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val friendsCount: Int = 0,
    val badges: List<String> = emptyList(),
    val isBanned: Boolean = false,
    val isSuspended: Boolean = false,
    val suspensionReason: String? = null,
    val banReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val reportsReceivedCount: Int = 0,
    val deviceModel: String = "",
    // CineCut Status Coin Badge System (Backend Authoritative)
    val coinType: CoinType = CoinType.BRONZE,
    val coinLevel: Int = 1,
    val coinAnimation: CoinAnimation = CoinAnimation.NONE,
    val coinEffect: String = "",
    val coinColor: Long = 0xFFCD7F32,
    val isCoinLimited: Boolean = false,
    val coinEditionNumber: Int? = null,
    val coinMaxSupply: Int? = null,
    val coinAssignedBy: String? = null,
    val coinAssignedAt: Long? = null,
    // Privacy Controls
    val isCoinBalancePublic: Boolean = false,
    val allowProjectInvites: Boolean = true,
    val allowTagging: Boolean = true
)

enum class FriendState {
    NONE,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    FRIENDS,
    BLOCKED
}

data class Friendship(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val friendUserId: String,
    val friendProfile: UserProfile,
    val state: FriendState = FriendState.FRIENDS,
    val initiatedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FriendRequest(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val senderUsername: String,
    val senderAvatar: String = "",
    val senderCoinType: CoinType = CoinType.DEFAULT,
    val senderCoinEdition: Int? = null,
    val receiverId: String,
    val receiverName: String,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis()
)

data class CoinAccount(
    val uid: String,
    val balance: Long = 0L,
    val totalEarned: Long = 0L,
    val totalSpent: Long = 0L,
    val lastTransactionAt: Long = System.currentTimeMillis()
)

data class CineCoinTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val amount: Long,
    val reason: String,
    val type: String, // "EARN", "SPEND", "PURCHASE", "REWARD", "REFUND", "ADMIN_GRANT", "ADMIN_ADJUSTMENT", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis(),
    val balanceAfter: Long = 0L,
    val idempotencyKey: String = UUID.randomUUID().toString()
)

enum class NotificationCategory {
    FRIEND,
    PROJECT,
    COLLABORATION,
    COIN,
    COMMENT,
    SYSTEM,
    SUPPORT
}

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val category: NotificationCategory = NotificationCategory.SYSTEM,
    val title: String,
    val message: String,
    val senderId: String? = null,
    val senderName: String? = null,
    val relatedEntityId: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val actionable: Boolean = false,
    val actionType: String? = null // "FRIEND_REQUEST", "PROJECT_INVITE"
)

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorTier: MembershipTier,
    val authorCoinType: CoinType = CoinType.DEFAULT,
    val authorCoinEdition: Int? = null,
    val content: String,
    val mediaUri: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = listOf("Cinematic", "ColorGrading", "4K"),
    val moderationStatus: String = "ACTIVE" // "ACTIVE", "RESTRICTED", "REMOVED", "FLAGGED"
)

data class CineRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val requiredTier: MembershipTier = MembershipTier.FREE,
    val requiredCoinType: CoinType? = null,
    val requiredCoinLevel: Int = 0,
    val memberCount: Int = 240,
    val isLive: Boolean = false,
    val currentTopic: String = "Color Grading Masterclass",
    val iconUrl: String = ""
)

data class RoomMessage(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val senderTier: MembershipTier,
    val senderCoinType: CoinType = CoinType.DEFAULT,
    val senderCoinEdition: Int? = null,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveStream(
    val id: String = UUID.randomUUID().toString(),
    val creatorId: String,
    val creatorName: String,
    val title: String,
    val hlsStreamUrl: String,
    val viewerCount: Int = 3450,
    val isLive: Boolean = true,
    val category: String = "Editing Workflow",
    val thumbnailUri: String = ""
)

// ================= ADMIN M3U8 CONTENT MANAGEMENT ================= //

enum class VideoPublishStatus {
    PUBLISHED, DRAFT, DELETED
}

enum class LiveStreamStatus {
    SCHEDULED, LIVE, ENDED
}

data class M3u8Video(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val m3u8Url: String,
    val thumbnailUrl: String = "",
    val category: String = "Cinematography",
    val tags: List<String> = listOf("4K", "HLS", "Cinematic"),
    val visibility: String = "PUBLIC", // "PUBLIC", "MEMBERS_ONLY", "UNLISTED"
    val status: VideoPublishStatus = VideoPublishStatus.PUBLISHED,
    val isFeatured: Boolean = false,
    val isRecommended: Boolean = true,
    val durationSeconds: Long = 734L,
    val resolutionLabel: String = "1080p60 HLS",
    val publishedByAdminUid: String = "usr_creator_8921",
    val publishedByAdminName: String = "CineCut Editorial",
    val viewsCount: Long = 18450L,
    val likesCount: Long = 1290L,
    val createdAt: Long = System.currentTimeMillis() - 86400000L * 4,
    val updatedAt: Long = System.currentTimeMillis()
)

data class M3u8LiveStream(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val m3u8Url: String,
    val thumbnailUrl: String = "",
    val category: String = "Masterclass",
    val tags: List<String> = listOf("Live", "Masterclass", "HLS"),
    val status: LiveStreamStatus = LiveStreamStatus.LIVE,
    val scheduledStartTime: Long = System.currentTimeMillis() - 3600000L,
    val isPublished: Boolean = true,
    val isFeatured: Boolean = true,
    val viewerCount: Int = 3450,
    val publishedByAdminUid: String = "usr_creator_8921",
    val publishedByAdminName: String = "CineCut Live Ops",
    val createdAt: Long = System.currentTimeMillis() - 86400000L * 2,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ContentCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
    val description: String,
    val itemCount: Int = 12,
    val isSystemDefault: Boolean = true
)

data class LiveChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val senderTier: MembershipTier,
    val senderCoinType: CoinType = CoinType.DEFAULT,
    val senderCoinEdition: Int? = null,
    val message: String,
    val coinTip: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class DirectMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val senderCoinType: CoinType = CoinType.DEFAULT,
    val senderCoinEdition: Int? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean = false
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val adminUid: String,
    val action: String,
    val target: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ModerationReport(
    val id: String = UUID.randomUUID().toString(),
    val reporterId: String,
    val reporterName: String = "Creator Member",
    val targetType: String, // "POST", "USER", "ROOM", "STREAM", "COMMENT"
    val targetId: String,
    val targetSnippet: String = "",
    val reason: String,
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val assignedModerator: String? = null,
    val resolution: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

data class AdminDashboardMetrics(
    val totalUsers: Long = 142850L,
    val newUsersToday: Long = 1240L,
    val activeUsers7d: Long = 68400L,
    val suspendedUsers: Long = 18L,
    val bannedUsers: Long = 7L,
    val totalProjects: Long = 425100L,
    val totalPosts: Long = 89400L,
    val totalComments: Long = 210900L,
    val activeMemberships: Long = 34200L,
    val cineCoinCirculation: Long = 18450000L,
    val activeLiveStreams: Int = 14,
    val openReports: Int = 8,
    val cineRoomMembers: Long = 42100L
)

data class AnalyticsPoint(
    val label: String,
    val value: Float,
    val secondaryValue: Float = 0f
)

enum class AdminNavDestination(
    val title: String,
    val requiredPermission: AdminPermission,
    val description: String
) {
    OVERVIEW("Overview", AdminPermission.VIEW_ANALYTICS, "Platform health, KPIs & real-time telemetry"),
    USERS("Users", AdminPermission.MANAGE_USERS, "Search, inspect, suspend, ban & manage roles"),
    CONTENT("Content", AdminPermission.MODERATE_CONTENT, "Posts, clips, comments & media moderation"),
    REPORTS("Reports", AdminPermission.MODERATE_CONTENT, "Flagged content, complaints & dispute resolution"),
    MESSAGES("Messages", AdminPermission.MODERATE_CONTENT, "Chat oversight, abuse monitoring & system broadcast"),
    PROJECTS("Projects", AdminPermission.VIEW_ANALYTICS, "Cloud projects, render pipelines & asset usage"),
    CINEROOMS("CineRooms", AdminPermission.MANAGE_ROOMS, "Rooms administration, tier-gates & announcements"),
    CINELIVE("CineLive", AdminPermission.MANAGE_LIVE, "Live broadcast monitor, viewer counts & termination"),
    MEMBERSHIPS("Memberships", AdminPermission.MANAGE_MEMBERSHIPS, "Subscribers, tier upgrades, Founder & VIP grants"),
    CINECOINS("CineCoins", AdminPermission.ADJUST_COINS, "Circulation ledger, transaction velocity & coin grants"),
    NOTIFICATIONS("Notifications", AdminPermission.SEND_NOTIFICATIONS, "Push broadcasts, targeted alerts & templates"),
    AI_MANAGEMENT("AI Management", AdminPermission.MANAGE_AI, "Autonomous presets, AI models & prompt engine"),
    FEATURE_FLAGS("Feature Flags", AdminPermission.MANAGE_FEATURE_FLAGS, "Remote switches & maintenance mode toggle"),
    ANALYTICS("Analytics", AdminPermission.VIEW_ANALYTICS, "Deep-dive growth curves, metrics & performance"),
    AUDIT_LOGS("Audit Logs", AdminPermission.VIEW_AUDIT_LOGS, "Immutable security trails & administrator actions"),
    SYSTEM_SETTINGS("System Settings", AdminPermission.SECURITY_CENTER, "App Check, Firebase health & security posture"),
    PAYMENT_GATEWAY("Payment Gateway", AdminPermission.SECURITY_CENTER, "ZapUPI API configuration, orders ledger & verification"),
    SUPPORT_TICKETS("Support Tickets", AdminPermission.MANAGE_SUPPORT_TICKETS, "Customer support center, ticket chat & resolutions"),
    FESTIVALS("Festival & Events", AdminPermission.MANAGE_FESTIVALS, "Remote festival themes, animations, bonuses & promotional offers")
}

// ================= ZAPUPI PAYMENT GATEWAY & ORDERS ================= //

data class ZapUpiGatewayConfig(
    val isEnabled: Boolean = true,
    val maskedApiKey: String = "••••••••••••••••••••153d54",
    val timeoutSeconds: Int = 480, // 8-minute countdown
    val currency: String = "INR",
    val isTestMode: Boolean = false,
    val status: String = "Connected ✓",
    val lastVerifiedAt: Long = System.currentTimeMillis()
)

enum class PaymentOrderStatus {
    PENDING, VERIFYING, PAID, FAILED, EXPIRED, REFUNDED
}

data class ZapUpiOrder(
    val orderId: String = "ZAP_CC_" + System.currentTimeMillis().toString().takeLast(8),
    val userId: String,
    val userDisplayName: String = "Creator Member",
    val amountInr: Double,
    val itemType: String, // "CINECOINS", "MEMBERSHIP"
    val itemId: String, // "coins_500", "coins_1500", "coins_5000", "tier_gold"
    val title: String,
    val status: PaymentOrderStatus = PaymentOrderStatus.PENDING,
    val qrPayload: String = "",
    val upiIntentUrl: String = "",
    val paytmIntentUrl: String = "",
    val utrNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 480000L, // 8 minutes
    val paidAt: Long? = null,
    val refundReason: String? = null
)

data class FeatureFlags(
    val aiDirectorEnabled: Boolean = true,
    val autonomousAiEnabled: Boolean = true,
    val cineLiveEnabled: Boolean = true,
    val cineRoomsEnabled: Boolean = true,
    val messagingEnabled: Boolean = true,
    val newEditorEnabled: Boolean = true,
    val maintenanceMode: Boolean = false
)

// ================= SUPPORT TICKET SYSTEM ================= //

enum class SupportTicketCategory(val title: String, val description: String) {
    ACCOUNT("Account", "Login, security, profile & verification"),
    PAYMENT("Payment", "UPI, card transactions, invoices & billing"),
    CINECOINS("CineCoins", "Wallet balance, coin purchases & coin transfers"),
    MEMBERSHIP("Membership", "Tiers, benefits, upgrades & renewals"),
    VIDEO_EDITOR("Video Editor", "Timeline, export, 4K rendering & codec issues"),
    AI_FEATURES("AI Features", "AI Director, auto-grading, presets & prompts"),
    CINELIVE("CineLive", "Broadcasting, live chat, stream quality & viewers"),
    CINEROOMS("CineRooms", "Virtual rooms, access permissions & invites"),
    TECHNICAL_ISSUE("Technical Issue", "Crashes, performance lags & device compatibility"),
    BUG_REPORT("Bug Report", "Unexpected behavior, glitch or visual error"),
    OTHER("Other", "General inquiries & feedback")
}

enum class SupportTicketStatus(val title: String) {
    OPEN("OPEN"),
    IN_PROGRESS("IN_PROGRESS"),
    WAITING_FOR_USER("WAITING_FOR_USER"),
    RESOLVED("RESOLVED"),
    CLOSED("CLOSED")
}

enum class SupportTicketPriority(val title: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    URGENT("URGENT")
}

data class SupportAttachment(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileUrl: String,
    val fileType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L
)

data class SupportTicketMessage(
    val id: String = UUID.randomUUID().toString(),
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String, // "USER", "SUPPORT", "ADMIN"
    val message: String,
    val isInternalNote: Boolean = false, // Strictly forbidden from user view
    val attachments: List<SupportAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class SupportTicket(
    val ticketId: String, // e.g. #CC-100001
    val userId: String,
    val userDisplayName: String,
    val userEmail: String = "",
    val subject: String,
    val category: SupportTicketCategory,
    val description: String,
    val priority: SupportTicketPriority = SupportTicketPriority.MEDIUM,
    val status: SupportTicketStatus = SupportTicketStatus.OPEN,
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val orderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val unreadUserCount: Int = 0,
    val unreadAdminCount: Int = 0,
    val attachments: List<SupportAttachment> = emptyList()
)

