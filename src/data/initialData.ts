import { SubscriptionPlanConfig, User } from '../types';

export const INITIAL_SUBSCRIPTION_PLANS: SubscriptionPlanConfig[] = [
  {
    id: 'plan-free',
    name: 'Free Creator',
    tagline: 'Basic HD trimming and standard filters',
    price: 0,
    currency: 'INR',
    period: 'lifetime',
    badgeColor: 'from-slate-600 to-slate-800',
    features: ['720p 30fps export', 'Standard filters', 'Watermark included', '5 AI generations/mo'],
    blueCoinsMonthly: 5,
    yellowCoinsDaily: 10
  },
  {
    id: 'plan-subscribed-creator',
    name: 'Pro Creator VIP',
    tagline: 'Unleash 4K 60fps, No Watermark & All AI Tools',
    price: 299,
    currency: 'INR',
    period: 'month',
    badgeColor: 'from-amber-500 to-yellow-600',
    features: ['4K 60fps HDR rendering', 'No watermark', 'Unlimited multi-track timeline', '50 Blue Coins/mo', 'Priority rendering queue'],
    blueCoinsMonthly: 50,
    yellowCoinsDaily: 50,
    highlighted: true
  },
  {
    id: 'plan-studio-elite',
    name: 'Studio Master',
    tagline: 'Team collaboration, 8K render & founder badge',
    price: 899,
    currency: 'INR',
    period: 'month',
    badgeColor: 'from-purple-500 to-indigo-700',
    features: ['8K ProRes encoding', 'Team cloud rooms', '200 Blue Coins/mo', 'Custom 3D LUT imports', 'Dedicated render node'],
    blueCoinsMonthly: 200,
    yellowCoinsDaily: 150
  }
];

export const INITIAL_DEMO_USER: User = {
  id: 'usr-guest-creator',
  userIdTag: 'VID-71829',
  username: 'CreatorStudio',
  email: 'creator@cincut.studio',
  role: 'creator',
  premiumRole: 'FREE',
  avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
  yellowCoins: 50,
  blueCoins: 10,
  streakDays: 1,
  isBanned: false,
  deviceModel: 'Web & Android Studio'
};

export interface CommunityCreator {
  id: string;
  userIdTag: string;
  username: string;
  displayName: string;
  avatar: string;
  bio: string;
  role: string;
  premiumRole: string;
  isOnline: boolean;
  projectsCount: number;
}

export const DISCOVERABLE_CREATORS: CommunityCreator[] = [
  {
    id: 'usr-creator-aarav',
    userIdTag: 'VID-48291',
    username: 'aarav_vfx',
    displayName: 'Aarav Sharma',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
    bio: 'Cinematic Colorist & 4K Multi-Track Editor',
    role: 'creator',
    premiumRole: 'VIP_CREATOR',
    isOnline: true,
    projectsCount: 24
  },
  {
    id: 'usr-creator-neha',
    userIdTag: 'VID-19284',
    username: 'neha_cine',
    displayName: 'Neha Kapoor',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
    bio: 'Travel Filmmaker & Bézier Speed Ramp Specialist',
    role: 'creator',
    premiumRole: 'VIP_CREATOR',
    isOnline: true,
    projectsCount: 38
  },
  {
    id: 'usr-creator-vikram',
    userIdTag: 'VID-82910',
    username: 'vikram_director',
    displayName: 'Vikramaditya Roy',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150',
    bio: 'Independent Director • Narrative Short Cuts',
    role: 'creator',
    premiumRole: 'STUDIO_MASTER',
    isOnline: false,
    projectsCount: 15
  },
  {
    id: 'usr-creator-riya',
    userIdTag: 'VID-63912',
    username: 'riya_reels',
    displayName: 'Riya Mehta',
    avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150',
    bio: 'Music Video & Dynamic Beat Sync Creator',
    role: 'creator',
    premiumRole: 'FREE',
    isOnline: true,
    projectsCount: 42
  },
  {
    id: 'usr-creator-kabir',
    userIdTag: 'VID-39182',
    username: 'kabir_sound',
    displayName: 'Kabir Joshi',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150',
    bio: 'Sound Designer & Foley Audio Mixer',
    role: 'creator',
    premiumRole: 'VIP_CREATOR',
    isOnline: true,
    projectsCount: 19
  },
  {
    id: 'usr-creator-pooja',
    userIdTag: 'VID-51029',
    username: 'pooja_motion',
    displayName: 'Pooja Verma',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    bio: 'Motion Graphic Animator & Keyframe Artist',
    role: 'creator',
    premiumRole: 'STUDIO_MASTER',
    isOnline: false,
    projectsCount: 31
  }
];
