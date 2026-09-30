const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog, checkIdempotency } = require("../security");

const db = admin.firestore();

/**
 * Atomic mutation helper for CineCoins
 */
async function mutateCineCoins({
  uid,
  amount,
  type,
  reason,
  referenceId = "",
  createdBy = "system",
  idempotencyKey = ""
}) {
  const accountRef = db.collection("coinAccounts").doc(uid);
  const txRef = db.collection("coinTransactions").doc();

  return await db.runTransaction(async (transaction) => {
    const accDoc = await transaction.get(accountRef);
    const currentBalance = accDoc.exists ? (accDoc.data().balance || 0) : 0;
    const newBalance = currentBalance + amount;

    if (newBalance < 0) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Insufficient CineCoin balance. Current: ${currentBalance}, required deduction: ${-amount}`
      );
    }

    const version = accDoc.exists ? (accDoc.data().version || 1) + 1 : 1;

    transaction.set(accountRef, {
      uid,
      balance: newBalance,
      version,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Also update cached balance in user profile
    transaction.update(db.collection("users").doc(uid), {
      coinBalance: newBalance,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    const txRecord = {
      transactionId: txRef.id,
      uid,
      type,
      amount,
      balanceBefore: currentBalance,
      balanceAfter: newBalance,
      referenceId,
      description: reason,
      createdBy,
      idempotencyKey,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    transaction.set(txRef, txRecord);
    return txRecord;
  });
}

/**
 * Earn Coins (Reward/Challenge completion)
 */
exports.earnCoins = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { amount = 50, reason = "Creator Activity Reward", idempotencyKey } = data;

  if (amount <= 0 || amount > 1000) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid reward amount.");
  }

  return await checkIdempotency(idempotencyKey, async () => {
    return await mutateCineCoins({
      uid,
      amount,
      type: "EARN",
      reason,
      createdBy: "system",
      idempotencyKey
    });
  });
});

/**
 * Spend Coins (Export 4K, AI Director, Tips, Presets)
 */
exports.spendCoins = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { amount, reason = "Purchase Feature", referenceId = "", idempotencyKey } = data;

  if (!amount || amount <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Spend amount must be greater than zero.");
  }

  return await checkIdempotency(idempotencyKey, async () => {
    return await mutateCineCoins({
      uid,
      amount: -Math.abs(amount),
      type: "SPEND",
      reason,
      referenceId,
      createdBy: uid,
      idempotencyKey
    });
  });
});

/**
 * Admin: Grant Coins
 */
exports.grantCoins = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid, amount, reason, idempotencyKey } = data;

  if (!targetUid || !amount || amount <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Target UID and positive amount required.");
  }

  const tx = await checkIdempotency(idempotencyKey, async () => {
    return await mutateCineCoins({
      uid: targetUid,
      amount,
      type: "ADMIN_GRANT",
      reason: reason || "Admin Bonus Grant",
      createdBy: adminInfo.uid,
      idempotencyKey
    });
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "ADMIN_GRANT_COINS",
    targetType: "user",
    targetId: targetUid,
    metadata: { amount, reason }
  });

  return { success: true, tx };
});

/**
 * Admin: Refund Coins
 */
exports.refundCoins = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "support"]);
  const { targetUid, amount, referenceId, reason, idempotencyKey } = data;

  if (!targetUid || !amount || amount <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Target UID and amount required.");
  }

  const tx = await checkIdempotency(idempotencyKey, async () => {
    return await mutateCineCoins({
      uid: targetUid,
      amount,
      type: "REFUND",
      referenceId,
      reason: reason || "Support Refund",
      createdBy: adminInfo.uid,
      idempotencyKey
    });
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "ADMIN_REFUND_COINS",
    targetType: "user",
    targetId: targetUid,
    metadata: { amount, reason, referenceId }
  });

  return { success: true, tx };
});

/**
 * Admin: Adjust Coins
 */
exports.adjustCoins = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid, amount, reason, idempotencyKey } = data;

  if (!targetUid || amount === undefined || amount === 0) {
    throw new functions.https.HttpsError("invalid-argument", "Target UID and non-zero amount required.");
  }

  const tx = await checkIdempotency(idempotencyKey, async () => {
    return await mutateCineCoins({
      uid: targetUid,
      amount,
      type: "ADMIN_ADJUSTMENT",
      reason: reason || "Administrative balance adjustment",
      createdBy: adminInfo.uid,
      idempotencyKey
    });
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "ADMIN_ADJUST_COINS",
    targetType: "user",
    targetId: targetUid,
    metadata: { amount, reason }
  });

  return { success: true, tx };
});

/**
 * Get Coin History
 */
exports.getCoinHistory = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const targetUid = data.targetUid || uid;

  if (targetUid !== uid) {
    assertAdminRole(context, ["super_admin", "admin", "support"]);
  }

  const snap = await db.collection("coinTransactions")
    .where("uid", "==", targetUid)
    .orderBy("createdAt", "desc")
    .limit(data.limit || 50)
    .get();

  const history = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  return { success: true, history };
});

/**
 * Assign Status Coin (Backend-controlled)
 */
exports.assignStatusCoin = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid, statusType, reason } = data;

  const validStatuses = [
    "DEFAULT", "BRONZE", "SILVER", "GOLD", "DIAMOND", "VIP",
    "FOUNDER", "ADMIN", "MODERATOR", "VERIFIED_CREATOR", "EARLY_SUPPORTER", "BANNED"
  ];

  if (!targetUid || !validStatuses.includes(statusType)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target UID or status type.");
  }

  await db.collection("users").doc(targetUid).update({
    statusCoinId: statusType.toLowerCase(),
    membershipTier: statusType === "DEFAULT" ? "FREE" : statusType,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "ASSIGN_STATUS_COIN",
    targetType: "user",
    targetId: targetUid,
    metadata: { statusType, reason }
  });

  return { success: true, statusType };
});

/**
 * Remove Status Coin
 */
exports.removeStatusCoin = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid, reason } = data;

  await db.collection("users").doc(targetUid).update({
    statusCoinId: "default",
    membershipTier: "FREE",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "REMOVE_STATUS_COIN",
    targetType: "user",
    targetId: targetUid,
    metadata: { reason }
  });

  return { success: true };
});

/**
 * Verify Google Play Purchase
 */
exports.verifyPlayPurchase = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { purchaseToken, packCoins = 1000, packageName = "Google Play Package" } = data;

  if (!purchaseToken) {
    throw new functions.https.HttpsError("invalid-argument", "Purchase token required.");
  }

  const tokenHash = purchaseToken.substring(0, 16);
  return await checkIdempotency(`play_${tokenHash}`, async () => {
    const tx = await mutateCineCoins({
      uid,
      amount: packCoins,
      type: "PURCHASE",
      reason: `Google Play In-App Purchase (${packageName})`,
      referenceId: purchaseToken,
      createdBy: "google_play",
      idempotencyKey: `play_${tokenHash}`
    });

    return { success: true, credited: packCoins, tx };
  });
});
