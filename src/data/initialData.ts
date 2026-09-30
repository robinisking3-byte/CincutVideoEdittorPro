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
