const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, checkIdempotency } = require("../security");

const db = admin.firestore();

/**
 * Send Friend Request
 */
exports.sendFriendRequest = functions.https.onCall(async (data, context) => {
  const senderId = assertAuthenticated(context);
  const { receiverId } = data;

  if (!receiverId || receiverId === senderId) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot friend yourself or missing receiver.");
  }

  return await checkIdempotency(`freq_${senderId}_${receiverId}`, async () => {
    // Check if target user exists and not blocked
    const receiverDoc = await db.collection("users").doc(receiverId).get();
    if (!receiverDoc.exists) throw new functions.https.HttpsError("not-found", "Target user does not exist.");

    const senderDoc = await db.collection("users").doc(senderId).get();
    const senderData = senderDoc.data() || {};

    const reqId = `freq_${senderId}_${receiverId}`;
    const reqRef = db.collection("friendRequests").doc(reqId);

    await reqRef.set({
      id: reqId,
      senderId,
      senderName: senderData.displayName || "Creator",
      senderUsername: senderData.username || "creator",
      receiverId,
      status: "PENDING",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Notify receiver
    await db.collection("users").doc(receiverId).collection("notifications").add({
      type: "FRIEND_REQUEST",
      title: "New Friend Request",
      body: `${senderData.displayName || "Someone"} sent you a friend request.`,
      senderId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      read: false
    });

    return { success: true, requestId: reqId };
  });
});

/**
 * Accept Friend Request
 */
exports.acceptFriendRequest = functions.https.onCall(async (data, context) => {
  const receiverId = assertAuthenticated(context);
  const { requestId } = data;
  if (!requestId) throw new functions.https.HttpsError("invalid-argument", "Request ID required.");

  const reqRef = db.collection("friendRequests").doc(requestId);
  const reqDoc = await reqRef.get();
  if (!reqDoc.exists) throw new functions.https.HttpsError("not-found", "Friend request not found.");

  const reqData = reqDoc.data();
  if (reqData.receiverId !== receiverId) {
    throw new functions.https.HttpsError("permission-denied", "Not authorized to accept this request.");
  }

  const senderId = reqData.senderId;

  const batch = db.batch();
  batch.update(reqRef, { status: "ACCEPTED", updatedAt: admin.firestore.FieldValue.serverTimestamp() });

  // Add mutual friend documents
  const f1 = db.collection("users").doc(receiverId).collection("friends").doc(senderId);
  batch.set(f1, {
    friendId: senderId,
    since: admin.firestore.FieldValue.serverTimestamp()
  });

  const f2 = db.collection("users").doc(senderId).collection("friends").doc(receiverId);
  batch.set(f2, {
    friendId: receiverId,
    since: admin.firestore.FieldValue.serverTimestamp()
  });

  await batch.commit();
  return { success: true };
});

/**
 * Reject Friend Request
 */
exports.rejectFriendRequest = functions.https.onCall(async (data, context) => {
  const receiverId = assertAuthenticated(context);
  const { requestId } = data;
  const reqRef = db.collection("friendRequests").doc(requestId);
  const reqDoc = await reqRef.get();
  if (!reqDoc.exists) throw new functions.https.HttpsError("not-found", "Request not found.");

  if (reqDoc.data().receiverId !== receiverId) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
  }

  await reqRef.update({ status: "REJECTED", updatedAt: admin.firestore.FieldValue.serverTimestamp() });
  return { success: true };
});

/**
 * Cancel Friend Request
 */
exports.cancelFriendRequest = functions.https.onCall(async (data, context) => {
  const senderId = assertAuthenticated(context);
  const { requestId } = data;
  const reqRef = db.collection("friendRequests").doc(requestId);
  const reqDoc = await reqRef.get();
  if (!reqDoc.exists) throw new functions.https.HttpsError("not-found", "Request not found.");

  if (reqDoc.data().senderId !== senderId) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
  }

  await reqRef.delete();
  return { success: true };
});

/**
 * Remove Friend
 */
exports.removeFriend = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { friendId } = data;

  const batch = db.batch();
  batch.delete(db.collection("users").doc(uid).collection("friends").doc(friendId));
  batch.delete(db.collection("users").doc(friendId).collection("friends").doc(uid));
  await batch.commit();

  return { success: true };
});

/**
 * Block User
 */
exports.blockUser = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { targetUid } = data;

  await db.collection("users").doc(uid).collection("blocked").doc(targetUid).set({
    blockedUid: targetUid,
    blockedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Remove existing friend relationship if any
  await db.collection("users").doc(uid).collection("friends").doc(targetUid).delete().catch(() => {});
  await db.collection("users").doc(targetUid).collection("friends").doc(uid).delete().catch(() => {});

  return { success: true };
});

/**
 * Unblock User
 */
exports.unblockUser = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { targetUid } = data;

  await db.collection("users").doc(uid).collection("blocked").doc(targetUid).delete();
  return { success: true };
});
