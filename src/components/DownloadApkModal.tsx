import React, { useState } from 'react';
import { 
  X, 
  Download, 
  CheckCircle2, 
  ShieldCheck, 
  Copy, 
  ExternalLink,
  Scissors,
  Sparkles,
  Palette,
  Check
} from 'lucide-react';

interface DownloadApkModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DownloadApkModal: React.FC<DownloadApkModalProps> = ({ isOpen, onClose }) => {
  const [downloadInitiated, setDownloadInitiated] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  if (!isOpen) return null;

  const apkFilename = 'CincutVideoEditorPro-v1.0.3.apk';
  const downloadUrl = `/${apkFilename}`;
  const githubReleaseUrl = 'https://github.com/robinisking3-byte/CincutVideoEdittorPro/releases';

  const handleDownload = () => {
    setDownloadInitiated(true);
    window.location.href = downloadUrl;
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.origin + downloadUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div 
        className="relative w-full max-w-2xl max-h-[92vh] overflow-y-auto bg-stone-900 border border-stone-700/80 rounded-3xl shadow-2xl text-slate-100"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Header */}
        <div className="px-6 py-5 bg-stone-850 border-b border-stone-800 flex items-center justify-between sticky top-0 z-10 backdrop-blur-md">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-cyan-500 via-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <Scissors className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-base font-bold text-white">Cincut Video Editor Pro</h3>
                <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                  v1.0.3 Production
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Official CineCut Pro App with all screens, real ZapUPI gateway &amp; 14 color filters
              </p>
            </div>
          </div>

          <button 
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-stone-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 space-y-5">
          
          {/* Main Direct Download Banner */}
          <div className="p-5 rounded-2xl bg-gradient-to-br from-cyan-950/70 via-stone-850 to-stone-900 border-2 border-cyan-500/50 shadow-xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400 font-mono">
                    Official App Package
                  </span>
                  <span className="text-[9px] px-2 py-0.5 rounded-full font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30 font-mono">
                    ALL FEATURES INCLUDED
                  </span>
                </div>
                <h4 className="text-lg font-black text-white">
                  Cincut Video Editor Pro v1.0.3
                </h4>
                <p className="text-xs text-slate-300 max-w-md leading-relaxed">
                  Official CineCut package <code className="text-cyan-300 font-mono bg-cyan-950/60 px-1.5 py-0.5 rounded border border-cyan-500/30">com.cincut.pro.studio</code>. Includes multi-track timeline, 14 color filters, real ZapUPI payment gateway, AI Director, CineRooms &amp; CineLive, and Admin Dashboard.
                </p>
              </div>

              <div className="flex flex-col items-stretch gap-2 shrink-0">
                <a
                  href={downloadUrl}
                  download={apkFilename}
                  onClick={handleDownload}
                  className="px-5 py-3 rounded-xl bg-gradient-to-r from-cyan-500 via-blue-500 to-indigo-600 hover:from-cyan-400 hover:to-blue-400 text-stone-950 font-black text-xs shadow-lg shadow-cyan-500/30 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
                >
                  <Download className="w-4 h-4" />
                  <span>Download CineCut APK (v1.0.3)</span>
                </a>

                <button
                  onClick={handleCopyLink}
                  className="px-3 py-1.5 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-300 text-[11px] font-medium flex items-center justify-center gap-1.5 border border-stone-750 transition-colors font-mono"
                >
                  <Copy className="w-3 h-3" />
                  <span>{copiedLink ? 'Link Copied!' : 'Copy Direct APK Link'}</span>
                </button>

                <a
                  href={githubReleaseUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-stone-850 hover:bg-stone-800 text-cyan-300 text-[11px] font-medium flex items-center justify-center gap-1.5 border border-cyan-500/30 transition-colors"
                >
                  <ExternalLink className="w-3 h-3" />
                  <span>GitHub Releases</span>
                </a>
              </div>
            </div>

            {downloadInitiated && (
              <div className="p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>
                  Download started! Opening <strong>{apkFilename}</strong> on your device.
                </span>
              </div>
            )}
          </div>

          {/* New Features in v2.0.0 */}
          <div className="p-4 rounded-2xl bg-stone-850 border border-stone-800 space-y-2.5 text-xs">
            <span className="font-bold text-slate-200 flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-cyan-400" />
              <span>What's Brand New in v2.0.0:</span>
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] text-slate-300">
              <div className="flex items-start gap-1.5 p-2 rounded-lg bg-stone-900 border border-stone-800">
                <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <strong className="text-white block">100% Brand New Identity:</strong>
                  Package ID <code className="text-cyan-300 font-mono">com.cincut.official.videoeditor</code> installs side-by-side with its own clean storage.
                </div>
              </div>
              <div className="flex items-start gap-1.5 p-2 rounded-lg bg-stone-900 border border-stone-800">
                <Palette className="w-3.5 h-3.5 text-amber-400 shrink-0 mt-0.5" />
                <div>
                  <strong className="text-white block">14 Cinematic Filters:</strong>
                  Teal &amp; Orange, Warm Sunset, Noir B&amp;W, Retro VHS, Tokyo Night, Emerald Film, Pastel Dream, and more.
                </div>
              </div>
            </div>
          </div>

          {/* Android App Specifications */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Android Technical Specifications
            </h4>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
              <div className="p-3 rounded-xl bg-stone-850 border border-stone-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">App Label</span>
                <span className="text-xs font-bold text-white truncate block">Cincut Video Editor Pro</span>
              </div>
              <div className="p-3 rounded-xl bg-stone-850 border border-stone-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Package ID</span>
                <span className="text-xs font-mono font-bold text-cyan-300">com.cincut.official.videoeditor</span>
              </div>
              <div className="p-3 rounded-xl bg-stone-850 border border-stone-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Version</span>
                <span className="text-xs font-mono font-bold text-emerald-400">2.0.0</span>
              </div>
              <div className="p-3 rounded-xl bg-stone-850 border border-stone-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Version Code</span>
                <span className="text-xs font-mono font-bold text-amber-400">1 (Fresh)</span>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-stone-850/90 border-t border-stone-800 flex items-center justify-between sticky bottom-0 z-10 backdrop-blur-md">
          <span className="text-[11px] text-slate-400 font-mono">
            Direct Path: {downloadUrl}
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-stone-800 hover:bg-stone-750 text-slate-200"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
