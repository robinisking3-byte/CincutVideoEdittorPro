const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Create User Report
 */
exports.createReport = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { targetType, targetId, reason, details = "" } = data;

  if (!targetType || !targetId || !reason) {
    throw new functions.https.HttpsError("invalid-argument", "Missing report parameters.");
  }

  const reportRef = db.collection("reports").doc();
  const reportData = {
    id: reportRef.id,
    reporterUid: uid,
    targetType, // "USER", "POST", "COMMENT", "ROOM_MESSAGE", "VIDEO"
    targetId,
    reason,
    details,
    status: "PENDING",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await reportRef.set(reportData);
  return { success: true, reportId: reportRef.id };
});

/**
 * Review / Resolve Report (Admin/Moderator)
 */
exports.resolveReport = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "moderator"]);
  const { reportId, actionTaken, resolutionNotes = "" } = data;

  if (!reportId || !actionTaken) throw new functions.https.HttpsError("invalid-argument", "Missing parameters.");

  await db.collection("reports").doc(reportId).update({
    status: "RESOLVED",
    actionTaken,
    resolutionNotes,
    resolvedBy: adminInfo.uid,
    resolvedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "RESOLVE_REPORT",
    targetType: "report",
    targetId: reportId,
    metadata: { actionTaken, resolutionNotes }
  });

  return { success: true, reportId, status: "RESOLVED" };
});

/**
 * Moderate User (Ban/Warning/Mute)
 */
exports.moderateUser = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "moderator"]);
  const { targetUid, action, reason } = data;

  if (!targetUid || !action) throw new functions.https.HttpsError("invalid-argument", "Target UID and action required.");

  const userRef = db.collection("users").doc(targetUid);
  const updates = { updatedAt: admin.firestore.FieldValue.serverTimestamp() };

  if (action === "BAN") {
    updates.isBanned = true;
    updates.accountStatus = "BANNED";
    updates.statusCoinId = "banned";
  } else if (action === "UNBAN") {
    updates.isBanned = false;
    updates.accountStatus = "ACTIVE";
    updates.statusCoinId = "default";
  } else if (action === "MUTE") {
    updates.isMuted = true;
  } else if (action === "UNMUTE") {
    updates.isMuted = false;
  }

  await userRef.update(updates);

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: `MODERATE_USER_${action}`,
    targetType: "user",
    targetId: targetUid,
    metadata: { reason }
  });

  return { success: true, targetUid, action };
});

/**
 * Moderate Content Item
 */
exports.moderateContentItem = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "moderator", "content_admin"]);
  const { collectionName, itemId, action, reason } = data;

  if (!collectionName || !itemId) throw new functions.https.HttpsError("invalid-argument", "Missing parameters.");

  const docRef = db.collection(collectionName).doc(itemId);
  if (action === "DELETE") {
    await docRef.delete();
  } else if (action === "HIDE") {
    await docRef.update({ isHidden: true, updatedAt: admin.firestore.FieldValue.serverTimestamp() });
  }

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: `MODERATE_CONTENT_${action}`,
    targetType: collectionName,
    targetId: itemId,
    metadata: { reason }
  });

  return { success: true, itemId, action };
});
