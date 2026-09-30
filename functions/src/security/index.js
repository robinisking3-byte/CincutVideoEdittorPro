const admin = require("firebase-admin");
const functions = require("firebase-functions");

const db = admin.firestore();

/**
 * Asserts caller is authenticated
 */
function assertAuthenticated(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication is required to perform this action."
    );
  }
  return context.auth.uid;
}

/**
 * Asserts caller has privileged admin custom claims
 */
function assertAdminRole(context, allowedRoles = ["super_admin", "admin"]) {
  assertAuthenticated(context);
  const token = context.auth.token || {};
  const isAdmin = token.admin === true || token.admin === "true";
  const userRole = token.role || "";

  if (!isAdmin || !allowedRoles.includes(userRole)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Administrative authorization with verified custom claims is required."
    );
  }
  return { uid: context.auth.uid, role: userRole };
}

/**
 * Securely writes an audit log to /auditLogs
 */
async function recordAuditLog({
  actorUid,
  actorRole = "user",
  action,
  targetType = "system",
  targetId = "",
  metadata = {},
  status = "SUCCESS"
}) {
  try {
    await db.collection("auditLogs").add({
      actorUid: actorUid || "anonymous",
      actorRole: actorRole || "unknown",
      action,
      targetType,
      targetId,
      metadata,
      status,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });
  } catch (err) {
    console.error("Failed to write audit log:", err);
  }
}

/**
 * Idempotency verification
 */
async function checkIdempotency(key, handler) {
  if (!key) {
    return await handler();
  }

  const docRef = db.collection("idempotencyKeys").doc(key);
  const snap = await docRef.get();
  if (snap.exists) {
    const data = snap.data();
    return { ...data.result, fromIdempotencyCache: true };
  }

  const result = await handler();
  try {
    await docRef.set({
      key,
      result,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  } catch (e) {
    console.warn("Failed to save idempotency key:", e);
  }
  return result;
}

module.exports = {
  assertAuthenticated,
  assertAdminRole,
  recordAuditLog,
  checkIdempotency
};
