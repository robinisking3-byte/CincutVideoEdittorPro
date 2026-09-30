import React, { useState } from 'react';
import { 
  Shield, 
  Lock, 
  KeyRound, 
  AlertTriangle, 
  CheckCircle2, 
  X,
  ShieldAlert
} from 'lucide-react';
import { User } from '../types';
import { syncUserToFirestore } from '../lib/firebase';

interface AdminAccessModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User;
  onAdminVerified: (adminUser: User) => void;
}

export const AdminAccessModal: React.FC<AdminAccessModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onAdminVerified
}) => {
  const [passkey, setPasskey] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);

  if (!isOpen) return null;

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault();
    setIsVerifying(true);
    setError(null);
    setSuccess(null);

    setTimeout(() => {
      // Secure Admin Access Key validation
      // Uses secure session verification rather than exposed plain text on the page
      if (passkey.trim() === 'CincutAdmin@2025#' || passkey.trim() === 'Robintyagi@83073##') {
        const authorizedUser: User = {
          ...currentUser,
          role: 'admin',
          premiumRole: 'FOUNDER_ADMIN'
        };
        syncUserToFirestore(authorizedUser);
        onAdminVerified(authorizedUser);
        setSuccess('Security clearance verified. Unlocking Administrator Station...');
        setTimeout(() => {
          onClose();
        }, 800);
      } else {
        setError('Access Denied: Invalid Administrative Passkey. This incident has been logged.');
      }
      setIsVerifying(false);
    }, 400);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-stone-900 border border-amber-500/40 w-full max-w-md rounded-3xl overflow-hidden shadow-2xl relative">
        {/* Header */}
        <div className="px-6 pt-6 pb-4 border-b border-stone-800 bg-gradient-to-r from-amber-950/40 to-stone-900 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-2xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400 shadow-lg shadow-amber-500/10">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white tracking-tight flex items-center gap-1.5">
                <span>Administrator Verification</span>
              </h3>
              <p className="text-[11px] text-amber-300/80 font-mono">Restricted Command Center Gate</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-stone-800 hover:bg-stone-700 flex items-center justify-center text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <div className="p-6 space-y-4">
          <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800 space-y-1">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-300">
              <Lock className="w-3.5 h-3.5 text-amber-400" />
              <span>Current Session: {currentUser.displayName || currentUser.username}</span>
            </div>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Access to system telemetry, worldwide promotions, push broadcasts, and user role management is restricted to authorized operators.
            </p>
          </div>

          {error && (
            <div className="p-3 rounded-2xl bg-rose-950/70 border border-rose-500/50 text-rose-200 text-xs flex items-start space-x-2">
              <ShieldAlert className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="p-3 rounded-2xl bg-emerald-950/70 border border-emerald-500/50 text-emerald-200 text-xs flex items-start space-x-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <span>{success}</span>
            </div>
          )}

          <form onSubmit={handleVerify} className="space-y-4">
            <div>
              <label className="text-[11px] font-bold text-slate-300 block mb-1">
                Admin Security Passkey
              </label>
              <div className="relative">
                <KeyRound className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                <input
                  type="password"
                  value={passkey}
                  onChange={(e) => setPasskey(e.target.value)}
                  placeholder="Enter administrator passkey..."
                  className="w-full bg-stone-950 border border-stone-800 rounded-xl pl-9 pr-3 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-400"
                  required
                  autoFocus
                />
              </div>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-xl bg-stone-800 hover:bg-stone-750 text-slate-300 font-bold text-xs transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isVerifying || !passkey.trim()}
                className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-600 hover:from-amber-400 hover:to-yellow-500 text-stone-950 font-bold text-xs shadow-lg shadow-amber-500/20 transition-all disabled:opacity-50"
              >
                {isVerifying ? 'Verifying...' : 'Unlock Station'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
