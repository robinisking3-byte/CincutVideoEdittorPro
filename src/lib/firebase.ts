import { initializeApp, getApps, getApp, FirebaseApp } from 'firebase/app';
import { 
  getFirestore, 
  Firestore, 
  collection, 
  doc, 
  setDoc, 
  getDoc,
  getDocs, 
  onSnapshot, 
  writeBatch,
  query, 
  where,
  orderBy, 
  limit,
  serverTimestamp,
  updateDoc,
  deleteDoc,
  addDoc,
  Unsubscribe
} from 'firebase/firestore';

import {
  getAuth,
  Auth,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  updateProfile,
  updatePassword,
  signOut,
  onAuthStateChanged,
  User as FirebaseUser
} from 'firebase/auth';

import type { 
  User, 
  SecurityIncident, 
  LockedIp, 
  CoinTransaction, 
  FestiveCampaign, 
  AiToolConfig, 
  VideoTemplate, 
  SystemAuditLog,
  FriendRequest,
  ChatMessage,
  GroupProject,
  GroupProjectMember,
  PremiumRoleType
} from '../types';

import rawConfig from '../../firebase-applet-config.json';

export interface FirebaseConfigType {
  projectId: string;
  appId: string;
  apiKey: string;
  authDomain: string;
  firestoreDatabaseId?: string;
  storageBucket?: string;
  messagingSenderId?: string;
  oAuthClientId?: string;
}

export const firebaseConfig: FirebaseConfigType = rawConfig as FirebaseConfigType;

let app: FirebaseApp | null = null;
let db: Firestore | null = null;
let auth: Auth | null = null;
let initError: string | null = null;

try {
  if (getApps().length === 0) {
    app = initializeApp(firebaseConfig);
  } else {
    app = getApp();
  }

  // Initialize Auth
  try {
    auth = getAuth(app);
  } catch (authErr) {
    console.warn('Firebase Auth initialization notice:', authErr);
  }

  // Initialize Firestore with custom databaseId if specified
  if (firebaseConfig.firestoreDatabaseId && firebaseConfig.firestoreDatabaseId.length > 0) {
    try {
      db = getFirestore(app, firebaseConfig.firestoreDatabaseId);
    } catch {
      db = getFirestore(app);
    }
  } else {
    db = getFirestore(app);
  }
} catch (err: unknown) {
  initError = err instanceof Error ? err.message : String(err);
  console.warn('Firebase initialization notice:', initError);
}

export { app, db, auth, initError };

export const isFirebaseActive = (): boolean => {
  return db !== null && !initError;
};

// Firestore Collections definition
export const COLLECTIONS = {
  USERS: 'users',
  FRIEND_REQUESTS: 'friend_requests',
  CHAT_MESSAGES: 'chat_messages',
  GROUP_PROJECTS: 'group_projects',
  SECURITY_INCIDENTS: 'security_incidents',
  LOCKED_IPS: 'locked_ips',
  COIN_TRANSACTIONS: 'coin_transactions',
  CAMPAIGNS: 'campaigns',
  AI_TOOLS: 'ai_tools',
  TEMPLATES: 'templates',
  AUDIT_LOGS: 'audit_logs',
  SYSTEM_STATUS: 'system_status'
} as const;

/* =========================================================================
 * 1. FIREBASE AUTHENTICATION (Google OAuth & Email/Password)
 * ========================================================================= */

const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({
  prompt: 'select_account'
});

/**
 * Sign in using Firebase Google OAuth Popup
 */
export async function signInWithGoogle(): Promise<{ success: boolean; user?: FirebaseUser; error?: string }> {
  if (!auth) {
    return { success: false, error: 'Firebase Auth is not initialized.' };
  }
  try {
    const result = await signInWithPopup(auth, googleProvider);
    return { success: true, user: result.user };
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Google OAuth Sign-In failed';
    console.error('Google Sign In Error:', message);
    return { success: false, error: message };
  }
}

/**
 * Register with Email and Password
 */
export async function registerWithEmail(
  email: string, 
  pass: string, 
  displayName: string
): Promise<{ success: boolean; user?: FirebaseUser; error?: string }> {
  if (!auth) {
    return { success: false, error: 'Firebase Auth is not initialized.' };
  }
  try {
    const cred = await createUserWithEmailAndPassword(auth, email, pass);
    if (cred.user) {
      await updateProfile(cred.user, { displayName });
    }
    return { success: true, user: cred.user };
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Registration failed';
    return { success: false, error: message };
  }
}

/**
 * Login with Email and Password
 */
export async function loginWithEmail(
  email: string, 
  pass: string
): Promise<{ success: boolean; user?: FirebaseUser; error?: string }> {
  if (!auth) {
    return { success: false, error: 'Firebase Auth is not initialized.' };
  }
  try {
    const cred = await signInWithEmailAndPassword(auth, email, pass);
    return { success: true, user: cred.user };
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Login failed';
    return { success: false, error: message };
  }
}

/**
 * Update current user password
 */
export async function modifyUserPassword(newPass: string): Promise<{ success: boolean; error?: string }> {
  if (!auth || !auth.currentUser) {
    return { success: false, error: 'No authenticated user session found.' };
  }
  try {
    await updatePassword(auth.currentUser, newPass);
    return { success: true };
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Password update failed';
    return { success: false, error: message };
  }
}

/**
 * Sign out from Firebase
 */
export async function logoutFirebaseAuth(): Promise<void> {
  if (auth) {
    await signOut(auth);
  }
}

/**
 * Listen to Auth changes
 */
export function onAuthStateListener(callback: (user: FirebaseUser | null) => void): Unsubscribe | null {
  if (!auth) return null;
  return onAuthStateChanged(auth, callback);
}

/* =========================================================================
 * 2. FIRESTORE REAL-TIME CHATS & MESSAGING (Live Database)
 * ========================================================================= */

/**
 * Real-time listener for private or project chat messages
 */
export function subscribeToChatMessages(
  chatId: string, 
  onMessages: (msgs: ChatMessage[]) => void
): Unsubscribe | null {
  if (!db) return null;
  try {
    const q = query(
      collection(db, COLLECTIONS.CHAT_MESSAGES),
      where('chatId', '==', chatId),
      orderBy('timestamp', 'asc'),
      limit(100)
    );

    return onSnapshot(q, (snapshot) => {
      const msgs: ChatMessage[] = [];
      snapshot.forEach(docSnap => {
        msgs.push({ id: docSnap.id, ...(docSnap.data() as Omit<ChatMessage, 'id'>) });
      });
      onMessages(msgs);
    }, (err) => {
      console.warn('Chat subscription notice:', err.message);
    });
  } catch (err) {
    console.warn('Failed to subscribe to chat:', err);
    return null;
  }
}

/**
 * Send a live chat message to Firestore
 */
export async function sendChatMessage(msg: Omit<ChatMessage, 'id'>): Promise<string | null> {
  if (!db) return null;
  try {
    const docRef = await addDoc(collection(db, COLLECTIONS.CHAT_MESSAGES), {
      ...msg,
      createdAt: serverTimestamp()
    });
    return docRef.id;
  } catch (err) {
    console.error('Error sending chat message:', err);
    return null;
  }
}

/* =========================================================================
 * 3. FRIENDS SYSTEM & USER ID MANAGEMENT
 * ========================================================================= */

/**
 * Helper to generate a unique 6-digit User ID Tag (e.g., VID-84920)
 */
export function generateUserIdTag(): string {
  const rand = Math.floor(10000 + Math.random() * 90000);
  return `VID-${rand}`;
}

/**
 * Real-time listener for friend requests (both received and sent)
 */
export function subscribeToFriendRequests(
  userId: string,
  onRequests: (reqs: FriendRequest[]) => void
): Unsubscribe | null {
  if (!db) return null;
  try {
    const q = query(
      collection(db, COLLECTIONS.FRIEND_REQUESTS),
      where('receiverId', '==', userId),
      limit(50)
    );

    return onSnapshot(q, (snapshot) => {
      const reqs: FriendRequest[] = [];
      snapshot.forEach(docSnap => {
        reqs.push({ id: docSnap.id, ...(docSnap.data() as Omit<FriendRequest, 'id'>) });
      });
      onRequests(reqs);
    }, (err) => {
      console.warn('Friend requests subscription notice:', err.message);
    });
  } catch (e) {
    console.warn('Could not subscribe to friend requests:', e);
    return null;
  }
}

/**
 * Send a friend request to another user
 */
export async function sendFriendRequestToUser(
  request: Omit<FriendRequest, 'id'>
): Promise<{ success: boolean; message: string }> {
  if (!db) return { success: false, message: 'Firestore offline' };
  try {
    await addDoc(collection(db, COLLECTIONS.FRIEND_REQUESTS), {
      ...request,
      status: 'pending',
      createdAt: new Date().toISOString()
    });
    return { success: true, message: 'Friend request sent successfully!' };
  } catch (err) {
    return { success: false, message: err instanceof Error ? err.message : 'Failed to send request' };
  }
}

/**
 * Accept or decline a friend request
 */
export async function updateFriendRequestStatus(
  requestId: string,
  newStatus: 'accepted' | 'declined',
  senderId: string,
  receiverId: string
): Promise<void> {
  if (!db) return;
  try {
    const ref = doc(db, COLLECTIONS.FRIEND_REQUESTS, requestId);
    await updateDoc(ref, { status: newStatus });

    // If accepted, link the friends in user documents
    if (newStatus === 'accepted') {
      const senderDoc = doc(db, COLLECTIONS.USERS, senderId);
      const receiverDoc = doc(db, COLLECTIONS.USERS, receiverId);

      // Add to friends lists
      const senderSnap = await getDoc(senderDoc);
      if (senderSnap.exists()) {
        const sData = senderSnap.data() as User;
        const friends = Array.from(new Set([...(sData.friendsList || []), receiverId]));
        await updateDoc(senderDoc, { friendsList: friends });
      }

      const recvSnap = await getDoc(receiverDoc);
      if (recvSnap.exists()) {
        const rData = recvSnap.data() as User;
        const friends = Array.from(new Set([...(rData.friendsList || []), senderId]));
        await updateDoc(receiverDoc, { friendsList: friends });
      }
    }
  } catch (err) {
    console.error('Error updating friend request:', err);
  }
}

/**
 * Block or Unblock a user
 */
export async function toggleBlockUser(
  currentUserId: string,
  targetUserId: string,
  isBlocked: boolean
): Promise<void> {
  if (!db) return;
  try {
    const userDoc = doc(db, COLLECTIONS.USERS, currentUserId);
    const snap = await getDoc(userDoc);
    if (snap.exists()) {
      const data = snap.data() as User;
      let blocked = data.blockedUsers || [];
      let friends = data.friendsList || [];

      if (isBlocked) {
        blocked = Array.from(new Set([...blocked, targetUserId]));
        // remove from friends if blocked
        friends = friends.filter(id => id !== targetUserId);
      } else {
        blocked = blocked.filter(id => id !== targetUserId);
      }

      await updateDoc(userDoc, { blockedUsers: blocked, friendsList: friends });
    }
  } catch (err) {
    console.error('Error blocking user:', err);
  }
}

/**
 * Remove a friend
 */
export async function removeFriend(userId: string, friendId: string): Promise<void> {
  if (!db) return;
  try {
    const uDoc = doc(db, COLLECTIONS.USERS, userId);
    const fDoc = doc(db, COLLECTIONS.USERS, friendId);

    const uSnap = await getDoc(uDoc);
    if (uSnap.exists()) {
      const data = uSnap.data() as User;
      const updated = (data.friendsList || []).filter(id => id !== friendId);
      await updateDoc(uDoc, { friendsList: updated });
    }

    const fSnap = await getDoc(fDoc);
    if (fSnap.exists()) {
      const data = fSnap.data() as User;
      const updated = (data.friendsList || []).filter(id => id !== userId);
      await updateDoc(fDoc, { friendsList: updated });
    }
  } catch (err) {
    console.error('Error removing friend:', err);
  }
}

/* =========================================================================
 * 4. GROUP PROJECTS (Live Collaborative Workspace - Up to 5 Members)
 * ========================================================================= */

/**
 * Real-time listener for group projects
 */
export function subscribeToGroupProjects(
  onProjects: (projects: GroupProject[]) => void
): Unsubscribe | null {
  if (!db) return null;
  try {
    const q = query(collection(db, COLLECTIONS.GROUP_PROJECTS), orderBy('updatedAt', 'desc'), limit(20));
    return onSnapshot(q, (snapshot) => {
      const items: GroupProject[] = [];
      snapshot.forEach(docSnap => {
        items.push({ id: docSnap.id, ...(docSnap.data() as Omit<GroupProject, 'id'>) });
      });
      onProjects(items);
    }, (err) => {
      console.warn('Group projects subscription notice:', err.message);
    });
  } catch (err) {
    console.warn('Failed to subscribe to group projects:', err);
    return null;
  }
}

/**
 * Create a new Collaborative Group Project (Max 5 members)
 */
export async function createNewGroupProject(
  projectData: Omit<GroupProject, 'id' | 'createdAt' | 'updatedAt' | 'activeEditorsCount'>
): Promise<{ success: boolean; projectId?: string; error?: string }> {
  if (!db) return { success: false, error: 'Database offline' };
  try {
    const docRef = await addDoc(collection(db, COLLECTIONS.GROUP_PROJECTS), {
      ...projectData,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      activeEditorsCount: projectData.members.length
    });
    return { success: true, projectId: docRef.id };
  } catch (err) {
    return { success: false, error: err instanceof Error ? err.message : 'Creation failed' };
  }
}

/**
 * Add a member to a group project (strictly enforcing max 5 members limit)
 */
export async function addMemberToProject(
  projectId: string, 
  newMember: GroupProjectMember
): Promise<{ success: boolean; message: string }> {
  if (!db) return { success: false, message: 'Database offline' };
  try {
    const projectRef = doc(db, COLLECTIONS.GROUP_PROJECTS, projectId);
    const snap = await getDoc(projectRef);
    if (!snap.exists()) {
      return { success: false, message: 'Project does not exist.' };
    }

    const data = snap.data() as GroupProject;
    if (data.members.length >= 5) {
      return { success: false, message: 'Group project reached the maximum limit of 5 members.' };
    }

    if (data.members.some(m => m.userId === newMember.userId)) {
      return { success: false, message: 'User is already a member of this project.' };
    }

    const updatedMembers = [...data.members, newMember];
    await updateDoc(projectRef, {
      members: updatedMembers,
      activeEditorsCount: updatedMembers.length,
      updatedAt: new Date().toISOString()
    });

    return { success: true, message: `${newMember.username} joined the group project!` };
  } catch (err) {
    return { success: false, message: err instanceof Error ? err.message : 'Error joining project' };
  }
}

/**
 * Update collaborative project tracks or properties
 */
export async function updateProjectCollaboration(
  projectId: string,
  updates: Partial<GroupProject>
): Promise<void> {
  if (!db) return;
  try {
    const projectRef = doc(db, COLLECTIONS.GROUP_PROJECTS, projectId);
    await updateDoc(projectRef, {
      ...updates,
      updatedAt: new Date().toISOString()
    });
  } catch (err) {
    console.error('Error updating project:', err);
  }
}

/* =========================================================================
 * 5. USER & ROLE MANAGEMENT
 * ========================================================================= */

/**
 * Update user profile (username, displayName, bio, avatar)
 */
export async function updateUserProfile(
  userId: string, 
  profile: Partial<User>
): Promise<void> {
  if (!db) return;
  try {
    const userRef = doc(db, COLLECTIONS.USERS, userId);
    await setDoc(userRef, profile, { merge: true });
  } catch (err) {
    console.error('Error updating profile in Firestore:', err);
  }
}

/**
 * Assign premium role to a user (VIP, FOUNDER, SUBSCRIBED, BASIC, BANNED, SPECIAL)
 */
export async function assignUserRole(
  userId: string,
  role: PremiumRoleType,
  specialPerk?: string,
  specialEffectStyle?: 'emerald_pulse' | 'neon_wave' | 'matrix_glow' | 'electric_spark'
): Promise<void> {
  if (!db) return;
  try {
    const userRef = doc(db, COLLECTIONS.USERS, userId);
    const updates: Partial<User> = {
      premiumRole: role,
      status: role === 'BANNED' ? 'banned' : 'active'
    };

    if (role === 'SPECIAL' && specialPerk) {
      updates.specialPerkDescription = specialPerk;
      if (specialEffectStyle) {
        updates.specialEffectStyle = specialEffectStyle;
      }
    }

    await setDoc(userRef, updates, { merge: true });
  } catch (err) {
    console.error('Error assigning role in Firestore:', err);
  }
}

/* =========================================================================
 * 6. INITIAL DATA SEEDING & DATA HELPERS
 * ========================================================================= */

export async function seedInitialFirestoreData(data: {
  users: User[];
  incidents: SecurityIncident[];
  lockedIps: LockedIp[];
  transactions: CoinTransaction[];
  campaigns: FestiveCampaign[];
  aiTools: AiToolConfig[];
  templates: VideoTemplate[];
  auditLogs: SystemAuditLog[];
}): Promise<{ success: boolean; message: string }> {
  if (!db) {
    return { success: false, message: 'Firestore is not initialized.' };
  }

  try {
    const batch = writeBatch(db);

    const usersSnap = await getDocs(collection(db, COLLECTIONS.USERS));
    if (usersSnap.empty) {
      data.users.forEach(user => {
        if (!db) return;
        const ref = doc(db, COLLECTIONS.USERS, user.id);
        batch.set(ref, user);
      });
    }

    const campaignsSnap = await getDocs(collection(db, COLLECTIONS.CAMPAIGNS));
    if (campaignsSnap.empty) {
      data.campaigns.forEach(camp => {
        if (!db) return;
        const ref = doc(db, COLLECTIONS.CAMPAIGNS, camp.id);
        batch.set(ref, camp);
      });
    }

    const aiToolsSnap = await getDocs(collection(db, COLLECTIONS.AI_TOOLS));
    if (aiToolsSnap.empty) {
      data.aiTools.forEach(tool => {
        if (!db) return;
        const ref = doc(db, COLLECTIONS.AI_TOOLS, tool.id);
        batch.set(ref, tool);
      });
    }

    const templatesSnap = await getDocs(collection(db, COLLECTIONS.TEMPLATES));
    if (templatesSnap.empty) {
      data.templates.forEach(tpl => {
        if (!db) return;
        const ref = doc(db, COLLECTIONS.TEMPLATES, tpl.id);
        batch.set(ref, tpl);
      });
    }

    const auditSnap = await getDocs(collection(db, COLLECTIONS.AUDIT_LOGS));
    if (auditSnap.empty) {
      data.auditLogs.forEach(log => {
        if (!db) return;
        const ref = doc(db, COLLECTIONS.AUDIT_LOGS, log.id);
        batch.set(ref, log);
      });
    }

    const metaRef = doc(db, COLLECTIONS.SYSTEM_STATUS, 'database_info');
    batch.set(metaRef, {
      lastSyncedAt: new Date().toISOString(),
      projectId: firebaseConfig.projectId,
      firestoreDatabaseId: firebaseConfig.firestoreDatabaseId,
      status: 'active',
      updatedAt: serverTimestamp()
    }, { merge: true });

    await batch.commit();
    return { success: true, message: 'Firestore seeded with initial data successfully.' };
  } catch (error: unknown) {
    const errText = error instanceof Error ? error.message : String(error);
    return { success: false, message: errText };
  }
}

export async function syncUserToFirestore(user: User): Promise<void> {
  if (!db) return;
  try {
    const userRef = doc(db, COLLECTIONS.USERS, user.id);
    await setDoc(userRef, user, { merge: true });
  } catch (err) {
    console.error('Error syncing user:', err);
  }
}

export async function syncTransactionToFirestore(tx: CoinTransaction): Promise<void> {
  if (!db) return;
  try {
    const txRef = doc(db, COLLECTIONS.COIN_TRANSACTIONS, tx.id);
    await setDoc(txRef, tx);
  } catch (err) {
    console.error('Error syncing transaction:', err);
  }
}

export async function syncAuditLogToFirestore(log: SystemAuditLog): Promise<void> {
  if (!db) return;
  try {
    const logRef = doc(db, COLLECTIONS.AUDIT_LOGS, log.id);
    await setDoc(logRef, log);
  } catch (err) {
    console.error('Error syncing audit log:', err);
  }
}

export async function syncCampaignToFirestore(campaign: FestiveCampaign): Promise<void> {
  if (!db) return;
  try {
    const campRef = doc(db, COLLECTIONS.CAMPAIGNS, campaign.id);
    await setDoc(campRef, campaign, { merge: true });
  } catch (err) {
    console.error('Error syncing campaign:', err);
  }
}

export async function syncTemplateToFirestore(tpl: VideoTemplate): Promise<void> {
  if (!db) return;
  try {
    const tplRef = doc(db, COLLECTIONS.TEMPLATES, tpl.id);
    await setDoc(tplRef, tpl, { merge: true });
  } catch (err) {
    console.error('Error syncing template:', err);
  }
}

export function subscribeToTemplates(onData: (templates: VideoTemplate[]) => void): Unsubscribe | null {
  if (!db) return null;
  try {
    const q = query(collection(db, COLLECTIONS.TEMPLATES), limit(50));
    return onSnapshot(q, (snapshot) => {
      const items: VideoTemplate[] = [];
      snapshot.forEach(docSnap => {
        items.push({ id: docSnap.id, ...(docSnap.data() as Omit<VideoTemplate, 'id'>) });
      });
      onData(items);
    }, (error) => {
      console.warn('Templates subscription notice:', error.message);
    });
  } catch (err) {
    console.warn('Failed to subscribe to templates:', err);
    return null;
  }
}

export async function syncPromotedFeatureToFirestore(promo: {
  id: string;
  title: string;
  category: string;
  description: string;
  badgeText: string;
  discountOrBonus: string;
  targetScreen: string;
  promotedBy?: string;
  timestamp?: number;
  isActive: boolean;
}): Promise<void> {
  if (!db) return;
  try {
    const promoRef = doc(db, 'promoted_features', promo.id);
    await setDoc(promoRef, { ...promo, timestamp: promo.timestamp || Date.now() }, { merge: true });
    
    await addDoc(collection(db, 'system_broadcasts'), {
      type: 'FEATURE_PROMOTED',
      featureId: promo.id,
      title: `🌟 Worldwide Feature Spotlight: ${promo.title}`,
      message: `${promo.title} is now highlighted worldwide with ${promo.discountOrBonus}!`,
      targetScreen: promo.targetScreen,
      timestamp: Date.now()
    });
  } catch (err) {
    console.warn('Error syncing promoted feature:', err);
  }
}

export function subscribeToPromotedFeatures(onData: (features: any[]) => void): Unsubscribe | null {
  if (!db) return null;
  try {
    const q = query(collection(db, 'promoted_features'), where('isActive', '==', true));
    return onSnapshot(q, (snapshot) => {
      const items: any[] = [];
      snapshot.forEach(docSnap => items.push(docSnap.data()));
      if (items.length > 0) {
        onData(items);
      }
    }, (error) => {
      console.warn('Firestore promoted features listener notice:', error.message);
    });
  } catch (e) {
    console.warn('Could not subscribe to promoted features:', e);
    return null;
  }
}

export function subscribeToFirestoreUsers(onData: (users: User[]) => void): Unsubscribe | null {
  if (!db) return null;
  try {
    const q = query(collection(db, COLLECTIONS.USERS), orderBy('username', 'asc'), limit(50));
    return onSnapshot(q, (snapshot) => {
      const items: User[] = [];
      snapshot.forEach(docSnap => items.push(docSnap.data() as User));
      if (items.length > 0) {
        onData(items);
      }
    }, (error) => {
      console.warn('Firestore users listener notice:', error.message);
    });
  } catch (e) {
    console.warn('Could not subscribe to users:', e);
    return null;
  }
}
