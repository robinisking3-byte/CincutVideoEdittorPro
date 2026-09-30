const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Super Admin: Assign User Role
 */
exports.assignUserRole = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin"]);
  const { targetUid, role } = data;
  const validRoles = ["super_admin", "admin", "moderator", "support", "content_admin", "user"];

  if (!targetUid || !validRoles.includes(role)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target UID or role.");
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
    action: "ASSIGN_USER_ROLE",
    targetType: "user",
    targetId: targetUid,
    metadata: { role }
  });

  return { success: true, targetUid, role };
});

/**
 * Get Admin Dashboard Aggregations
 */
exports.getAdminDashboardAggregations = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin", "support", "moderator", "content_admin"]);

  const [usersSnap, ordersSnap, ticketsSnap, auditSnap] = await Promise.all([
    db.collection("users").count().get(),
    db.collection("paymentOrders").where("status", "==", "PAID").count().get(),
    db.collection("supportTickets").where("status", "in", ["OPEN", "IN_PROGRESS"]).count().get(),
    db.collection("auditLogs").orderBy("timestamp", "desc").limit(20).get()
  ]);

  const recentLogs = auditSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));

  return {
    success: true,
    totalUsers: usersSnap.data().count,
    paidOrders: ordersSnap.data().count,
    openTickets: ticketsSnap.data().count,
    systemStatus: "HEALTHY",
    uptimeSeconds: process.uptime(),
    recentAuditLogs: recentLogs
  };
});

/**
 * Fetch Audit Logs
 */
exports.fetchAuditLogs = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin"]);
  const limit = data.limit || 50;

  const snap = await db.collection("auditLogs")
    .orderBy("timestamp", "desc")
    .limit(limit)
    .get();

  const logs = snap.docs.map(d => ({ id: d.id, ...d.data() }));
  return { success: true, logs };
});
