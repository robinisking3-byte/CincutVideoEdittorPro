export type PremiumRoleType = 'FREE' | 'VIP_CREATOR' | 'PRO_EDITOR' | 'FOUNDER_ADMIN' | 'MODERATOR';

export interface User {
  id: string;
  userIdTag?: string;
  username: string;
  email: string;
  role?: string;
  premiumRole?: PremiumRoleType;
  avatar?: string;
  yellowCoins?: number;
  blueCoins?: number;
  streakDays?: number;
  isBanned?: boolean;
  bannedReason?: string;
  ipAddress?: string;
  deviceModel?: string;
  friendsList?: string[];
  createdAt?: string;
  lastActive?: string;
}

export interface SubscriptionPlanConfig {
  id: string;
  name: string;
  tagline: string;
  price: number;
  currency: string;
  period: 'month' | 'year' | 'lifetime';
  badgeColor: string;
  features: string[];
  blueCoinsMonthly: number;
  yellowCoinsDaily: number;
  highlighted?: boolean;
}

export interface SubscriptionRecord {
  id: string;
  userId: string;
  planId: string;
  planName: string;
  status: 'active' | 'cancelled' | 'expired';
  startedAt: string;
  expiresAt: string;
  autoRenew: boolean;
  paymentGateway: string;
  amount: number;
}

export interface SecurityIncident {
  id: string;
  timestamp: string;
  ip: string;
  userId?: string;
  type: string;
  action: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  status: 'active' | 'blocked' | 'resolved';
  details: string;
}

export interface LockedIp {
  ip: string;
  reason: string;
  lockedAt: string;
  lockedBy: string;
  attemptsCount: number;
}

export interface CoinTransaction {
  id: string;
  userId: string;
  username: string;
  coinType: 'YELLOW' | 'BLUE';
  amount: number;
  balanceAfter: number;
  description: string;
  timestamp: string;
  category: 'REWARD' | 'SPEND' | 'PURCHASE' | 'BONUS' | 'ADMIN_GRANT';
}

export interface FestiveCampaign {
  id: string;
  name: string;
  festivalType: 'DIWALI' | 'HOLI' | 'EID' | 'NEW_YEAR' | 'CHRISTMAS';
  startDate: string;
  endDate: string;
  active: boolean;
  themeColor: string;
  bannerTitle: string;
  bannerSubtitle: string;
  coinBonusYellow: number;
  coinBonusBlue: number;
  redeemCode: string;
}

export interface AiToolConfig {
  id: string;
  name: string;
  category: 'VISION' | 'AUDIO' | 'ENHANCE' | 'GENERATIVE';
  blueCoinCost: number;
  enabled: boolean;
  demoMode: boolean;
  description: string;
  iconName: string;
}

export interface VideoTemplate {
  id: string;
  title: string;
  creatorId: string;
  creatorName: string;
  aspectRatio: string;
  category: string;
  likes: number;
  uses: number;
  status: 'pending' | 'approved' | 'rejected';
  featuredWorldwide?: boolean;
  createdAt: string;
}

export interface SystemAuditLog {
  id: string;
  timestamp: string;
  adminName: string;
  category: 'SECURITY' | 'ROLE' | 'FINANCE' | 'SYSTEM' | 'FRIENDS' | 'MODERATION';
  action: string;
  details: string;
  ip?: string;
}

export interface FriendRequest {
  id: string;
  senderId: string;
  senderUserIdTag?: string;
  senderUsername: string;
  senderAvatar?: string;
  senderPremiumRole?: PremiumRoleType;
  receiverId: string;
  receiverUserIdTag?: string;
  receiverUsername: string;
  status: 'pending' | 'accepted' | 'declined';
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  receiverId: string;
  text: string;
  timestamp: string;
  read: boolean;
  projectAttachment?: string;
}

export interface GroupProjectMember {
  userId: string;
  userIdTag?: string;
  username: string;
  avatar?: string;
  premiumRole?: PremiumRoleType;
  role: 'owner' | 'editor' | 'viewer';
  joinedAt: string;
  isOnline: boolean;
}

export interface GroupProject {
  id: string;
  title: string;
  description?: string;
  ownerId: string;
  ownerName: string;
  members: GroupProjectMember[];
  updatedAt: string;
  activeTracksCount: number;
  aspectRatio: string;
}

export interface SavedExportedVideo {
  id: string;
  title: string;
  videoUrl: string;
  aspectRatio: string;
  filter: string;
  resolution: string;
  duration: number;
  createdAt: string;
  thumbnailGradient?: string;
  fileSizeBytes?: number;
}

export interface SavedProject {
  id: string;
  title: string;
  aspectRatio: string;
  filter: string;
  duration: number;
  updatedAt: string;
  thumbnailGradient: string;
  videoSrc?: string;
  captionText?: string;
}

