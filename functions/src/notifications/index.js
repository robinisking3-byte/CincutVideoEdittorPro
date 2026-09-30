const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole } = require("../security");

const db = admin.firestore();

/**
 * Send Individual Notification
 */
exports.sendNotification = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { targetUid, type = "SYSTEM", title, body, metadata = {} } = data;

  if (!targetUid || !title || !body) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required notification fields.");
  }

  const notifRef = await db.collection("users").doc(targetUid).collection("notifications").add({
    type,
    title,
    body,
    metadata,
    senderId: uid,
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Try sending FCM push if token exists
  try {
    const userDoc = await db.collection("users").doc(targetUid).get();
    const fcmToken = userDoc.exists ? userDoc.data().fcmToken : null;
    if (fcmToken) {
      await admin.messaging().send({
        token: fcmToken,
        notification: { title, body },
        data: { type, ...metadata }
      });
    }
  } catch (err) {
    console.warn("FCM push delivery note:", err.message);
  }

  return { success: true, notificationId: notifRef.id };
});

/**
 * Admin: Send Bulk Notification
 */
exports.sendBulkNotification = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { title, body, category = "ANNOUNCEMENT" } = data;

  if (!title || !body) throw new functions.https.HttpsError("invalid-argument", "Title and body required.");

  // Broadcast to global topic or active users
  try {
    await admin.messaging().send({
      topic: "cinecut_community",
      notification: { title, body },
      data: { category }
    });
  } catch (e) {
    console.warn("Topic push delivery note:", e.message);
  }

  // Also write to announcement collection
  await db.collection("announcements").add({
    title,
    body,
    category,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true };
});

/**
 * Mark Notification Read
 */
exports.markNotificationRead = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { notificationId, markAll = false } = data;

  if (markAll) {
    const snap = await db.collection("users").doc(uid).collection("notifications").where("read", "==", false).get();
    const batch = db.batch();
    snap.docs.forEach(doc => batch.update(doc.ref, { read: true }));
    await batch.commit();
    return { success: true, markedCount: snap.size };
  }

  if (notificationId) {
    await db.collection("users").doc(uid).collection("notifications").doc(notificationId).update({ read: true });
    return { success: true, notificationId };
  }

  return { success: false, reason: "Missing notificationId or markAll parameter" };
});
