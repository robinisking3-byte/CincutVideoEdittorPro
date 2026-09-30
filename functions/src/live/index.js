const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Admin: Create Live Stream
 */
exports.createLive = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { title, hlsStreamUrl, category = "Trailer Editing", description = "" } = data;

  if (!title || !hlsStreamUrl) {
    throw new functions.https.HttpsError("invalid-argument", "Title and HLS Stream URL required.");
  }

  const streamRef = db.collection("liveStreams").doc();
  const streamData = {
    id: streamRef.id,
    creatorId: adminInfo.uid,
    creatorName: "CineCut Official",
    title,
    description,
    hlsStreamUrl,
    viewerCount: 0,
    isLive: false,
    status: "SCHEDULED",
    category,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await streamRef.set(streamData);
  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "CREATE_LIVE_STREAM",
    targetType: "liveStream",
    targetId: streamRef.id,
    metadata: { title }
  });

  return { success: true, stream: streamData };
});

/**
 * Update Live Stream
 */
exports.updateLive = functions.https.onCall(async (data, context) => {
  assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { streamId, updates } = data;
  if (!streamId) throw new functions.https.HttpsError("invalid-argument", "Stream ID required.");

  const cleanUpdates = { updatedAt: admin.firestore.FieldValue.serverTimestamp() };
  const allowed = ["title", "description", "hlsStreamUrl", "category", "isLive", "status"];
  for (const k of allowed) {
    if (updates && updates[k] !== undefined) cleanUpdates[k] = updates[k];
  }

  await db.collection("liveStreams").doc(streamId).update(cleanUpdates);
  return { success: true, streamId };
});

/**
 * Publish / Start Live
 */
exports.publishLive = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { streamId } = data;

  await db.collection("liveStreams").doc(streamId).update({
    isLive: true,
    status: "LIVE",
    startedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "START_LIVE_STREAM",
    targetType: "liveStream",
    targetId: streamId
  });

  return { success: true, streamId, status: "LIVE" };
});

/**
 * End Live Stream
 */
exports.endLive = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { streamId } = data;

  await db.collection("liveStreams").doc(streamId).update({
    isLive: false,
    status: "ENDED",
    endedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "END_LIVE_STREAM",
    targetType: "liveStream",
    targetId: streamId
  });

  return { success: true, streamId, status: "ENDED" };
});

/**
 * Delete Live Stream
 */
exports.deleteLive = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { streamId } = data;
  await db.collection("liveStreams").doc(streamId).delete();
  return { success: true, streamId };
});

/**
 * Update Live State (viewer count aggregation, state broadcast)
 */
exports.updateLiveState = functions.https.onCall(async (data, context) => {
  const { streamId, viewerDelta = 0 } = data;
  if (!streamId) throw new functions.https.HttpsError("invalid-argument", "Stream ID required.");

  await db.collection("liveStreams").doc(streamId).update({
    viewerCount: admin.firestore.FieldValue.increment(viewerDelta),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true };
});
