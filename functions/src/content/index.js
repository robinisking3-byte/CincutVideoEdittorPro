const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAdminRole, recordAuditLog } = require("../security");

const db = admin.firestore();

/**
 * Content Admin: Create Video
 */
exports.createVideo = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { title, description, videoUrl, thumbnailUrl, category, tags = [], isFeatured = false } = data;

  if (!title || !videoUrl) {
    throw new functions.https.HttpsError("invalid-argument", "Title and video URL are required.");
  }

  const docRef = db.collection("m3u8Videos").doc();
  const videoData = {
    id: docRef.id,
    title,
    description: description || "",
    videoUrl,
    m3u8Url: videoUrl,
    thumbnailUrl: thumbnailUrl || "",
    category: category || "General",
    tags: Array.isArray(tags) ? tags : [],
    status: "DRAFT",
    isFeatured: !!isFeatured,
    views: 0,
    likes: 0,
    createdBy: adminInfo.uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await docRef.set(videoData);
  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "CREATE_VIDEO_CONTENT",
    targetType: "video",
    targetId: docRef.id,
    metadata: { title }
  });

  return { success: true, video: videoData };
});

/**
 * Update Video
 */
exports.updateVideo = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { videoId, updates } = data;
  if (!videoId) throw new functions.https.HttpsError("invalid-argument", "Video ID required.");

  const docRef = db.collection("m3u8Videos").doc(videoId);
  const cleanUpdates = { updatedAt: admin.firestore.FieldValue.serverTimestamp() };
  const allowed = ["title", "description", "videoUrl", "m3u8Url", "thumbnailUrl", "category", "tags", "isFeatured", "status"];
  for (const k of allowed) {
    if (updates && updates[k] !== undefined) cleanUpdates[k] = updates[k];
  }

  await docRef.update(cleanUpdates);
  return { success: true, videoId };
});

/**
 * Publish Video
 */
exports.publishVideo = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { videoId } = data;

  await db.collection("m3u8Videos").doc(videoId).update({
    status: "PUBLISHED",
    publishedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "PUBLISH_VIDEO",
    targetType: "video",
    targetId: videoId
  });

  return { success: true, videoId, status: "PUBLISHED" };
});

/**
 * Unpublish Video
 */
exports.unpublishVideo = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { videoId } = data;

  await db.collection("m3u8Videos").doc(videoId).update({
    status: "UNPUBLISHED",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, videoId, status: "UNPUBLISHED" };
});

/**
 * Delete Video
 */
exports.deleteVideo = functions.https.onCall(async (data, context) => {
  const adminInfo = assertAdminRole(context, ["super_admin", "admin", "content_admin"]);
  const { videoId } = data;
  await db.collection("m3u8Videos").doc(videoId).delete();

  await recordAuditLog({
    actorUid: adminInfo.uid,
    actorRole: adminInfo.role,
    action: "DELETE_VIDEO",
    targetType: "video",
    targetId: videoId
  });

  return { success: true, videoId };
});
