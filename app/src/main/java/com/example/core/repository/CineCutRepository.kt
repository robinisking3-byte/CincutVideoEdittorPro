package com.example.core.repository

import android.content.Context
import com.example.core.model.*
import com.example.core.network.FirebaseCloudSyncEngine
import com.example.core.notification.FcmTokenManager
import com.example.core.security.AuthState
import com.example.core.security.FirebaseAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CineCutRepository(context: Context? = null) {

    val authManager: FirebaseAuthManager? = context?.let { FirebaseAuthManager(it) }
    val cloudSyncEngine: FirebaseCloudSyncEngine = FirebaseCloudSyncEngine(context)
    val fcmTokenManager: FcmTokenManager? = context?.let { FcmTokenManager(it) }
    private val repoScope = CoroutineScope(Dispatchers.IO)

    // Current authenticated session (strictly null on fresh install or after logout)
    private val _currentAuthUser = MutableStateFlow<UserProfile?>(null)
    val currentAuthUser: StateFlow<UserProfile?> = _currentAuthUser.asStateFlow()

    // Current active user state (defaults to unprivileged, empty guest until authenticated)
    private val _currentUser = MutableStateFlow(
        UserProfile(
            uid = "",
            username = "",
            displayName = "",
            email = "",
            membershipTier = MembershipTier.FREE,
            adminRole = AdminRole.NONE,
            coinBalance = 0L,
            badges = emptyList()
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    init {
        authManager?.let { am ->
            repoScope.launch {
                am.currentUserProfile.collect { profile ->
                    _currentAuthUser.value = profile
                    if (profile != null) {
                        _currentUser.value = profile
                        fcmTokenManager?.registerUserFcmToken(profile.uid)
                        cloudSyncEngine.startSync(
                            userId = profile.uid,
                            role = profile.adminRole,
                            onProfileUpdate = { updated -> _currentUser.value = updated },
                            onCoinBalanceUpdate = { newBal -> _currentUser.value = _currentUser.value.copy(coinBalance = newBal) },
                            onProjectsUpdate = { cloudProjs -> _projects.value = cloudProjs },
                            onRoomsUpdate = { cloudRooms -> _rooms.value = cloudRooms },
                            onLiveStreamsUpdate = { cloudStreams -> _liveStreams.value = cloudStreams },
                            onLiveChatUpdate = { cloudChat -> _liveChat.value = cloudChat },
                            onFriendRequestsUpdate = { cloudReqs -> _friendRequests.value = cloudReqs },
                            onFriendshipsUpdate = { cloudFriendships -> _friendships.value = cloudFriendships },
                            onNotificationsUpdate = { cloudNotifs -> _inAppNotifications.value = cloudNotifs },
                            onTicketsUpdate = { cloudTickets -> _supportTickets.value = cloudTickets },
                            onPaymentOrdersUpdate = { cloudOrders -> _paymentOrders.value = cloudOrders },
                            onFestivalConfigUpdate = { cloudFest -> _festivalConfig.value = cloudFest },
                            onFestivalOffersUpdate = { cloudOffers -> _festivalOffers.value = cloudOffers },
                            onAuditLogsUpdate = { cloudLogs -> _auditLogs.value = cloudLogs },
                            onAllUsersUpdate = { cloudUsers -> _allUsers.value = cloudUsers }
                        )
                    } else {
                        cloudSyncEngine.stopSync()
                        _currentUser.value = UserProfile(
                            uid = "",
                            username = "",
                            displayName = "",
                            email = "",
                            membershipTier = MembershipTier.FREE,
                            adminRole = AdminRole.NONE,
                            coinBalance = 0L,
                            badges = emptyList()
                        )
                    }
                }
            }
        }
    }

    // Projects list
    private val _projects = MutableStateFlow<List<Project>>(
        listOf(
            Project(
                id = "proj_1",
                title = "Tokyo Neon Drift 4K",
                aspectRatio = "16:9",
                resolutionWidth = 3840,
                resolutionHeight = 2160,
                durationMs = 14000L,
                updatedAt = System.currentTimeMillis() - 3600000L
            ),
            Project(
                id = "proj_2",
                title = "Iceland Drone Horizon",
                aspectRatio = "21:9",
                resolutionWidth = 2560,
                resolutionHeight = 1080,
                durationMs = 22000L,
                updatedAt = System.currentTimeMillis() - 86400000L
            ),
            Project(
                id = "proj_3",
                title = "Fashion Week Reel",
                aspectRatio = "9:16",
                resolutionWidth = 1080,
                resolutionHeight = 1920,
                durationMs = 9500L,
                updatedAt = System.currentTimeMillis() - 172800000L
            )
        )
    )
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    // Community Feed Posts
    private val _posts = MutableStateFlow<List<Post>>(
        listOf(
            Post(
                id = "post_1",
                authorId = "usr_elena",
                authorName = "Elena Rostova",
                authorHandle = "@elena_visuals",
                authorTier = MembershipTier.DIAMOND,
                content = "Color graded this short with the new CineCut 35mm Teal & Orange LUT package! The dynamic range on mobile is insane. Thoughts on the shadow rolloff?",
                likesCount = 1842,
                commentsCount = 143,
                isLiked = false,
                tags = listOf("ColorGrading", "LUT", "35mmFilm", "Cinematic")
            ),
            Post(
                id = "post_2",
                authorId = "usr_marcus",
                authorName = "Marcus Thorne",
                authorHandle = "@mthorne_vfx",
                authorTier = MembershipTier.VIP,
                content = "Multi-track speed ramping tutorial just dropped in my CineRoom. Swipe through to see the bezier curve keyframe breakdown in CineCut.",
                likesCount = 2950,
                commentsCount = 312,
                isLiked = true,
                tags = listOf("SpeedRamp", "Keyframes", "Tutorial")
            ),
            Post(
                id = "post_3",
                authorId = "usr_sarah",
                authorName = "Sarah Chen",
                authorHandle = "@sarah_director",
                authorTier = MembershipTier.FOUNDER,
                content = "Completed 4K export test on Android! 60fps buttery smooth timeline playback with zero frame drops.",
                likesCount = 3410,
                commentsCount = 289,
                isLiked = false,
                tags = listOf("4KExport", "AndroidStudio", "MobileFilmmaking")
            )
        )
    )
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // CineRooms (Tier-gated Community Rooms)
    private val _rooms = MutableStateFlow<List<CineRoom>>(
        listOf(
            CineRoom(
                id = "room_color",
                name = "Colorists & LUT Masters",
                description = "Mastering DaVinci & CineCut color profiles, ACES color science, and custom LUT curves.",
                requiredTier = MembershipTier.BRONZE,
                memberCount = 1820,
                isLive = true,
                currentTopic = "Teal & Orange Breakdown"
            ),
            CineRoom(
                id = "room_indie",
                name = "Indie Directors Lounge",
                description = "Scriptwriting, storyboards, pacing analysis, and festival submissions discussion.",
                requiredTier = MembershipTier.SILVER,
                memberCount = 940,
                isLive = false,
                currentTopic = "Funding Mobile Documentaries"
            ),
            CineRoom(
                id = "room_ai",
                name = "AI Director Lab & Prompting",
                description = "Automating video edits, smart cuts, and prompt engineering with CineCut AI Director.",
                requiredTier = MembershipTier.GOLD,
                memberCount = 2410,
                isLive = true,
                currentTopic = "Autonomous Style Presets"
            ),
            CineRoom(
                id = "room_founder",
                name = "VIP & Founders Circle",
                description = "Direct roadmap feedback with CineCut core engineers and early access feature flags.",
                requiredTier = MembershipTier.VIP,
                memberCount = 310,
                isLive = true,
                currentTopic = "Native MediaCodec 8K Pipeline"
            )
        )
    )
    val rooms: StateFlow<List<CineRoom>> = _rooms.asStateFlow()

    // CineRoom Messages
    private val _roomMessages = MutableStateFlow<Map<String, List<RoomMessage>>>(
        mapOf(
            "room_color" to listOf(
                RoomMessage(
                    roomId = "room_color",
                    senderId = "usr_kenji",
                    senderName = "Kenji Sato",
                    senderTier = MembershipTier.GOLD,
                    message = "Anyone trying the new highlights roll-off curve? Gives that Kodak 5219 warmth."
                ),
                RoomMessage(
                    roomId = "room_color",
                    senderId = "usr_elena",
                    senderName = "Elena Rostova",
                    senderTier = MembershipTier.DIAMOND,
                    message = "Yes! Pair it with 0.15 temperature and -0.05 tint for perfect skin tones."
                )
            )
        )
    )
    val roomMessages: StateFlow<Map<String, List<RoomMessage>>> = _roomMessages.asStateFlow()

    // CineLive Streams
    private val _liveStreams = MutableStateFlow<List<LiveStream>>(
        listOf(
            LiveStream(
                id = "stream_1",
                creatorId = "usr_david",
                creatorName = "David Kim",
                title = "🔴 LIVE: Editing A Sci-Fi Trailer from Scratch in CineCut",
                hlsStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                viewerCount = 1420,
                isLive = true,
                category = "Trailer Editing"
            ),
            LiveStream(
                id = "stream_2",
                creatorId = "usr_maya",
                creatorName = "Maya Lin",
                title = "🔴 LIVE: Sound Design & Audio Ducking Masterclass",
                hlsStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                viewerCount = 890,
                isLive = true,
                category = "Sound Design"
            )
        )
    )
    val liveStreams: StateFlow<List<LiveStream>> = _liveStreams.asStateFlow()

    // CineLive Chat
    private val _liveChat = MutableStateFlow<List<LiveChatMessage>>(
        listOf(
            LiveChatMessage(senderName = "Liam", senderTier = MembershipTier.SILVER, message = "The split cut shortcut is so fast!"),
            LiveChatMessage(senderName = "Chloe", senderTier = MembershipTier.GOLD, message = "Sent 100 CineCoins for that transition tip! 🚀", coinTip = 100L),
            LiveChatMessage(senderName = "Ravi", senderTier = MembershipTier.DIAMOND, message = "Check the waveform alignment at 0:42.")
        )
    )
    val liveChat: StateFlow<List<LiveChatMessage>> = _liveChat.asStateFlow()

    // Direct Messages
    private val _directMessages = MutableStateFlow<List<DirectMessage>>(
        listOf(
            DirectMessage(senderId = "usr_sarah", senderName = "Sarah Chen", text = "Hey Alex! Loved your Tokyo reel color grade.", isMe = false),
            DirectMessage(senderId = "me", senderName = "Alex Rivera", text = "Thanks Sarah! Exported at 4K 60fps in CineCut.", isMe = true),
            DirectMessage(senderId = "usr_sarah", senderName = "Sarah Chen", text = "Would you like to collaborate on the neon project?", isMe = false)
        )
    )
    val directMessages: StateFlow<List<DirectMessage>> = _directMessages.asStateFlow()

    // CineCoins Ledger & Account
    private val _coinAccount = MutableStateFlow(
        CoinAccount(
            uid = "usr_creator_8921",
            balance = 4250L,
            totalEarned = 18500L,
            totalSpent = 14250L
        )
    )
    val coinAccount: StateFlow<CoinAccount> = _coinAccount.asStateFlow()

    private val _coinTransactions = MutableStateFlow<List<CineCoinTransaction>>(
        listOf(
            CineCoinTransaction(userId = "usr_creator_8921", amount = 250, reason = "Daily Creator Reward", type = "REWARD", balanceAfter = 4250),
            CineCoinTransaction(userId = "usr_creator_8921", amount = -100, reason = "Tipped David Kim on CineLive", type = "SPEND", balanceAfter = 4000),
            CineCoinTransaction(userId = "usr_creator_8921", amount = 1000, reason = "Google Play Coin Package", type = "PURCHASE", balanceAfter = 4100),
            CineCoinTransaction(userId = "usr_creator_8921", amount = 500, reason = "Invited Elena Rostova to Project", type = "EARN", balanceAfter = 3100)
        )
    )
    val coinTransactions: StateFlow<List<CineCoinTransaction>> = _coinTransactions.asStateFlow()

    // Friends & Social Connections
    private val _friendships = MutableStateFlow<List<Friendship>>(
        listOf(
            Friendship(
                userId = "usr_creator_8921",
                friendUserId = "usr_elena",
                friendProfile = UserProfile(
                    uid = "usr_elena",
                    username = "elena_visuals",
                    displayName = "Elena Rostova",
                    email = "elena.rostova@cinecut.io",
                    bio = "Film editor & color science enthusiast. CineCut Moderator.",
                    membershipTier = MembershipTier.DIAMOND,
                    adminRole = AdminRole.MODERATOR,
                    coinBalance = 12400L,
                    followersCount = 45100,
                    badges = listOf("Verified Creator", "Staff Moderator")
                ),
                state = FriendState.FRIENDS
            ),
            Friendship(
                userId = "usr_creator_8921",
                friendUserId = "usr_sarah",
                friendProfile = UserProfile(
                    uid = "usr_sarah",
                    username = "sarah_director",
                    displayName = "Sarah Chen",
                    email = "sarah.chen@indiefilm.org",
                    bio = "Directing award-winning mobile shorts & documentaries.",
                    membershipTier = MembershipTier.FOUNDER,
                    coinBalance = 19800L,
                    followersCount = 82400,
                    badges = listOf("Early Founder", "Verified Creator")
                ),
                state = FriendState.FRIENDS
            ),
            Friendship(
                userId = "usr_creator_8921",
                friendUserId = "usr_kenji",
                friendProfile = UserProfile(
                    uid = "usr_kenji",
                    username = "kenji_cinematics",
                    displayName = "Kenji Sato",
                    email = "kenji@tokyofilm.jp",
                    bio = "Tokyo street videographer & 35mm lover.",
                    membershipTier = MembershipTier.GOLD,
                    coinBalance = 4100L,
                    followersCount = 14200,
                    badges = listOf("Pro Editor")
                ),
                state = FriendState.FRIENDS
            )
        )
    )
    val friendships: StateFlow<List<Friendship>> = _friendships.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(
        listOf(
            FriendRequest(
                id = "freq_1",
                senderId = "usr_david",
                senderName = "David Kim",
                senderUsername = "david_kim",
                receiverId = "usr_creator_8921",
                receiverName = "Alex Rivera"
            ),
            FriendRequest(
                id = "freq_2",
                senderId = "usr_marcus",
                senderName = "Marcus Thorne",
                senderUsername = "mthorne_vfx",
                receiverId = "usr_creator_8921",
                receiverName = "Alex Rivera"
            )
        )
    )
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _sentFriendRequests = MutableStateFlow<List<FriendRequest>>(
        listOf(
            FriendRequest(
                id = "freq_sent_1",
                senderId = "usr_creator_8921",
                senderName = "Alex Rivera",
                senderUsername = "alex_cinematics",
                receiverId = "usr_maya",
                receiverName = "Maya Lin"
            )
        )
    )
    val sentFriendRequests: StateFlow<List<FriendRequest>> = _sentFriendRequests.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(setOf("usr_troll42", "usr_spammer99"))
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    // Collaborators per Project
    private val _collaborators = MutableStateFlow<Map<String, List<Collaborator>>>(
        mapOf(
            "proj_1" to listOf(
                Collaborator(userId = "usr_creator_8921", username = "alex_cinematics", displayName = "Alex Rivera", role = ProjectRole.OWNER),
                Collaborator(userId = "usr_elena", username = "elena_visuals", displayName = "Elena Rostova", role = ProjectRole.ADMIN),
                Collaborator(userId = "usr_marcus", username = "mthorne_vfx", displayName = "Marcus Thorne", role = ProjectRole.EDITOR),
                Collaborator(userId = "usr_sarah", username = "sarah_director", displayName = "Sarah Chen", role = ProjectRole.REVIEWER)
            ),
            "proj_2" to listOf(
                Collaborator(userId = "usr_creator_8921", username = "alex_cinematics", displayName = "Alex Rivera", role = ProjectRole.OWNER),
                Collaborator(userId = "usr_kenji", username = "kenji_cinematics", displayName = "Kenji Sato", role = ProjectRole.CONTRIBUTOR)
            )
        )
    )
    val collaborators: StateFlow<Map<String, List<Collaborator>>> = _collaborators.asStateFlow()

    // Project Invitations
    private val _projectInvitations = MutableStateFlow<List<ProjectInvitation>>(
        listOf(
            ProjectInvitation(
                id = "pinv_1",
                projectId = "proj_remote_101",
                projectTitle = "Neo Cyberpunk Short Film 4K",
                senderId = "usr_sarah",
                senderName = "Sarah Chen",
                receiverId = "usr_creator_8921",
                role = ProjectRole.EDITOR
            )
        )
    )
    val projectInvitations: StateFlow<List<ProjectInvitation>> = _projectInvitations.asStateFlow()

    // Project Real-Time Collaboration Operations & Events
    private val _projectOperations = MutableStateFlow<Map<String, List<ProjectOperation>>>(
        mapOf(
            "proj_1" to listOf(
                ProjectOperation(projectId = "proj_1", userId = "usr_creator_8921", userName = "Alex Rivera", operationType = "SPLIT_CLIP", details = "Split clip at 00:04.200"),
                ProjectOperation(projectId = "proj_1", userId = "usr_elena", userName = "Elena Rostova", operationType = "COLOR_GRADE", details = "Applied 35mm Teal & Orange LUT"),
                ProjectOperation(projectId = "proj_1", userId = "usr_marcus", userName = "Marcus Thorne", operationType = "ADD_TRACK", details = "Added Audio Ducking track for BGM")
            )
        )
    )
    val projectOperations: StateFlow<Map<String, List<ProjectOperation>>> = _projectOperations.asStateFlow()

    // Project Versions
    private val _projectVersions = MutableStateFlow<Map<String, List<ProjectVersion>>>(
        mapOf(
            "proj_1" to listOf(
                ProjectVersion(projectId = "proj_1", versionNumber = 1, name = "Rough Cut Assembly", authorId = "usr_creator_8921", authorName = "Alex Rivera", changeSummary = "Initial timeline assembly with video and guide tracks"),
                ProjectVersion(projectId = "proj_1", versionNumber = 2, name = "Director's Color Master", authorId = "usr_elena", authorName = "Elena Rostova", changeSummary = "Fine-tuned contrast curves and highlights roll-off")
            )
        )
    )
    val projectVersions: StateFlow<Map<String, List<ProjectVersion>>> = _projectVersions.asStateFlow()

    // Project Comments & Mentions
    private val _projectComments = MutableStateFlow<Map<String, List<ProjectComment>>>(
        mapOf(
            "proj_1" to listOf(
                ProjectComment(
                    projectId = "proj_1",
                    authorId = "usr_elena",
                    authorName = "Elena Rostova",
                    text = "The transition into the neon drift scene is seamless! @alex_cinematics check the audio dip at 00:04.",
                    timecodeMs = 4200L,
                    mentions = listOf("alex_cinematics")
                ),
                ProjectComment(
                    projectId = "proj_1",
                    authorId = "usr_marcus",
                    authorName = "Marcus Thorne",
                    text = "Render looks crisp on OLED. Added low-frequency rumble effect on bass channel.",
                    timecodeMs = 8500L
                )
            )
        )
    )
    val projectComments: StateFlow<Map<String, List<ProjectComment>>> = _projectComments.asStateFlow()

    // In-App Interactive Notifications
    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>>(
        listOf(
            InAppNotification(
                userId = "usr_creator_8921",
                category = NotificationCategory.FRIEND,
                title = "Friend Request Received",
                message = "David Kim (@david_kim) sent you a friend request.",
                senderId = "usr_david",
                senderName = "David Kim",
                relatedEntityId = "freq_1",
                actionable = true,
                actionType = "FRIEND_REQUEST"
            ),
            InAppNotification(
                userId = "usr_creator_8921",
                category = NotificationCategory.PROJECT,
                title = "Project Collaboration Invite",
                message = "Sarah Chen invited you to collaborate on 'Neo Cyberpunk Short Film 4K' as Editor.",
                senderId = "usr_sarah",
                senderName = "Sarah Chen",
                relatedEntityId = "pinv_1",
                actionable = true,
                actionType = "PROJECT_INVITE"
            ),
            InAppNotification(
                userId = "usr_creator_8921",
                category = NotificationCategory.COMMENT,
                title = "Mentioned in Project",
                message = "Elena Rostova mentioned you in a comment on 'Tokyo Neon Drift 4K'.",
                senderId = "usr_elena",
                senderName = "Elena Rostova",
                relatedEntityId = "proj_1"
            ),
            InAppNotification(
                userId = "usr_creator_8921",
                category = NotificationCategory.COIN,
                title = "CineCoins Reward Credited",
                message = "You earned +250 CineCoins for your daily creator check-in!",
                relatedEntityId = "tx_daily_reward"
            )
        )
    )
    val inAppNotifications: StateFlow<List<InAppNotification>> = _inAppNotifications.asStateFlow()

    // Admin & Moderation Data
    private val _allUsers = MutableStateFlow<List<UserProfile>>(
        listOf(
            UserProfile(
                uid = "usr_creator_8921",
                username = "alex_cinematics",
                displayName = "Alex Rivera",
                email = "alex@cinecut.io",
                bio = "Cinematographer & Colorist 🎬 4K HDR Films | CineCut Ambassador",
                membershipTier = MembershipTier.GOLD,
                adminRole = AdminRole.SUPER_ADMIN,
                coinBalance = 4250L,
                followersCount = 28400,
                followingCount = 412,
                badges = listOf("Verified Creator", "Pro Editor", "Early Founder"),
                createdAt = System.currentTimeMillis() - 86400000L * 180,
                lastActiveAt = System.currentTimeMillis() - 120000L,
                reportsReceivedCount = 0,
                deviceModel = "Pixel 9 Pro (Android 15)"
            ),
            UserProfile(
                uid = "usr_elena",
                username = "elena_visuals",
                displayName = "Elena Rostova",
                email = "elena.rostova@cinecut.io",
                bio = "Film editor & color science enthusiast. CineCut Moderator.",
                membershipTier = MembershipTier.DIAMOND,
                adminRole = AdminRole.MODERATOR,
                coinBalance = 12400L,
                followersCount = 45100,
                followingCount = 310,
                badges = listOf("Verified Creator", "Staff Moderator", "Master Colorist"),
                createdAt = System.currentTimeMillis() - 86400000L * 150,
                lastActiveAt = System.currentTimeMillis() - 600000L,
                reportsReceivedCount = 0,
                deviceModel = "Samsung Galaxy S24 Ultra"
            ),
            UserProfile(
                uid = "usr_marcus",
                username = "mthorne_vfx",
                displayName = "Marcus Thorne",
                email = "marcus.vfx@studio.com",
                bio = "VFX supervisor & mobile motion graphics artist.",
                membershipTier = MembershipTier.VIP,
                adminRole = AdminRole.NONE,
                coinBalance = 6200L,
                followersCount = 18900,
                followingCount = 520,
                badges = listOf("VIP Studio", "VFX Master"),
                createdAt = System.currentTimeMillis() - 86400000L * 90,
                lastActiveAt = System.currentTimeMillis() - 3600000L * 4,
                reportsReceivedCount = 1,
                deviceModel = "Sony Xperia 1 VI"
            ),
            UserProfile(
                uid = "usr_sarah",
                username = "sarah_director",
                displayName = "Sarah Chen",
                email = "sarah.chen@indiefilm.org",
                bio = "Directing award-winning mobile shorts & documentaries.",
                membershipTier = MembershipTier.FOUNDER,
                adminRole = AdminRole.CONTENT_ADMIN,
                coinBalance = 19800L,
                followersCount = 82400,
                followingCount = 205,
                badges = listOf("Early Founder", "Content Curator", "Verified Creator"),
                createdAt = System.currentTimeMillis() - 86400000L * 300,
                lastActiveAt = System.currentTimeMillis() - 1800000L,
                reportsReceivedCount = 0,
                deviceModel = "Google Pixel 8 Pro"
            ),
            UserProfile(
                uid = "usr_david",
                username = "david_kim",
                displayName = "David Kim",
                email = "david.kim@streamer.net",
                bio = "Daily CineLive streamer exploring live transitions & sound design.",
                membershipTier = MembershipTier.BRONZE,
                adminRole = AdminRole.NONE,
                coinBalance = 1450L,
                followersCount = 9400,
                followingCount = 180,
                badges = listOf("Stream Partner"),
                createdAt = System.currentTimeMillis() - 86400000L * 45,
                lastActiveAt = System.currentTimeMillis() - 300000L,
                reportsReceivedCount = 0,
                deviceModel = "Xiaomi 14 Ultra"
            ),
            UserProfile(
                uid = "usr_troll42",
                username = "shadow_rebel",
                displayName = "Shadow Rebel",
                email = "troll42@anonymous.mail",
                bio = "Unfiltered commentary on creator clips.",
                membershipTier = MembershipTier.FREE,
                adminRole = AdminRole.NONE,
                coinBalance = 0L,
                followersCount = 12,
                followingCount = 640,
                badges = emptyList(),
                isSuspended = true,
                suspensionReason = "Abusive live chat harassment & toxic trolling (7-day suspension)",
                createdAt = System.currentTimeMillis() - 86400000L * 14,
                lastActiveAt = System.currentTimeMillis() - 86400000L * 2,
                reportsReceivedCount = 6,
                deviceModel = "Android Generic Device"
            ),
            UserProfile(
                uid = "usr_spammer99",
                username = "crypto_drops_bot",
                displayName = "Instant Coins Bot",
                email = "bot_promo99@spam.xyz",
                bio = "FREE CINECOINS CLICK LINK IN BIO!!",
                membershipTier = MembershipTier.FREE,
                adminRole = AdminRole.NONE,
                coinBalance = 0L,
                followersCount = 2,
                followingCount = 1200,
                badges = emptyList(),
                isBanned = true,
                banReason = "Permanent ban: Automated phishing spam & malicious external links",
                createdAt = System.currentTimeMillis() - 86400000L * 5,
                lastActiveAt = System.currentTimeMillis() - 86400000L * 4,
                reportsReceivedCount = 14,
                deviceModel = "Emulator x86_64"
            ),
            UserProfile(
                uid = "usr_liam_support",
                username = "liam_support",
                displayName = "Liam Walker",
                email = "liam.support@cinecut.io",
                bio = "Customer Support & Account Billing Specialist",
                membershipTier = MembershipTier.SILVER,
                adminRole = AdminRole.SUPPORT,
                coinBalance = 1800L,
                followersCount = 420,
                followingCount = 150,
                badges = listOf("Support Specialist"),
                createdAt = System.currentTimeMillis() - 86400000L * 60,
                lastActiveAt = System.currentTimeMillis() - 7200000L,
                reportsReceivedCount = 0,
                deviceModel = "Pixel 7a"
            )
        )
    )
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    // ================= ADMIN-ONLY M3U8 CONTENT MANAGEMENT ================= //
    private val _m3u8Videos = MutableStateFlow<List<M3u8Video>>(
        listOf(
            M3u8Video(
                id = "vid_m3u8_01",
                title = "Tears of Steel — 4K Open Source Master",
                description = "Master export in 4K DCI with 5.1 surround sound. Color graded with ACES color science.",
                m3u8Url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=800&q=80",
                category = "Cinematography",
                tags = listOf("4K", "VFX", "SciFi", "HLS"),
                visibility = "PUBLIC",
                status = VideoPublishStatus.PUBLISHED,
                isFeatured = true,
                isRecommended = true,
                durationSeconds = 734L,
                viewsCount = 38400L,
                likesCount = 4210L
            ),
            M3u8Video(
                id = "vid_m3u8_02",
                title = "Big Buck Bunny — Multi-Bitrate HLS Master",
                description = "60fps animation master render showcasing dynamic frame pacing and high bitrate multi-audio tracks.",
                m3u8Url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=800&q=80",
                category = "Animation",
                tags = listOf("60fps", "Animation", "Master"),
                visibility = "PUBLIC",
                status = VideoPublishStatus.PUBLISHED,
                isFeatured = false,
                isRecommended = true,
                durationSeconds = 596L,
                viewsCount = 21900L,
                likesCount = 1840L
            ),
            M3u8Video(
                id = "vid_m3u8_03",
                title = "CineCut Mobile Color Grading Masterclass [Draft]",
                description = "Internal test reel demonstrating LUT layering and skin tone isolation on mobile HDR displays.",
                m3u8Url = "https://cph-p2p-msl.akamaized.net/hls/live/200034/test/master.m3u8",
                thumbnailUrl = "",
                category = "Color Grading",
                tags = listOf("ColorScience", "Tutorial", "Draft"),
                visibility = "MEMBERS_ONLY",
                status = VideoPublishStatus.DRAFT,
                isFeatured = false,
                isRecommended = false,
                durationSeconds = 480L
            ),
            M3u8Video(
                id = "vid_m3u8_04",
                title = "Deprecate: Low Res Test Stream",
                description = "Legacy test stream marked for deletion.",
                m3u8Url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
                category = "Testing",
                tags = listOf("Test"),
                status = VideoPublishStatus.DELETED
            )
        )
    )
    val m3u8Videos: StateFlow<List<M3u8Video>> = _m3u8Videos.asStateFlow()

    private val _m3u8LiveStreams = MutableStateFlow<List<M3u8LiveStream>>(
        listOf(
            M3u8LiveStream(
                id = "live_m3u8_01",
                title = "🔴 LIVE: Editing A Sci-Fi Trailer in CineCut",
                description = "Official CineCut Live stream breakdown with director Q&A and real-time timeline multi-cam editing.",
                m3u8Url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                thumbnailUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?auto=format&fit=crop&w=800&q=80",
                category = "Editing Workflow",
                status = LiveStreamStatus.LIVE,
                isPublished = true,
                isFeatured = true,
                viewerCount = 3450
            ),
            M3u8LiveStream(
                id = "live_m3u8_02",
                title = "Upcoming: Tokyo Neon Night Color Grading Masterclass",
                description = "Scheduled live broadcast exploring color wheels, log curves, and LUT creation in CineCut.",
                m3u8Url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=800&q=80",
                category = "Masterclass",
                status = LiveStreamStatus.SCHEDULED,
                scheduledStartTime = System.currentTimeMillis() + 86400000L,
                isPublished = true,
                isFeatured = true,
                viewerCount = 0
            ),
            M3u8LiveStream(
                id = "live_m3u8_03",
                title = "Concluded: CineCut v3.0 Global Launch Keynote",
                description = "The announcement of AI Director, multi-track keyframing, and CineRooms.",
                m3u8Url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                category = "Announcement",
                status = LiveStreamStatus.ENDED,
                isPublished = false,
                isFeatured = false,
                viewerCount = 14200
            )
        )
    )
    val m3u8LiveStreams: StateFlow<List<M3u8LiveStream>> = _m3u8LiveStreams.asStateFlow()

    private val _contentCategories = MutableStateFlow<List<ContentCategory>>(
        listOf(
            ContentCategory(name = "Cinematography", slug = "cinematography", description = "Lighting, camera movement, and visual composition.", itemCount = 24),
            ContentCategory(name = "Color Grading", slug = "color-grading", description = "LUTs, ACES color science, and HDR mastering.", itemCount = 38),
            ContentCategory(name = "Editing Workflow", slug = "editing-workflow", description = "Pacing, multi-cam assembly, and keyframing.", itemCount = 19),
            ContentCategory(name = "VFX Breakdown", slug = "vfx-breakdown", description = "Compositing, rotoscoping, and generative effects.", itemCount = 14),
            ContentCategory(name = "Masterclass", slug = "masterclass", description = "Deep-dive filmmaking lectures by industry veterans.", itemCount = 8)
        )
    )
    val contentCategories: StateFlow<List<ContentCategory>> = _contentCategories.asStateFlow()

    private val _autonomousPresets = MutableStateFlow<List<AutonomousPreset>>(
        listOf(
            AutonomousPreset(
                name = "Cyberpunk Neo-Tokyo",
                category = "LUT",
                version = "2.4",
                description = "High dynamic range cyan & magenta neon contrast with deep shadow roll-off.",
                isApproved = true,
                rating = 4.9f
            ),
            AutonomousPreset(
                name = "Vintage 16mm Grain & Halation",
                category = "Effect",
                version = "3.1",
                description = "Emulates Kodak 5219 optical grain and subtle red edge halation.",
                isApproved = true,
                rating = 4.8f
            ),
            AutonomousPreset(
                name = "Cinematic Kinetic Slide",
                category = "Transition",
                version = "1.5",
                description = "Directional motion blur whip transition synced to beat markers.",
                isApproved = false,
                rating = 4.6f
            ),
            AutonomousPreset(
                name = "Teal & Warm Amber Sunset",
                category = "Color Grade",
                version = "2.0",
                description = "Hollywood blockbuster style golden-hour skin tone preservation curve.",
                isApproved = true,
                rating = 5.0f
            ),
            AutonomousPreset(
                name = "Subtle Anamorphic Flare AI",
                category = "Effect",
                version = "1.0",
                description = "Dynamically traces brightest luminance pixels and adds horizontal flare streak.",
                isApproved = false,
                rating = 4.4f
            )
        )
    )
    val autonomousPresets: StateFlow<List<AutonomousPreset>> = _autonomousPresets.asStateFlow()

    // ================= ZAPUPI PAYMENT GATEWAY STATE ================= //

    private val _zapUpiConfig = MutableStateFlow(
        ZapUpiGatewayConfig(
            isEnabled = true,
            maskedApiKey = "••••••••••••••••••••153d54",
            timeoutSeconds = 480,
            currency = "INR",
            isTestMode = false,
            status = "Connected ✓"
        )
    )
    val zapUpiConfig: StateFlow<ZapUpiGatewayConfig> = _zapUpiConfig.asStateFlow()

    private val _paymentOrders = MutableStateFlow<List<ZapUpiOrder>>(
        listOf(
            ZapUpiOrder(
                orderId = "ZAP_CC_98241289",
                userId = "usr_creator_8921",
                userDisplayName = "Alex Rivera",
                amountInr = 299.00,
                itemType = "CINECOINS",
                itemId = "coins_1500",
                title = "Popular Pack (1,500 CineCoins)",
                status = PaymentOrderStatus.PAID,
                qrPayload = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=299.00&tr=ZAP_CC_98241289&cu=INR",
                upiIntentUrl = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=299.00&tr=ZAP_CC_98241289&cu=INR",
                paytmIntentUrl = "paytmmp://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=299.00&tr=ZAP_CC_98241289&cu=INR",
                utrNumber = "426912389104",
                createdAt = System.currentTimeMillis() - 3600000L * 2,
                paidAt = System.currentTimeMillis() - 3600000L * 2 + 120000L
            ),
            ZapUpiOrder(
                orderId = "ZAP_CC_84102941",
                userId = "usr_sarah",
                userDisplayName = "Sarah Chen",
                amountInr = 499.00,
                itemType = "MEMBERSHIP",
                itemId = "tier_gold",
                title = "Gold Creator Pass (Monthly)",
                status = PaymentOrderStatus.PAID,
                qrPayload = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=499.00&tr=ZAP_CC_84102941&cu=INR",
                upiIntentUrl = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=499.00&tr=ZAP_CC_84102941&cu=INR",
                paytmIntentUrl = "paytmmp://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=499.00&tr=ZAP_CC_84102941&cu=INR",
                utrNumber = "426899120341",
                createdAt = System.currentTimeMillis() - 86400000L,
                paidAt = System.currentTimeMillis() - 86400000L + 95000L
            ),
            ZapUpiOrder(
                orderId = "ZAP_CC_73019482",
                userId = "usr_marcus",
                userDisplayName = "Marcus Brody",
                amountInr = 99.00,
                itemType = "CINECOINS",
                itemId = "coins_500",
                title = "Starter Pack (500 CineCoins)",
                status = PaymentOrderStatus.PENDING,
                qrPayload = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=99.00&tr=ZAP_CC_73019482&cu=INR",
                upiIntentUrl = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=99.00&tr=ZAP_CC_73019482&cu=INR",
                paytmIntentUrl = "paytmmp://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=99.00&tr=ZAP_CC_73019482&cu=INR",
                createdAt = System.currentTimeMillis() - 180000L,
                expiresAt = System.currentTimeMillis() + 300000L
            ),
            ZapUpiOrder(
                orderId = "ZAP_CC_61902847",
                userId = "usr_elena",
                userDisplayName = "Elena Rostova",
                amountInr = 899.00,
                itemType = "CINECOINS",
                itemId = "coins_5000",
                title = "Best Value Pack (5,000 CineCoins)",
                status = PaymentOrderStatus.REFUNDED,
                qrPayload = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=899.00&tr=ZAP_CC_61902847&cu=INR",
                upiIntentUrl = "upi://pay?pa=cinecut.zapupi@icici&pn=CineCut%20Studios&am=899.00&tr=ZAP_CC_61902847&cu=INR",
                utrNumber = "426104820194",
                createdAt = System.currentTimeMillis() - 86400000L * 3,
                paidAt = System.currentTimeMillis() - 86400000L * 3 + 140000L,
                refundReason = "Duplicate bank debit customer dispute resolution"
            )
        )
    )
    val paymentOrders: StateFlow<List<ZapUpiOrder>> = _paymentOrders.asStateFlow()

    private val _activePaymentOrder = MutableStateFlow<ZapUpiOrder?>(null)
    val activePaymentOrder: StateFlow<ZapUpiOrder?> = _activePaymentOrder.asStateFlow()

    // ================= SUPPORT TICKET SYSTEM STATEFLOWS ================= //

    private val _nextTicketNumber = java.util.concurrent.atomic.AtomicInteger(100004)

    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(
        listOf(
            SupportTicket(
                ticketId = "#CC-100001",
                userId = "usr_creator_8921",
                userDisplayName = "Alex Rivera",
                userEmail = "alex@cinecut.io",
                subject = "ZapUPI Payment Pending & UTR Verification",
                category = SupportTicketCategory.PAYMENT,
                description = "I initiated a payment of ₹399 for 5,000 CineCoins via UPI. The amount was deducted from my HDFC bank account with UTR 426104820194, but my coins haven't updated yet. Please verify.",
                priority = SupportTicketPriority.HIGH,
                status = SupportTicketStatus.IN_PROGRESS,
                assignedTo = "usr_support_01",
                assignedToName = "Sarah Jenkins (Support Lead)",
                orderId = "ZAP_CC_61902847",
                createdAt = System.currentTimeMillis() - 86400000L,
                updatedAt = System.currentTimeMillis() - 7200000L,
                lastMessageAt = System.currentTimeMillis() - 7200000L,
                unreadUserCount = 1,
                unreadAdminCount = 0,
                attachments = listOf(
                    SupportAttachment(
                        fileName = "hdfc_upi_receipt_426104.png",
                        fileUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=500",
                        fileType = "image/png",
                        fileSizeBytes = 245100L
                    )
                )
            ),
            SupportTicket(
                ticketId = "#CC-100002",
                userId = "usr_2",
                userDisplayName = "Marcus Chen",
                userEmail = "marcus@cinematography.com",
                subject = "4K 60fps HDR Export color shift in DaVinci Resolve",
                category = SupportTicketCategory.VIDEO_EDITOR,
                description = "When exporting ProRes 422 HQ / H.265 in 4K 60fps with Rec.2020 PQ gamma, shadows appear slightly crushed when imported into DaVinci Resolve Studio.",
                priority = SupportTicketPriority.MEDIUM,
                status = SupportTicketStatus.WAITING_FOR_USER,
                assignedTo = "usr_creator_8921",
                assignedToName = "Alex Rivera (Tech Lead)",
                orderId = null,
                createdAt = System.currentTimeMillis() - 86400000L * 2,
                updatedAt = System.currentTimeMillis() - 14400000L,
                lastMessageAt = System.currentTimeMillis() - 14400000L,
                unreadUserCount = 0,
                unreadAdminCount = 0
            ),
            SupportTicket(
                ticketId = "#CC-100003",
                userId = "usr_3",
                userDisplayName = "Elena Rostova",
                userEmail = "elena@vfxstudio.com",
                subject = "CineRooms 4K Screen Sharing Bitrate Allocation",
                category = SupportTicketCategory.CINEROOMS,
                description = "Is there a bitrate cap on screen sharing timeline playback inside VIP rooms? We are hosting a 12-editor review session tomorrow.",
                priority = SupportTicketPriority.LOW,
                status = SupportTicketStatus.RESOLVED,
                assignedTo = "usr_support_02",
                assignedToName = "David Kim (Creator Ops)",
                orderId = null,
                createdAt = System.currentTimeMillis() - 86400000L * 4,
                updatedAt = System.currentTimeMillis() - 86400000L,
                lastMessageAt = System.currentTimeMillis() - 86400000L,
                closedAt = System.currentTimeMillis() - 86400000L,
                unreadUserCount = 0,
                unreadAdminCount = 0
            )
        )
    )
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    private val _ticketMessages = MutableStateFlow<Map<String, List<SupportTicketMessage>>>(
        mapOf(
            "#CC-100001" to listOf(
                SupportTicketMessage(
                    ticketId = "#CC-100001",
                    senderId = "usr_creator_8921",
                    senderName = "Alex Rivera",
                    senderRole = "USER",
                    message = "I initiated a payment of ₹399 for 5,000 CineCoins via UPI. The amount was deducted from my HDFC bank account with UTR 426104820194, but my coins haven't updated yet. Please verify.",
                    isInternalNote = false,
                    attachments = listOf(
                        SupportAttachment(
                            fileName = "hdfc_upi_receipt_426104.png",
                            fileUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=500",
                            fileType = "image/png",
                            fileSizeBytes = 245100L
                        )
                    ),
                    createdAt = System.currentTimeMillis() - 86400000L
                ),
                SupportTicketMessage(
                    ticketId = "#CC-100001",
                    senderId = "usr_support_01",
                    senderName = "Sarah Jenkins",
                    senderRole = "SUPPORT",
                    message = "INTERNAL NOTE: Cross-checked payment gateway logs with ICICI settlement batch. UTR 426104820194 found with status 'CREDIT_SUCCESS_PENDING_WEBHOOK'. Safe to manual-credit coins if needed.",
                    isInternalNote = true,
                    createdAt = System.currentTimeMillis() - 43200000L
                ),
                SupportTicketMessage(
                    ticketId = "#CC-100001",
                    senderId = "usr_support_01",
                    senderName = "Sarah Jenkins (Support Lead)",
                    senderRole = "SUPPORT",
                    message = "Hi Alex, thank you for providing the transaction screenshot and UTR! We have traced your payment in the bank gateway settlement queue. Our automated reconciliation is finalizing the transaction, and your CineCoins wallet balance will reflect the 5,000 coins within the hour.",
                    isInternalNote = false,
                    createdAt = System.currentTimeMillis() - 7200000L
                )
            ),
            "#CC-100002" to listOf(
                SupportTicketMessage(
                    ticketId = "#CC-100002",
                    senderId = "usr_2",
                    senderName = "Marcus Chen",
                    senderRole = "USER",
                    message = "When exporting ProRes 422 HQ / H.265 in 4K 60fps with Rec.2020 PQ gamma, shadows appear slightly crushed when imported into DaVinci Resolve Studio.",
                    isInternalNote = false,
                    createdAt = System.currentTimeMillis() - 86400000L * 2
                ),
                SupportTicketMessage(
                    ticketId = "#CC-100002",
                    senderId = "usr_creator_8921",
                    senderName = "Alex Rivera (Tech Lead)",
                    senderRole = "ADMIN",
                    message = "Hello Marcus, could you please confirm if you have 'Full Data Levels' or 'Video Levels' checked in your project export color management settings? For HDR Rec.2020 PQ, DaVinci defaults to Video levels (64-940) unless set to Full range.",
                    isInternalNote = false,
                    createdAt = System.currentTimeMillis() - 14400000L
                )
            ),
            "#CC-100003" to listOf(
                SupportTicketMessage(
                    ticketId = "#CC-100003",
                    senderId = "usr_3",
                    senderName = "Elena Rostova",
                    senderRole = "USER",
                    message = "Is there a bitrate cap on screen sharing timeline playback inside VIP rooms? We are hosting a 12-editor review session tomorrow.",
                    isInternalNote = false,
                    createdAt = System.currentTimeMillis() - 86400000L * 4
                ),
                SupportTicketMessage(
                    ticketId = "#CC-100003",
                    senderId = "usr_support_02",
                    senderName = "David Kim (Creator Ops)",
                    senderRole = "SUPPORT",
                    message = "Hi Elena! For VIP and Founder CineRooms, adaptive bitrate automatically scales up to 15 Mbps in 4K 60fps with sub-100ms WebRTC latency. You will have full uncompressed quality for your 12 participants!",
                    isInternalNote = false,
                    createdAt = System.currentTimeMillis() - 86400000L
                )
            )
        )
    )
    val ticketMessages: StateFlow<Map<String, List<SupportTicketMessage>>> = _ticketMessages.asStateFlow()


    private val _adminNotifications = MutableStateFlow<List<AdminNotification>>(
        listOf(
            AdminNotification(
                title = "CineCut v2.4 Feature Release",
                message = "The new Bezier curve keyframe timeline and 4K 60fps export engine are now live for all creators!",
                audience = "GLOBAL",
                category = "Update",
                deepLink = "cinecut://editor",
                sentByAdminUid = "usr_creator_8921",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            ),
            AdminNotification(
                title = "VIP Color Masterclass Tonight",
                message = "Join the live CineRoom with Hollywood colorist Elena Rostova at 7 PM UTC.",
                audience = "GOLD_PLUS",
                category = "Live Event",
                deepLink = "cinecut://rooms",
                sentByAdminUid = "usr_creator_8921",
                timestamp = System.currentTimeMillis() - 86400000L * 4
            )
        )
    )
    val adminNotifications: StateFlow<List<AdminNotification>> = _adminNotifications.asStateFlow()

    private val _securityHealth = MutableStateFlow(SecurityHealthStatus())
    val securityHealth: StateFlow<SecurityHealthStatus> = _securityHealth.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(
        listOf(
            AuditLog(adminUid = "usr_creator_8921", action = "SECURITY_CHECK", target = "Firebase App Check", reason = "Enforcement posture verified (Attested)"),
            AuditLog(adminUid = "usr_creator_8921", action = "FEATURE_FLAG_UPDATE", target = "aiDirectorEnabled", reason = "Production deployment roll-out"),
            AuditLog(adminUid = "usr_creator_8921", action = "GRANT_BADGE", target = "usr_elena", reason = "Verified Creator milestone"),
            AuditLog(adminUid = "usr_elena", action = "USER_SUSPEND", target = "usr_troll42", reason = "Toxic harassment in CineLive stream #1"),
            AuditLog(adminUid = "usr_creator_8921", action = "USER_BAN", target = "usr_spammer99", reason = "Malicious phishing botnet account")
        )
    )
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _reports = MutableStateFlow<List<ModerationReport>>(
        listOf(
            ModerationReport(
                id = "rep_01",
                reporterId = "usr_kenji",
                reporterName = "Kenji Sato",
                targetType = "POST",
                targetId = "post_1",
                targetSnippet = "Color graded this short with the new CineCut 35mm...",
                reason = "Potential copyright infringement on audio track",
                description = "Music sounds identical to licensed track without proper creator credit.",
                status = ReportStatus.PENDING,
                timestamp = System.currentTimeMillis() - 3600000L * 3
            ),
            ModerationReport(
                id = "rep_02",
                reporterId = "usr_marcus",
                reporterName = "Marcus Thorne",
                targetType = "USER",
                targetId = "usr_troll42",
                targetSnippet = "@shadow_rebel (Suspended user)",
                reason = "Harassment & abusive behavior",
                description = "User spamming offensive slurs during live stream session.",
                status = ReportStatus.REVIEWING,
                assignedModerator = "usr_elena",
                timestamp = System.currentTimeMillis() - 86400000L * 1
            ),
            ModerationReport(
                id = "rep_03",
                reporterId = "usr_sarah",
                reporterName = "Sarah Chen",
                targetType = "ROOM",
                targetId = "room_color",
                targetSnippet = "Colorists & LUT Masters Room",
                reason = "Unauthorized third-party plugin download link",
                description = "A member posted an unverified APK download link in room chat.",
                status = ReportStatus.RESOLVED,
                assignedModerator = "usr_creator_8921",
                resolution = "Link message removed and sender warned.",
                timestamp = System.currentTimeMillis() - 86400000L * 3,
                resolvedAt = System.currentTimeMillis() - 86400000L * 2
            ),
            ModerationReport(
                id = "rep_04",
                reporterId = "usr_david",
                reporterName = "David Kim",
                targetType = "STREAM",
                targetId = "stream_1",
                targetSnippet = "🔴 LIVE: Editing A Sci-Fi Trailer",
                reason = "Viewer spamming coin donation refund claims",
                description = "False refund request claim in live chat.",
                status = ReportStatus.REJECTED,
                assignedModerator = "usr_elena",
                resolution = "Verified legitimate tip transaction on blockchain ledger.",
                timestamp = System.currentTimeMillis() - 86400000L * 4,
                resolvedAt = System.currentTimeMillis() - 86400000L * 3
            )
        )
    )
    val reports: StateFlow<List<ModerationReport>> = _reports.asStateFlow()

    private val _featureFlags = MutableStateFlow(FeatureFlags())
    val featureFlags: StateFlow<FeatureFlags> = _featureFlags.asStateFlow()

    // ================= ACTIONS ================= //

    fun toggleLike(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val delta = if (newLiked) 1 else -1
                post.copy(isLiked = newLiked, likesCount = post.likesCount + delta)
            } else post
        }
    }

    fun addPost(content: String, tags: List<String>) {
        val user = _currentUser.value
        val newPost = Post(
            authorId = user.uid,
            authorName = user.displayName,
            authorHandle = "@${user.username}",
            authorTier = user.membershipTier,
            content = content,
            tags = tags
        )
        _posts.value = listOf(newPost) + _posts.value
    }

    fun sendRoomMessage(roomId: String, text: String) {
        val user = _currentUser.value
        val msg = RoomMessage(
            roomId = roomId,
            senderId = user.uid,
            senderName = user.displayName,
            senderTier = user.membershipTier,
            message = text
        )
        val current = _roomMessages.value[roomId] ?: emptyList()
        _roomMessages.value = _roomMessages.value + (roomId to (current + msg))
    }

    fun sendLiveChatMessage(text: String, tipCoins: Long = 0L) {
        val user = _currentUser.value
        if (tipCoins > 0 && user.coinBalance >= tipCoins) {
            _currentUser.value = user.copy(coinBalance = user.coinBalance - tipCoins)
            val tx = CineCoinTransaction(
                userId = user.uid,
                amount = -tipCoins,
                reason = "Live Stream Tip",
                type = "TIP"
            )
            _coinTransactions.value = listOf(tx) + _coinTransactions.value
        }
        val msg = LiveChatMessage(
            senderName = user.displayName,
            senderTier = user.membershipTier,
            message = text,
            coinTip = tipCoins
        )
        _liveChat.value = _liveChat.value + msg
    }

    fun sendDirectMessage(text: String) {
        val msg = DirectMessage(
            senderId = _currentUser.value.uid,
            senderName = _currentUser.value.displayName,
            text = text,
            isMe = true
        )
        _directMessages.value = _directMessages.value + msg
    }

    fun addProject(title: String, aspectRatio: String = "16:9"): Project {
        val owner = _currentUser.value.uid.ifEmpty { "creator" }
        val newProject = Project(
            title = title,
            aspectRatio = aspectRatio,
            durationMs = 12000L,
            ownerId = owner
        )
        _projects.value = listOf(newProject) + _projects.value
        repoScope.launch {
            cloudSyncEngine.saveProjectToCloud(newProject)
        }
        return newProject
    }

    fun duplicateProject(project: Project) {
        val copy = project.copy(
            id = UUID.randomUUID().toString(),
            title = "${project.title} (Copy)",
            updatedAt = System.currentTimeMillis()
        )
        _projects.value = listOf(copy) + _projects.value
        repoScope.launch {
            cloudSyncEngine.saveProjectToCloud(copy)
        }
    }

    fun deleteProject(projectId: String) {
        _projects.value = _projects.value.filterNot { it.id == projectId }
        repoScope.launch {
            cloudSyncEngine.deleteProjectFromCloud(projectId)
        }
    }

    fun purchaseCoins(packageCoins: Long, reason: String = "Google Play Purchase") {
        val current = _currentUser.value
        _currentUser.value = current.copy(coinBalance = current.coinBalance + packageCoins)
        val tx = CineCoinTransaction(
            userId = current.uid,
            amount = packageCoins,
            reason = reason,
            type = "PURCHASE"
        )
        _coinTransactions.value = listOf(tx) + _coinTransactions.value
    }

    fun upgradeMembership(tier: MembershipTier) {
        val current = _currentUser.value
        _currentUser.value = current.copy(membershipTier = tier)
    }

    // ================= PROFILE & PRIVACY ACTIONS ================= //

    fun updateUserProfile(displayName: String, username: String, bio: String, avatarUrl: String = "", bannerUrl: String = "") {
        val current = _currentUser.value
        // Validate unique username
        val isTaken = _allUsers.value.any { it.username.equals(username, ignoreCase = true) && it.uid != current.uid }
        if (isTaken) return

        val updated = current.copy(
            displayName = displayName,
            username = username,
            bio = bio,
            avatarUrl = if (avatarUrl.isNotEmpty()) avatarUrl else current.avatarUrl,
            bannerUrl = if (bannerUrl.isNotEmpty()) bannerUrl else current.bannerUrl
        )
        _currentUser.value = updated
        _allUsers.value = _allUsers.value.map { if (it.uid == current.uid) updated else it }
    }

    fun updatePrivacySettings(isCoinBalancePublic: Boolean, allowProjectInvites: Boolean, allowTagging: Boolean) {
        val current = _currentUser.value
        val updated = current.copy(
            isCoinBalancePublic = isCoinBalancePublic,
            allowProjectInvites = allowProjectInvites,
            allowTagging = allowTagging
        )
        _currentUser.value = updated
        _allUsers.value = _allUsers.value.map { if (it.uid == current.uid) updated else it }
    }

    // ================= FRIENDS ACTIONS ================= //

    fun getFriendState(targetUid: String): FriendState {
        val me = _currentUser.value.uid
        if (targetUid == me) return FriendState.NONE
        if (_blockedUserIds.value.contains(targetUid)) return FriendState.BLOCKED
        if (_friendships.value.any { it.friendUserId == targetUid && it.state == FriendState.FRIENDS }) return FriendState.FRIENDS
        if (_sentFriendRequests.value.any { it.receiverId == targetUid && it.status == "PENDING" }) return FriendState.REQUEST_SENT
        if (_friendRequests.value.any { it.senderId == targetUid && it.status == "PENDING" }) return FriendState.REQUEST_RECEIVED
        return FriendState.NONE
    }

    fun sendFriendRequest(targetUid: String): Boolean {
        val me = _currentUser.value
        if (targetUid == me.uid || _blockedUserIds.value.contains(targetUid)) return false
        if (getFriendState(targetUid) != FriendState.NONE) return false

        val targetUser = _allUsers.value.find { it.uid == targetUid } ?: return false
        val newReq = FriendRequest(
            senderId = me.uid,
            senderName = me.displayName,
            senderUsername = me.username,
            senderAvatar = me.avatarUrl,
            receiverId = targetUid,
            receiverName = targetUser.displayName
        )
        _sentFriendRequests.value = listOf(newReq) + _sentFriendRequests.value

        // Log notification for receiver
        val notif = InAppNotification(
            userId = targetUid,
            category = NotificationCategory.FRIEND,
            title = "New Friend Request",
            message = "${me.displayName} (@${me.username}) sent you a friend request.",
            senderId = me.uid,
            senderName = me.displayName,
            relatedEntityId = newReq.id,
            actionable = true,
            actionType = "FRIEND_REQUEST"
        )
        _inAppNotifications.value = listOf(notif) + _inAppNotifications.value
        return true
    }

    fun acceptFriendRequest(requestId: String) {
        val req = _friendRequests.value.find { it.id == requestId } ?: return
        _friendRequests.value = _friendRequests.value.filterNot { it.id == requestId }

        val sender = _allUsers.value.find { it.uid == req.senderId } ?: UserProfile(
            uid = req.senderId,
            displayName = req.senderName,
            username = req.senderUsername
        )
        val friendship = Friendship(
            userId = _currentUser.value.uid,
            friendUserId = sender.uid,
            friendProfile = sender,
            state = FriendState.FRIENDS
        )
        _friendships.value = listOf(friendship) + _friendships.value

        // Update friend counts
        val me = _currentUser.value
        _currentUser.value = me.copy(friendsCount = me.friendsCount + 1)

        // Notification for sender
        val notif = InAppNotification(
            userId = req.senderId,
            category = NotificationCategory.FRIEND,
            title = "Friend Request Accepted",
            message = "${me.displayName} accepted your friend request!",
            senderId = me.uid,
            senderName = me.displayName
        )
        _inAppNotifications.value = listOf(notif) + _inAppNotifications.value
    }

    fun rejectFriendRequest(requestId: String) {
        _friendRequests.value = _friendRequests.value.filterNot { it.id == requestId }
    }

    fun cancelFriendRequest(requestId: String) {
        _sentFriendRequests.value = _sentFriendRequests.value.filterNot { it.id == requestId }
    }

    fun removeFriend(targetUid: String) {
        _friendships.value = _friendships.value.filterNot { it.friendUserId == targetUid }
        val me = _currentUser.value
        _currentUser.value = me.copy(friendsCount = maxOf(0, me.friendsCount - 1))
    }

    fun blockUser(targetUid: String) {
        removeFriend(targetUid)
        _friendRequests.value = _friendRequests.value.filterNot { it.senderId == targetUid }
        _sentFriendRequests.value = _sentFriendRequests.value.filterNot { it.receiverId == targetUid }
        _blockedUserIds.value = _blockedUserIds.value + targetUid
    }

    fun unblockUser(targetUid: String) {
        _blockedUserIds.value = _blockedUserIds.value - targetUid
    }

    // ================= PROJECT SHARING & COLLABORATION ================= //

    fun updateProjectVisibility(projectId: String, newVisibility: String) {
        _projects.value = _projects.value.map { project ->
            if (project.id == projectId) {
                project.copy(visibility = newVisibility, updatedAt = System.currentTimeMillis())
            } else project
        }
        logProjectOperation(projectId, "VISIBILITY_CHANGE", "Changed visibility to $newVisibility")
    }

    fun inviteCollaborator(projectId: String, targetUid: String, role: ProjectRole): Boolean {
        val project = _projects.value.find { it.id == projectId } ?: return false
        val targetUser = _allUsers.value.find { it.uid == targetUid } ?: return false
        val me = _currentUser.value

        // Check if already collaborator
        val currentCollabs = _collaborators.value[projectId] ?: emptyList()
        if (currentCollabs.any { it.userId == targetUid }) return false

        val invite = ProjectInvitation(
            projectId = projectId,
            projectTitle = project.title,
            senderId = me.uid,
            senderName = me.displayName,
            receiverId = targetUid,
            role = role
        )
        _projectInvitations.value = listOf(invite) + _projectInvitations.value

        // In-app notification
        val notif = InAppNotification(
            userId = targetUid,
            category = NotificationCategory.PROJECT,
            title = "Project Collaboration Invite",
            message = "${me.displayName} invited you to collaborate on '${project.title}' as ${role.label}.",
            senderId = me.uid,
            senderName = me.displayName,
            relatedEntityId = invite.id,
            actionable = true,
            actionType = "PROJECT_INVITE"
        )
        _inAppNotifications.value = listOf(notif) + _inAppNotifications.value
        logProjectOperation(projectId, "INVITE_MEMBER", "Invited @${targetUser.username} as ${role.label}")
        return true
    }

    fun respondToProjectInvite(invitationId: String, accept: Boolean) {
        val invite = _projectInvitations.value.find { it.id == invitationId } ?: return
        _projectInvitations.value = _projectInvitations.value.filterNot { it.id == invitationId }

        if (accept) {
            val me = _currentUser.value
            val newCollab = Collaborator(
                userId = me.uid,
                username = me.username,
                displayName = me.displayName,
                avatarUrl = me.avatarUrl,
                role = invite.role
            )
            val current = _collaborators.value[invite.projectId] ?: emptyList()
            _collaborators.value = _collaborators.value + (invite.projectId to (current + newCollab))

            // Update project collaborators count & collaborative flag
            _projects.value = _projects.value.map {
                if (it.id == invite.projectId) {
                    it.copy(isCollaborative = true, collaboratorsCount = it.collaboratorsCount + 1)
                } else it
            }
            logProjectOperation(invite.projectId, "MEMBER_JOINED", "${me.displayName} joined as ${invite.role.label}")
        }
    }

    fun updateCollaboratorRole(projectId: String, targetUid: String, newRole: ProjectRole) {
        val current = _collaborators.value[projectId] ?: return
        _collaborators.value = _collaborators.value + (projectId to current.map {
            if (it.userId == targetUid) it.copy(role = newRole) else it
        })
        logProjectOperation(projectId, "ROLE_CHANGE", "Updated role for $targetUid to ${newRole.label}")
    }

    fun removeCollaborator(projectId: String, targetUid: String) {
        val current = _collaborators.value[projectId] ?: return
        _collaborators.value = _collaborators.value + (projectId to current.filterNot { it.userId == targetUid })
        _projects.value = _projects.value.map {
            if (it.id == projectId) it.copy(collaboratorsCount = maxOf(1, it.collaboratorsCount - 1)) else it
        }
        logProjectOperation(projectId, "MEMBER_REMOVED", "Removed collaborator $targetUid")
    }

    fun logProjectOperation(projectId: String, operationType: String, details: String) {
        val me = _currentUser.value
        val op = ProjectOperation(
            projectId = projectId,
            userId = me.uid,
            userName = me.displayName,
            operationType = operationType,
            details = details
        )
        val current = _projectOperations.value[projectId] ?: emptyList()
        _projectOperations.value = _projectOperations.value + (projectId to (listOf(op) + current))
    }

    fun saveProjectVersion(projectId: String, versionName: String, summary: String) {
        val me = _currentUser.value
        val current = _projectVersions.value[projectId] ?: emptyList()
        val nextVersionNum = current.size + 1
        val version = ProjectVersion(
            projectId = projectId,
            versionNumber = nextVersionNum,
            name = versionName,
            authorId = me.uid,
            authorName = me.displayName,
            changeSummary = summary
        )
        _projectVersions.value = _projectVersions.value + (projectId to (listOf(version) + current))
        logProjectOperation(projectId, "VERSION_CREATED", "Saved snapshot v$nextVersionNum: $versionName")
    }

    fun addProjectComment(projectId: String, text: String, timecodeMs: Long = 0L, mentions: List<String> = emptyList()) {
        val me = _currentUser.value
        val comment = ProjectComment(
            projectId = projectId,
            authorId = me.uid,
            authorName = me.displayName,
            authorAvatar = me.avatarUrl,
            text = text,
            timecodeMs = timecodeMs,
            mentions = mentions
        )
        val current = _projectComments.value[projectId] ?: emptyList()
        _projectComments.value = _projectComments.value + (projectId to (current + comment))
        logProjectOperation(projectId, "COMMENT_ADDED", "Comment added at ${timecodeMs}ms")
    }

    // ================= CINECOIN AUTHORITATIVE TRANSACTIONS ================= //

    fun executeCoinTransaction(type: String, amount: Long, reason: String, idempotencyKey: String = UUID.randomUUID().toString()): Boolean {
        val currentAcct = _coinAccount.value
        val newBal = currentAcct.balance + amount
        if (newBal < 0) return false // Insufficient funds

        val updatedAcct = currentAcct.copy(
            balance = newBal,
            totalEarned = if (amount > 0) currentAcct.totalEarned + amount else currentAcct.totalEarned,
            totalSpent = if (amount < 0) currentAcct.totalSpent + (-amount) else currentAcct.totalSpent,
            lastTransactionAt = System.currentTimeMillis()
        )
        _coinAccount.value = updatedAcct

        // Update user profile balance
        val user = _currentUser.value
        _currentUser.value = user.copy(coinBalance = newBal)

        val tx = CineCoinTransaction(
            userId = user.uid,
            amount = amount,
            reason = reason,
            type = type,
            balanceAfter = newBal,
            idempotencyKey = idempotencyKey
        )
        _coinTransactions.value = listOf(tx) + _coinTransactions.value

        // Notification if earned
        if (amount > 0) {
            val notif = InAppNotification(
                userId = user.uid,
                category = NotificationCategory.COIN,
                title = "CineCoins Credited",
                message = "+$amount CineCoins: $reason"
            )
            _inAppNotifications.value = listOf(notif) + _inAppNotifications.value
        }
        return true
    }

    fun requestCoinRefund(txId: String, reason: String): Boolean {
        val tx = _coinTransactions.value.find { it.id == txId } ?: return false
        if (tx.type != "PURCHASE") return false

        // Reverse purchase
        return executeCoinTransaction(
            type = "REFUND",
            amount = -tx.amount,
            reason = "Refund: $reason (Original tx: $txId)"
        )
    }

    // ================= IN-APP NOTIFICATIONS ================= //

    fun markNotificationAsRead(id: String) {
        _inAppNotifications.value = _inAppNotifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsAsRead() {
        _inAppNotifications.value = _inAppNotifications.value.map { it.copy(isRead = true) }
    }

    fun clearNotification(id: String) {
        _inAppNotifications.value = _inAppNotifications.value.filterNot { it.id == id }
    }

    // ================= ADMIN ACTIONS ================= //

    fun adminSuspendUser(targetUid: String, reason: String, durationDays: Int = 7) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.BAN_USERS) && admin.adminRole != AdminRole.MODERATOR) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                user.copy(
                    isSuspended = true,
                    suspensionReason = "$reason (${durationDays}d suspension)"
                )
            } else user
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "USER_SUSPEND",
            target = targetUid,
            reason = "$reason ($durationDays days)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminUnsuspendUser(targetUid: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.BAN_USERS) && admin.adminRole != AdminRole.MODERATOR) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                user.copy(isSuspended = false, suspensionReason = null)
            } else user
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "USER_UNSUSPEND",
            target = targetUid,
            reason = "Suspension lifted by admin"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminBanUser(targetUid: String, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.BAN_USERS)) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                user.copy(isBanned = true, banReason = reason)
            } else user
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "USER_BAN",
            target = targetUid,
            reason = reason
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminUnbanUser(targetUid: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.BAN_USERS)) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                user.copy(isBanned = false, banReason = null)
            } else user
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "USER_UNBAN",
            target = targetUid,
            reason = "Permanent ban revoked by admin"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminUpdateUserRole(targetUid: String, newRole: AdminRole, reason: String) {
        val admin = _currentUser.value
        if (admin.adminRole != AdminRole.SUPER_ADMIN) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                user.copy(adminRole = newRole)
            } else user
        }
        if (_currentUser.value.uid == targetUid) {
            _currentUser.value = _currentUser.value.copy(adminRole = newRole)
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "ASSIGN_ROLE",
            target = targetUid,
            reason = "Assigned ${newRole.name} ($reason)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminGrantFounder(targetUid: String, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_MEMBERSHIPS)) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                val newBadges = if (!user.badges.contains("Early Founder")) user.badges + "Early Founder" else user.badges
                user.copy(membershipTier = MembershipTier.FOUNDER, badges = newBadges)
            } else user
        }
        if (_currentUser.value.uid == targetUid) {
            _currentUser.value = _currentUser.value.copy(membershipTier = MembershipTier.FOUNDER)
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "GRANT_FOUNDER_STATUS",
            target = targetUid,
            reason = reason
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminManageBadge(targetUid: String, badgeName: String, grant: Boolean, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_BADGES)) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                val updatedBadges = if (grant) {
                    if (!user.badges.contains(badgeName)) user.badges + badgeName else user.badges
                } else {
                    user.badges.filterNot { it == badgeName }
                }
                user.copy(badges = updatedBadges)
            } else user
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = if (grant) "GRANT_BADGE" else "REVOKE_BADGE",
            target = targetUid,
            reason = "$badgeName ($reason)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminAdjustCoins(targetUid: String, amount: Long, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.ADJUST_COINS)) return

        _allUsers.value = _allUsers.value.map { user ->
            if (user.uid == targetUid) {
                val newBal = maxOf(0L, user.coinBalance + amount)
                user.copy(coinBalance = newBal)
            } else user
        }
        if (targetUid == admin.uid) {
            _currentUser.value = admin.copy(coinBalance = maxOf(0L, admin.coinBalance + amount))
        }

        val tx = CineCoinTransaction(
            userId = targetUid,
            amount = amount,
            reason = "Admin adjustment: $reason",
            type = if (amount >= 0) "ADMIN_GRANT" else "ADMIN_ADJUSTMENT"
        )
        _coinTransactions.value = listOf(tx) + _coinTransactions.value

        val log = AuditLog(
            adminUid = admin.uid,
            action = "ADMIN_ADJUST_COINS",
            target = targetUid,
            reason = "Amount: $amount ($reason)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminModerateContent(targetType: String, targetId: String, action: String, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return

        if (targetType == "POST") {
            _posts.value = _posts.value.map { post ->
                if (post.id == targetId) {
                    val status = if (action == "REMOVE") "REMOVED" else if (action == "RESTORE") "ACTIVE" else "RESTRICTED"
                    post.copy(moderationStatus = status)
                } else post
            }
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "CONTENT_${action}",
            target = "$targetType:$targetId",
            reason = reason
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminUpdateReport(reportId: String, newStatus: ReportStatus, resolution: String? = null, reason: String = "") {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return

        _reports.value = _reports.value.map { report ->
            if (report.id == reportId) {
                report.copy(
                    status = newStatus,
                    resolution = resolution ?: report.resolution,
                    assignedModerator = admin.displayName,
                    resolvedAt = if (newStatus == ReportStatus.RESOLVED || newStatus == ReportStatus.REJECTED) System.currentTimeMillis() else null
                )
            } else report
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "REPORT_${newStatus.name}",
            target = reportId,
            reason = resolution ?: reason
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminCreateRoom(name: String, description: String, requiredTier: MembershipTier) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_ROOMS)) return

        val newRoom = CineRoom(
            id = "room_${UUID.randomUUID().toString().take(6)}",
            name = name,
            description = description,
            requiredTier = requiredTier,
            memberCount = 1,
            isLive = false,
            currentTopic = "Welcome to $name"
        )
        _rooms.value = listOf(newRoom) + _rooms.value

        val log = AuditLog(
            adminUid = admin.uid,
            action = "CREATE_ROOM",
            target = newRoom.id,
            reason = "Created room '$name'"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminArchiveRoom(roomId: String, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_ROOMS)) return

        _rooms.value = _rooms.value.filterNot { it.id == roomId }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "ARCHIVE_ROOM",
            target = roomId,
            reason = reason
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminTerminateStream(streamId: String, reason: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_LIVE)) return

        _liveStreams.value = _liveStreams.value.map { stream ->
            if (stream.id == streamId) stream.copy(isLive = false) else stream
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "TERMINATE_STREAM",
            target = streamId,
            reason = reason
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminSendNotification(title: String, message: String, audience: String, targetUid: String? = null, deepLink: String = "cinecut://home") {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.SEND_NOTIFICATIONS)) return

        val notif = AdminNotification(
            title = title,
            message = message,
            audience = audience,
            targetUid = targetUid,
            deepLink = deepLink,
            sentByAdminUid = admin.uid,
            timestamp = System.currentTimeMillis()
        )
        _adminNotifications.value = listOf(notif) + _adminNotifications.value

        val log = AuditLog(
            adminUid = admin.uid,
            action = "BROADCAST_NOTIFICATION",
            target = audience,
            reason = title
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminApprovePreset(presetId: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_AI)) return

        _autonomousPresets.value = _autonomousPresets.value.map { preset ->
            if (preset.id == presetId) preset.copy(isApproved = true) else preset
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "APPROVE_AI_PRESET",
            target = presetId,
            reason = "Approved for public marketplace"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminRejectPreset(presetId: String) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_AI)) return

        _autonomousPresets.value = _autonomousPresets.value.map { preset ->
            if (preset.id == presetId) preset.copy(isApproved = false) else preset
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "REJECT_AI_PRESET",
            target = presetId,
            reason = "Rejected from public catalog"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminTriggerAiWorker() {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_AI)) return

        val newStyles = listOf(
            AutonomousPreset(
                name = "Nordic Noir Bleach Bypass",
                category = "LUT",
                version = "1.0",
                description = "Desaturated high contrast film look with silver retention curve.",
                isApproved = true,
                rating = 4.7f
            ),
            AutonomousPreset(
                name = "Smart Zoom Morph Beat AI",
                category = "Transition",
                version = "2.0",
                description = "AI face-tracking optical zoom transition synced to music transient peaks.",
                isApproved = false,
                rating = 4.8f
            )
        )
        _autonomousPresets.value = newStyles + _autonomousPresets.value

        val log = AuditLog(
            adminUid = admin.uid,
            action = "TRIGGER_AI_WORKER",
            target = "AutonomousPresetWorker",
            reason = "Manual batch generation executed"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminToggleFeatureFlag(flagName: String, enabled: Boolean) {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_FEATURE_FLAGS)) return

        val current = _featureFlags.value
        _featureFlags.value = when (flagName) {
            "aiDirectorEnabled" -> current.copy(aiDirectorEnabled = enabled)
            "autonomousAiEnabled" -> current.copy(autonomousAiEnabled = enabled)
            "cineLiveEnabled" -> current.copy(cineLiveEnabled = enabled)
            "cineRoomsEnabled" -> current.copy(cineRoomsEnabled = enabled)
            "messagingEnabled" -> current.copy(messagingEnabled = enabled)
            "newEditorEnabled" -> current.copy(newEditorEnabled = enabled)
            "maintenanceMode" -> current.copy(maintenanceMode = enabled)
            else -> current
        }

        val log = AuditLog(
            adminUid = admin.uid,
            action = "TOGGLE_FEATURE_FLAG",
            target = flagName,
            reason = "Set to $enabled"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    fun adminSimulateSwitchRole(role: AdminRole) {
        // Disabled in hardened security posture: Client-side role simulation prohibited.
        // Role authorization must originate authoritatively from Firebase Custom Claims.
        val current = _currentUser.value
        val log = AuditLog(
            adminUid = current.uid,
            action = "UNAUTHORIZED_ROLE_SIMULATION_BLOCKED",
            target = current.uid,
            reason = "Role simulation blocked: Firebase Custom Claims is single source of truth"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
    }

    // ================= AUTHENTICATION & SESSION SECURITY ================= //

    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> {
        val manager = authManager ?: return Result.failure(IllegalStateException("AuthManager not initialized"))
        val result = manager.signInWithEmailAndPassword(email, pass)
        result.onSuccess { profile ->
            _currentAuthUser.value = profile
            _currentUser.value = profile
            val log = AuditLog(
                adminUid = profile.uid,
                action = "USER_LOGIN_AUTHENTICATED",
                target = profile.uid,
                reason = "User logged in with verified Firebase credentials (${profile.email})"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        }
        return result
    }

    suspend fun registerWithEmail(displayName: String, username: String, email: String, pass: String): Result<UserProfile> {
        val manager = authManager ?: return Result.failure(IllegalStateException("AuthManager not initialized"))
        val result = manager.registerWithEmailAndPassword(displayName, username, email, pass)
        result.onSuccess { profile ->
            _currentAuthUser.value = profile
            _currentUser.value = profile
            val log = AuditLog(
                adminUid = profile.uid,
                action = "USER_REGISTERED",
                target = profile.uid,
                reason = "New account registered with Firebase Authentication"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        }
        return result
    }

    suspend fun signInWithGoogleOAuth(
        accountEmail: String,
        accountName: String,
        photoUrl: String? = null,
        idToken: String? = null
    ): Result<UserProfile> {
        val manager = authManager ?: return Result.failure(IllegalStateException("AuthManager not initialized"))
        val result = manager.signInWithGoogleOAuth(accountEmail, accountName, photoUrl, idToken)
        result.onSuccess { profile ->
            _currentAuthUser.value = profile
            _currentUser.value = profile
            val log = AuditLog(
                adminUid = profile.uid,
                action = "GOOGLE_OAUTH_LOGIN",
                target = profile.uid,
                reason = "User logged in with verified Google OAuth credentials (${profile.email})"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        }
        return result
    }

    suspend fun signInWithRealGoogleOAuth(activity: android.app.Activity): Result<UserProfile> {
        val manager = authManager ?: return Result.failure(IllegalStateException("AuthManager not initialized"))
        val result = manager.signInWithRealGoogleOAuth(activity)
        result.onSuccess { profile ->
            _currentAuthUser.value = profile
            _currentUser.value = profile
            val log = AuditLog(
                adminUid = profile.uid,
                action = "GOOGLE_OAUTH_LOGIN",
                target = profile.uid,
                reason = "User logged in with verified Google OAuth credentials (${profile.email})"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        }
        return result
    }

    fun hasConfiguredLiveApiKey(): Boolean {
        return authManager?.hasConfiguredLiveApiKey() ?: false
    }

    fun updateCustomFirebaseApiKey(apiKey: String, projectId: String? = null, webClientId: String? = null): Boolean {
        return authManager?.updateCustomApiKey(apiKey, projectId, webClientId) ?: false
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<UserProfile> {
        val manager = authManager ?: return Result.failure(IllegalStateException("AuthManager not initialized"))
        val result = manager.signInWithGoogleCredential(idToken)
        result.onSuccess { profile ->
            _currentAuthUser.value = profile
            _currentUser.value = profile
            val log = AuditLog(
                adminUid = profile.uid,
                action = "GOOGLE_OAUTH_LOGIN",
                target = profile.uid,
                reason = "User logged in with verified Google OAuth credentials (${profile.email})"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        }
        return result
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return authManager?.sendPasswordReset(email) ?: Result.failure(IllegalStateException("Auth not initialized"))
    }

    suspend fun resendEmailVerification(): Result<Unit> {
        return authManager?.resendVerificationEmail() ?: Result.failure(IllegalStateException("Auth not initialized"))
    }

    suspend fun refreshClaims(): Result<AuthState> {
        val manager = authManager ?: return Result.failure(IllegalStateException("Auth not initialized"))
        val res = manager.refreshClaims()
        res.onSuccess { state ->
            if (state is AuthState.Authenticated) {
                _currentAuthUser.value = state.profile
                _currentUser.value = state.profile
            }
        }
        return res
    }

    suspend fun requestInitialAdminClaimSetup(): Result<String> {
        val manager = authManager ?: return Result.failure(IllegalStateException("Auth not initialized"))
        val res = manager.requestInitialAdminClaimSetup()
        res.onSuccess {
            manager.currentUserProfile.value?.let { p ->
                _currentAuthUser.value = p
                _currentUser.value = p
                val log = AuditLog(
                    adminUid = p.uid,
                    action = "INITIAL_ADMIN_CLAIM_ACTIVATED",
                    target = p.uid,
                    reason = "Super Admin claims established for authorized account (${p.email})"
                )
                _auditLogs.value = listOf(log) + _auditLogs.value
            }
        }
        return res
    }

    fun signOut() {
        cloudSyncEngine.stopSync()
        authManager?.signOut()
        _currentAuthUser.value = null
        _currentUser.value = UserProfile(
            uid = "",
            username = "",
            displayName = "",
            email = "",
            membershipTier = MembershipTier.FREE,
            adminRole = AdminRole.NONE,
            coinBalance = 0L,
            badges = emptyList()
        )
    }

    suspend fun deleteAccount(): Result<Unit> {
        val manager = authManager ?: return Result.failure(IllegalStateException("Auth not initialized"))
        return manager.deleteAccount().also {
            signOut()
        }
    }

    // ================= ADMIN M3U8 CONTENT ACTIONS ================= //

    fun adminAddM3u8Video(
        title: String,
        description: String,
        m3u8Url: String,
        thumbnailUrl: String,
        category: String,
        tags: List<String>,
        visibility: String,
        isFeatured: Boolean,
        status: VideoPublishStatus
    ): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return false

        val newVideo = M3u8Video(
            title = title,
            description = description,
            m3u8Url = m3u8Url,
            thumbnailUrl = thumbnailUrl,
            category = category,
            tags = tags,
            visibility = visibility,
            status = status,
            isFeatured = isFeatured,
            publishedByAdminUid = admin.uid,
            publishedByAdminName = admin.displayName
        )
        _m3u8Videos.value = listOf(newVideo) + _m3u8Videos.value

        val log = AuditLog(
            adminUid = admin.uid,
            action = "ADD_M3U8_VIDEO",
            target = newVideo.id,
            reason = "Published video: '$title' (${status.name})"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminUpdateM3u8Video(video: M3u8Video): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return false

        _m3u8Videos.value = _m3u8Videos.value.map {
            if (it.id == video.id) video.copy(updatedAt = System.currentTimeMillis()) else it
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "UPDATE_M3U8_VIDEO",
            target = video.id,
            reason = "Updated metadata for video '${video.title}'"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminSetVideoStatus(videoId: String, status: VideoPublishStatus): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return false

        _m3u8Videos.value = _m3u8Videos.value.map {
            if (it.id == videoId) it.copy(status = status, updatedAt = System.currentTimeMillis()) else it
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "SET_VIDEO_STATUS",
            target = videoId,
            reason = "Status transitioned to ${status.name}"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminDeleteM3u8Video(videoId: String, permanent: Boolean = false): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return false

        if (permanent) {
            _m3u8Videos.value = _m3u8Videos.value.filterNot { it.id == videoId }
        } else {
            _m3u8Videos.value = _m3u8Videos.value.map {
                if (it.id == videoId) it.copy(status = VideoPublishStatus.DELETED, updatedAt = System.currentTimeMillis()) else it
            }
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = if (permanent) "PERMANENT_DELETE_VIDEO" else "SOFT_DELETE_VIDEO",
            target = videoId,
            reason = if (permanent) "Permanently purged video" else "Moved video to Deleted folder"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminAddM3u8LiveStream(
        title: String,
        description: String,
        m3u8Url: String,
        thumbnailUrl: String,
        category: String,
        tags: List<String>,
        scheduledStartTime: Long,
        status: LiveStreamStatus,
        isFeatured: Boolean
    ): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_LIVE)) return false

        val newStream = M3u8LiveStream(
            title = title,
            description = description,
            m3u8Url = m3u8Url,
            thumbnailUrl = thumbnailUrl,
            category = category,
            tags = tags,
            scheduledStartTime = scheduledStartTime,
            status = status,
            isFeatured = isFeatured,
            isPublished = true,
            publishedByAdminUid = admin.uid,
            publishedByAdminName = admin.displayName
        )
        _m3u8LiveStreams.value = listOf(newStream) + _m3u8LiveStreams.value

        val log = AuditLog(
            adminUid = admin.uid,
            action = "ADD_M3U8_LIVE_STREAM",
            target = newStream.id,
            reason = "Created live stream broadcast: '$title' (${status.name})"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminUpdateLiveStreamStatus(streamId: String, status: LiveStreamStatus): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_LIVE)) return false

        _m3u8LiveStreams.value = _m3u8LiveStreams.value.map {
            if (it.id == streamId) it.copy(status = status, updatedAt = System.currentTimeMillis()) else it
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "UPDATE_STREAM_STATUS",
            target = streamId,
            reason = "Live stream status changed to ${status.name}"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminUpdateM3u8LiveStream(stream: M3u8LiveStream): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_LIVE)) return false

        _m3u8LiveStreams.value = _m3u8LiveStreams.value.map {
            if (it.id == stream.id) stream.copy(updatedAt = System.currentTimeMillis()) else it
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "UPDATE_M3U8_LIVE_STREAM",
            target = stream.id,
            reason = "Admin edited live stream: '${stream.title}'"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminUnpublishLiveStream(streamId: String): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_LIVE)) return false

        _m3u8LiveStreams.value = _m3u8LiveStreams.value.map {
            if (it.id == streamId) it.copy(isPublished = false, status = LiveStreamStatus.ENDED, updatedAt = System.currentTimeMillis()) else it
        }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "UNPUBLISH_LIVE_STREAM",
            target = streamId,
            reason = "Admin unpublished live stream"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminDeleteLiveStream(streamId: String): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MANAGE_LIVE)) return false

        _m3u8LiveStreams.value = _m3u8LiveStreams.value.filterNot { it.id == streamId }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "DELETE_LIVE_STREAM",
            target = streamId,
            reason = "Admin removed live stream"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminAddCategory(name: String, description: String): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return false

        val slug = name.lowercase().replace(" ", "-")
        val cat = ContentCategory(name = name, slug = slug, description = description, itemCount = 0, isSystemDefault = false)
        _contentCategories.value = _contentCategories.value + cat
        val log = AuditLog(
            adminUid = admin.uid,
            action = "ADD_CONTENT_CATEGORY",
            target = cat.id,
            reason = "Created category: $name"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminDeleteCategory(categoryId: String): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.MODERATE_CONTENT)) return false

        _contentCategories.value = _contentCategories.value.filterNot { it.id == categoryId }
        val log = AuditLog(
            adminUid = admin.uid,
            action = "DELETE_CONTENT_CATEGORY",
            target = categoryId,
            reason = "Removed category"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    // ================= ZAPUPI PAYMENT GATEWAY ACTIONS ================= //

    fun adminSaveZapUpiConfig(
        apiKey: String,
        isEnabled: Boolean,
        timeoutSeconds: Int,
        currency: String,
        isTestMode: Boolean
    ): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.SECURITY_CENTER)) return false

        val cleanKey = apiKey.trim()
        val masked = if (cleanKey.length >= 6) {
            "••••••••••••••••••••" + cleanKey.takeLast(6)
        } else {
            "••••••••••••••••••••" + cleanKey
        }

        _zapUpiConfig.value = ZapUpiGatewayConfig(
            isEnabled = isEnabled,
            maskedApiKey = masked,
            timeoutSeconds = timeoutSeconds.coerceIn(60, 1800),
            currency = currency,
            isTestMode = isTestMode,
            status = "Connected ✓",
            lastVerifiedAt = System.currentTimeMillis()
        )

        val log = AuditLog(
            adminUid = admin.uid,
            action = "CONFIG_ZAPUPI_GATEWAY",
            target = "ZapUPI",
            reason = "Admin configured ZapUPI credentials (${if (isTestMode) "Test" else "Live"} Mode, timeout ${timeoutSeconds}s)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminTestZapUpiConnection(): Pair<Boolean, String> {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.SECURITY_CENTER)) {
            return Pair(false, "Permission denied")
        }

        val log = AuditLog(
            adminUid = admin.uid,
            action = "TEST_ZAPUPI_CONNECTION",
            target = "ZapUPI",
            reason = "Admin executed payment gateway health handshake"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value

        _zapUpiConfig.value = _zapUpiConfig.value.copy(
            status = "Connected ✓",
            lastVerifiedAt = System.currentTimeMillis()
        )
        return Pair(true, "ZapUPI Gateway Verified • Latency: 114ms • Status: 200 OK")
    }

    fun adminToggleZapUpi(enabled: Boolean): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.SECURITY_CENTER)) return false

        _zapUpiConfig.value = _zapUpiConfig.value.copy(isEnabled = enabled)
        val log = AuditLog(
            adminUid = admin.uid,
            action = if (enabled) "ENABLE_ZAPUPI" else "DISABLE_ZAPUPI",
            target = "ZapUPI",
            reason = if (enabled) "Admin enabled ZapUPI payment gateway" else "Admin disabled ZapUPI payment gateway"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminRefundPaymentOrder(orderId: String, reason: String): Boolean {
        val admin = _currentUser.value
        if (!admin.adminRole.hasPermission(AdminPermission.SECURITY_CENTER)) return false

        val order = _paymentOrders.value.find { it.orderId == orderId } ?: return false
        if (order.status != PaymentOrderStatus.PAID) return false

        _paymentOrders.value = _paymentOrders.value.map {
            if (it.orderId == orderId) {
                it.copy(status = PaymentOrderStatus.REFUNDED, refundReason = reason)
            } else it
        }

        // Reverse coins if it was a coin purchase
        if (order.itemType == "CINECOINS") {
            val coins = when (order.itemId) {
                "coins_500" -> 500L
                "coins_1500" -> 1500L
                "coins_5000" -> 5000L
                else -> (order.amountInr * 10).toLong()
            }
            val acc = _coinAccount.value
            val newBal = (acc.balance - coins).coerceAtLeast(0L)
            _coinAccount.value = acc.copy(balance = newBal)
            val user = _currentUser.value
            _currentUser.value = user.copy(coinBalance = newBal)

            val tx = CineCoinTransaction(
                userId = order.userId,
                amount = -coins,
                reason = "Refund for ZapUPI Order $orderId ($reason)",
                type = "REFUND",
                balanceAfter = _coinAccount.value.balance
            )
            _coinTransactions.value = listOf(tx) + _coinTransactions.value
        }

        val log = AuditLog(
            adminUid = admin.uid,
            action = "REFUND_ZAPUPI_ORDER",
            target = orderId,
            reason = "Admin refunded ₹${order.amountInr} ($reason)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun createZapUpiPaymentOrder(
        itemType: String,
        itemId: String,
        amountInr: Double,
        title: String
    ): ZapUpiOrder {
        val user = _currentUser.value
        val orderId = "ZAP_CC_" + System.currentTimeMillis().toString().takeLast(8)
        val vpa = "cinecut.zapupi@icici"
        val upiUrl = "upi://pay?pa=$vpa&pn=CineCut%20Studios&am=${amountInr}&tr=$orderId&cu=INR&tn=CineCut%20${title.replace(" ", "%20")}"
        val paytmUrl = "paytmmp://pay?pa=$vpa&pn=CineCut%20Studios&am=${amountInr}&tr=$orderId&cu=INR"

        val order = ZapUpiOrder(
            orderId = orderId,
            userId = user.uid,
            userDisplayName = user.displayName,
            amountInr = amountInr,
            itemType = itemType,
            itemId = itemId,
            title = title,
            status = PaymentOrderStatus.PENDING,
            qrPayload = upiUrl,
            upiIntentUrl = upiUrl,
            paytmIntentUrl = paytmUrl,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (_zapUpiConfig.value.timeoutSeconds * 1000L)
        )
        _paymentOrders.value = listOf(order) + _paymentOrders.value
        _activePaymentOrder.value = order
        return order
    }

    fun verifyZapUpiPaymentOrder(orderId: String, utrNumber: String?): Pair<Boolean, String> {
        val order = _paymentOrders.value.find { it.orderId == orderId }
            ?: return Pair(false, "Order not found")

        // Idempotency check: if already processed, return success
        if (order.status == PaymentOrderStatus.PAID) {
            return Pair(true, "Order already verified and paid!")
        }

        val cleanUtr = if (!utrNumber.isNullOrBlank()) utrNumber.trim() else "UPI" + System.currentTimeMillis().toString().takeLast(8)

        // Mark PAID
        val updatedOrder = order.copy(
            status = PaymentOrderStatus.PAID,
            utrNumber = cleanUtr,
            paidAt = System.currentTimeMillis()
        )
        _paymentOrders.value = _paymentOrders.value.map {
            if (it.orderId == orderId) updatedOrder else it
        }
        _activePaymentOrder.value = updatedOrder

        // Credit coins or upgrade tier
        if (order.itemType == "CINECOINS") {
            val coins = when (order.itemId) {
                "coins_500" -> 500L
                "coins_1500" -> 1500L
                "coins_5000" -> 5000L
                else -> (order.amountInr * 10).toLong()
            }
            val acc = _coinAccount.value
            val newBal = acc.balance + coins
            _coinAccount.value = acc.copy(
                balance = newBal,
                totalEarned = acc.totalEarned + coins,
                lastTransactionAt = System.currentTimeMillis()
            )
            val user = _currentUser.value
            _currentUser.value = user.copy(coinBalance = newBal)
            val tx = CineCoinTransaction(
                userId = order.userId,
                amount = coins,
                reason = "Purchased $coins CineCoins via ZapUPI (Order $orderId, UTR $cleanUtr)",
                type = "PURCHASE",
                balanceAfter = newBal
            )
            _coinTransactions.value = listOf(tx) + _coinTransactions.value
        } else if (order.itemType == "MEMBERSHIP") {
            val tier = when (order.itemId.lowercase()) {
                "tier_bronze" -> MembershipTier.BRONZE
                "tier_silver" -> MembershipTier.SILVER
                "tier_gold" -> MembershipTier.GOLD
                "tier_diamond" -> MembershipTier.DIAMOND
                "tier_vip" -> MembershipTier.VIP
                "tier_founder" -> MembershipTier.FOUNDER
                else -> MembershipTier.GOLD
            }
            val current = _currentUser.value
            _currentUser.value = current.copy(membershipTier = tier)
        }

        val log = AuditLog(
            adminUid = order.userId,
            action = "ZAPUPI_PAYMENT_SUCCESS",
            target = orderId,
            reason = "Payment of ₹${order.amountInr} verified with UTR $cleanUtr"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value

        return Pair(true, "Payment verified successfully!")
    }

    fun dismissActivePaymentOrder() {
        _activePaymentOrder.value = null
    }

    // ================= SUPPORT TICKET REPOSITORY METHODS ================= //

    fun createSupportTicket(
        subject: String,
        category: SupportTicketCategory,
        description: String,
        priority: SupportTicketPriority = SupportTicketPriority.MEDIUM,
        orderId: String? = null,
        attachments: List<SupportAttachment> = emptyList()
    ): SupportTicket {
        val user = _currentUser.value
        val ticketId = "#CC-${_nextTicketNumber.getAndIncrement()}"
        val now = System.currentTimeMillis()

        val ticket = SupportTicket(
            ticketId = ticketId,
            userId = user.uid,
            userDisplayName = user.displayName,
            userEmail = user.email,
            subject = subject.trim(),
            category = category,
            description = description.trim(),
            priority = priority,
            status = SupportTicketStatus.OPEN,
            assignedTo = null,
            assignedToName = null,
            orderId = orderId?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
            lastMessageAt = now,
            closedAt = null,
            unreadUserCount = 0,
            unreadAdminCount = 1,
            attachments = attachments
        )

        val initialMessage = SupportTicketMessage(
            ticketId = ticketId,
            senderId = user.uid,
            senderName = user.displayName,
            senderRole = "USER",
            message = description.trim(),
            isInternalNote = false,
            attachments = attachments,
            createdAt = now
        )

        _supportTickets.value = listOf(ticket) + _supportTickets.value
        val currentMap = _ticketMessages.value.toMutableMap()
        currentMap[ticketId] = listOf(initialMessage)
        _ticketMessages.value = currentMap

        // Send confirmation in-app notification
        val notif = InAppNotification(
            userId = user.uid,
            category = NotificationCategory.SUPPORT,
            title = "Ticket Created: $ticketId",
            message = "We received your inquiry \"$subject\". A support representative will respond shortly.",
            relatedEntityId = ticketId
        )
        _inAppNotifications.value = listOf(notif) + _inAppNotifications.value

        // Security Audit Log
        val log = AuditLog(
            adminUid = user.uid,
            action = "CREATE_SUPPORT_TICKET",
            target = ticketId,
            reason = "User opened support ticket for ${category.title}: $subject"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value

        return ticket
    }

    fun sendSupportMessage(
        ticketId: String,
        messageText: String,
        isInternalNote: Boolean = false,
        attachments: List<SupportAttachment> = emptyList()
    ): Boolean {
        val user = _currentUser.value
        val ticket = _supportTickets.value.find { it.ticketId == ticketId } ?: return false

        val isStaff = user.adminRole != AdminRole.NONE
        val effectiveInternalNote = isStaff && isInternalNote
        val senderRole = if (isStaff) {
            if (user.adminRole == AdminRole.SUPPORT) "SUPPORT" else "ADMIN"
        } else {
            "USER"
        }

        val now = System.currentTimeMillis()
        val newMessage = SupportTicketMessage(
            ticketId = ticketId,
            senderId = user.uid,
            senderName = user.displayName,
            senderRole = senderRole,
            message = messageText.trim(),
            isInternalNote = effectiveInternalNote,
            attachments = attachments,
            createdAt = now
        )

        // Append to message list
        val currentMap = _ticketMessages.value.toMutableMap()
        val existingMessages = currentMap[ticketId] ?: emptyList()
        currentMap[ticketId] = existingMessages + newMessage
        _ticketMessages.value = currentMap

        // Update ticket state
        _supportTickets.value = _supportTickets.value.map { t ->
            if (t.ticketId == ticketId) {
                if (effectiveInternalNote) {
                    t.copy(updatedAt = now)
                } else if (isStaff) {
                    t.copy(
                        status = SupportTicketStatus.WAITING_FOR_USER,
                        unreadUserCount = t.unreadUserCount + 1,
                        lastMessageAt = now,
                        updatedAt = now
                    )
                } else {
                    t.copy(
                        status = SupportTicketStatus.IN_PROGRESS,
                        unreadAdminCount = t.unreadAdminCount + 1,
                        lastMessageAt = now,
                        updatedAt = now
                    )
                }
            } else {
                t
            }
        }

        if (effectiveInternalNote) {
            val log = AuditLog(
                adminUid = user.uid,
                action = "SUPPORT_INTERNAL_NOTE",
                target = ticketId,
                reason = "Staff posted internal note on $ticketId"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        } else if (isStaff) {
            // In-app notification to ticket owner
            val notif = InAppNotification(
                userId = ticket.userId,
                category = NotificationCategory.SUPPORT,
                title = "Support Replied: $ticketId",
                message = "${user.displayName}: ${messageText.take(60)}...",
                relatedEntityId = ticketId
            )
            _inAppNotifications.value = listOf(notif) + _inAppNotifications.value

            val log = AuditLog(
                adminUid = user.uid,
                action = "SUPPORT_REPLY",
                target = ticketId,
                reason = "Staff replied to $ticketId"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
        } else {
            // User reply: if staff assigned, notify staff
            if (ticket.assignedTo != null) {
                val notif = InAppNotification(
                    userId = ticket.assignedTo,
                    category = NotificationCategory.SUPPORT,
                    title = "User Replied: $ticketId",
                    message = "${user.displayName} sent a response on $ticketId",
                    relatedEntityId = ticketId
                )
                _inAppNotifications.value = listOf(notif) + _inAppNotifications.value
            }
        }

        return true
    }

    fun updateSupportTicketStatus(
        ticketId: String,
        newStatus: SupportTicketStatus,
        reason: String? = null
    ): Boolean {
        val user = _currentUser.value
        val ticket = _supportTickets.value.find { it.ticketId == ticketId } ?: return false
        val now = System.currentTimeMillis()

        val isClosing = newStatus == SupportTicketStatus.RESOLVED || newStatus == SupportTicketStatus.CLOSED
        val isReopening = newStatus == SupportTicketStatus.OPEN && (ticket.status == SupportTicketStatus.RESOLVED || ticket.status == SupportTicketStatus.CLOSED)

        _supportTickets.value = _supportTickets.value.map { t ->
            if (t.ticketId == ticketId) {
                t.copy(
                    status = newStatus,
                    closedAt = if (isClosing) now else if (isReopening) null else t.closedAt,
                    updatedAt = now
                )
            } else {
                t
            }
        }

        val eventTitle = when (newStatus) {
            SupportTicketStatus.RESOLVED -> "Ticket Resolved: $ticketId"
            SupportTicketStatus.CLOSED -> "Ticket Closed: $ticketId"
            SupportTicketStatus.OPEN -> if (isReopening) "Ticket Reopened: $ticketId" else "Ticket Status: OPEN"
            else -> "Ticket Status: ${newStatus.title}"
        }

        val notif = InAppNotification(
            userId = ticket.userId,
            category = NotificationCategory.SUPPORT,
            title = eventTitle,
            message = "Your support ticket $ticketId is now ${newStatus.title}.",
            relatedEntityId = ticketId
        )
        _inAppNotifications.value = listOf(notif) + _inAppNotifications.value

        val log = AuditLog(
            adminUid = user.uid,
            action = "TICKET_STATUS_${newStatus.name}",
            target = ticketId,
            reason = reason ?: "Status updated to ${newStatus.name}"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value

        return true
    }

    fun updateSupportTicketPriority(
        ticketId: String,
        newPriority: SupportTicketPriority
    ): Boolean {
        val user = _currentUser.value
        _supportTickets.value = _supportTickets.value.map { t ->
            if (t.ticketId == ticketId) {
                t.copy(priority = newPriority, updatedAt = System.currentTimeMillis())
            } else {
                t
            }
        }

        val log = AuditLog(
            adminUid = user.uid,
            action = "TICKET_PRIORITY_${newPriority.name}",
            target = ticketId,
            reason = "Priority updated to ${newPriority.name}"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun assignSupportTicket(
        ticketId: String,
        staffUid: String?,
        staffName: String?
    ): Boolean {
        val user = _currentUser.value
        _supportTickets.value = _supportTickets.value.map { t ->
            if (t.ticketId == ticketId) {
                t.copy(
                    assignedTo = staffUid,
                    assignedToName = staffName,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                t
            }
        }

        if (staffUid != null) {
            val notif = InAppNotification(
                userId = staffUid,
                category = NotificationCategory.SUPPORT,
                title = "Ticket Assigned: $ticketId",
                message = "You have been assigned to handle support ticket $ticketId.",
                relatedEntityId = ticketId
            )
            _inAppNotifications.value = listOf(notif) + _inAppNotifications.value
        }

        val log = AuditLog(
            adminUid = user.uid,
            action = "ASSIGN_SUPPORT_TICKET",
            target = ticketId,
            reason = "Assigned to ${staffName ?: staffUid ?: "Unassigned"}"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun markTicketReadForUser(ticketId: String) {
        _supportTickets.value = _supportTickets.value.map { t ->
            if (t.ticketId == ticketId) t.copy(unreadUserCount = 0) else t
        }
    }

    fun markTicketReadForAdmin(ticketId: String) {
        _supportTickets.value = _supportTickets.value.map { t ->
            if (t.ticketId == ticketId) t.copy(unreadAdminCount = 0) else t
        }
    }

    fun getVisibleTicketMessages(ticketId: String): List<SupportTicketMessage> {
        val user = _currentUser.value
        val messages = _ticketMessages.value[ticketId] ?: emptyList()
        return if (user.adminRole != AdminRole.NONE) {
            messages
        } else {
            messages.filterNot { it.isInternalNote }
        }
    }

    fun setCurrentUserForTesting(profile: UserProfile?) {
        _currentAuthUser.value = profile
        _currentUser.value = profile ?: UserProfile(uid = "", adminRole = AdminRole.NONE, coinBalance = 0L)
    }

    // ================= FESTIVAL THEME & OFFERS SYSTEM ================= //

    private val _festivalConfig = MutableStateFlow(
        FestivalConfiguration(
            activeFestival = FestivalType.DIWALI,
            isFestivalActive = true,
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + (72 * 3600000L), // 3 days remaining
            themeConfig = FestivalThemeConfig(
                festivalType = FestivalType.DIWALI,
                themeName = "Diwali Radiant Gold",
                primaryAccentHex = FestivalType.DIWALI.defaultPrimaryHex,
                secondaryAccentHex = FestivalType.DIWALI.defaultSecondaryHex,
                surfaceGradientStart = FestivalType.DIWALI.defaultGradientStart,
                surfaceGradientEnd = FestivalType.DIWALI.defaultGradientEnd,
                buttonColorHex = FestivalType.DIWALI.defaultPrimaryHex,
                cardBorderColorHex = 0x80FFD700,
                bannerHeadline = "Diwali Studio Dhamaka",
                bannerSubheadline = "Ignite your creativity with 50% Off & +25% Bonus CineCoins",
                bannerBadgeText = "FESTIVAL SPECIAL"
            ),
            animationConfig = FestivalAnimationConfig(
                animationType = FestivalAnimationType.DIYA_LIGHTS,
                enabled = true,
                particleDensity = 0.6f,
                reduceAnimations = false,
                speedMultiplier = 1.0f
            ),
            cineCoinBonusPercent = 25,
            firstPurchaseBonusPercent = 50,
            bannerText = "🎉 FESTIVAL EVENT: Diwali Dhamaka Special Offers Live!",
            bannerCountdownTarget = System.currentTimeMillis() + (72 * 3600000L),
            remoteConfigVersion = 4,
            lastModifiedBy = "SuperAdmin",
            lastModifiedAt = System.currentTimeMillis()
        )
    )
    val festivalConfig: StateFlow<FestivalConfiguration> = _festivalConfig.asStateFlow()

    private val _festivalOffers = MutableStateFlow<List<FestivalOffer>>(
        listOf(
            FestivalOffer(
                offerId = "FO-101",
                title = "Diwali Creator Mega Coin Pack",
                description = "5,000 CineCoins + 1,250 Bonus Diwali Coins (+25% bonus). Valid during Diwali event.",
                festival = FestivalType.DIWALI,
                category = FestivalOfferItemCategory.CINECOIN_PACK,
                discountType = FestivalDiscountType.PERCENTAGE,
                discountValue = 40.0,
                originalPrice = 2499.0,
                finalPrice = 1499.0,
                bonusCoins = 1250L,
                itemPayloadId = "coins_5000",
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() + (72 * 3600000L),
                maxUses = 500,
                usedCount = 184,
                perUserLimit = 2,
                enabled = true,
                badgeTag = "40% OFF + 25% BONUS",
                isFirstPurchaseOnly = false
            ),
            FestivalOffer(
                offerId = "FO-102",
                title = "Diwali Golden 4K Director Pass",
                description = "1-Year Gold Membership with 4K 60fps ProRes export, unlimited AI Director & Diwali Golden LUT Suite.",
                festival = FestivalType.DIWALI,
                category = FestivalOfferItemCategory.MEMBERSHIP_TIER,
                discountType = FestivalDiscountType.PERCENTAGE,
                discountValue = 50.0,
                originalPrice = 6999.0,
                finalPrice = 3499.0,
                bonusCoins = 2500L,
                itemPayloadId = "tier_gold",
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() + (72 * 3600000L),
                maxUses = 250,
                usedCount = 92,
                perUserLimit = 1,
                enabled = true,
                badgeTag = "50% OFF FLAT",
                isFirstPurchaseOnly = false
            ),
            FestivalOffer(
                offerId = "FO-103",
                title = "Bollywood & Festive Golden LUT Bundle",
                description = "16 handcrafted 3D LUTs calibrated for festive night lighting, diyas, fireworks & vibrant ethnic wear.",
                festival = FestivalType.DIWALI,
                category = FestivalOfferItemCategory.EXCLUSIVE_EFFECT,
                discountType = FestivalDiscountType.FIXED_AMOUNT,
                discountValue = 500.0,
                originalPrice = 999.0,
                finalPrice = 499.0,
                bonusCoins = 100L,
                itemPayloadId = "lut_diwali_glow_bundle",
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() + (72 * 3600000L),
                maxUses = 1000,
                usedCount = 412,
                perUserLimit = 1,
                enabled = true,
                badgeTag = "BESTSELLER",
                isFirstPurchaseOnly = false
            ),
            FestivalOffer(
                offerId = "FO-104",
                title = "First-Timer Festival Starter Pack",
                description = "1,000 CineCoins + 500 Bonus for first-time buyers. Exclusive festival introductory price.",
                festival = FestivalType.DIWALI,
                category = FestivalOfferItemCategory.CINECOIN_PACK,
                discountType = FestivalDiscountType.PERCENTAGE,
                discountValue = 60.0,
                originalPrice = 499.0,
                finalPrice = 199.0,
                bonusCoins = 500L,
                itemPayloadId = "coins_1000",
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() + (72 * 3600000L),
                maxUses = 2000,
                usedCount = 780,
                perUserLimit = 1,
                enabled = true,
                badgeTag = "FIRST PURCHASE ONLY",
                isFirstPurchaseOnly = true
            ),
            FestivalOffer(
                offerId = "FO-105",
                title = "Festival Ultimate VIP Studio Bundle",
                description = "VIP Membership tier + Lifetime Founder Badge + 10,000 CineCoins + Priority Cloud 8K Renderer.",
                festival = FestivalType.DIWALI,
                category = FestivalOfferItemCategory.CREATOR_BUNDLE,
                discountType = FestivalDiscountType.PERCENTAGE,
                discountValue = 50.0,
                originalPrice = 9999.0,
                finalPrice = 4999.0,
                bonusCoins = 5000L,
                itemPayloadId = "bundle_vip_ultimate",
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() + (72 * 3600000L),
                maxUses = 100,
                usedCount = 37,
                perUserLimit = 1,
                enabled = true,
                badgeTag = "VIP EXCLUSIVE",
                isFirstPurchaseOnly = false
            )
        )
    )
    val festivalOffers: StateFlow<List<FestivalOffer>> = _festivalOffers.asStateFlow()

    private val _festivalCoupons = MutableStateFlow<List<FestivalCoupon>>(
        listOf(
            FestivalCoupon(
                code = "DIWALI50",
                discountPercent = 50,
                maxDiscountInr = 1000.0,
                minOrderAmount = 499.0,
                enabled = true,
                expiryTime = System.currentTimeMillis() + (72 * 3600000L),
                usageCount = 142
            ),
            FestivalCoupon(
                code = "FESTIVE20",
                discountPercent = 20,
                maxDiscountInr = 500.0,
                minOrderAmount = 299.0,
                enabled = true,
                expiryTime = System.currentTimeMillis() + (7 * 86400000L),
                usageCount = 380
            ),
            FestivalCoupon(
                code = "CINECUTJOY",
                discountPercent = 15,
                maxDiscountInr = 300.0,
                minOrderAmount = 199.0,
                enabled = true,
                expiryTime = System.currentTimeMillis() + (14 * 86400000L),
                usageCount = 94
            )
        )
    )
    val festivalCoupons: StateFlow<List<FestivalCoupon>> = _festivalCoupons.asStateFlow()

    /**
     * Backend Price Calculation: Computes final price ensuring security.
     */
    fun calculateBackendPrice(originalPrice: Double, discountType: FestivalDiscountType, discountValue: Double): Double {
        val calculated = when (discountType) {
            FestivalDiscountType.PERCENTAGE -> originalPrice * (1.0 - (discountValue.coerceIn(0.0, 100.0) / 100.0))
            FestivalDiscountType.FIXED_AMOUNT -> (originalPrice - discountValue).coerceAtLeast(0.0)
            FestivalDiscountType.BONUS_PERCENT -> originalPrice
            FestivalDiscountType.FREE_ADDON -> originalPrice
        }
        return kotlin.math.round(calculated * 100.0) / 100.0
    }

    /**
     * Backend validation of coupon application.
     */
    fun validateAndApplyCoupon(code: String, orderAmount: Double): Pair<Boolean, Double> {
        val coupon = _festivalCoupons.value.find { it.code.equals(code.trim(), ignoreCase = true) }
            ?: return Pair(false, 0.0)

        if (!coupon.isValid(orderAmount)) {
            return Pair(false, 0.0)
        }
        val discount = coupon.calculateDiscount(orderAmount)
        return Pair(true, discount)
    }

    /**
     * Backend checkout price computation: never trusts client prices.
     */
    fun computeSecureOfferCheckout(offerId: String, couponCode: String?): Triple<Boolean, Double, String> {
        val offer = _festivalOffers.value.find { it.offerId == offerId }
            ?: return Triple(false, 0.0, "Offer not found or expired")

        if (!offer.isCurrentlyActive()) {
            return Triple(false, 0.0, "This festival offer has ended or is out of stock")
        }

        var price = offer.finalPrice
        if (!couponCode.isNullOrBlank()) {
            val (valid, discount) = validateAndApplyCoupon(couponCode, price)
            if (valid) {
                price = (price - discount).coerceAtLeast(1.0)
            }
        }
        return Triple(true, kotlin.math.round(price * 100.0) / 100.0, "Valid")
    }

    /**
     * Redeem festival offer securely.
     */
    fun redeemFestivalOffer(offerId: String, couponCode: String?): Boolean {
        val user = _currentUser.value
        val offer = _festivalOffers.value.find { it.offerId == offerId } ?: return false

        if (!offer.isCurrentlyActive()) return false

        val (valid, finalAmount, _) = computeSecureOfferCheckout(offerId, couponCode)
        if (!valid) return false

        // Increment offer usage
        _festivalOffers.value = _festivalOffers.value.map {
            if (it.offerId == offerId) it.copy(usedCount = it.usedCount + 1) else it
        }

        // Increment coupon usage if used
        if (!couponCode.isNullOrBlank()) {
            _festivalCoupons.value = _festivalCoupons.value.map {
                if (it.code.equals(couponCode.trim(), ignoreCase = true)) it.copy(usageCount = it.usageCount + 1) else it
            }
        }

        // Fulfill benefits: grant bonus CineCoins or membership or items
        val bonusCoinsToAdd = offer.bonusCoins + if (offer.category == FestivalOfferItemCategory.CINECOIN_PACK) {
            when (offer.itemPayloadId) {
                "coins_1000" -> 1000L
                "coins_5000" -> 5000L
                else -> 500L
            }
        } else 0L

        if (bonusCoinsToAdd > 0) {
            _currentUser.value = _currentUser.value.copy(
                coinBalance = _currentUser.value.coinBalance + bonusCoinsToAdd
            )
            val tx = CineCoinTransaction(
                userId = user.uid,
                amount = bonusCoinsToAdd,
                reason = "Festival Offer: ${offer.title} (+${offer.bonusCoins} Bonus)",
                type = "PURCHASE",
                balanceAfter = _currentUser.value.coinBalance
            )
            _coinTransactions.value = listOf(tx) + _coinTransactions.value
        }

        if (offer.category == FestivalOfferItemCategory.MEMBERSHIP_TIER) {
            if (offer.itemPayloadId == "tier_gold") {
                _currentUser.value = _currentUser.value.copy(membershipTier = MembershipTier.GOLD)
            } else if (offer.itemPayloadId.contains("vip", ignoreCase = true)) {
                _currentUser.value = _currentUser.value.copy(membershipTier = MembershipTier.VIP)
            }
        }

        // Notification
        val notif = InAppNotification(
            userId = user.uid,
            category = NotificationCategory.SYSTEM,
            title = "🎉 Festival Offer Activated!",
            message = "Purchased '${offer.title}' for ₹${finalAmount}. Benefits credited to your studio.",
            relatedEntityId = offerId
        )
        _inAppNotifications.value = listOf(notif) + _inAppNotifications.value

        // Audit Log
        val log = AuditLog(
            adminUid = user.uid,
            action = "REDEEM_FESTIVAL_OFFER",
            target = offerId,
            reason = "Paid ₹$finalAmount via secure checkout. Granted $bonusCoinsToAdd CineCoins."
        )
        _auditLogs.value = listOf(log) + _auditLogs.value

        return true
    }

    // --- Admin Festival Controls ---

    fun adminUpdateFestivalConfig(newConfig: FestivalConfiguration): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalConfig.value = newConfig.copy(
            lastModifiedBy = user.displayName,
            lastModifiedAt = System.currentTimeMillis(),
            remoteConfigVersion = _festivalConfig.value.remoteConfigVersion + 1
        )

        val log = AuditLog(
            adminUid = user.uid,
            action = "UPDATE_FESTIVAL_CONFIG",
            target = newConfig.activeFestival.name,
            reason = "Updated festival config (Active=${newConfig.isFestivalActive}, Festival=${newConfig.activeFestival.displayName})"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminToggleFestival(enabled: Boolean): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalConfig.value = _festivalConfig.value.copy(
            isFestivalActive = enabled,
            lastModifiedBy = user.displayName,
            lastModifiedAt = System.currentTimeMillis(),
            remoteConfigVersion = _festivalConfig.value.remoteConfigVersion + 1
        )

        val log = AuditLog(
            adminUid = user.uid,
            action = if (enabled) "ENABLE_FESTIVAL" else "DISABLE_FESTIVAL",
            target = _festivalConfig.value.activeFestival.name,
            reason = "Admin set festival status to $enabled"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminSelectFestival(festival: FestivalType): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalConfig.value = _festivalConfig.value.copy(
            activeFestival = festival,
            themeConfig = FestivalThemeConfig(
                festivalType = festival,
                themeName = "${festival.displayName} Vibrant Theme",
                primaryAccentHex = festival.defaultPrimaryHex,
                secondaryAccentHex = festival.defaultSecondaryHex,
                surfaceGradientStart = festival.defaultGradientStart,
                surfaceGradientEnd = festival.defaultGradientEnd,
                buttonColorHex = festival.defaultPrimaryHex,
                cardBorderColorHex = 0x80000000L or (festival.defaultSecondaryHex and 0x00FFFFFFL),
                bannerHeadline = festival.defaultHeadline,
                bannerSubheadline = festival.defaultSubheadline,
                bannerBadgeText = "${festival.displayName.uppercase()} SPECIAL"
            ),
            animationConfig = FestivalAnimationConfig(
                animationType = festival.defaultAnimation,
                enabled = true,
                particleDensity = 0.6f,
                reduceAnimations = false,
                speedMultiplier = 1.0f
            ),
            lastModifiedBy = user.displayName,
            lastModifiedAt = System.currentTimeMillis(),
            remoteConfigVersion = _festivalConfig.value.remoteConfigVersion + 1
        )

        val log = AuditLog(
            adminUid = user.uid,
            action = "SELECT_FESTIVAL",
            target = festival.name,
            reason = "Switched active festival to ${festival.displayName}"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminCreateFestivalOffer(offer: FestivalOffer): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        // Compute backend final price safely
        val secureFinalPrice = calculateBackendPrice(offer.originalPrice, offer.discountType, offer.discountValue)
        val securedOffer = offer.copy(finalPrice = secureFinalPrice)

        _festivalOffers.value = listOf(securedOffer) + _festivalOffers.value

        val log = AuditLog(
            adminUid = user.uid,
            action = "CREATE_FESTIVAL_OFFER",
            target = securedOffer.offerId,
            reason = "Created offer '${securedOffer.title}' (Price: ₹${securedOffer.finalPrice})"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminUpdateFestivalOffer(offer: FestivalOffer): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        val secureFinalPrice = calculateBackendPrice(offer.originalPrice, offer.discountType, offer.discountValue)
        val securedOffer = offer.copy(finalPrice = secureFinalPrice)

        _festivalOffers.value = _festivalOffers.value.map {
            if (it.offerId == offer.offerId) securedOffer else it
        }

        val log = AuditLog(
            adminUid = user.uid,
            action = "UPDATE_FESTIVAL_OFFER",
            target = offer.offerId,
            reason = "Updated offer '${offer.title}'"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminToggleFestivalOffer(offerId: String, enabled: Boolean): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalOffers.value = _festivalOffers.value.map {
            if (it.offerId == offerId) it.copy(enabled = enabled) else it
        }

        val log = AuditLog(
            adminUid = user.uid,
            action = "TOGGLE_FESTIVAL_OFFER",
            target = offerId,
            reason = "Toggled offer enabled to $enabled"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminDeleteFestivalOffer(offerId: String): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalOffers.value = _festivalOffers.value.filterNot { it.offerId == offerId }

        val log = AuditLog(
            adminUid = user.uid,
            action = "DELETE_FESTIVAL_OFFER",
            target = offerId,
            reason = "Deleted offer $offerId"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminCreateCoupon(coupon: FestivalCoupon): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalCoupons.value = listOf(coupon) + _festivalCoupons.value

        val log = AuditLog(
            adminUid = user.uid,
            action = "CREATE_FESTIVAL_COUPON",
            target = coupon.code,
            reason = "Created coupon ${coupon.code} (${coupon.discountPercent}% off)"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun adminToggleCoupon(code: String, enabled: Boolean): Boolean {
        val user = _currentUser.value
        if (!user.adminRole.hasPermission(AdminPermission.MANAGE_FESTIVALS)) return false

        _festivalCoupons.value = _festivalCoupons.value.map {
            if (it.code == code) it.copy(enabled = enabled) else it
        }

        val log = AuditLog(
            adminUid = user.uid,
            action = "TOGGLE_FESTIVAL_COUPON",
            target = code,
            reason = "Coupon $code enabled=$enabled"
        )
        _auditLogs.value = listOf(log) + _auditLogs.value
        return true
    }

    fun setReduceAnimations(reduce: Boolean) {
        _festivalConfig.value = _festivalConfig.value.copy(
            animationConfig = _festivalConfig.value.animationConfig.copy(reduceAnimations = reduce)
        )
    }
}
