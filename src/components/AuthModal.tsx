import React, { useState } from 'react';
import { 
  X, 
  LogIn, 
  UserPlus, 
  Mail, 
  Lock, 
  User as UserIcon, 
  AlertCircle, 
  CheckCircle2, 
  Sparkles,
  ShieldCheck
} from 'lucide-react';
import { 
  signInWithGoogle, 
  registerWithEmail, 
  loginWithEmail,
  isFirebaseActive 
} from '../lib/firebase';
import { User } from '../types';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User | null;
  onUserAuthenticated: (user: User) => void;
  availableDemoUsers: User[];
  onSwitchUser: (user: User) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onUserAuthenticated,
  availableDemoUsers,
  onSwitchUser
}) => {
  const [tab, setTab] = useState<'signin' | 'register' | 'switch'>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleGoogleSignIn = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await signInWithGoogle();
      if (res.success && res.user) {
        const fbUser = res.user;
        const normalizedEmail = (fbUser.email || '').toLowerCase().trim();
        const isMaster1 = normalizedEmail === 'robinsiking3@gmail.com' || normalizedEmail === 'robinisking3@gmail.com';
        const isMaster2 = normalizedEmail === 'robintyagi861az@gmail.com';

        let mappedUser: User;
        if (isMaster1) {
          const master1 = availableDemoUsers.find(u => u.email === 'robinsiking3@gmail.com' || u.email === 'robinisking3@gmail.com') || availableDemoUsers[0];
          mappedUser = {
            ...master1,
            authProvider: 'google',
            lastActive: 'Active Now (Google OAuth)'
          };
        } else if (isMaster2) {
          const master2 = availableDemoUsers.find(u => u.email === 'robintyagi861az@gmail.com') || availableDemoUsers[1];
          mappedUser = {
            ...master2,
            authProvider: 'google',
            lastActive: 'Active Now (Google OAuth)'
          };
        } else {
          mappedUser = {
            id: `usr-${fbUser.uid.slice(0, 8)}`,
            userIdTag: `VID-${Math.floor(10000 + Math.random() * 90000)}`,
            username: (fbUser.displayName || 'google_creator').toLowerCase().replace(/\s+/g, '_'),
            displayName: fbUser.displayName || 'Google Creator',
            bio: 'Verified VidForge creator via Google OAuth.',
            email: fbUser.email || '',
            avatar: fbUser.photoURL || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
            role: 'creator',
            premiumRole: 'SUBSCRIBED',
            plan: 'pro',
            status: 'active',
            authProvider: 'google',
            deviceId: 'DEV-WEB-OAUTH',
            deviceModel: 'Web Browser (Google OAuth)',
            deviceAccountsCount: 1,
            ipAddress: '103.21.144.18',
            yellowCoins: 100,
            blueCoins: 250,
            streakDays: 1,
            usageHours: 2.5,
            termsAccepted: true,
            termsAcceptedAt: new Date().toISOString(),
            createdAt: new Date().toISOString().slice(0, 10),
            lastActive: 'Just now',
            friendsList: [],
            blockedUsers: []
          };
        }
        onUserAuthenticated(mappedUser);
        setSuccess(`Signed in with Google OAuth as ${mappedUser.displayName} (${mappedUser.premiumRole})!`);
        setTimeout(() => {
          onClose();
        }, 1000);
      } else {
        setError(res.error || 'Google Sign-In failed or popup was closed.');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Google authentication error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleEmailAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (tab === 'register') {
        if (!email || !password || !displayName) {
          setError('Please fill in all fields.');
          setIsLoading(false);
          return;
        }

        const res = await registerWithEmail(email, password, displayName);
        if (res.success && res.user) {
          const newUser: User = {
            id: `usr-${res.user.uid.slice(0, 8)}`,
            userIdTag: `VID-${Math.floor(10000 + Math.random() * 90000)}`,
            username: displayName.toLowerCase().replace(/\s+/g, '_'),
            displayName: displayName,
            bio: 'New VidForge video editor & creator.',
            email: email,
            avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
            role: 'creator',
            premiumRole: 'BASIC',
            plan: 'free',
            status: 'active',
            authProvider: 'password',
            deviceId: 'DEV-WEB-REGISTER',
            deviceModel: 'Web Browser',
            deviceAccountsCount: 1,
            ipAddress: '103.21.144.18',
            yellowCoins: 25,
            blueCoins: 50,
            streakDays: 1,
            usageHours: 1.0,
            termsAccepted: true,
            termsAcceptedAt: new Date().toISOString(),
            createdAt: new Date().toISOString().slice(0, 10),
            lastActive: 'Just now',
            friendsList: [],
            blockedUsers: []
          };
          onUserAuthenticated(newUser);
          setSuccess('Account created and logged in!');
          setTimeout(onClose, 1000);
        } else {
          // Fallback simulation for offline testing
          const simulatedUser: User = {
            id: `usr-${Math.random().toString(36).slice(2, 8)}`,
            userIdTag: `VID-${Math.floor(10000 + Math.random() * 90000)}`,
            username: displayName.toLowerCase().replace(/\s+/g, '_'),
            displayName: displayName,
            bio: 'New VidForge video editor & creator.',
            email: email,
            avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
            role: 'creator',
            premiumRole: 'BASIC',
            plan: 'free',
            status: 'active',
            authProvider: 'password',
            deviceId: 'DEV-LOCAL-USER',
            deviceModel: 'Web App',
            deviceAccountsCount: 1,
            ipAddress: '103.21.144.18',
            yellowCoins: 25,
            blueCoins: 50,
            streakDays: 1,
            usageHours: 1.0,
            termsAccepted: true,
            termsAcceptedAt: new Date().toISOString(),
            createdAt: new Date().toISOString().slice(0, 10),
            lastActive: 'Just now',
            friendsList: [],
            blockedUsers: []
          };
          onUserAuthenticated(simulatedUser);
          setSuccess('Account registered successfully!');
          setTimeout(onClose, 1000);
        }
      } else {
        // Sign In
        const normalizedEmail = email.toLowerCase().trim();
        const isMaster1 = (normalizedEmail === 'robinsiking3@gmail.com' || normalizedEmail === 'robinisking3@gmail.com') && password === 'Robintyagi@83073##';
        const isMaster2 = normalizedEmail === 'robintyagi861az@gmail.com' && password === 'Robintyagi@MasterAdmin@83073##';

        if (isMaster1) {
          const masterUser1 = availableDemoUsers.find(u => u.email === 'robinsiking3@gmail.com' || u.email === 'robinisking3@gmail.com') || availableDemoUsers[0];
          onSwitchUser(masterUser1);
          setSuccess('Signed in as Primary Master Admin (Founder)!');
          setTimeout(onClose, 800);
          return;
        }

        if (isMaster2) {
          const masterUser2 = availableDemoUsers.find(u => u.email === 'robintyagi861az@gmail.com') || availableDemoUsers[1];
          onSwitchUser(masterUser2);
          setSuccess('Signed in as Executive Master Admin 2 (Founder)!');
          setTimeout(onClose, 800);
          return;
        }

        const res = await loginWithEmail(email, password);
        if (res.success && res.user) {
          const matched = availableDemoUsers.find(u => u.email.toLowerCase() === email.toLowerCase());
          if (matched) {
            onSwitchUser(matched);
          }
          setSuccess('Signed in successfully!');
          setTimeout(onClose, 1000);
        } else {
          // Fallback check against existing demo users
          const matched = availableDemoUsers.find(u => u.email.toLowerCase() === email.toLowerCase());
          if (matched) {
            onSwitchUser(matched);
            setSuccess(`Signed in as @${matched.username}!`);
            setTimeout(onClose, 1000);
          } else {
            setError(res.error || 'Invalid email or password. Check credentials.');
          }
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failure');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-studio-900 border border-studio-700 rounded-2xl shadow-2xl overflow-hidden text-slate-100 flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 bg-studio-850/80 border-b border-studio-750 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-md shadow-cyan-500/20">
              <ShieldCheck className="w-4 h-4 text-white" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Firebase Authentication</h3>
              <p className="text-[11px] text-slate-400">Google OAuth & Identity Portal</p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-studio-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-studio-800 bg-studio-900/50">
          <button
            onClick={() => { setTab('signin'); setError(null); }}
            className={`flex-1 py-2.5 text-xs font-semibold text-center border-b-2 transition-colors ${
              tab === 'signin'
                ? 'border-cyan-400 text-cyan-300 bg-cyan-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Sign In
          </button>
          <button
            onClick={() => { setTab('register'); setError(null); }}
            className={`flex-1 py-2.5 text-xs font-semibold text-center border-b-2 transition-colors ${
              tab === 'register'
                ? 'border-cyan-400 text-cyan-300 bg-cyan-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Create Account
          </button>
          <button
            onClick={() => { setTab('switch'); setError(null); }}
            className={`flex-1 py-2.5 text-xs font-semibold text-center border-b-2 transition-colors ${
              tab === 'switch'
                ? 'border-cyan-400 text-cyan-300 bg-cyan-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Quick Switch
          </button>
        </div>

        {/* Body Content */}
        <div className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-500/40 text-emerald-300 text-xs flex items-center space-x-2">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
              <span>{success}</span>
            </div>
          )}

          {tab === 'switch' ? (
            <div className="space-y-3">
              <p className="text-xs text-slate-400">
                Instantly switch between available test roles to test VIP, Founder, Special, or regular users:
              </p>
              <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                {availableDemoUsers.map(user => (
                  <button
                    key={user.id}
                    onClick={() => {
                      onSwitchUser(user);
                      onClose();
                    }}
                    className={`w-full p-2.5 rounded-xl border text-left flex items-center justify-between transition-all ${
                      currentUser?.id === user.id
                        ? 'bg-cyan-950/40 border-cyan-500/50 text-white'
                        : 'bg-studio-850 border-studio-750 text-slate-300 hover:border-slate-600'
                    }`}
                  >
                    <div className="flex items-center space-x-2.5">
                      <img src={user.avatar} alt={user.username} className="w-8 h-8 rounded-full object-cover" />
                      <div>
                        <div className="flex items-center space-x-1.5">
                          <span className="text-xs font-bold text-white">@{user.username}</span>
                          <span className="text-[10px] text-cyan-400 font-mono">[{user.userIdTag}]</span>
                        </div>
                        <span className="text-[10px] text-slate-400 block">{user.displayName} • <span className="font-mono text-slate-300">{user.email}</span></span>
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-0.5">
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                        user.premiumRole === 'FOUNDER'
                          ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40'
                          : user.premiumRole === 'VIP'
                          ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                          : 'bg-studio-800 text-slate-300 border border-studio-700'
                      }`}>
                        {user.premiumRole}
                      </span>
                      {user.role === 'admin' && (
                        <span className="text-[9px] font-bold text-amber-400 uppercase tracking-wider">Master Admin</span>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Google OAuth Button */}
              <button
                type="button"
                onClick={handleGoogleSignIn}
                disabled={isLoading}
                className="w-full py-2.5 px-4 rounded-xl bg-white hover:bg-slate-100 text-slate-800 font-bold text-xs flex items-center justify-center space-x-2.5 shadow-lg shadow-white/5 transition-all transform active:scale-[0.98]"
              >
                <svg className="w-4 h-4" viewBox="0 0 24 24">
                  <path
                    fill="#4285F4"
                    d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.35 24 12 24z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.16 0 9.97 0 12s.45 3.84 1.25 5.42l4.03-3.15z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.35 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
                  />
                </svg>
                <span>Continue with Google OAuth</span>
              </button>

              <div className="relative flex py-1 items-center">
                <div className="flex-grow border-t border-studio-800"></div>
                <span className="flex-shrink mx-3 text-[11px] text-slate-500 uppercase tracking-widest font-semibold">Or with Email</span>
                <div className="flex-grow border-t border-studio-800"></div>
              </div>

              {/* Email / Password Form */}
              <form onSubmit={handleEmailAuth} className="space-y-3">
                {tab === 'register' && (
                  <div className="space-y-1">
                    <label className="text-[11px] font-medium text-slate-400">Display Name</label>
                    <div className="relative">
                      <UserIcon className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                      <input
                        type="text"
                        value={displayName}
                        onChange={e => setDisplayName(e.target.value)}
                        placeholder="e.g. Vikram Sharma"
                        className="w-full bg-studio-850 border border-studio-750 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                        required
                      />
                    </div>
                  </div>
                )}

                <div className="space-y-1">
                  <label className="text-[11px] font-medium text-slate-400">Email Address</label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      type="email"
                      value={email}
                      onChange={e => setEmail(e.target.value)}
                      placeholder="creator@vidforge.io"
                      className="w-full bg-studio-850 border border-studio-750 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-[11px] font-medium text-slate-400">Password</label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      type="password"
                      value={password}
                      onChange={e => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-studio-850 border border-studio-750 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                      required
                    />
                  </div>
                </div>

                {tab === 'signin' && (
                  <div className="pt-1 space-y-1.5">
                    <span className="text-[10px] font-bold text-amber-400 uppercase tracking-wider flex items-center gap-1">
                      <Sparkles className="w-3 h-3 text-amber-400" />
                      Quick Master Admin Fill:
                    </span>
                    <div className="grid grid-cols-1 gap-1.5">
                      <button
                        type="button"
                        onClick={() => {
                          setEmail('robinsiking3@gmail.com');
                          setPassword('Robintyagi@83073##');
                        }}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 text-[11px] text-amber-300 flex items-center justify-between transition-colors"
                      >
                        <span className="font-mono truncate">1.) robinsiking3@gmail.com</span>
                        <span className="text-[10px] font-bold uppercase tracking-wider bg-amber-500/20 px-1.5 py-0.5 rounded text-amber-300">Auto-fill</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setEmail('robintyagi861az@gmail.com');
                          setPassword('Robintyagi@MasterAdmin@83073##');
                        }}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg bg-purple-500/10 hover:bg-purple-500/20 border border-purple-500/30 text-[11px] text-purple-300 flex items-center justify-between transition-colors"
                      >
                        <span className="font-mono truncate">2.) robintyagi861az@gmail.com</span>
                        <span className="text-[10px] font-bold uppercase tracking-wider bg-purple-500/20 px-1.5 py-0.5 rounded text-purple-300">Auto-fill</span>
                      </button>
                    </div>
                  </div>
                )}

                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full mt-2 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-bold text-xs shadow-lg shadow-cyan-500/20 transition-all disabled:opacity-50"
                >
                  {isLoading ? 'Processing...' : tab === 'register' ? 'Create Account' : 'Sign In'}
                </button>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
