import React, { useState, useEffect } from 'react';
import { 
  Smartphone, 
  Download, 
  Database, 
  ShieldCheck, 
  Scissors, 
  Sparkles, 
  ExternalLink,
  Crown,
  Bell,
  Coins
} from 'lucide-react';
import { MobileAppSimulatorView } from './views/MobileAppSimulatorView';
import { AuthModal } from './components/AuthModal';
import { DownloadApkModal } from './components/DownloadApkModal';
import { INITIAL_DEMO_USER } from './data/initialData';
import { User, SubscriptionPlanConfig } from './types';
import { syncUserToFirestore, subscribeToFirestoreUsers } from './lib/firebase';

export function App() {
  const [currentUser, setCurrentUser] = useState<User>(INITIAL_DEMO_USER);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isDownloadApkModalOpen, setIsDownloadApkModalOpen] = useState(false);
  const [isFirebaseModalOpen, setIsFirebaseModalOpen] = useState(false);
  const [dbStatus, setDbStatus] = useState<'connected' | 'connecting' | 'idle'>('connecting');

  useEffect(() => {
    try {
      // Sync initial user
      syncUserToFirestore(currentUser);
      const unsubscribe = subscribeToFirestoreUsers((users) => {
        setDbStatus('connected');
        const found = users.find(u => u.id === currentUser.id);
        if (found) {
          setCurrentUser(prev => ({ ...prev, ...found }));
        }
      });
      return () => {
        if (unsubscribe) unsubscribe();
      };
    } catch {
      setDbStatus('idle');
    }
  }, [currentUser.id]);

  const handleSubscribeSuccess = (plan: SubscriptionPlanConfig, txn: any) => {
    const updated: User = {
      ...currentUser,
      premiumRole: 'VIP_CREATOR',
      blueCoins: (currentUser.blueCoins || 0) + plan.blueCoinsMonthly,
      yellowCoins: (currentUser.yellowCoins || 0) + plan.yellowCoinsDaily
    };
    setCurrentUser(updated);
    syncUserToFirestore(updated);
  };

  return (
    <div className="min-h-screen bg-[#07090e] text-slate-100 flex flex-col font-sans selection:bg-cyan-500 selection:text-white">
      {/* Top Studio Bar */}
      <header className="h-16 border-b border-stone-800 bg-stone-950/80 backdrop-blur-md px-4 lg:px-8 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-500 via-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-cyan-950/50">
            <Scissors className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-extrabold text-white tracking-tight">Cincut Video Editor Pro</h1>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-mono font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">
                v1.0.0
              </span>
            </div>
            <p className="text-[11px] text-slate-400 hidden sm:block">
              Android Video Editor, Real-Time Export Pipeline &amp; Admin Command Station
            </p>
          </div>
        </div>

        {/* Right side global actions */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Firestore Status Badge */}
          <button 
            onClick={() => setIsFirebaseModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-stone-900 border border-stone-800 hover:border-cyan-500/40 text-xs transition-all"
            title="View Firestore Database Connection"
          >
            <span className={`w-2 h-2 rounded-full ${dbStatus === 'connected' ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`} />
            <span className="text-slate-300 font-mono text-[11px] hidden md:inline">Firestore:</span>
            <span className="text-cyan-400 font-bold text-[11px]">ai-studio-vidflowpro</span>
          </button>

          {/* User Profile Pill */}
          <button
            onClick={() => setIsAuthModalOpen(true)}
            className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-stone-900 hover:bg-stone-850 border border-stone-800 text-xs transition-all"
          >
            <img 
              src={currentUser.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'} 
              alt={currentUser.username} 
              className="w-5 h-5 rounded-full object-cover border border-cyan-400/50" 
            />
            <span className="font-bold text-white text-xs max-w-[100px] truncate">{currentUser.username}</span>
            {currentUser.role === 'admin' ? (
              <span className="text-[10px] text-amber-400 font-mono" title="Administrator">👑</span>
            ) : (
              <span className="text-[10px] text-cyan-400 font-mono" title="Creator">✨</span>
            )}
          </button>

          {/* Download Real APK Button */}
          <button
            onClick={() => setIsDownloadApkModalOpen(true)}
            className="py-1.5 px-3.5 rounded-xl text-xs font-bold bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-stone-950 flex items-center gap-1.5 shadow-md transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Download APK</span>
          </button>
        </div>
      </header>

      {/* Main Simulator Viewport */}
      <main className="flex-1 overflow-y-auto">
        <MobileAppSimulatorView
          currentUser={currentUser}
          onOpenDownloadApkModal={() => setIsDownloadApkModalOpen(true)}
          onOpenFirebaseModal={() => setIsFirebaseModalOpen(true)}
          onSubscribeSuccess={handleSubscribeSuccess}
          onUpdateCurrentUser={(u) => setCurrentUser(u)}
        />
      </main>

      {/* Auth Modal */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        currentUser={currentUser}
        onUserAuthenticated={(u) => setCurrentUser(u)}
      />

      {/* Download Android APK Modal */}
      <DownloadApkModal
        isOpen={isDownloadApkModalOpen}
        onClose={() => setIsDownloadApkModalOpen(false)}
      />

      {/* Simple Firestore Info Modal */}
      {isFirebaseModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-in fade-in">
          <div className="bg-stone-900 border border-stone-700 w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-stone-800 pb-3">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
                  <Database className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base">Firebase Cloud Database</h3>
                  <span className="text-[10px] text-emerald-400 font-mono">Live Real-time Sync</span>
                </div>
              </div>
              <button onClick={() => setIsFirebaseModalOpen(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <div className="space-y-2 text-xs">
              <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800 space-y-1 font-mono">
                <div className="flex justify-between">
                  <span className="text-slate-400">Database ID:</span>
                  <span className="text-cyan-400">ai-studio-vidflowpro...</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Status:</span>
                  <span className="text-emerald-400 font-bold">ONLINE & SYNCED</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Collections:</span>
                  <span className="text-white">users, promoted_features, system_broadcasts</span>
                </div>
              </div>

              <p className="text-slate-400 text-[11px] leading-relaxed">
                All features promoted in the Admin Console or changes to user accounts, subscriptions, and wallet coins are automatically synchronized in real time across the simulated app and all physical Android APK installations.
              </p>
            </div>

            <button
              onClick={() => setIsFirebaseModalOpen(false)}
              className="w-full py-2.5 rounded-xl bg-stone-800 hover:bg-stone-750 text-white font-bold text-xs"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
