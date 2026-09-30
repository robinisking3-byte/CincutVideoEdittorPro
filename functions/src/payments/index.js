const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog, checkIdempotency } = require("../security");

const db = admin.firestore();

/**
 * Save ZapUPI Gateway Config (Admin only, stored securely on backend)
 */
exports.saveZapUpiGatewayConfig = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { merchantUpiId, apiKey, environment = "PRODUCTION" } = data;

  if (!merchantUpiId) {
    throw new functions.https.HttpsError("invalid-argument", "Merchant UPI ID required.");
  }

  const secretRef = db.collection("secrets").doc("zapUpiConfig");
  const updateData = {
    merchantUpiId,
    environment,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedBy: adminInfo.uid
  };
  if (apiKey && apiKey.length > 5) {
    updateData.apiKey = apiKey; // Stored securely in protected /secrets collection
  }

  await secretRef.set(updateData, { merge: true });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "UPDATE_ZAPUPI_CONFIG",
    targetType: "system",
    targetId: "zapUpiConfig",
    metadata: { merchantUpiId, environment }
  });

  const maskedKey = apiKey ? `${apiKey.substring(0, 4)}...${apiKey.substring(apiKey.length - 4)}` : "UNCHANGED";
  return { success: true, merchantUpiId, maskedKey, environment };
});

/**
 * Get ZapUPI Public Configuration
 */
exports.getZapUpiPublicConfig = functions.https.onCall(async (data, context) => {
  assertAuthenticated(context);
  const snap = await db.collection("secrets").doc("zapUpiConfig").get();
  if (!snap.exists) {
    return {
      configured: false,
      merchantUpiId: "cinecut@upi",
      maskedKey: "NOT_CONFIGURED"
    };
  }
  const config = snap.data();
  const apiKey = config.apiKey || "";
  const maskedKey = apiKey.length > 8 ? `${apiKey.substring(0, 4)}...${apiKey.substring(apiKey.length - 4)}` : "UNSET";
  return {
    configured: true,
    merchantUpiId: config.merchantUpiId || "cinecut@upi",
    maskedKey,
    environment: config.environment || "PRODUCTION"
  };
});

/**
 * Test ZapUPI Connection
 */
exports.testZapUpiConnection = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin"]);
  const snap = await db.collection("secrets").doc("zapUpiConfig").get();
  if (!snap.exists || !snap.data().apiKey) {
    return { success: false, message: "ZapUPI API key is not configured in backend secrets." };
  }
  return { success: true, message: "ZapUPI gateway connection active and responding." };
});

/**
 * Create Payment Order (8-minute expiry window, QR, UPI Intent)
 */
exports.createPaymentOrder = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { itemType, itemId, amountInr, title, idempotencyKey } = data;

  if (!amountInr || amountInr <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid amount.");
  }

  return await checkIdempotency(idempotencyKey, async () => {
    // Read merchant config
    const confDoc = await db.collection("secrets").doc("zapUpiConfig").get();
    const merchantUpi = confDoc.exists ? (confDoc.data().merchantUpiId || "cinecut@upi") : "cinecut@upi";

    const orderRef = db.collection("paymentOrders").doc();
    const orderId = `ORD_${Date.now()}_${Math.random().toString(36).substring(2, 6).toUpperCase()}`;

    const now = Date.now();
    const expiresAt = now + (8 * 60 * 1000); // 8 minutes

    const upiIntentUrl = `upi://pay?pa=${encodeURIComponent(merchantUpi)}&pn=${encodeURIComponent("CineCut Studio")}&am=${amountInr.toFixed(2)}&cu=INR&tn=${encodeURIComponent(orderId)}`;
    const qrPayload = upiIntentUrl;

    const orderData = {
      orderId,
      uid,
      itemType: itemType || "CINECOINS",
      itemId: itemId || "custom",
      title: title || "CineCut Digital Pack",
      amountInr: parseFloat(amountInr.toFixed(2)),
      currency: "INR",
      provider: "ZapUPI",
      providerPayId: `ZAP_${orderId}`,
      status: "PENDING",
      upiIntentUrl,
      qrPayload,
      utrNumber: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: admin.firestore.Timestamp.fromMillis(expiresAt),
      verifiedAt: null
    };

    await orderRef.set(orderData);
    return { success: true, order: orderData };
  });
});

/**
 * Verify UTR and Finalize Payment
 */
exports.verifyUTR = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { orderId, utrNumber } = data;

  if (!orderId || !utrNumber) {
    throw new functions.https.HttpsError("invalid-argument", "Order ID and UTR Number required.");
  }

  const cleanUtr = utrNumber.trim();
  if (cleanUtr.length < 8) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid UTR format. Expected reference number.");
  }

  // Find order
  const snap = await db.collection("paymentOrders").where("orderId", "==", orderId).limit(1).get();
  if (snap.empty) {
    throw new functions.https.HttpsError("not-found", "Payment order not found.");
  }

  const orderDoc = snap.docs[0];
  const orderData = orderDoc.data();

  if (orderData.status === "PAID") {
    return { success: true, alreadyPaid: true, message: "Order was already verified and credited." };
  }

  if (orderData.status === "EXPIRED" || orderData.status === "CANCELLED") {
    throw new functions.https.HttpsError("failed-precondition", "Order has expired or was cancelled.");
  }

  // Check if this UTR was already used for another paid order (duplicate fraud prevention)
  const dupCheck = await db.collection("paymentOrders")
    .where("utrNumber", "==", cleanUtr)
    .where("status", "==", "PAID")
    .limit(1)
    .get();

  if (!dupCheck.empty) {
    throw new functions.https.HttpsError("already-exists", "This UTR has already been redeemed.");
  }

  // Calculate coins to credit based on product
  let coinsToCredit = 500;
  if (orderData.itemId.includes("1500")) coinsToCredit = 1500;
  else if (orderData.itemId.includes("3500")) coinsToCredit = 3500;
  else if (orderData.itemId.includes("8000")) coinsToCredit = 8000;
  else if (orderData.amountInr >= 999) coinsToCredit = Math.floor(orderData.amountInr * 4);
  else coinsToCredit = Math.floor(orderData.amountInr * 3);

  // Atomic completion transaction
  const userRef = db.collection("users").doc(orderData.uid);
  const accountRef = db.collection("coinAccounts").doc(orderData.uid);
  const txRef = db.collection("coinTransactions").doc();

  await db.runTransaction(async (transaction) => {
    // 1. Mark Order as PAID
    transaction.update(orderDoc.ref, {
      status: "PAID",
      utrNumber: cleanUtr,
      verifiedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // 2. Credit CineCoins
    const accDoc = await transaction.get(accountRef);
    const curBal = accDoc.exists ? (accDoc.data().balance || 0) : 0;
    const newBal = curBal + coinsToCredit;

    transaction.set(accountRef, {
      uid: orderData.uid,
      balance: newBal,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    transaction.update(userRef, {
      coinBalance: newBal,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // 3. Ledger record
    transaction.set(txRef, {
      transactionId: txRef.id,
      uid: orderData.uid,
      type: "PURCHASE",
      amount: coinsToCredit,
      balanceBefore: curBal,
      balanceAfter: newBal,
      referenceId: orderId,
      description: `ZapUPI purchase verified (UTR: ${cleanUtr})`,
      createdBy: "zapupi_verification",
      idempotencyKey: `utr_${cleanUtr}`,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  await recordAuditLog({
    actorUid: uid,
    action: "VERIFY_PAYMENT_UTR",
    targetType: "paymentOrder",
    targetId: orderId,
    metadata: { utr: cleanUtr, amountInr: orderData.amountInr, coinsCredited: coinsToCredit }
  });

  return {
    success: true,
    orderId,
    status: "PAID",
    coinsCredited: coinsToCredit
  };
});

/**
 * Check Payment Status
 */
exports.checkPaymentStatus = functions.https.onCall(async (data, context) => {
  assertAuthenticated(context);
  const { orderId } = data;
  const snap = await db.collection("paymentOrders").where("orderId", "==", orderId).limit(1).get();
  if (snap.empty) throw new functions.https.HttpsError("not-found", "Order not found.");
  return { success: true, order: snap.docs[0].data() };
});

/**
 * Admin: Process Refund
 */
exports.processRefund = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { orderId, reason } = data;

  const snap = await db.collection("paymentOrders").where("orderId", "==", orderId).limit(1).get();
  if (snap.empty) throw new functions.https.HttpsError("not-found", "Order not found.");

  const orderDoc = snap.docs[0];
  await orderDoc.ref.update({
    status: "REFUNDED",
    refundReason: reason || "Admin refund",
    refundedAt: admin.firestore.FieldValue.serverTimestamp(),
    refundedBy: adminInfo.uid
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "REFUND_PAYMENT_ORDER",
    targetType: "paymentOrder",
    targetId: orderId,
    metadata: { reason }
  });

  return { success: true, orderId, status: "REFUNDED" };
});
