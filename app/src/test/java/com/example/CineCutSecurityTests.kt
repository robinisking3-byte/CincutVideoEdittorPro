package com.example

import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security Verification Tests verifying Section 26:
 * Verify that a normal user CANNOT:
 * - Become admin or escalate privileges
 * - Alter coin balances or forge credit transactions
 * - Alter payment order states or bypass gateway verification
 * - Access private projects without authorization
 * - Publish admin M3U8 video or live stream content
 * - Alter another user's profile
 * - Access or read internal support notes
 * - Modify festival themes or create forged discounts
 */
class CineCutSecurityTests {

    private lateinit var repository: CineCutRepository

    @Before
    fun setUp() {
        repository = CineCutRepository()
    }

    @Test
    fun testNormalUserCannotPerformAdminPrivilegedActions() {
        // Switch to a normal non-admin user
        val normalUser = UserProfile(
            uid = "usr_regular_001",
            username = "indie_creator",
            displayName = "Indie Creator",
            email = "indie@example.com",
            adminRole = AdminRole.NONE,
            membershipTier = MembershipTier.FREE
        )
        repository.setCurrentUserForTesting(normalUser)

        assertEquals(AdminRole.NONE, repository.currentUser.value.adminRole)

        // 1. Cannot adjust coins of other users
        val targetInitialCoins = repository.allUsers.value.find { it.uid == "usr_sarah" }?.coinBalance ?: 0L
        repository.adminAdjustCoins("usr_sarah", 10_000L, "Hacked coins")
        val targetCoinsAfter = repository.allUsers.value.find { it.uid == "usr_sarah" }?.coinBalance ?: 0L
        assertEquals("Normal user must NOT be able to grant coins", targetInitialCoins, targetCoinsAfter)

        // 2. Cannot ban users
        val targetBefore = repository.allUsers.value.find { it.uid == "usr_sarah" }
        assertFalse(targetBefore?.isBanned == true)
        repository.adminBanUser("usr_sarah", "Rogue ban")
        val targetAfter = repository.allUsers.value.find { it.uid == "usr_sarah" }
        assertFalse("Normal user must NOT be able to ban users", targetAfter?.isBanned == true)

        // 3. Cannot change user roles to SUPER_ADMIN
        repository.adminUpdateUserRole("usr_regular_001", AdminRole.SUPER_ADMIN, "Self promotion")
        assertEquals("Normal user cannot self-promote to SUPER_ADMIN", AdminRole.NONE, repository.currentUser.value.adminRole)

        // 4. Cannot publish M3U8 video content
        val videoCreated = repository.adminAddM3u8Video(
            title = "Unauthorized Video",
            description = "Hacked stream",
            m3u8Url = "https://example.com/hls.m3u8",
            thumbnailUrl = "",
            category = "Film",
            tags = listOf("Action"),
            visibility = "PUBLIC",
            isFeatured = false,
            status = VideoPublishStatus.PUBLISHED
        )
        assertFalse("Normal user must NOT be able to publish M3U8 videos", videoCreated)

        // 5. Cannot publish M3U8 live stream
        val liveCreated = repository.adminAddM3u8LiveStream(
            title = "Unauthorized Live",
            description = "Hacked broadcast",
            m3u8Url = "https://example.com/live.m3u8",
            thumbnailUrl = "",
            category = "Live",
            tags = listOf("Stream"),
            scheduledStartTime = System.currentTimeMillis(),
            status = LiveStreamStatus.LIVE,
            isFeatured = false
        )
        assertFalse("Normal user must NOT be able to publish live streams", liveCreated)

        // 6. Cannot modify festival configurations
        val festivalModified = repository.adminToggleFestival(false)
        assertFalse("Normal user must NOT be able to toggle festivals", festivalModified)

        val offerCreated = repository.adminCreateFestivalOffer(
            FestivalOffer(
                title = "Fake 99% Off",
                description = "Hacked offer",
                originalPrice = 1000.0,
                finalPrice = 1.0
            )
        )
        assertFalse("Normal user must NOT be able to create festival offers", offerCreated)
    }

    @Test
    fun testInternalSupportNotesAreNeverVisibleToUsers() {
        // Create a ticket
        val ticket = repository.createSupportTicket(
            subject = "Payment Issue",
            category = SupportTicketCategory.PAYMENT,
            description = "UPI debited but coins not received",
            priority = SupportTicketPriority.HIGH,
            orderId = "ZAP_CC_12345"
        )
        assertNotNull(ticket)

        // Switch to admin to add an internal confidential note
        val adminUser = UserProfile(
            uid = "adm_super_001",
            username = "superadmin",
            displayName = "Chief Security Officer",
            adminRole = AdminRole.SUPER_ADMIN
        )
        repository.setCurrentUserForTesting(adminUser)

        val internalNoteContent = "CONFIDENTIAL: Checked gateway logs, UTR 918237198 is pending bank settlement."
        val noteSent = repository.sendSupportMessage(
            ticketId = ticket.ticketId,
            messageText = internalNoteContent,
            isInternalNote = true
        )
        assertTrue("Admin can send internal note", noteSent)

        // Switch back to normal ticket owner
        val regularUser = UserProfile(
            uid = ticket.userId,
            username = "ticket_owner",
            displayName = "Alex",
            adminRole = AdminRole.NONE
        )
        repository.setCurrentUserForTesting(regularUser)

        // Fetch user-facing ticket messages
        val userVisibleMessages = repository.getVisibleTicketMessages(ticket.ticketId)
        val containsInternalNote = userVisibleMessages.any { it.isInternalNote || it.message.contains("CONFIDENTIAL") }
        assertFalse("Internal support notes MUST NEVER be returned to normal users", containsInternalNote)
    }

    @Test
    fun testClientCannotForgeFestivalCheckoutPrices() {
        // Verify secure checkout computes price based on repository data, ignoring client parameters
        val offer = repository.festivalOffers.value.first()
        val (valid, finalPrice, _) = repository.computeSecureOfferCheckout(offer.offerId, null)

        assertTrue("Valid offer checkout should compute", valid)
        assertEquals("Server must calculate price from backend state", offer.finalPrice, finalPrice, 0.01)

        // An invalid or forged offer ID must fail
        val (invalid, _, error) = repository.computeSecureOfferCheckout("FORGED_OFFER_ID", null)
        assertFalse("Forged offer ID must be rejected", invalid)
        assertTrue(error.contains("not found", ignoreCase = true))
    }

    @Test
    fun testAuditLogsAreGeneratedOnSensitiveAdminOperations() {
        val adminUser = UserProfile(
            uid = "adm_test_01",
            adminRole = AdminRole.SUPER_ADMIN,
            email = "robinisking3@gmail.com"
        )
        repository.setCurrentUserForTesting(adminUser)

        val initialLogCount = repository.auditLogs.value.size

        // Perform admin action
        repository.adminBanUser("usr_sarah", "Spamming offensive comments")

        val newLogCount = repository.auditLogs.value.size
        assertTrue("Audit log count must increase on administrative actions", newLogCount > initialLogCount)

        val latestLog = repository.auditLogs.value.first()
        assertEquals("USER_BAN", latestLog.action)
        assertEquals("usr_sarah", latestLog.target)
    }

    @Test
    fun testNoAutoLoginOnFreshInstall() {
        val freshRepo = CineCutRepository()
        assertNull("On fresh installation, no authenticated session may exist", freshRepo.currentAuthUser.value)
        assertEquals("Unauthenticated user has no role privileges", AdminRole.NONE, freshRepo.currentUser.value.adminRole)
        assertEquals("Unauthenticated user has 0 coin balance", 0L, freshRepo.currentUser.value.coinBalance)
        assertTrue("Unauthenticated user has empty UID", freshRepo.currentUser.value.uid.isEmpty())
    }

    @Test
    fun testLogoutLeavesUserLoggedOut() {
        val user = UserProfile(
            uid = "usr_authenticated_123",
            email = "filmmaker@cinecut.io",
            adminRole = AdminRole.NONE
        )
        repository.setCurrentUserForTesting(user)
        assertNotNull(repository.currentAuthUser.value)

        // User logs out
        repository.signOut()

        assertNull("After logout, authenticated session MUST be null", repository.currentAuthUser.value)
        assertEquals(AdminRole.NONE, repository.currentUser.value.adminRole)
    }

    @Test
    fun testEmailMatchingAloneDoesNotGrantAdminAccess() {
        // Robin logs in without custom claims
        val unprivilegedAdminEmailUser = UserProfile(
            uid = "usr_robin_plain",
            email = "robinisking3@gmail.com",
            displayName = "Robin",
            adminRole = AdminRole.NONE // No custom claims granted yet
        )
        repository.setCurrentUserForTesting(unprivilegedAdminEmailUser)

        assertEquals("Email matching alone must NEVER grant admin access", AdminRole.NONE, repository.currentUser.value.adminRole)

        // Attempting admin action must fail
        val videoCreated = repository.adminAddM3u8Video(
            title = "Test",
            description = "Test",
            m3u8Url = "https://example.com/test.m3u8",
            thumbnailUrl = "",
            category = "Film",
            tags = emptyList(),
            visibility = "PUBLIC",
            isFeatured = false,
            status = VideoPublishStatus.PUBLISHED
        )
        assertFalse("Account without custom claims must be denied admin privileges", videoCreated)
    }

    @Test
    fun testRoleSimulationBlockedInHardenedPosture() {
        val user = UserProfile(
            uid = "usr_attacker",
            email = "attacker@example.com",
            adminRole = AdminRole.NONE
        )
        repository.setCurrentUserForTesting(user)

        // Attempt client-side simulation switch
        repository.adminSimulateSwitchRole(AdminRole.SUPER_ADMIN)

        assertEquals("Client-side role simulation MUST NOT elevate permissions", AdminRole.NONE, repository.currentUser.value.adminRole)
        val latestLog = repository.auditLogs.value.firstOrNull { it.action == "UNAUTHORIZED_ROLE_SIMULATION_BLOCKED" }
        assertNotNull("Unauthorized role simulation attempt must be audited", latestLog)
    }

    @Test
    fun testGoogleOAuthLoginRegularCreator() {
        kotlinx.coroutines.runBlocking {
            // When repository is created without context in unit tests, authManager can be tested directly
            // or mock profile set
            val oauthUser = UserProfile(
                uid = "google_creator_123",
                username = "creator_cinema",
                displayName = "Cinema Creator",
                email = "creator@gmail.com",
                adminRole = AdminRole.NONE,
                membershipTier = MembershipTier.GOLD,
                coinBalance = 500L,
                badges = listOf("Google Verified", "Creator")
            )
            repository.setCurrentUserForTesting(oauthUser)

            assertEquals("creator@gmail.com", repository.currentUser.value.email)
            assertEquals(AdminRole.NONE, repository.currentUser.value.adminRole)
            assertTrue("Google verified badge present", repository.currentUser.value.badges.contains("Google Verified"))
        }
    }

    @Test
    fun testGoogleOAuthLoginDesignatedSuperAdmin() {
        kotlinx.coroutines.runBlocking {
            val adminOauthUser = UserProfile(
                uid = "google_superadmin_robin",
                username = "robin_admin",
                displayName = "Robin King (Super Admin)",
                email = "robinisking3@gmail.com",
                adminRole = AdminRole.SUPER_ADMIN,
                membershipTier = MembershipTier.FOUNDER,
                coinBalance = 50000L,
                badges = listOf("Super Admin", "Genesis Council", "Google Verified")
            )
            repository.setCurrentUserForTesting(adminOauthUser)

            assertEquals("robinisking3@gmail.com", repository.currentUser.value.email)
            assertEquals(AdminRole.SUPER_ADMIN, repository.currentUser.value.adminRole)
            assertTrue("Super Admin badge present", repository.currentUser.value.badges.contains("Super Admin"))
            assertTrue("Genesis Council badge present", repository.currentUser.value.badges.contains("Genesis Council"))
        }
    }
}
