const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated, assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Create Support Ticket
 */
exports.createTicket = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { subject, category = "Other", description, priority = "MEDIUM", attachmentUrl = "", relatedOrderId = "" } = data;

  if (!subject || !description) {
    throw new functions.https.HttpsError("invalid-argument", "Subject and description required.");
  }

  // Generate sequential ticket number #CC-100xxx
  const counterRef = db.collection("counters").doc("supportTicketNumber");
  let ticketId = "";

  await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(counterRef);
    const num = (doc.exists ? (doc.data().currentNumber || 100000) : 100000) + 1;
    transaction.set(counterRef, { currentNumber: num }, { merge: true });
    ticketId = `CC-${num}`;
  });

  const userDoc = await db.collection("users").doc(uid).get();
  const userName = userDoc.exists ? (userDoc.data().displayName || "Creator") : "Creator";

  const ticketData = {
    ticketId,
    id: ticketId,
    userId: uid,
    userName,
    subject: subject.trim(),
    category,
    description: description.trim(),
    priority,
    status: "OPEN",
    attachmentUrl,
    relatedOrderId,
    assignedAdminId: null,
    assignedAdminName: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  const ticketRef = db.collection("supportTickets").doc(ticketId);
  await ticketRef.set(ticketData);

  // Initial message
  await ticketRef.collection("messages").add({
    senderId: uid,
    senderName: userName,
    isAdmin: false,
    message: description.trim(),
    attachmentUrl,
    isInternalNote: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, ticketId, ticket: ticketData };
});

/**
 * Reply to Support Ticket
 */
exports.replyTicket = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { ticketId, message, attachmentUrl = "" } = data;

  if (!ticketId || !message) {
    throw new functions.https.HttpsError("invalid-argument", "Ticket ID and message required.");
  }

  const ticketRef = db.collection("supportTickets").doc(ticketId);
  const ticketDoc = await ticketRef.get();
  if (!ticketDoc.exists) throw new functions.https.HttpsError("not-found", "Ticket not found.");

  const ticketData = ticketDoc.data();
  const token = context.auth.token || {};
  const isAdmin = token.admin === true || token.admin === "true";

  if (!isAdmin && ticketData.userId !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized to reply to this ticket.");
  }

  const userDoc = await db.collection("users").doc(uid).get();
  const senderName = userDoc.exists ? (userDoc.data().displayName || (isAdmin ? "CineCut Support" : "Creator")) : (isAdmin ? "CineCut Support" : "Creator");

  await ticketRef.collection("messages").add({
    senderId: uid,
    senderName,
    isAdmin,
    message: message.trim(),
    attachmentUrl,
    isInternalNote: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Update status based on who replied
  const nextStatus = isAdmin ? "WAITING_FOR_USER" : "IN_PROGRESS";
  await ticketRef.update({
    status: nextStatus,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Notify user if admin replied
  if (isAdmin && ticketData.userId) {
    await db.collection("users").doc(ticketData.userId).collection("notifications").add({
      type: "SUPPORT_UPDATE",
      title: `Update on Ticket #${ticketId}`,
      body: `Support team replied: "${message.substring(0, 60)}..."`,
      ticketId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      read: false
    });
  }

  return { success: true };
});

/**
 * Add Internal Note (Admin only, STRICTLY invisible to normal users)
 */
exports.addInternalNote = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "support", "moderator"]);
  const { ticketId, note } = data;

  if (!ticketId || !note) throw new functions.https.HttpsError("invalid-argument", "Ticket ID and note required.");

  const ticketRef = db.collection("supportTickets").doc(ticketId);
  await ticketRef.collection("messages").add({
    senderId: adminInfo.uid,
    senderName: "Staff Internal Note",
    isAdmin: true,
    message: `[CONFIDENTIAL INTERNAL NOTE] ${note.trim()}`,
    isInternalNote: true,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "ADD_SUPPORT_INTERNAL_NOTE",
    targetType: "supportTicket",
    targetId: ticketId
  });

  return { success: true };
});

/**
 * Assign Support Ticket (Admin only)
 */
exports.assignTicket = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "support"]);
  const { ticketId, adminUid, adminName } = data;

  await db.collection("supportTickets").doc(ticketId).update({
    assignedAdminId: adminUid || adminInfo.uid,
    assignedAdminName: adminName || "Support Specialist",
    status: "IN_PROGRESS",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, ticketId };
});

/**
 * Change Ticket Status
 */
exports.changeTicketStatus = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "support"]);
  const { ticketId, status } = data;
  const valid = ["OPEN", "IN_PROGRESS", "WAITING_FOR_USER", "RESOLVED", "CLOSED"];
  if (!valid.includes(status)) throw new functions.https.HttpsError("invalid-argument", "Invalid status.");

  await db.collection("supportTickets").doc(ticketId).update({
    status,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, ticketId, status };
});

/**
 * Reopen Ticket
 */
exports.reopenTicket = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { ticketId } = data;

  const ticketRef = db.collection("supportTickets").doc(ticketId);
  const doc = await ticketRef.get();
  if (!doc.exists) throw new functions.https.HttpsError("not-found", "Ticket not found.");

  await ticketRef.update({
    status: "OPEN",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, ticketId, status: "OPEN" };
});

/**
 * Close Ticket
 */
exports.closeTicket = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { ticketId } = data;

  await db.collection("supportTickets").doc(ticketId).update({
    status: "CLOSED",
    closedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, ticketId, status: "CLOSED" };
});
