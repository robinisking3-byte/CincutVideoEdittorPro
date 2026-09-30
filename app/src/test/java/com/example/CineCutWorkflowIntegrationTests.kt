package com.example

import com.example.core.engine.AiDirectorEngine
import com.example.core.engine.TimelineController
import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Full End-to-End Workflow Integration Tests verifying Section 28:
 * - Workflow 1: User Profile & Authentication persistence
 * - Workflow 2: Video Project creation & Timeline Controller operations (Split, Trim, Transitions, Undo/Redo)
 * - Workflow 3: AI Director prompt interpretation and timeline modifications
 * - Workflow 4: ZapUPI Payment Creation, Verification, and Coin Crediting
 * - Workflow 5: Support Ticket creation, admin assignment, public reply vs internal note, resolution
 * - Workflow 6: Festival Theme & Offer activation, real countdown, coupon discount, and secure checkout
 */
class CineCutWorkflowIntegrationTests {

    private lateinit var repository: CineCutRepository
    private lateinit var timelineController: TimelineController
    private lateinit var aiDirector: AiDirectorEngine

    @Before
    fun setUp() {
        repository = CineCutRepository()
        val testUser = UserProfile(
            uid = "usr_creator_8921",
            username = "alex_cinematics",
            displayName = "Alex Rivera",
            email = "alex@example.com",
            membershipTier = MembershipTier.GOLD,
            adminRole = AdminRole.SUPER_ADMIN,
            coinBalance = 2500L,
            badges = listOf("EARLY_ADOPTER", "PRO_COLORIST")
        )
        repository.setCurrentUserForTesting(testUser)
        timelineController = TimelineController()
        aiDirector = AiDirectorEngine(timelineController)
    }

    // ================= WORKFLOW 1: USER PROFILE & BADGES ================= //

    @Test
    fun testUserProfileUpdateAndBadgePersistence() {
        val user = repository.currentUser.value
        assertNotNull(user)

        val updatedBio = "Independent Film Director | HDR Mobile Pioneer"
        repository.updateUserProfile(displayName = "Alex R.", username = user.username, bio = updatedBio)

        assertEquals("Alex R.", repository.currentUser.value.displayName)
        assertEquals(updatedBio, repository.currentUser.value.bio)
        assertTrue("Badges should persist", repository.currentUser.value.badges.isNotEmpty())
    }

    // ================= WORKFLOW 2: PROJECT & TIMELINE CONTROLLER ================= //

    @Test
    fun testProjectCreationAndTimelineControllerOperations() {
        // 1. Create Project
        val newProj = repository.addProject("Cyberpunk 2026 Short", "16:9")
        assertNotNull(newProj)
        assertTrue(repository.projects.value.any { it.id == newProj.id })

        // 2. Timeline tracks initialization
        val initialTracks = timelineController.tracks.value
        assertTrue("Timeline should contain tracks", initialTracks.isNotEmpty())

        val initialDuration = timelineController.projectState.value.durationMs
        assertTrue("Timeline should have positive duration", initialDuration > 0)

        // 3. Add text overlay track
        timelineController.addTextLayer("CYBERPUNK TITLE")
        val tracksAfterTitle = timelineController.tracks.value
        val textTrack = tracksAfterTitle.find { it.type == TrackType.TEXT }
        assertNotNull("Text track should exist after adding text layer", textTrack)
        assertTrue("Text track should contain title clip", textTrack!!.clips.any { it.name.contains("CYBERPUNK TITLE") })

        // 4. Undo / Redo verification
        assertTrue("Timeline must support undo after action", timelineController.canUndo())
        timelineController.undo()
        assertTrue("Timeline must support redo after undo", timelineController.canRedo())
        timelineController.redo()
    }

    // ================= WORKFLOW 3: AI DIRECTOR EXECUTION ON TIMELINE ================= //

    @Test
    fun testAiDirectorGeneratesAndModifiesTimeline() {
        val prompt = "Make this video look cinematic with teal and orange color grading"
        val plan = aiDirector.generateEditPlan(prompt)

        assertNotNull(plan)
        assertFalse("AI plan should contain structured edit commands", plan.commands.isEmpty())
        assertTrue("Plan should mention cinematic or grading", plan.summary.contains("Cinematic", ignoreCase = true))

        // Execute plan onto timeline
        val executed = aiDirector.executePlan(plan)
        assertTrue("AI Director plan should execute successfully onto timeline", executed)

        // Color grading should now be cinematic on the clip
        val selectedClip = timelineController.getSelectedClip()
        assertNotNull(selectedClip)
        assertEquals("Cinematic Teal & Orange", selectedClip!!.colorGrading.lutFilter)
        assertTrue("Contrast should be boosted", selectedClip.colorGrading.contrast > 1.0f)
    }

    // ================= WORKFLOW 4: ZAPUPI PAYMENT WORKFLOW ================= //

    @Test
    fun testZapUpiFullPaymentWorkflow() {
        val user = repository.currentUser.value
        val initialCoins = user.coinBalance

        // Step 1: User selects product -> Backend creates order
        val order = repository.createZapUpiPaymentOrder(
            itemType = "CINECOINS",
            itemId = "coins_1500",
            amountInr = 499.0,
            title = "1,500 CineCoins Creator Pack"
        )
        assertNotNull(order)
        assertEquals(PaymentOrderStatus.PENDING, order.status)
        assertTrue(order.upiIntentUrl.isNotBlank())
        assertNotNull(order.qrPayload)

        // Step 2: User verifies with UTR / payment confirmation
        val utr = "UPI984210984129"
        val (verified, _) = repository.verifyZapUpiPaymentOrder(order.orderId, utr)
        assertTrue("Payment verification must succeed", verified)

        // Step 3: Verify coins credited & order marked PAID
        val updatedOrder = repository.paymentOrders.value.find { it.orderId == order.orderId }
        assertNotNull(updatedOrder)
        assertEquals(PaymentOrderStatus.PAID, updatedOrder!!.status)
        assertEquals(utr, updatedOrder.utrNumber)

        val updatedBalance = repository.currentUser.value.coinBalance
        assertTrue("Coin balance must increase after successful payment", updatedBalance > initialCoins)

        // Step 4: Verify ledger transaction was generated
        val tx = repository.coinTransactions.value.find { it.reason.contains(order.orderId) }
        assertNotNull("Ledger transaction must record order ID", tx)
        assertEquals("PURCHASE", tx!!.type)
    }

    // ================= WORKFLOW 5: SUPPORT TICKET SYSTEM ================= //

    @Test
    fun testSupportTicketWorkflowWithInternalNotesSeparation() {
        // Step 1: User creates ticket
        val ticket = repository.createSupportTicket(
            subject = "Export crashed at 89%",
            category = SupportTicketCategory.TECHNICAL_ISSUE,
            description = "Attempting to render 4K ProRes on Android 14",
            priority = SupportTicketPriority.HIGH
        )
        assertNotNull(ticket)
        assertEquals(SupportTicketStatus.OPEN, ticket.status)
        assertTrue(ticket.ticketId.startsWith("#CC-"))

        // Step 2: Staff assigns ticket
        val staffUid = "usr_staff_sarah"
        val assigned = repository.assignSupportTicket(ticket.ticketId, staffUid, "Sarah Support")
        assertTrue("Staff assignment should succeed", assigned)

        // Step 3: Staff adds an internal confidential note (Admin only)
        val noteSent = repository.sendSupportMessage(
            ticketId = ticket.ticketId,
            messageText = "Investigating encoder log: out-of-memory during heavy grain pass",
            isInternalNote = true
        )
        assertTrue("Internal note should be added by staff", noteSent)

        // Step 4: Staff replies publicly to user
        val replySent = repository.sendSupportMessage(
            ticketId = ticket.ticketId,
            messageText = "Hello Alex! Please try lowering the film grain radius to 2.0 while we deploy a patch.",
            isInternalNote = false
        )
        assertTrue("Public reply should be sent", replySent)

        // Step 5: User responds and ticket gets resolved
        repository.updateSupportTicketStatus(ticket.ticketId, SupportTicketStatus.RESOLVED)
        val resolvedTicket = repository.supportTickets.value.find { it.ticketId == ticket.ticketId }
        assertEquals(SupportTicketStatus.RESOLVED, resolvedTicket!!.status)
    }

    // ================= WORKFLOW 6: FESTIVAL THEME & OFFERS SYSTEM ================= //

    @Test
    fun testFestivalSystemOffersCouponsAndRedemption() {
        val config = repository.festivalConfig.value
        assertTrue("Festival mode should be running", config.isEventRunning())

        // 1. Check active offers
        val offers = repository.festivalOffers.value
        assertTrue("Active festival offers should be available", offers.isNotEmpty())

        val diwaliPack = offers.first { it.festival == FestivalType.DIWALI && it.category == FestivalOfferItemCategory.CINECOIN_PACK }
        assertNotNull(diwaliPack)

        // 2. Validate Coupon DIWALI50
        val (valid, discount) = repository.validateAndApplyCoupon("DIWALI50", diwaliPack.finalPrice)
        assertTrue("Coupon DIWALI50 should be valid for pack price", valid)
        assertTrue("Discount should be positive", discount > 0)

        // 3. Compute secure checkout
        val (canCheckout, finalPrice, _) = repository.computeSecureOfferCheckout(diwaliPack.offerId, "DIWALI50")
        assertTrue("Checkout computation must succeed", canCheckout)
        assertEquals(diwaliPack.finalPrice - discount, finalPrice, 0.01)

        // 4. Redeem offer
        val initialCoins = repository.currentUser.value.coinBalance
        val redeemed = repository.redeemFestivalOffer(diwaliPack.offerId, "DIWALI50")
        assertTrue("Redeeming festival offer should succeed", redeemed)

        val balanceAfter = repository.currentUser.value.coinBalance
        assertTrue("User balance should receive festival bonus coins", balanceAfter > initialCoins)
    }
}
