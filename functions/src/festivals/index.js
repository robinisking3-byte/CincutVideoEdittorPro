const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Admin: Update Festival Configuration
 */
exports.updateFestivalConfig = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { festivalType, isEnabled, discountPercentage, bannerText, themeColorHex } = data;

  const configRef = db.collection("festivalConfig").doc("current");
  const updateData = {
    festivalType: festivalType || "NONE",
    isEnabled: !!isEnabled,
    discountPercentage: discountPercentage || 0,
    bannerText: bannerText || "",
    themeColorHex: themeColorHex || "0xFFD4AF37",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedBy: adminInfo.uid
  };

  await configRef.set(updateData, { merge: true });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "UPDATE_FESTIVAL_CONFIG",
    targetType: "system",
    targetId: "festivalConfig",
    metadata: { festivalType, isEnabled }
  });

  return { success: true, config: updateData };
});

/**
 * Activate Festival
 */
exports.activateFestival = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { festivalType } = data;

  await db.collection("festivalConfig").doc("current").update({
    festivalType: festivalType || "DIWALI",
    isEnabled: true,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, festivalType, isEnabled: true };
});

/**
 * Deactivate Festival
 */
exports.deactivateFestival = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin"]);
  await db.collection("festivalConfig").doc("current").update({
    isEnabled: false,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true, isEnabled: false };
});

/**
 * Admin: Create Festival Offer
 */
exports.createOffer = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { festivalId, title, originalPrice, discountPercentage, bonusCoins = 0, couponCode = null } = data;

  if (!title || !originalPrice) throw new functions.https.HttpsError("invalid-argument", "Missing parameters.");

  const docRef = db.collection("festivalOffers").doc();
  const disc = discountPercentage || 0;
  const finalPrice = Math.max(0, originalPrice * (1.0 - (disc / 100.0)));

  const offerData = {
    offerId: docRef.id,
    id: docRef.id,
    festivalId: festivalId || "DIWALI",
    title,
    originalPrice,
    discountPercentage: disc,
    finalPrice: parseFloat(finalPrice.toFixed(2)),
    bonusCoins,
    couponCode: couponCode ? couponCode.toUpperCase().trim() : null,
    isEnabled: true,
    usageCount: 0,
    usageLimit: data.usageLimit || 1000,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await docRef.set(offerData);
  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "CREATE_FESTIVAL_OFFER",
    targetType: "festivalOffer",
    targetId: docRef.id,
    metadata: { title, finalPrice }
  });

  return { success: true, offer: offerData };
});

/**
 * Update Offer
 */
exports.updateOffer = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin"]);
  const { offerId, updates } = data;
  await db.collection("festivalOffers").doc(offerId).update({
    ...updates,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true, offerId };
});

/**
 * Disable Offer
 */
exports.disableOffer = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin"]);
  const { offerId } = data;
  await db.collection("festivalOffers").doc(offerId).update({
    isEnabled: false,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true, offerId };
});

/**
 * Redeem Festival Offer (Secure Backend Price Validation & Bonus Coins Distribution)
 */
exports.redeemFestivalOffer = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { offerId, couponCode } = data;

  if (!offerId) throw new functions.https.HttpsError("invalid-argument", "Offer ID required.");

  const offerRef = db.collection("festivalOffers").doc(offerId);
  const userRef = db.collection("users").doc(uid);
  const accountRef = db.collection("coinAccounts").doc(uid);
  const txRef = db.collection("coinTransactions").doc();

  return await db.runTransaction(async (transaction) => {
    const offerDoc = await transaction.get(offerRef);
    if (!offerDoc.exists) throw new functions.https.HttpsError("not-found", "Festival offer not found.");

    const offer = offerDoc.data();
    if (!offer.isEnabled) throw new functions.https.HttpsError("failed-precondition", "This offer is no longer active.");

    if (offer.usageLimit && offer.usageCount >= offer.usageLimit) {
      throw new functions.https.HttpsError("resource-exhausted", "Offer redemption limit reached.");
    }

    // Validate Coupon if provided
    let extraDiscount = 0;
    if (couponCode) {
      const codeUpper = couponCode.toUpperCase().trim();
      if (offer.couponCode && offer.couponCode === codeUpper) {
        extraDiscount = offer.finalPrice * 0.10; // 10% coupon
      } else if (codeUpper === "DIWALI50" || codeUpper === "FESTIVAL50") {
        extraDiscount = offer.finalPrice * 0.50; // 50% promo coupon
      }
    }

    const calculatedPrice = Math.max(0, offer.finalPrice - extraDiscount);
    const bonusCoins = offer.bonusCoins || 500;

    // Credit bonus coins
    const accDoc = await transaction.get(accountRef);
    const curBal = accDoc.exists ? (accDoc.data().balance || 0) : 0;
    const newBal = curBal + bonusCoins;

    transaction.set(accountRef, {
      uid,
      balance: newBal,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    transaction.update(userRef, {
      coinBalance: newBal,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    transaction.update(offerRef, {
      usageCount: admin.firestore.FieldValue.increment(1)
    });

    // Record ledger transaction
    transaction.set(txRef, {
      transactionId: txRef.id,
      uid,
      type: "REWARD",
      amount: bonusCoins,
      balanceBefore: curBal,
      balanceAfter: newBal,
      referenceId: offerId,
      description: `Festival Offer: ${offer.title} (+${bonusCoins} bonus coins)`,
      createdBy: "festival_redemption",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      offerId,
      calculatedPrice,
      bonusCoinsCredited: bonusCoins,
      newBalance: newBal
    };
  });
});
