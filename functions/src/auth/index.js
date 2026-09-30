const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Setup Initial Super Admin Custom Claims
 * Strictly authorized for: "robinisking3@gmail.com"
 */
exports.setupInitialAdminClaim = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
  const designatedAdminEmail = "robinisking3@gmail.com";

  if (callerEmail !== designatedAdminEmail) {
    await recordAuditLog({
      actorUid: uid,
      actorRole: "user",
      action: "UNAUTHORIZED_ADMIN_CLAIM_ATTEMPT",
      targetType: "user",
      targetId: uid,
      metadata: { attemptedEmail: callerEmail },
      status: "REJECTED"
    });
    throw new functions.https.HttpsError(
      "permission-denied",
      "Admin access has not been granted to this account."
    );
  }

  const currentClaims = context.auth.token || {};
  await admin.auth().setCustomUserClaims(uid, {
    ...currentClaims,
    admin: true,
    role: "super_admin"
  });

  await db.collection("users").doc(uid).set({
    uid,
    email: designatedAdminEmail,
    role: "super_admin",
    admin: true,
    accountStatus: "ACTIVE",
    isVerified: true,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  await recordAuditLog({
    actorUid: uid,
    actorRole: "super_admin",
    action: "INITIAL_ADMIN_CLAIM_ESTABLISHED",
    targetType: "user",
    targetId: uid,
    metadata: { email: designatedAdminEmail }
  });

  return {
    success: true,
    message: "Super Admin custom claims granted. Please refresh session token."
  };
});

/**
 * Initialize User Profile on registration
 */
exports.initializeUserProfile = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const email = (context.auth.token.email || data.email || "").toLowerCase().trim();
  const displayName = data.displayName || (email.split("@")[0] || "Creator");
  const rawUsername = data.username || ("creator_" + uid.substring(0, 6));
  const normalized = rawUsername.toLowerCase().trim();

  const userRef = db.collection("users").doc(uid);
  const userSnap = await userRef.get();

  if (userSnap.exists) {
    return { success: true, profile: userSnap.data() };
  }

  // Transactionally claim username and create user document & coinAccount
  await db.runTransaction(async (transaction) => {
    const unameRef = db.collection("usernames").doc(normalized);
    const unameDoc = await transaction.get(unameRef);
    if (unameDoc.exists && unameDoc.data().uid !== uid) {
      throw new functions.https.HttpsError("already-exists", "Username is already taken.");
    }

    transaction.set(unameRef, {
      normalizedUsername: normalized,
      uid,
      claimedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    const newProfile = {
      uid,
      username: normalized,
      displayName,
      email,
      bio: "CineCut Mobile Creator",
      photoUrl: "",
      role: "user",
      admin: false,
      membershipTier: "FREE",
      accountStatus: "ACTIVE",
      creatorStatus: "NORMAL",
      verificationStatus: "UNVERIFIED",
      statusCoinId: "default",
      coinBalance: 0,
      badges: ["NEW_CREATOR"],
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      lastSeen: admin.firestore.FieldValue.serverTimestamp(),
      privacy: { isCoinBalancePublic: true, allowProjectInvites: true, allowTagging: true }
    };

    transaction.set(userRef, newProfile);

    // Coin account document
    const coinAccRef = db.collection("coinAccounts").doc(uid);
    transaction.set(coinAccRef, {
      uid,
      balance: 0,
      version: 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  return { success: true, message: "User profile initialized." };
});

/**
 * Validate Username format and availability
 */
exports.validateUsername = functions.https.onCall(async (data) => {
  const username = (data.username || "").trim();
  if (!username) {
    return { valid: false, reason: "Username cannot be empty." };
  }
  if (username.length < 3 || username.length > 20) {
    return { valid: false, reason: "Username must be between 3 and 20 characters." };
  }
  const regex = /^[a-zA-Z0-9_]+$/;
  if (!regex.test(username)) {
    return { valid: false, reason: "Username can only contain alphanumeric characters and underscores." };
  }

  const reserved = ["admin", "superadmin", "cinecut", "official", "moderator", "support", "help", "system", "root"];
  const normalized = username.toLowerCase();
  if (reserved.includes(normalized)) {
    return { valid: false, reason: "This username is reserved." };
  }

  const doc = await db.collection("usernames").doc(normalized).get();
  if (doc.exists) {
    return { valid: false, reason: "Username is already taken." };
  }

  return { valid: true, normalizedUsername: normalized };
});

/**
 * Claim Username
 */
exports.claimUsername = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const username = (data.username || "").trim();
  const normalized = username.toLowerCase();

  const reserved = ["admin", "superadmin", "cinecut", "official", "moderator", "support", "help", "system"];
  if (reserved.includes(normalized) || username.length < 3 || username.length > 20) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid or reserved username.");
  }

  await db.runTransaction(async (transaction) => {
    const unameRef = db.collection("usernames").doc(normalized);
    const unameDoc = await transaction.get(unameRef);
    if (unameDoc.exists && unameDoc.data().uid !== uid) {
      throw new functions.https.HttpsError("already-exists", "Username already taken.");
    }

    const userRef = db.collection("users").doc(uid);
    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "User profile not found.");
    }

    const oldUsername = (userDoc.data().username || "").toLowerCase();
    if (oldUsername && oldUsername !== normalized) {
      transaction.delete(db.collection("usernames").doc(oldUsername));
    }

    transaction.set(unameRef, {
      normalizedUsername: normalized,
      uid,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    transaction.update(userRef, {
      username: normalized,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  return { success: true, username: normalized };
});

/**
 * Suspend User (Moderator/Admin)
 */
exports.suspendUser = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "moderator"]);
  const { targetUid, reason } = data;
  if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "Target UID required.");

  await db.collection("users").doc(targetUid).update({
    accountStatus: "SUSPENDED",
    isSuspended: true,
    suspensionReason: reason || "Terms violation",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "SUSPEND_USER",
    targetType: "user",
    targetId: targetUid,
    metadata: { reason }
  });

  return { success: true };
});

/**
 * Restore User (Admin)
 */
exports.restoreUser = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin"]);
  const { targetUid } = data;
  if (!targetUid) throw new functions.https.HttpsError("invalid-argument", "Target UID required.");

  await db.collection("users").doc(targetUid).update({
    accountStatus: "ACTIVE",
    isSuspended: false,
    isBanned: false,
    suspensionReason: null,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "RESTORE_USER",
    targetType: "user",
    targetId: targetUid
  });

  return { success: true };
});

/**
 * Delete Account
 */
exports.deleteAccount = functions.https.onCall(async (data, context) => {
  const callerUid = assertAuthenticated(context);
  const targetUid = data.targetUid || callerUid;

  if (targetUid !== callerUid) {
    assertAdminRole(context, ["super_admin"]);
  }

  // Soft delete / anonymize user
  await db.collection("users").doc(targetUid).update({
    accountStatus: "DELETED",
    isDeleted: true,
    deletedAt: admin.firestore.FieldValue.serverTimestamp(),
    displayName: "Deleted Creator",
    photoUrl: "",
    bio: ""
  });

  await admin.auth().deleteUser(targetUid).catch(() => {});

  await recordAuditLog({
    actorUid: callerUid,
    action: "DELETE_ACCOUNT",
    targetType: "user",
    targetId: targetUid
  });

  return { success: true };
});

/**
 * Super Admin: Update Role
 */
exports.updateRole = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin"]);
  const { targetUid, role } = data;
  const validRoles = ["super_admin", "admin", "moderator", "support", "content_admin", "user"];
  if (!validRoles.includes(role)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid role specified.");
  }

  const isPrivileged = role !== "user";
  await admin.auth().setCustomUserClaims(targetUid, {
    admin: isPrivileged,
    role
  });

  await db.collection("users").doc(targetUid).update({
    role,
    admin: isPrivileged,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "ASSIGN_ROLE",
    targetType: "user",
    targetId: targetUid,
    metadata: { assignedRole: role }
  });

  return { success: true, targetUid, role };
});
