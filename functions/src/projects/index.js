const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { assertAuthenticated } = require("../security");

const db = admin.firestore();

/**
 * Create Project
 */
exports.createProject = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const title = (data.title || "Untitled Cinematic").trim();
  const aspectRatio = data.aspectRatio || "16:9";

  const projectRef = db.collection("projects").doc();
  const projectId = projectRef.id;

  const projectData = {
    projectId,
    id: projectId,
    ownerId: uid,
    title,
    aspectRatio,
    resolutionWidth: data.resolutionWidth || 1920,
    resolutionHeight: data.resolutionHeight || 1080,
    frameRate: data.frameRate || 30,
    durationMs: data.durationMs || 10000,
    isArchived: false,
    isCollaborative: false,
    collaboratorsCount: 1,
    version: 1,
    visibility: "PRIVATE",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await projectRef.set(projectData);

  // Set owner in collaborators subcollection
  await projectRef.collection("collaborators").doc(uid).set({
    userId: uid,
    role: "OWNER",
    joinedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, project: projectData };
});

/**
 * Update Project
 */
exports.updateProject = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, updates } = data;
  if (!projectId) throw new functions.https.HttpsError("invalid-argument", "Project ID required.");

  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists) throw new functions.https.HttpsError("not-found", "Project not found.");

  // Check collaborator role
  const collabDoc = await projRef.collection("collaborators").doc(uid).get();
  const role = collabDoc.exists ? collabDoc.data().role : (projDoc.data().ownerId === uid ? "OWNER" : null);

  if (!role || ["VIEWER", "REVIEWER"].includes(role)) {
    throw new functions.https.HttpsError("permission-denied", "Insufficient project permissions to edit.");
  }

  const allowedFields = ["title", "description", "aspectRatio", "durationMs", "thumbnailUri", "visibility"];
  const sanitized = { updatedAt: admin.firestore.FieldValue.serverTimestamp() };
  for (const f of allowedFields) {
    if (updates && updates[f] !== undefined) sanitized[f] = updates[f];
  }

  await projRef.update(sanitized);
  return { success: true };
});

/**
 * Delete Project
 */
exports.deleteProject = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId } = data;
  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists) throw new functions.https.HttpsError("not-found", "Project not found.");

  if (projDoc.data().ownerId !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Only the project owner can delete this project.");
  }

  await projRef.delete();
  return { success: true };
});

/**
 * Duplicate Project
 */
exports.duplicateProject = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId } = data;
  const origRef = db.collection("projects").doc(projectId);
  const origDoc = await origRef.get();
  if (!origDoc.exists) throw new functions.https.HttpsError("not-found", "Original project not found.");

  const origData = origDoc.data();
  const newRef = db.collection("projects").doc();
  const newId = newRef.id;

  const duplicatedData = {
    ...origData,
    projectId: newId,
    id: newId,
    ownerId: uid,
    title: `${origData.title || "Project"} (Copy)`,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await newRef.set(duplicatedData);
  await newRef.collection("collaborators").doc(uid).set({
    userId: uid,
    role: "OWNER",
    joinedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, newProjectId: newId, project: duplicatedData };
});

/**
 * Archive Project
 */
exports.archiveProject = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId } = data;
  const projRef = db.collection("projects").doc(projectId);
  await projRef.update({
    isArchived: true,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});

/**
 * Restore Project
 */
exports.restoreProject = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId } = data;
  const projRef = db.collection("projects").doc(projectId);
  await projRef.update({
    isArchived: false,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});

/**
 * Invite Collaborator
 */
exports.inviteCollaborator = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, receiverId, role = "EDITOR" } = data;
  if (!projectId || !receiverId) throw new functions.https.HttpsError("invalid-argument", "Missing parameters.");

  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists) throw new functions.https.HttpsError("not-found", "Project not found.");

  // Check if caller can manage members
  const collabDoc = await projRef.collection("collaborators").doc(uid).get();
  const callerRole = collabDoc.exists ? collabDoc.data().role : (projDoc.data().ownerId === uid ? "OWNER" : null);

  if (!callerRole || !["OWNER", "ADMIN"].includes(callerRole)) {
    throw new functions.https.HttpsError("permission-denied", "Only project Owners or Admins can invite collaborators.");
  }

  const invRef = projRef.collection("invitations").doc(receiverId);
  await invRef.set({
    projectId,
    projectTitle: projDoc.data().title || "Untitled",
    senderId: uid,
    receiverId,
    role,
    status: "PENDING",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Notify recipient
  await db.collection("users").doc(receiverId).collection("notifications").add({
    type: "PROJECT_INVITE",
    title: "Project Collaboration Invite",
    body: `You were invited to collaborate on "${projDoc.data().title}".`,
    projectId,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    read: false
  });

  return { success: true };
});

/**
 * Accept Invitation
 */
exports.acceptInvitation = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId } = data;
  const projRef = db.collection("projects").doc(projectId);
  const invRef = projRef.collection("invitations").doc(uid);
  const invDoc = await invRef.get();
  if (!invDoc.exists || invDoc.data().status !== "PENDING") {
    throw new functions.https.HttpsError("not-found", "No pending invitation found.");
  }

  const role = invDoc.data().role || "EDITOR";
  const userDoc = await db.collection("users").doc(uid).get();
  const userData = userDoc.data() || {};

  const batch = db.batch();
  batch.update(invRef, { status: "ACCEPTED", updatedAt: admin.firestore.FieldValue.serverTimestamp() });
  batch.set(projRef.collection("collaborators").doc(uid), {
    userId: uid,
    username: userData.username || "creator",
    displayName: userData.displayName || "Collaborator",
    avatarUrl: userData.photoUrl || "",
    role,
    joinedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  batch.update(projRef, {
    isCollaborative: true,
    collaboratorsCount: admin.firestore.FieldValue.increment(1)
  });
  await batch.commit();

  return { success: true, role };
});

/**
 * Reject Invitation
 */
exports.rejectInvitation = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId } = data;
  await db.collection("projects").doc(projectId).collection("invitations").doc(uid).update({
    status: "REJECTED",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});

/**
 * Remove Collaborator
 */
exports.removeCollaborator = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, targetUid } = data;
  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();

  const isOwner = projDoc.exists && projDoc.data().ownerId === uid;
  const isSelf = targetUid === uid;

  if (!isOwner && !isSelf) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized to remove collaborator.");
  }

  await projRef.collection("collaborators").doc(targetUid).delete();
  await projRef.update({
    collaboratorsCount: admin.firestore.FieldValue.increment(-1)
  });

  return { success: true };
});

/**
 * Change Collaborator Role
 */
exports.changeCollaboratorRole = functions.https.onCall(async (data, context) => {
  const uid = assertAuthenticated(context);
  const { projectId, targetUid, newRole } = data;
  const validRoles = ["ADMIN", "EDITOR", "CONTRIBUTOR", "REVIEWER", "VIEWER"];
  if (!validRoles.includes(newRole)) throw new functions.https.HttpsError("invalid-argument", "Invalid project role.");

  const projRef = db.collection("projects").doc(projectId);
  const projDoc = await projRef.get();
  if (!projDoc.exists || projDoc.data().ownerId !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Only project owners can modify roles.");
  }

  await projRef.collection("collaborators").doc(targetUid).update({
    role: newRole,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, newRole };
});
