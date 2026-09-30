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
  Shield
} from 'lucide-react';
import { 
  signInWithGoogle, 
  registerWithEmail, 
  loginWithEmail,
  syncUserToFirestore,
  db
} from '../lib/firebase';
import { doc, getDoc } from 'firebase/firestore';
import { User } from '../types';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User | null;
  onUserAuthenticated: (user: User) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onUserAuthenticated
}) => {
  const [tab, setTab] = useState<'signin' | 'register'>('signin');
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
    setSuccess(null);
    try {
      const res = await signInWithGoogle();
      if (res.success && res.user) {
        const fbUser = res.user;
        let finalUser: User;

        // Check if user document already exists in Firestore
        let existingUser: User | null = null;
        if (db) {
          try {
            const userDocSnap = await getDoc(doc(db, 'users', fbUser.uid));
            if (userDocSnap.exists()) {
              existingUser = userDocSnap.data() as User;
            }
          } catch (fetchErr) {
            console.warn('Notice checking existing user in Firestore:', fetchErr);
          }
        }

        if (existingUser) {
          finalUser = {
            ...existingUser,
            lastActive: 'Active Now (Google OAuth)'
          };
        } else {
          // Standard Creator user — NO automatic admin rights!
          finalUser = {
            id: fbUser.uid,
            userIdTag: `VID-${Math.floor(10000 + Math.random() * 90000)}`,
            username: (fbUser.displayName || fbUser.email?.split('@')[0] || 'creator')
              .toLowerCase()
              .replace(/[^a-z0-9_]/g, '_'),
            displayName: fbUser.displayName || 'Cincut Creator',
            email: fbUser.email || '',
            avatar: fbUser.photoURL || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
            role: 'creator',
            premiumRole: 'FREE',
            status: 'active',
            yellowCoins: 50,
            blueCoins: 10,
            streakDays: 1,
            createdAt: new Date().toISOString().slice(0, 10),
            lastActive: 'Active Now (Google OAuth)'
          };
          // Persist user to Firestore
          await syncUserToFirestore(finalUser);
        }

        onUserAuthenticated(finalUser);
        setSuccess(`Signed in successfully as ${finalUser.displayName}!`);
        setTimeout(() => {
          onClose();
        }, 800);
      } else {
        setError(res.error || 'Google Sign-In was cancelled or failed.');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Google authentication error occurred.');
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

        if (password.length < 6) {
          setError('Password must be at least 6 characters long.');
          setIsLoading(false);
          return;
        }

        const res = await registerWithEmail(email, password, displayName);
        if (res.success && res.user) {
          const newUser: User = {
            id: res.user.uid,
            userIdTag: `VID-${Math.floor(10000 + Math.random() * 90000)}`,
            username: displayName.toLowerCase().replace(/[^a-z0-9_]/g, '_'),
            displayName: displayName.trim(),
            email: email.trim(),
            avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
            role: 'creator',
            premiumRole: 'FREE',
            status: 'active',
            yellowCoins: 50,
            blueCoins: 10,
            streakDays: 1,
            createdAt: new Date().toISOString().slice(0, 10),
            lastActive: 'Just now'
          };

          await syncUserToFirestore(newUser);
          onUserAuthenticated(newUser);
          setSuccess('Account created successfully! Welcome to Cincut.');
          setTimeout(onClose, 800);
        } else {
          setError(res.error || 'Registration failed. Please check your details.');
        }
      } else {
        // Sign In with email & password
        if (!email || !password) {
          setError('Please enter both email and password.');
          setIsLoading(false);
          return;
        }

        const res = await loginWithEmail(email, password);
        if (res.success && res.user) {
          let userProfile: User = {
            id: res.user.uid,
            userIdTag: `VID-${Math.floor(10000 + Math.random() * 90000)}`,
            username: (res.user.displayName || email.split('@')[0]).toLowerCase().replace(/[^a-z0-9_]/g, '_'),
            displayName: res.user.displayName || 'Cincut Creator',
            email: email.trim(),
            avatar: res.user.photoURL || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
            role: 'creator',
            premiumRole: 'FREE',
            status: 'active',
            yellowCoins: 50,
            blueCoins: 10,
            streakDays: 1,
            lastActive: 'Just now'
          };

          if (db) {
            try {
              const snap = await getDoc(doc(db, 'users', res.user.uid));
              if (snap.exists()) {
                userProfile = snap.data() as User;
              }
            } catch (err) {
              console.warn('Notice fetching user profile:', err);
            }
          }

          onUserAuthenticated(userProfile);
          setSuccess(`Welcome back, ${userProfile.displayName}!`);
          setTimeout(onClose, 800);
        } else {
          setError(res.error || 'Invalid email or password. Please verify your credentials.');
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-stone-900 border border-stone-800 w-full max-w-md rounded-3xl overflow-hidden shadow-2xl relative">
        {/* Header */}
        <div className="px-6 pt-6 pb-4 border-b border-stone-800/80 flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <LogIn className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white tracking-tight">Cincut Creator Account</h3>
              <p className="text-[11px] text-slate-400">Sign in to sync cuts, templates &amp; wallet</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-stone-800 hover:bg-stone-700 flex items-center justify-center text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Tab Selection */}
        <div className="p-6 space-y-4">
          <div className="flex bg-stone-950 p-1 rounded-2xl border border-stone-800">
            <button
              onClick={() => {
                setTab('signin');
                setError(null);
                setSuccess(null);
              }}
              className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-2 ${
                tab === 'signin'
                  ? 'bg-stone-800 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <LogIn className="w-3.5 h-3.5" />
              <span>Sign In</span>
            </button>
            <button
              onClick={() => {
                setTab('register');
                setError(null);
                setSuccess(null);
              }}
              className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-2 ${
                tab === 'register'
                  ? 'bg-stone-800 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <UserPlus className="w-3.5 h-3.5" />
              <span>Create Account</span>
            </button>
          </div>

          {/* Feedback Notices */}
          {error && (
            <div className="p-3 rounded-2xl bg-rose-950/60 border border-rose-500/40 text-rose-200 text-xs flex items-start space-x-2.5">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <span className="leading-relaxed">{error}</span>
            </div>
          )}

          {success && (
            <div className="p-3 rounded-2xl bg-emerald-950/60 border border-emerald-500/40 text-emerald-200 text-xs flex items-start space-x-2.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <span className="leading-relaxed">{success}</span>
            </div>
          )}

          <div className="space-y-4">
            {/* Google OAuth Button */}
            <button
              type="button"
              onClick={handleGoogleSignIn}
              disabled={isLoading}
              className="w-full py-2.5 px-4 rounded-xl bg-white hover:bg-slate-100 text-slate-900 font-bold text-xs flex items-center justify-center space-x-2.5 shadow-lg shadow-white/5 transition-all transform active:scale-[0.98] disabled:opacity-50"
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
              <span>Continue with Google</span>
            </button>

            <div className="relative flex items-center justify-center">
              <div className="border-t border-stone-800 w-full" />
              <span className="bg-stone-900 px-3 text-[10px] uppercase font-mono text-slate-500 absolute">
                or use email
              </span>
            </div>

            {/* Email / Password Form */}
            <form onSubmit={handleEmailAuth} className="space-y-3">
              {tab === 'register' && (
                <div>
                  <label className="text-[11px] font-medium text-slate-400 block mb-1">Display Name</label>
                  <div className="relative">
                    <UserIcon className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      type="text"
                      value={displayName}
                      onChange={e => setDisplayName(e.target.value)}
                      placeholder="e.g. Alex Rivera"
                      className="w-full bg-stone-950 border border-stone-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                      required={tab === 'register'}
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="text-[11px] font-medium text-slate-400 block mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                  <input
                    type="email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    placeholder="creator@cincut.studio"
                    className="w-full bg-stone-950 border border-stone-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="text-[11px] font-medium text-slate-400 block mb-1">Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
                  <input
                    type="password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-stone-950 border border-stone-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                    required
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full mt-2 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-bold text-xs shadow-lg shadow-cyan-500/20 transition-all disabled:opacity-50"
              >
                {isLoading ? 'Processing...' : tab === 'register' ? 'Create Account' : 'Sign In'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
