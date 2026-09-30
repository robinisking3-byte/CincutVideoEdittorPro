import React, { useState } from 'react';
import { Ban, ShieldAlert, Gavel, RefreshCw, LogOut } from 'lucide-react';
import { RoleCoinBadge } from './RoleCoinBadge';
import { User } from '../types';

interface BannedLockoutModalProps {
  user: User;
  onSwitchToAdmin: () => void;
}

export const BannedLockoutModal: React.FC<BannedLockoutModalProps> = ({
  user,
  onSwitchToAdmin
}) => {
  const [hammerActive, setHammerActive] = useState(false);

  const triggerHammerSmash = () => {
    setHammerActive(true);
    setTimeout(() => setHammerActive(false), 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/95 backdrop-blur-xl animate-in fade-in duration-300">
      <div className="relative w-full max-w-lg bg-gradient-to-b from-stone-950 via-rose-950/40 to-black border-2 border-rose-600/80 rounded-3xl shadow-[0_0_80px_rgba(225,29,72,0.5)] p-8 text-center text-slate-100 flex flex-col items-center">
        
        {/* Warning Caution Banner */}
        <div className="w-full py-1.5 px-4 mb-6 rounded-lg bg-rose-600 text-white font-mono font-black text-xs uppercase tracking-widest flex items-center justify-center space-x-2 shadow-lg shadow-rose-600/30">
          <ShieldAlert className="w-4 h-4" />
          <span>SECURITY ENFORCEMENT: ACCOUNT ACCESS LOCKED</span>
          <ShieldAlert className="w-4 h-4" />
        </div>

        {/* Hammer Crushing Coin Visual Stage */}
        <div className="relative my-4 flex items-center justify-center cursor-pointer" onClick={triggerHammerSmash}>
          {/* Animated Gavel / Hammer */}
          <div className={`absolute -top-12 z-20 text-rose-500 transition-transform duration-300 ${
            hammerActive ? 'translate-y-6 rotate-[-70deg] scale-125' : 'animate-bounce rotate-[-25deg]'
          }`}>
            <Gavel className="w-16 h-16 drop-shadow-[0_0_15px_rgba(244,63,94,1)]" />
          </div>

          {/* Smashed Coin Visual */}
          <div className={`transition-all duration-200 ${hammerActive ? 'scale-75 rotate-12 blur-[1px]' : ''}`}>
            <RoleCoinBadge role="BANNED" size="xl" interactive={false} />
          </div>
        </div>

        <button
          onClick={triggerHammerSmash}
          className="mt-2 text-[11px] font-mono text-rose-400/80 hover:text-rose-300 underline"
        >
          [ Click to replay Hammer Crushing Animation ]
        </button>

        {/* Ban Details */}
        <div className="mt-5 space-y-2">
          <h2 className="text-xl font-extrabold text-white tracking-tight">
            Account @{user.username} is Banned
          </h2>
          <p className="text-xs text-rose-300/90 max-w-sm mx-auto leading-relaxed">
            Every feature on this platform is locked. You cannot view projects, edit videos, send messages, or perform transactions.
          </p>
          <div className="p-3 bg-black/60 rounded-xl border border-rose-900/60 text-[11px] font-mono text-slate-400 mt-3 text-left space-y-1">
            <div><span className="text-slate-500">User ID:</span> {user.userIdTag}</div>
            <div><span className="text-slate-500">Reason:</span> Multi-account botting / security policy breach</div>
            <div><span className="text-slate-500">Authority:</span> System Security & Master Admin</div>
          </div>
        </div>

        {/* Action Button */}
        <div className="mt-8 flex items-center space-x-3 w-full">
          <button
            onClick={onSwitchToAdmin}
            className="flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-700 hover:from-cyan-500 hover:to-blue-600 text-white font-bold text-xs flex items-center justify-center space-x-2 shadow-lg transition-all"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Switch to Master Admin (@robin_master)</span>
          </button>
        </div>
      </div>
    </div>
  );
};
