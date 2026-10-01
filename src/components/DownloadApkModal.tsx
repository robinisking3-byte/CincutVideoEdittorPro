import React, { useState } from 'react';
import { 
  X, 
  Download, 
  Smartphone, 
  CheckCircle2, 
  ShieldCheck, 
  Copy, 
  ExternalLink,
  Scissors,
  RefreshCw,
  AlertTriangle
} from 'lucide-react';

interface DownloadApkModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DownloadApkModal: React.FC<DownloadApkModalProps> = ({ isOpen, onClose }) => {
  const [downloadInitiated, setDownloadInitiated] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  if (!isOpen) return null;

  const apkFilename = 'CincutVideoEdittorPro-v1.0.2.apk';
  const downloadUrl = `/${apkFilename}`;
  const githubReleaseUrl = 'https://github.com/robinisking3-byte/CincutVideoEdittorPro/releases';

  const handleDownload = () => {
    setDownloadInitiated(true);
    // Direct link to the compiled Android APK
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
                <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                  v1.0.2 (Build Code 3)
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Official Native Android App with Real ZapUPI Gateway &amp; Media Export Suite
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
          <div className="p-5 rounded-2xl bg-gradient-to-br from-cyan-950/60 via-stone-850 to-stone-900 border-2 border-cyan-500/40 shadow-xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400 font-mono">
                    Newest Release
                  </span>
                  <span className="text-[9px] px-2 py-0.5 rounded-full font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 font-mono">
                    BUMPED VERSION CODE 3
                  </span>
                </div>
                <h4 className="text-lg font-bold text-white">
                  Cincut Video Editor Pro v1.0.2
                </h4>
                <p className="text-xs text-slate-300 max-w-md">
                  Includes live ZapUPI Payment Gateway, 12-digit UTR Verification, real Media Picking from gallery, and direct video file export rendering!
                </p>
              </div>

              <div className="flex flex-col items-stretch gap-2 shrink-0">
                <a
                  href={downloadUrl}
                  download={apkFilename}
                  onClick={handleDownload}
                  className="px-5 py-3 rounded-xl bg-gradient-to-r from-cyan-500 via-blue-500 to-indigo-600 hover:from-cyan-400 hover:to-blue-400 text-stone-950 font-extrabold text-xs shadow-lg shadow-cyan-500/30 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
                >
                  <Download className="w-4 h-4" />
                  <span>Download Latest APK (v1.0.2)</span>
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
                  <span>GitHub Releases (Direct APK)</span>
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

          {/* Device Upgrade / Installation Steps */}
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 space-y-2.5 text-xs">
            <div className="flex items-center gap-2 text-amber-400 font-bold">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>How to make sure your device runs the newest app:</span>
            </div>
            <ol className="list-decimal pl-5 space-y-1.5 text-slate-200 text-[11px] leading-relaxed">
              <li>
                <strong>Step 1:</strong> If you already have an older build installed on your phone or emulator, <strong>uninstall / remove the previous "Cincut" app</strong> from your home screen or App Settings to clear stale cached files.
              </li>
              <li>
                <strong>Step 2:</strong> Download and install the fresh <strong>{apkFilename}</strong> (Version Code <strong>3</strong>).
              </li>
              <li>
                <strong>Step 3:</strong> Android will recognize it as a fresh package and launch the newest version with the live ZapUPI gateway and real media import suite!
              </li>
            </ol>
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
                <span className="text-xs font-mono font-bold text-cyan-300">com.cincut.pro.studio</span>
              </div>
              <div className="p-3 rounded-xl bg-stone-850 border border-stone-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Version</span>
                <span className="text-xs font-mono font-bold text-emerald-400">1.0.2</span>
              </div>
              <div className="p-3 rounded-xl bg-stone-850 border border-stone-800">
                <span className="text-slate-400 block text-[10px] uppercase font-mono">Version Code</span>
                <span className="text-xs font-mono font-bold text-amber-400">3 (Latest)</span>
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
