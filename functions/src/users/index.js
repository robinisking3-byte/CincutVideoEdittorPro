const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Update Profile
 */
exports.updateProfile = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { displayName, bio, photoUrl } = data;

  const updates = {
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  if (typeof displayName === "string") updates.displayName = displayName.trim().substring(0, 50);
  if (typeof bio === "string") updates.bio = bio.trim().substring(0, 300);
  if (typeof photoUrl === "string") updates.photoUrl = photoUrl;

  await db.collection("users").doc(uid).set(updates, { merge: true });
  return { success: true, updates };
});

/**
 * Update Privacy Settings
 */
exports.updatePrivacy = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { isCoinBalancePublic, allowProjectInvites, allowTagging } = data;

  const privacy = {
    isCoinBalancePublic: !!isCoinBalancePublic,
    allowProjectInvites: allowProjectInvites !== false,
    allowTagging: allowTagging !== false
  };

  await db.collection("users").doc(uid).update({
    privacy,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, privacy };
});

/**
 * Update Creator Status
 */
exports.updateCreatorStatus = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid, creatorStatus, verificationStatus } = data;
  if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "Target UID required.");

  const updates = {
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  if (creatorStatus) updates.creatorStatus = creatorStatus;
  if (verificationStatus) updates.verificationStatus = verificationStatus;

  await db.collection("users").doc(targetUid).update(updates);
  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "UPDATE_CREATOR_STATUS",
    targetType: "user",
    targetId: targetUid,
    metadata: { creatorStatus, verificationStatus }
  });

  return { success: true };
});

/**
 * Admin: Manage User Badges
 */
exports.manageUserBadges = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { targetUid, badgeName, grant, reason } = data;
  if (!targetUid || !badgeName) throw new functions.https.HttpsError("invalid-argument", "Missing parameters.");

  const userRef = db.collection("users").doc(targetUid);
  await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(userRef);
    if (!doc.exists) throw new functions.https.HttpsError("not-found", "User not found.");

    const badges = doc.data().badges || [];
    let updatedBadges;
    if (grant) {
      if (!badges.includes(badgeName)) updatedBadges = [...badges, badgeName];
      else updatedBadges = badges;
    } else {
      updatedBadges = badges.filter(b => b !== badgeName);
    }

    transaction.update(userRef, {
      badges: updatedBadges,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: grant ? "GRANT_BADGE" : "REVOKE_BADGE",
    targetType: "user",
    targetId: targetUid,
    metadata: { badgeName, reason }
  });

  return { success: true };
});

/**
 * Admin: Grant Founder Status (Limited to 100 Edition Numbers)
 */
exports.grantFounderStatus = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid, reason } = data;
  if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "Target UID required.");

  const counterRef = db.collection("counters").doc("founderCoinEdition");
  const userRef = db.collection("users").doc(targetUid);

  let assignedEdition = 0;
  await db.runTransaction(async (transaction) => {
    const counterDoc = await transaction.get(counterRef);
    const currentCount = counterDoc.exists ? (counterDoc.data().count || 0) : 0;
    if (currentCount >= 100) {
      throw new functions.https.HttpsError("failed-precondition", "All 100 Founder Edition Coins have already been issued.");
    }

    assignedEdition = currentCount + 1;
    transaction.set(counterRef, {
      count: assignedEdition,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    transaction.update(userRef, {
      membershipTier: "FOUNDER",
      statusCoinId: "founder",
      founderEditionNumber: assignedEdition,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "GRANT_FOUNDER_STATUS",
    targetType: "user",
    targetId: targetUid,
    metadata: { edition: assignedEdition, reason }
  });

  return { success: true, edition: assignedEdition };
});
