package com.example.core.model

import java.util.UUID

/**
 * Supported Festivals in CineCut.
 * Visual assets avoid raw emoji in actual UI and instead use professional iconography and canvas particle animations.
 */
enum class FestivalType(
    val id: String,
    val displayName: String,
    val celebrationTitle: String,
    val defaultPrimaryHex: Long,
    val defaultSecondaryHex: Long,
    val defaultAccentHex: Long,
    val defaultGradientStart: Long,
    val defaultGradientEnd: Long,
    val defaultAnimation: FestivalAnimationType,
    val defaultHeadline: String,
    val defaultSubheadline: String
) {
    DIWALI(
        id = "diwali",
        displayName = "Diwali",
        celebrationTitle = "Festival of Lights",
        defaultPrimaryHex = 0xFFFF9900,
        defaultSecondaryHex = 0xFFFFD700,
        defaultAccentHex = 0xFFFF5722,
        defaultGradientStart = 0xFF2B1400,
        defaultGradientEnd = 0xFF140800,
        defaultAnimation = FestivalAnimationType.DIYA_LIGHTS,
        defaultHeadline = "Diwali Studio Dhamaka",
        defaultSubheadline = "Ignite your creativity with 50% Off & +25% CineCoins Bonus"
    ),
    HOLI(
        id = "holi",
        displayName = "Holi",
        celebrationTitle = "Festival of Colors",
        defaultPrimaryHex = 0xFFFF007F,
        defaultSecondaryHex = 0xFF00E5FF,
        defaultAccentHex = 0xFFFFD600,
        defaultGradientStart = 0xFF2A0825,
        defaultGradientEnd = 0xFF0B172A,
        defaultAnimation = FestivalAnimationType.COLOR_SPLASH,
        defaultHeadline = "Holi Color Splash Fest",
        defaultSubheadline = "Splash your films with vivid color grades, bonus coins & creator perks"
    ),
    JANMASHTAMI(
        id = "janmashtami",
        displayName = "Janmashtami",
        celebrationTitle = "Krishna Janmotsav",
        defaultPrimaryHex = 0xFF1E88E5,
        defaultSecondaryHex = 0xFFFFD54F,
        defaultAccentHex = 0xFF00E676,
        defaultGradientStart = 0xFF0D1B2A,
        defaultGradientEnd = 0xFF082218,
        defaultAnimation = FestivalAnimationType.PEACOCK_FEATHER,
        defaultHeadline = "Janmashtami Divine Celebration",
        defaultSubheadline = "Celebrate artistic grace with celestial LUT presets & coin packs"
    ),
    RAKSHA_BANDHAN(
        id = "raksha_bandhan",
        displayName = "Raksha Bandhan",
        celebrationTitle = "Bond of Protection",
        defaultPrimaryHex = 0xFFE91E63,
        defaultSecondaryHex = 0xFFFFC107,
        defaultAccentHex = 0xFFAB47BC,
        defaultGradientStart = 0xFF280B1C,
        defaultGradientEnd = 0xFF1E0A24,
        defaultAnimation = FestivalAnimationType.RAKHI_MOTIF,
        defaultHeadline = "Raksha Bandhan Creator Ties",
        defaultSubheadline = "Share premium editing licenses & gift CineCoins to your creative crew"
    ),
    DUSSEHRA(
        id = "dussehra",
        displayName = "Dussehra",
        celebrationTitle = "Vijayadashami Triumph",
        defaultPrimaryHex = 0xFFFF6F00,
        defaultSecondaryHex = 0xFFFFD54F,
        defaultAccentHex = 0xFFE53935,
        defaultGradientStart = 0xFF281102,
        defaultGradientEnd = 0xFF1F0404,
        defaultAnimation = FestivalAnimationType.FIREWORKS_LIGHTS,
        defaultHeadline = "Dussehra Victory Sale",
        defaultSubheadline = "Conquer your cinematic vision with victory bundles & 4K exports"
    ),
    HALLOWEEN(
        id = "halloween",
        displayName = "Halloween",
        celebrationTitle = "Spooky Creator Night",
        defaultPrimaryHex = 0xFFFF6D00,
        defaultSecondaryHex = 0xFF7C4DFF,
        defaultAccentHex = 0xFF76FF03,
        defaultGradientStart = 0xFF1F0B24,
        defaultGradientEnd = 0xFF0D0212,
        defaultAnimation = FestivalAnimationType.PUMPKINS_BATS,
        defaultHeadline = "Halloween Horror Studio Night",
        defaultSubheadline = "Chilling sound effects, dark cinematic grading & spooky packs"
    ),
    CHRISTMAS(
        id = "christmas",
        displayName = "Christmas",
        celebrationTitle = "Season of Joy & Cheer",
        defaultPrimaryHex = 0xFFD32F2F,
        defaultSecondaryHex = 0xFF388E3C,
        defaultAccentHex = 0xFFFFD700,
        defaultGradientStart = 0xFF21070A,
        defaultGradientEnd = 0xFF051B0D,
        defaultAnimation = FestivalAnimationType.SNOWFALL,
        defaultHeadline = "Winter Wonderland Studio Sale",
        defaultSubheadline = "Wrap up your year with holiday discounts & bonus CineCoins"
    ),
    INDEPENDENCE_DAY(
        id = "independence_day",
        displayName = "Independence Day",
        celebrationTitle = "Freedom to Create",
        defaultPrimaryHex = 0xFFFF6F00,
        defaultSecondaryHex = 0xFF00E5FF,
        defaultAccentHex = 0xFF2E7D32,
        defaultGradientStart = 0xFF211103,
        defaultGradientEnd = 0xFF041908,
        defaultAnimation = FestivalAnimationType.TRICOLOR_CELEBRATION,
        defaultHeadline = "Independence Day Freedom Fest",
        defaultSubheadline = "Celebrate creative freedom with patriotic offers and unlimited renders"
    ),
    REPUBLIC_DAY(
        id = "republic_day",
        displayName = "Republic Day",
        celebrationTitle = "Constitution & Unity",
        defaultPrimaryHex = 0xFFFF6F00,
        defaultSecondaryHex = 0xFF1565C0,
        defaultAccentHex = 0xFF2E7D32,
        defaultGradientStart = 0xFF200F04,
        defaultGradientEnd = 0xFF04111E,
        defaultAnimation = FestivalAnimationType.TRICOLOR_CELEBRATION,
        defaultHeadline = "Republic Day Creator Honors",
        defaultSubheadline = "Honoring the creative vanguard with special CineCoin bonus reserves"
    );

    companion object {
        fun fromId(id: String): FestivalType {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DIWALI
        }
    }
}

/**
 * Lightweight, hardware-optimized particle animation styles.
 */
enum class FestivalAnimationType(val title: String, val description: String) {
    DIYA_LIGHTS("Diya Lights & Sparkles", "Flickering golden diyas, rising sparks and ambient warm motes"),
    COLOR_SPLASH("Color Splash Particles", "Vibrant festive color bursts, drifting pigment clouds"),
    PEACOCK_FEATHER("Peacock Feather Particles", "Graceful iridescent feather sparkles and celestial glow"),
    RAKHI_MOTIF("Rakhi Thread & Motifs", "Decorative festive threads and protective golden sparkles"),
    FIREWORKS_LIGHTS("Victory Fireworks & Lights", "Subtle firework bursts and celebratory light trails"),
    PUMPKINS_BATS("Spooky Fog & Bat Silhouettes", "Mystic glowing embers, soft fog particles and silhouettes"),
    SNOWFALL("Winter Snowfall", "Gentle drifting snow crystals and holiday twinkle particles"),
    TRICOLOR_CELEBRATION("Tricolor Flutter Particles", "Saffron, white and green confetti ribbon particles")
}

/**
 * Configuration for runtime festival particle animations.
 */
data class FestivalAnimationConfig(
    val animationType: FestivalAnimationType = FestivalAnimationType.DIYA_LIGHTS,
    val enabled: Boolean = true,
    val particleDensity: Float = 0.6f, // 0.2f (sparse) to 1.0f (dense)
    val reduceAnimations: Boolean = false, // Critical for low-end devices
    val speedMultiplier: Float = 1.0f
)

/**
 * Remote-configurable theme styling applied across CineCut.
 */
data class FestivalThemeConfig(
    val festivalType: FestivalType = FestivalType.DIWALI,
    val themeName: String = "Diwali Radiant Gold",
    val primaryAccentHex: Long = 0xFFFF9900,
    val secondaryAccentHex: Long = 0xFFFFD700,
    val surfaceGradientStart: Long = 0xFF2B1400,
    val surfaceGradientEnd: Long = 0xFF140800,
    val buttonColorHex: Long = 0xFFFF9900,
    val cardBorderColorHex: Long = 0x66FFD700,
    val bannerHeadline: String = "Diwali Studio Dhamaka",
    val bannerSubheadline: String = "Ignite your creativity with 50% Off & +25% CineCoins Bonus",
    val bannerBadgeText: String = "FESTIVAL SPECIAL",
    val bannerImageUrl: String = "",
    val enableCardAccents: Boolean = true,
    val enableButtonStyling: Boolean = true,
    val enableHomeHeroStyling: Boolean = true,
    val enableStoreStyling: Boolean = true,
    val enableEditorAccents: Boolean = true
)

/**
 * Discount computation types.
 */
enum class FestivalDiscountType(val title: String) {
    PERCENTAGE("Percentage Off (%)"),
    FIXED_AMOUNT("Flat Discount (₹)"),
    BONUS_PERCENT("Bonus Item/Coins (%)"),
    FREE_ADDON("Free Bundle Addon")
}

/**
 * Item Category for festival offers.
 */
enum class FestivalOfferItemCategory(val title: String) {
    ALL("All Offers"),
    CINECOIN_PACK("CineCoins Packs"),
    MEMBERSHIP_TIER("Memberships"),
    EXCLUSIVE_TEMPLATE("Festival Templates"),
    EXCLUSIVE_EFFECT("Exclusive LUTs & Effects"),
    CREATOR_BUNDLE("Limited Bundles")
}

/**
 * Backend-calculated Festival Offer model.
 * The client NEVER calculates final prices for checkout!
 */
data class FestivalOffer(
    val offerId: String = "FO-" + UUID.randomUUID().toString().take(6).uppercase(),
    val title: String,
    val description: String,
    val festival: FestivalType = FestivalType.DIWALI,
    val category: FestivalOfferItemCategory = FestivalOfferItemCategory.CINECOIN_PACK,
    val discountType: FestivalDiscountType = FestivalDiscountType.PERCENTAGE,
    val discountValue: Double = 30.0,
    val originalPrice: Double = 999.0,
    val finalPrice: Double = 699.0, // Pre-calculated by backend
    val bonusCoins: Long = 0L,
    val itemPayloadId: String = "", // e.g. "coins_1500", "tier_gold", "lut_diwali_glow"
    val startTime: Long = System.currentTimeMillis() - 86400000L,
    val endTime: Long = System.currentTimeMillis() + (72 * 3600000L), // 3 days remaining
    val maxUses: Int = 1000,
    val usedCount: Int = 142,
    val perUserLimit: Int = 1,
    val enabled: Boolean = true,
    val badgeTag: String = "FESTIVAL SPECIAL",
    val isFirstPurchaseOnly: Boolean = false,
    val couponCode: String? = null
) {
    fun isCurrentlyActive(now: Long = System.currentTimeMillis()): Boolean {
        return enabled && now >= startTime && now <= endTime && (maxUses <= 0 || usedCount < maxUses)
    }

    fun isSoldOut(): Boolean = maxUses > 0 && usedCount >= maxUses
    fun remainingTimeMillis(now: Long = System.currentTimeMillis()): Long = (endTime - now).coerceAtLeast(0L)
}

/**
 * Coupon codes for festival promotions.
 */
data class FestivalCoupon(
    val code: String,
    val discountPercent: Int = 20,
    val maxDiscountInr: Double = 500.0,
    val minOrderAmount: Double = 299.0,
    val enabled: Boolean = true,
    val expiryTime: Long = System.currentTimeMillis() + (7 * 86400000L),
    val usageCount: Int = 38
) {
    fun isValid(orderAmount: Double, now: Long = System.currentTimeMillis()): Boolean {
        return enabled && now <= expiryTime && orderAmount >= minOrderAmount
    }

    fun calculateDiscount(orderAmount: Double): Double {
        if (orderAmount < minOrderAmount) return 0.0
        val discount = (orderAmount * discountPercent) / 100.0
        return discount.coerceAtMost(maxDiscountInr)
    }
}

/**
 * Master configuration object stored in Firestore `/config/festivals`.
 * Admins remotely configure this so no APK release is required.
 */
data class FestivalConfiguration(
    val activeFestival: FestivalType = FestivalType.DIWALI,
    val isFestivalActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis() - 86400000L,
    val endDate: Long = System.currentTimeMillis() + (5 * 86400000L),
    val themeConfig: FestivalThemeConfig = FestivalThemeConfig(
        festivalType = FestivalType.DIWALI,
        themeName = "Diwali Radiant Gold",
        primaryAccentHex = FestivalType.DIWALI.defaultPrimaryHex,
        secondaryAccentHex = FestivalType.DIWALI.defaultSecondaryHex,
        surfaceGradientStart = FestivalType.DIWALI.defaultGradientStart,
        surfaceGradientEnd = FestivalType.DIWALI.defaultGradientEnd,
        buttonColorHex = FestivalType.DIWALI.defaultPrimaryHex,
        cardBorderColorHex = 0x66FFD700,
        bannerHeadline = FestivalType.DIWALI.defaultHeadline,
        bannerSubheadline = FestivalType.DIWALI.defaultSubheadline
    ),
    val animationConfig: FestivalAnimationConfig = FestivalAnimationConfig(
        animationType = FestivalAnimationType.DIYA_LIGHTS,
        enabled = true,
        particleDensity = 0.6f,
        reduceAnimations = false,
        speedMultiplier = 1.0f
    ),
    val cineCoinBonusPercent: Int = 25,
    val firstPurchaseBonusPercent: Int = 50,
    val bannerText: String = "Special Festival Offer: Up to 50% Off & +25% Bonus CineCoins!",
    val bannerCountdownTarget: Long = System.currentTimeMillis() + (5 * 86400000L),
    val remoteConfigVersion: Int = 2,
    val lastModifiedBy: String = "SuperAdmin",
    val lastModifiedAt: Long = System.currentTimeMillis()
) {
    fun isEventRunning(now: Long = System.currentTimeMillis()): Boolean {
        return isFestivalActive && now >= startDate && now <= endDate
    }
}
