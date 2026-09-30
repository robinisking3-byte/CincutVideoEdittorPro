const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated } = require("../security");

const db = admin.firestore();

/**
 * Create Collaboration Operation (Operation-based collaborative editing model)
 */
exports.createOperation = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, type, payload, baseVersion = 1, clientId } = data;

  if (!projectId || !type) {
    throw new functions.https.HttpsError("invalid-argument", "Project ID and Operation type required.");
  }

  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists) throw new functions.https.HttpsError("not-found", "Project not found.");

  // Check editing permission
  const collabDoc = await projRef.collection("collaborators").doc(uid).get();
  const role = collabDoc.exists ? collabDoc.data().role : (projDoc.data().ownerId === uid ? "OWNER" : null);

  if (!role || ["VIEWER", "REVIEWER"].includes(role)) {
    throw new functions.https.HttpsError("permission-denied", "Insufficient permissions to execute operations.");
  }

  const opRef = projRef.collection("operations").doc();
  const operationId = opRef.id;

  const operationData = {
    operationId,
    userId: uid,
    type,
    payload: payload || {},
    baseVersion,
    clientId: clientId || "client",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await opRef.set(operationData);

  // Update project version & timestamp
  await projRef.update({
    version: admin.firestore.FieldValue.increment(1),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, operationId, operationData };
});

/**
 * Validate Operation
 */
exports.validateOperation = functions.https.onCall(async (data, context) => {
  assertAuthenticated(context);
  const { operation } = data;
  if (!operation || !operation.type) return { valid: false, reason: "Missing operation type" };

  const validTypes = [
    "ADD_CLIP", "DELETE_CLIP", "TRIM_CLIP", "MOVE_CLIP", "SPLIT_CLIP",
    "UPDATE_TEXT", "UPDATE_AUDIO", "ADD_EFFECT", "REMOVE_EFFECT",
    "MOVE_TRACK", "CHANGE_SPEED", "ADD_KEYFRAME", "DELETE_KEYFRAME"
  ];

  if (!validTypes.includes(operation.type)) {
    return { valid: false, reason: `Unknown operation type ${operation.type}` };
  }

  return { valid: true };
});

/**
 * Apply Operation (atomic batch or server checkpoint)
 */
exports.applyOperation = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, operationId } = data;

  const opRef = db.collection("projects").doc(projectId).collection("operations").doc(operationId);
  const opDoc = await opRef.get();
  if (!opDoc.exists) throw new functions.https.HttpsError("not-found", "Operation not found.");

  await opRef.update({ applied: true, appliedAt: admin.firestore.FieldValue.serverTimestamp() });
  return { success: true, operationId };
});

/**
 * Create Project Version Checkpoint
 */
exports.createVersion = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, versionName, changeSummary } = data;

  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists) throw new functions.https.HttpsError("not-found", "Project not found.");

  const currentVersion = projDoc.data().version || 1;
  const userDoc = await db.collection("users").doc(uid).get();
  const userName = userDoc.exists ? (userDoc.data().displayName || "Creator") : "Creator";

  const verRef = projRef.collection("versions").doc();
  const verData = {
    id: verRef.id,
    projectId,
    versionNumber: currentVersion,
    name: versionName || `v${currentVersion}`,
    authorId: uid,
    authorName: userName,
    changeSummary: changeSummary || "Project checkpoint",
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  };

  await verRef.set(verData);
  return { success: true, version: verData };
});

/**
 * Add Comment with Timecode
 */
exports.addComment = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, text, timecodeMs = 0, mentions = [] } = data;
  if (!projectId || !text) throw new functions.https.HttpsError("invalid-argument", "Missing parameters.");

  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists) throw new functions.https.HttpsError("not-found", "Project not found.");

  const userDoc = await db.collection("users").doc(uid).get();
  const userData = userDoc.data() || {};

  const commRef = projRef.collection("comments").doc();
  const commentData = {
    id: commRef.id,
    projectId,
    authorId: uid,
    authorName: userData.displayName || "Creator",
    authorAvatar: userData.photoUrl || "",
    text,
    timecodeMs,
    mentions,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await commRef.set(commentData);

  // Notify mentioned users
  if (Array.isArray(mentions)) {
    for (const uname of mentions) {
      const uDoc = await db.collection("usernames").doc(uname.toLowerCase().trim()).get();
      if (uDoc.exists) {
        const targetUid = uDoc.data().uid;
        if (targetUid !== uid) {
          await db.collection("users").doc(targetUid).collection("notifications").add({
            type: "MENTION",
            title: "You were mentioned",
            body: `${userData.displayName || "Someone"} mentioned you on "${projDoc.data().title}".`,
            projectId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false
          });
        }
      }
    }
  }

  return { success: true, comment: commentData };
});

/**
 * Mention User Helper
 */
exports.mentionUser = functions.https.onCall(async (data, context) => {
  assertAuthenticated(context);
  const { username } = data;
  const uname = (username || "").toLowerCase().trim();
  const doc = await db.collection("usernames").doc(uname).get();
  if (!doc.exists) return { found: false };
  const uid = doc.data().uid;
  const userDoc = await db.collection("users").doc(uid).get();
  return { found: true, uid, profile: userDoc.data() };
});
