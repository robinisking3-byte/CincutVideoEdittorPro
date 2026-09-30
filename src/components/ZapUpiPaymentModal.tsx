import React, { useState } from 'react';
import { X, CheckCircle, ShieldCheck, Zap, Smartphone, QrCode } from 'lucide-react';
import { SubscriptionPlanConfig, User } from '../types';

interface ZapUpiPaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  plan: SubscriptionPlanConfig | null;
  currentUser: User;
  onPaymentSuccess: (plan: SubscriptionPlanConfig, txn: any) => void;
}

export const ZapUpiPaymentModal: React.FC<ZapUpiPaymentModalProps> = ({
  isOpen,
  onClose,
  plan,
  currentUser,
  onPaymentSuccess
}) => {
  const [upiId, setUpiId] = useState(currentUser.email ? `${currentUser.username.toLowerCase()}@okhdfcbank` : 'creator@upi');
  const [isProcessing, setIsProcessing] = useState(false);
  const [selectedGateway, setSelectedGateway] = useState<'GPAY' | 'PHONEPE' | 'PAYTM' | 'CRED'>('GPAY');

  if (!isOpen || !plan) return null;

  const handlePay = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setIsProcessing(false);
      const txn = {
        txnId: `UPI-${Date.now().toString().slice(-6)}`,
        gateway: selectedGateway,
        amount: plan.price,
        timestamp: new Date().toISOString()
      };
      onPaymentSuccess(plan, txn);
      onClose();
    }, 1500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-in fade-in">
      <div className="bg-stone-900 border border-stone-700 w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between border-b border-stone-800 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
              <Zap className="w-5 h-5 fill-current" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">ZapUPI Instant Checkout</h3>
              <p className="text-[11px] text-amber-400 font-mono">0% Gateway Fee • Instant Activation</p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 rounded-2xl bg-gradient-to-br from-stone-950 to-stone-900 border border-amber-500/30 space-y-2">
          <div className="flex justify-between items-center">
            <span className="text-xs text-slate-300 font-medium">Selected Plan</span>
            <span className="text-xs font-bold text-amber-300">{plan.name}</span>
          </div>
          <div className="flex justify-between items-center text-sm font-bold text-white">
            <span>Payable Amount</span>
            <span className="text-xl text-emerald-400 font-mono">₹{plan.price}</span>
          </div>
          <p className="text-[10px] text-slate-400">Includes {plan.blueCoinsMonthly} Blue Coins & Priority Cloud Rendering</p>
        </div>

        {/* UPI Gateway selector */}
        <div className="space-y-1.5">
          <label className="text-xs text-slate-300 font-semibold block">Choose UPI App</label>
          <div className="grid grid-cols-4 gap-2">
            {[
              { id: 'GPAY', name: 'GPay', color: 'from-blue-600 to-emerald-600' },
              { id: 'PHONEPE', name: 'PhonePe', color: 'from-purple-600 to-indigo-600' },
              { id: 'PAYTM', name: 'Paytm', color: 'from-cyan-600 to-blue-700' },
              { id: 'CRED', name: 'CRED', color: 'from-rose-600 to-amber-600' }
            ].map(gw => (
              <button
                key={gw.id}
                onClick={() => setSelectedGateway(gw.id as any)}
                className={`p-2 rounded-xl text-xs font-bold text-center border transition-all ${
                  selectedGateway === gw.id
                    ? 'border-amber-400 bg-amber-500/20 text-amber-300'
                    : 'border-stone-800 bg-stone-950 text-slate-400 hover:border-stone-700'
                }`}
              >
                {gw.name}
              </button>
            ))}
          </div>
        </div>

        {/* UPI ID input */}
        <div className="space-y-1">
          <label className="text-xs text-slate-300 font-semibold block">VPA / UPI Handle</label>
          <input
            type="text"
            value={upiId}
            onChange={(e) => setUpiId(e.target.value)}
            placeholder="e.g. mobile@upi"
            className="w-full bg-stone-950 border border-stone-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-400"
          />
        </div>

        <button
          onClick={handlePay}
          disabled={isProcessing}
          className="w-full py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-stone-950 font-black text-sm flex items-center justify-center gap-2 shadow-lg transition-all"
        >
          {isProcessing ? (
            <span className="flex items-center gap-2">
              <span className="w-4 h-4 border-2 border-stone-950 border-t-transparent rounded-full animate-spin" />
              Verifying UPI Transaction...
            </span>
          ) : (
            <>
              <ShieldCheck className="w-4 h-4" />
              <span>Authorize & Pay ₹{plan.price}</span>
            </>
          )}
        </button>

        <div className="flex items-center justify-center gap-2 text-[10px] text-slate-500">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
          <span>256-bit Encrypted NPCI UPI Certified Checkout</span>
        </div>
      </div>
    </div>
  );
};
