package com.example

import com.example.core.model.*
import com.example.core.repository.CineCutRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests verifying Section 26:
 * - Coin calculations & purchases
 * - Role & Permission logic
 * - Payment calculations
 * - Friend states & relationship constraints
 * - Project permissions & collaboration roles
 * - Festival offer calculations & coupons
 */
class CineCutUnitTests {

    private lateinit var repository: CineCutRepository

    @Before
    fun setUp() {
        repository = CineCutRepository()
        val testUser = UserProfile(
            uid = "usr_alex_unit_test",
            username = "alex_cinematics",
            displayName = "Alex Rivera",
            email = "alex@example.com",
            membershipTier = MembershipTier.GOLD,
            adminRole = AdminRole.SUPER_ADMIN,
            coinBalance = 2000L,
            badges = listOf("EARLY_ADOPTER", "PRO_COLORIST")
        )
        repository.setCurrentUserForTesting(testUser)
    }

    // ================= 1. COIN CALCULATIONS ================= //

    @Test
    fun testCoinBalanceAdjustmentAndIdempotency() {
        val initialBalance = repository.currentUser.value.coinBalance
        assertTrue("Initial coin balance should be positive", initialBalance >= 0)

        // Test crediting coins via admin adjustment
        val bonusAmount = 500L
        repository.adminAdjustCoins(repository.currentUser.value.uid, bonusAmount, "Test Grant")
        assertEquals(initialBalance + bonusAmount, repository.currentUser.value.coinBalance)

        // Test purchasing coin pack
        val packCoins = 1000L
        repository.purchaseCoins(packCoins, "Google Play Package")
        assertEquals(initialBalance + bonusAmount + packCoins, repository.currentUser.value.coinBalance)
    }

    @Test
    fun testLiveStreamCoinTipDeduction() {
        val currentBalance = repository.currentUser.value.coinBalance
        val tipAmount = 100L
        repository.sendLiveChatMessage("Great grading breakdown!", tipCoins = tipAmount)
        assertEquals(currentBalance - tipAmount, repository.currentUser.value.coinBalance)
    }

    // ================= 2. ROLE & PERMISSION ENFORCEMENT ================= //

    @Test
    fun testAdminRolePermissionsMatrix() {
        // NONE role has no admin permissions
        assertFalse(AdminRole.NONE.hasPermission(AdminPermission.VIEW_ANALYTICS))
        assertFalse(AdminRole.NONE.hasPermission(AdminPermission.MANAGE_USERS))
        assertFalse(AdminRole.NONE.hasPermission(AdminPermission.MANAGE_FESTIVALS))
        assertFalse(AdminRole.NONE.hasPermission(AdminPermission.MANAGE_SUPPORT_TICKETS))

        // SUPPORT role has support tickets and reports permissions, but NOT festivals or coin adjustments
        assertTrue(AdminRole.SUPPORT.hasPermission(AdminPermission.MANAGE_SUPPORT_TICKETS))
        assertTrue(AdminRole.SUPPORT.hasPermission(AdminPermission.VIEW_ANALYTICS))
        assertFalse(AdminRole.SUPPORT.hasPermission(AdminPermission.ADJUST_COINS))
        assertFalse(AdminRole.SUPPORT.hasPermission(AdminPermission.MANAGE_FESTIVALS))
        assertFalse(AdminRole.SUPPORT.hasPermission(AdminPermission.SECURITY_CENTER))

        // CONTENT_ADMIN can publish content but cannot ban users or change roles
        assertTrue(AdminRole.CONTENT_ADMIN.hasPermission(AdminPermission.MODERATE_CONTENT))
        assertTrue(AdminRole.CONTENT_ADMIN.hasPermission(AdminPermission.MANAGE_ROOMS))
        assertFalse(AdminRole.CONTENT_ADMIN.hasPermission(AdminPermission.BAN_USERS))
        assertFalse(AdminRole.CONTENT_ADMIN.hasPermission(AdminPermission.MANAGE_ADMIN_ROLES))

        // ADMIN and SUPER_ADMIN have full privileges
        assertTrue(AdminRole.ADMIN.hasPermission(AdminPermission.MANAGE_FESTIVALS))
        assertTrue(AdminRole.ADMIN.hasPermission(AdminPermission.MANAGE_SUPPORT_TICKETS))
        assertTrue(AdminRole.ADMIN.hasPermission(AdminPermission.ADJUST_COINS))
        assertTrue(AdminRole.SUPER_ADMIN.hasPermission(AdminPermission.MANAGE_ADMIN_ROLES))
        assertTrue(AdminRole.SUPER_ADMIN.hasPermission(AdminPermission.SECURITY_CENTER))
    }

    // ================= 3. PAYMENT CALCULATIONS ================= //

    @Test
    fun testZapUpiOrderCreationAndExpirationCalculation() {
        val amount = 499.0
        val order = repository.createZapUpiPaymentOrder(
            amountInr = amount,
            title = "1500 CineCoins",
            itemType = "CINECOINS",
            itemId = "coins_1500"
        )

        assertNotNull(order)
        assertEquals(499.0, order.amountInr, 0.01)
        assertEquals(PaymentOrderStatus.PENDING, order.status)
        assertTrue("Order ID should start with ZAP_CC_", order.orderId.startsWith("ZAP_CC_"))
        assertTrue("UPI Intent URL should contain VPA", order.upiIntentUrl.contains("pa=cinecut.zapupi@icici"))
        assertTrue("UPI Intent URL should contain order ID", order.upiIntentUrl.contains(order.orderId))

        // Check 8-minute (480s) timer expiration
        val expectedMinExpiration = order.createdAt + (470 * 1000L)
        val expectedMaxExpiration = order.createdAt + (490 * 1000L)
        assertTrue("ExpiresAt should be ~8 minutes from creation", order.expiresAt in expectedMinExpiration..expectedMaxExpiration)
        assertFalse("Newly created order should not be expired", order.expiresAt <= System.currentTimeMillis())
    }

    // ================= 4. FRIEND STATES & RELATIONSHIPS ================= //

    @Test
    fun testFriendRelationshipStateTransitions() {
        val userA = repository.currentUser.value.uid
        val targetUid = "usr_liam_support"

        // Send request
        val requestSent = repository.sendFriendRequest(targetUid)
        assertTrue("Friend request sending should succeed", requestSent)

        val state = repository.getFriendState(targetUid)
        assertEquals(FriendState.REQUEST_SENT, state)

        // Self-request must be prohibited
        val selfRequest = repository.sendFriendRequest(userA)
        assertFalse("Self friend request must be blocked", selfRequest)
    }

    // ================= 5. PROJECT PERMISSIONS & ROLES ================= //

    @Test
    fun testProjectCollaborationPermissions() {
        assertTrue("OWNER role can edit", ProjectRole.OWNER.canEdit())
        assertTrue("ADMIN role can edit", ProjectRole.ADMIN.canEdit())
        assertTrue("EDITOR role can edit", ProjectRole.EDITOR.canEdit())
        assertTrue("CONTRIBUTOR role can edit", ProjectRole.CONTRIBUTOR.canEdit())
        assertFalse("REVIEWER role cannot edit", ProjectRole.REVIEWER.canEdit())
        assertFalse("VIEWER role cannot edit", ProjectRole.VIEWER.canEdit())

        assertTrue("REVIEWER role can comment", ProjectRole.REVIEWER.canComment())
        assertTrue("OWNER can manage members", ProjectRole.OWNER.canManageMembers())
        assertFalse("EDITOR cannot manage members", ProjectRole.EDITOR.canManageMembers())
    }

    // ================= 6. FESTIVAL OFFER CALCULATIONS & COUPONS ================= //

    @Test
    fun testFestivalOfferBackendDiscountCalculations() {
        // Percentage discount: 1000 with 30% off = 700
        val price1 = repository.calculateBackendPrice(1000.0, FestivalDiscountType.PERCENTAGE, 30.0)
        assertEquals(700.0, price1, 0.01)

        // Percentage discount: 50% off
        val price2 = repository.calculateBackendPrice(2499.0, FestivalDiscountType.PERCENTAGE, 50.0)
        assertEquals(1249.50, price2, 0.01)

        // Fixed amount discount: 999 with 500 flat off = 499
        val price3 = repository.calculateBackendPrice(999.0, FestivalDiscountType.FIXED_AMOUNT, 500.0)
        assertEquals(499.0, price3, 0.01)

        // Fixed discount exceeding price cannot become negative
        val price4 = repository.calculateBackendPrice(300.0, FestivalDiscountType.FIXED_AMOUNT, 500.0)
        assertEquals(0.0, price4, 0.01)
    }

    @Test
    fun testFestivalCouponValidationAndDiscountCap() {
        // DIWALI50: 50% off up to max ₹1000, min order ₹499
        val (valid1, disc1) = repository.validateAndApplyCoupon("DIWALI50", 1500.0)
        assertTrue("Valid coupon on qualifying order amount", valid1)
        assertEquals(750.0, disc1, 0.01) // 50% of 1500 = 750 (under 1000 cap)

        // Exceeding cap: order of 3000 -> 50% = 1500, but capped at 1000
        val (valid2, disc2) = repository.validateAndApplyCoupon("DIWALI50", 3000.0)
        assertTrue(valid2)
        assertEquals(1000.0, disc2, 0.01)

        // Below minimum order
        val (valid3, disc3) = repository.validateAndApplyCoupon("DIWALI50", 200.0)
        assertFalse("Coupon must fail if below minimum order threshold", valid3)
        assertEquals(0.0, disc3, 0.01)

        // Non-existent coupon
        val (valid4, disc4) = repository.validateAndApplyCoupon("INVALID_CODE_999", 1000.0)
        assertFalse("Non-existent coupon must be rejected", valid4)
        assertEquals(0.0, disc4, 0.01)
    }
}
