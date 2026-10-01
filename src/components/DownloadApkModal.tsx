import React, { useState } from 'react';
import { 
  X, 
  Download, 
  Smartphone, 
  CheckCircle2, 
  ShieldCheck, 
  Copy, 
  FileCode, 
  ExternalLink,
  Sparkles,
  Scissors,
  Wand2,
  Lock,
  ArrowRight,
  ShieldAlert
} from 'lucide-react';

interface DownloadApkModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DownloadApkModal: React.FC<DownloadApkModalProps> = ({ isOpen, onClose }) => {
  const [downloadInitiated, setDownloadInitiated] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  if (!isOpen) return null;

  const handleDownload = () => {
    setDownloadInitiated(true);
    // Direct link to the compiled Android APK
    window.location.href = '/CincutVideoEdittorPro-v1.0.0.apk';
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.origin + '/CincutVideoEdittorPro-v1.0.0.apk');
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div 
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto bg-studio-900 border border-studio-700/80 rounded-3xl shadow-2xl shadow-cyan-950/40 text-slate-100"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Header */}
        <div className="px-6 py-5 bg-studio-850 border-b border-studio-750 flex items-center justify-between sticky top-0 z-10 backdrop-blur-md">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-cyan-500 via-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <Scissors className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-base font-bold text-white">Cincut Video Editor Pro</h3>
                <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
                  v1.0.0 Production
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Official Native Android App (.apk) with Standalone Package ID &amp; Integrated Admin Station
              </p>
            </div>
          </div>

          <button 
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-studio-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 space-y-6">
          
          {/* Main Direct Download Banner */}
          <div className="p-5 rounded-2xl bg-gradient-to-br from-cyan-950/60 via-studio-850 to-studio-900 border-2 border-cyan-500/40 shadow-xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400 font-mono">
                  Verified Android Package
                </span>
                <h4 className="text-lg font-bold text-white">
                  Cincut Video Editor Pro v1.0.0
                </h4>
                <p className="text-xs text-slate-300 max-w-md">
                  Brand new standalone package ID (<code className="text-cyan-300 font-mono">com.cincut.pro.studio</code>). Installs cleanly without loading or conflicting with any older apps.
                </p>
              </div>

              <div className="flex flex-col items-stretch gap-2 shrink-0">
                <a
                  href="/CincutVideoEdittorPro-v1.0.0.apk"
                  download="CincutVideoEdittorPro-v1.0.0.apk"
                  onClick={handleDownload}
                  className="px-5 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-stone-950 font-extrabold text-xs shadow-lg shadow-cyan-500/30 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
                >
                  <Download className="w-4 h-4" />
                  <span>Download Cincut APK v1.0.0</span>
                </a>

                <button
                  onClick={handleCopyLink}
                  className="px-3 py-1.5 rounded-lg bg-studio-800 hover:bg-studio-750 text-slate-300 text-[11px] font-medium flex items-center justify-center gap-1.5 border border-studio-700 transition-colors"
                >
                  <Copy className="w-3 h-3" />
                  <span>{copiedLink ? 'Link Copied!' : 'Copy Direct Link'}</span>
                </button>

                <a
                  href="https://github.com/robinisking3-byte/CincutVideoEdittorPro/releases"
                  target="_blank"
                  rel="noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-studio-850 hover:bg-studio-800 text-cyan-300 text-[11px] font-medium flex items-center justify-center gap-1.5 border border-cyan-500/30 transition-colors"
                >
                  <ExternalLink className="w-3 h-3" />
                  <span>GitHub Releases &amp; Source</span>
                </a>
              </div>
            </div>

              {downloadInitiated && (
              <div className="p-2.5 rounded-xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Download started! Check your browser downloads for <strong>CincutVideoEdittorPro-v1.0.0.apk</strong>.</span>
              </div>
            )}
          </div>

          {/* Android App Specifications */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Android Technical Specifications
            </h4>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
              <div className="p-3 rounded-xl bg-studio-850 border border-studio-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">App Label</span>
                <span className="text-xs font-bold text-white truncate block">Cincut Video Editor Pro</span>
              </div>
              <div className="p-3 rounded-xl bg-studio-850 border border-studio-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Target SDK</span>
                <span className="text-xs font-mono font-bold text-white">Android 15 (API 35)</span>
              </div>
              <div className="p-3 rounded-xl bg-studio-850 border border-studio-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Package ID</span>
                <span className="text-xs font-mono font-bold text-cyan-300">com.cincut.pro.studio</span>
              </div>
              <div className="p-3 rounded-xl bg-studio-850 border border-studio-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Version</span>
                <span className="text-xs font-mono font-bold text-emerald-400">1.0.1 (Code 2)</span>
              </div>
            </div>
          </div>

          {/* Installation Instructions */}
          <div className="p-4 rounded-2xl bg-studio-850 border border-studio-750 space-y-2 text-xs">
            <span className="font-bold text-slate-200 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-cyan-400" />
              <span>Clean Installation (New Standalone Package):</span>
            </span>
            <ol className="list-decimal pl-5 space-y-1.5 text-slate-300 text-[11px] leading-relaxed">
              <li>Tap <strong>Download Cincut APK v1.0.1</strong> directly on your Android phone.</li>
              <li>This version uses a brand new package ID (<code>com.cincut.pro.studio</code>), ensuring your device will never read or conflict with older installed app versions.</li>
              <li>When opening the downloaded APK, tap <strong>Settings</strong> and enable <strong>"Allow from this source"</strong> if prompted.</li>
              <li>Tap <strong>Install</strong> to launch the clean, standalone v1.0.1 app.</li>
            </ol>
          </div>

        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-studio-850/90 border-t border-studio-750 flex items-center justify-between sticky bottom-0 z-10 backdrop-blur-md">
          <span className="text-[11px] text-slate-400 font-mono">
            Direct Path: /CincutVideoEdittorPro-v1.0.0.apk
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-studio-800 hover:bg-studio-750 text-slate-200"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
