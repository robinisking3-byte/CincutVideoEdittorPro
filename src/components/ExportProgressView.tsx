import React, { useState, useEffect } from 'react';
import { 
  Film, 
  CheckCircle, 
  Clock, 
  Cpu, 
  Sliders, 
  Download, 
  X, 
  Pause, 
  Play, 
  AlertCircle, 
  Sparkles, 
  Layers, 
  HardDrive,
  Activity
} from 'lucide-react';

export interface ExportProgressViewProps {
  progress: number; // 0 to 100
  isExporting: boolean;
  isComplete: boolean;
  projectTitle: string;
  resolution: string;
  fps: number;
  aspectRatio?: string;
  totalDurationSeconds?: number;
  videoBlobUrl?: string | null;
  onCancel?: () => void;
  onPauseToggle?: (isPaused: boolean) => void;
  onDone?: () => void;
  onDownload?: () => void;
  compact?: boolean;
}

export const ExportProgressView: React.FC<ExportProgressViewProps> = ({
  progress,
  isExporting,
  isComplete,
  projectTitle,
  resolution,
  fps,
  aspectRatio = '9:16',
  totalDurationSeconds = 15.0,
  videoBlobUrl,
  onCancel,
  onPauseToggle,
  onDone,
  onDownload,
  compact = false
}) => {
  const [isPaused, setIsPaused] = useState(false);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [elapsedSeconds, setElapsedSeconds] = useState<number>(0);

  // Track elapsed time while exporting
  useEffect(() => {
    if (!isExporting || isComplete) {
      if (!isExporting && !isComplete) {
        setElapsedSeconds(0);
        setStartTime(Date.now());
      }
      return;
    }

    const interval = setInterval(() => {
      if (!isPaused) {
        setElapsedSeconds((prev) => prev + 1);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [isExporting, isComplete, isPaused]);

  // Reset timer on new export run
  useEffect(() => {
    if (isExporting && progress <= 5) {
      setStartTime(Date.now());
      setElapsedSeconds(0);
      setIsPaused(false);
    }
  }, [isExporting]);

  // Calculate estimated remaining time (ETA)
  const calculateEta = (): { text: string; secondsRemaining: number } => {
    if (isComplete) return { text: '0s', secondsRemaining: 0 };
    if (progress <= 2) return { text: 'Calculating...', secondsRemaining: 15 };

    // Standard video render simulation: total frames = duration * fps
    const totalFrames = Math.max(30, Math.round(totalDurationSeconds * fps));
    const renderedFrames = Math.round((progress / 100) * totalFrames);
    const remainingFrames = totalFrames - renderedFrames;

    // Rate: frames per second rendered
    const effectiveElapsed = Math.max(1, elapsedSeconds);
    const fpsRenderSpeed = renderedFrames / effectiveElapsed;

    let secondsRemaining = 0;
    if (fpsRenderSpeed > 0) {
      secondsRemaining = Math.max(1, Math.round(remainingFrames / fpsRenderSpeed));
    } else {
      secondsRemaining = Math.max(1, Math.round(((100 - progress) / (progress / effectiveElapsed))));
    }

    if (secondsRemaining < 60) {
      return { text: `${secondsRemaining}s remaining`, secondsRemaining };
    }
    const mins = Math.floor(secondsRemaining / 60);
    const secs = secondsRemaining % 60;
    return { text: `${mins}m ${secs}s remaining`, secondsRemaining };
  };

  const eta = calculateEta();
  const totalFrames = Math.round(totalDurationSeconds * fps);
  const currentFrame = Math.min(totalFrames, Math.round((progress / 100) * totalFrames));

  // Determine current processing stage
  const getRenderStage = (pct: number) => {
    if (pct < 18) {
      return {
        title: 'Timeline & Audio Stems Decoding',
        desc: 'Analyzing track keyframes and multi-track audio waveform',
        badge: 'Stage 1/5'
      };
    } else if (pct < 42) {
      return {
        title: '3D LUT Color Matrix & Effects',
        desc: 'Applying Cinematic color grading and HDR luminance curves',
        badge: 'Stage 2/5'
      };
    } else if (pct < 68) {
      return {
        title: 'Subtitle Sync & Motion Graphic Blending',
        desc: 'Rendering bouncing karaoke captions and animated transitions',
        badge: 'Stage 3/5'
      };
    } else if (pct < 92) {
      return {
        title: 'H.264 / AVC Hardware Acceleration',
        desc: 'Encoding bitstream via Qualcomm Snapdragon / Adreno GPU',
        badge: 'Stage 4/5'
      };
    } else {
      return {
        title: 'MP4 Container Muxing & Integrity Audit',
        desc: 'Packaging metadata tags, fast-start moov atom & audio mix',
        badge: 'Stage 5/5'
      };
    }
  };

  const stage = getRenderStage(progress);

  // Estimated file size based on resolution and duration
  const getEstimatedFileSize = () => {
    let mbPerSec = 1.2;
    if (resolution === '720p') mbPerSec = 0.6;
    if (resolution === '1080p') mbPerSec = 1.4;
    if (resolution === '4K') mbPerSec = 3.8;
    if (resolution === '8K') mbPerSec = 8.5;
    return (totalDurationSeconds * mbPerSec).toFixed(1);
  };

  const handleTogglePause = () => {
    const nextState = !isPaused;
    setIsPaused(nextState);
    if (onPauseToggle) {
      onPauseToggle(nextState);
    }
  };

  // Compact Mode (for embedding directly inside smartphone simulator or overlay cards)
  if (compact) {
    return (
      <div className="p-3 rounded-2xl bg-stone-900/95 border border-cyan-500/40 shadow-xl space-y-2 backdrop-blur-md">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 flex items-center justify-center">
              {isComplete ? (
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
              ) : (
                <Film className={`w-3.5 h-3.5 ${isExporting && !isPaused ? 'animate-spin-slow' : ''}`} />
              )}
            </div>
            <div>
              <h5 className="text-[11px] font-bold text-white leading-tight truncate max-w-[140px]">
                {isComplete ? 'Export Complete!' : projectTitle}
              </h5>
              <span className="text-[9px] text-cyan-400 font-mono">
                {resolution} @ {fps}fps • {aspectRatio}
              </span>
            </div>
          </div>
          <div className="text-right">
            <span className="text-xs font-black text-cyan-400 font-mono">
              {isComplete ? '100%' : `${progress}%`}
            </span>
            <span className="text-[8px] text-slate-400 block font-mono">
              {isComplete ? 'Finished' : eta.text}
            </span>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-stone-950 h-2 rounded-full overflow-hidden border border-stone-800 p-0.5">
          <div 
            className={`h-full rounded-full transition-all duration-200 ${
              isComplete
                ? 'bg-gradient-to-r from-emerald-500 to-teal-400'
                : isPaused
                ? 'bg-amber-400'
                : 'bg-gradient-to-r from-cyan-500 via-blue-500 to-indigo-500'
            }`}
            style={{ width: `${isComplete ? 100 : progress}%` }}
          />
        </div>

        {/* Frame / ETA footer */}
        <div className="flex items-center justify-between text-[9px] text-slate-400 font-mono pt-0.5">
          <span>Frame {currentFrame}/{totalFrames}</span>
          <span className="text-slate-300">{stage.title}</span>
        </div>
      </div>
    );
  }

  // Full Rich Component Mode (for Export Studio modal or dedicated view)
  return (
    <div className="w-full space-y-5 animate-in fade-in">
      {/* Header with Visual Status Indicator */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-2xl bg-gradient-to-r from-stone-950 via-stone-900 to-stone-950 border border-stone-800">
        <div className="flex items-center gap-3">
          <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all ${
            isComplete
              ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40 shadow-lg shadow-emerald-950/40'
              : 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30 shadow-lg shadow-cyan-950/40'
          }`}>
            {isComplete ? (
              <CheckCircle className="w-6 h-6 animate-in zoom-in" />
            ) : (
              <Film className={`w-6 h-6 ${isExporting && !isPaused ? 'animate-spin-slow' : ''}`} />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h4 className="font-bold text-white text-base">
                {isComplete ? 'Rendering Finished!' : `Rendering ${projectTitle}`}
              </h4>
              <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-bold uppercase border ${
                isComplete 
                  ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
                  : isPaused
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
                  : 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30 animate-pulse'
              }`}>
                {isComplete ? 'READY' : isPaused ? 'PAUSED' : 'ACTIVE'}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5 flex items-center gap-2">
              <span>H.264 MP4</span>
              <span>•</span>
              <span className="text-cyan-400 font-mono font-semibold">{resolution} @ {fps}fps</span>
              <span>•</span>
              <span>{aspectRatio} Ratio</span>
            </p>
          </div>
        </div>

        {/* Right side live ETA & Elapsed badges */}
        <div className="flex items-center gap-2">
          <div className="px-3 py-1.5 rounded-xl bg-stone-900 border border-stone-800 text-right">
            <span className="text-[10px] text-slate-400 block font-sans">Estimated Time</span>
            <span className="text-xs font-bold text-amber-300 font-mono flex items-center gap-1 justify-end">
              <Clock className="w-3 h-3 text-amber-400" />
              {isComplete ? '0s' : eta.text}
            </span>
          </div>

          <div className="px-3 py-1.5 rounded-xl bg-stone-900 border border-stone-800 text-right">
            <span className="text-[10px] text-slate-400 block font-sans">Elapsed</span>
            <span className="text-xs font-bold text-white font-mono flex items-center gap-1 justify-end">
              <Activity className="w-3 h-3 text-cyan-400" />
              {Math.floor(elapsedSeconds / 60).toString().padStart(2, '0')}:{(elapsedSeconds % 60).toString().padStart(2, '0')}
            </span>
          </div>
        </div>
      </div>

      {/* Main Real-Time Progress Bar & Percentage Readout */}
      <div className="p-4 rounded-2xl bg-stone-950 border border-stone-800 space-y-3">
        <div className="flex items-end justify-between">
          <div className="space-y-0.5">
            <span className="text-[11px] font-mono text-cyan-400 font-bold uppercase tracking-wider">
              {stage.badge}
            </span>
            <h5 className="text-sm font-bold text-white flex items-center gap-1.5">
              <span>{stage.title}</span>
            </h5>
            <p className="text-xs text-slate-400">{stage.desc}</p>
          </div>

          <div className="text-right">
            <div className="flex items-baseline gap-1 justify-end">
              <span className="text-3xl font-black text-white font-mono tracking-tight">
                {isComplete ? 100 : progress}
              </span>
              <span className="text-base font-bold text-cyan-400">%</span>
            </div>
            <span className="text-[10px] text-slate-500 font-mono">
              Frame {currentFrame} of {totalFrames}
            </span>
          </div>
        </div>

        {/* Progress Bar Container */}
        <div className="relative w-full bg-stone-900 h-4 rounded-full overflow-hidden border border-stone-800 p-0.5 shadow-inner">
          <div 
            className={`h-full rounded-full transition-all duration-300 relative ${
              isComplete
                ? 'bg-gradient-to-r from-emerald-500 to-teal-400 shadow-lg shadow-emerald-500/20'
                : isPaused
                ? 'bg-amber-500'
                : 'bg-gradient-to-r from-cyan-500 via-blue-500 to-indigo-500 shadow-lg shadow-cyan-500/20'
            }`}
            style={{ width: `${isComplete ? 100 : progress}%` }}
          >
            {/* Shimmer light bar */}
            {!isComplete && !isPaused && (
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent animate-shimmer" />
            )}
          </div>
        </div>

        {/* Pipeline Step Dots */}
        <div className="grid grid-cols-5 gap-1.5 pt-1">
          {[
            { label: 'Audio/Stems', pct: 18 },
            { label: '3D LUTs', pct: 42 },
            { label: 'Subtitles', pct: 68 },
            { label: 'H.264 GPU', pct: 92 },
            { label: 'MP4 Mux', pct: 100 }
          ].map((step, idx) => {
            const isDone = (isComplete ? 100 : progress) >= step.pct;
            const isCurrent = !isDone && ((isComplete ? 100 : progress) >= (idx === 0 ? 0 : [18, 42, 68, 92][idx - 1]));
            return (
              <div 
                key={idx} 
                className={`p-2 rounded-xl border text-center transition-all ${
                  isDone 
                    ? 'bg-cyan-500/10 border-cyan-500/40 text-cyan-300'
                    : isCurrent
                    ? 'bg-amber-500/10 border-amber-500/40 text-amber-200 ring-1 ring-amber-500/30'
                    : 'bg-stone-900/60 border-stone-800 text-slate-500'
                }`}
              >
                <span className="text-[9px] font-mono font-bold block truncate">{step.label}</span>
                <span className="text-[8px] opacity-75">{isDone ? '✓ Done' : isCurrent ? 'Active' : 'Queued'}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Hardware & Encoding Telemetry Stats Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
        <div className="p-2.5 rounded-xl bg-stone-950 border border-stone-800">
          <div className="flex items-center gap-1.5 text-slate-400 mb-1">
            <Cpu className="w-3.5 h-3.5 text-cyan-400" />
            <span className="text-[10px]">Render Speed</span>
          </div>
          <span className="text-xs font-bold text-white font-mono">
            {isPaused ? 'Paused' : isComplete ? '60.0 fps (Avg)' : '54.2 fps (1.8x)'}
          </span>
        </div>

        <div className="p-2.5 rounded-xl bg-stone-950 border border-stone-800">
          <div className="flex items-center gap-1.5 text-slate-400 mb-1">
            <Layers className="w-3.5 h-3.5 text-purple-400" />
            <span className="text-[10px]">Audio Bitrate</span>
          </div>
          <span className="text-xs font-bold text-white font-mono">320 kbps AAC</span>
        </div>

        <div className="p-2.5 rounded-xl bg-stone-950 border border-stone-800">
          <div className="flex items-center gap-1.5 text-slate-400 mb-1">
            <HardDrive className="w-3.5 h-3.5 text-emerald-400" />
            <span className="text-[10px]">Estimated Size</span>
          </div>
          <span className="text-xs font-bold text-emerald-400 font-mono">
            ~{getEstimatedFileSize()} MB
          </span>
        </div>

        <div className="p-2.5 rounded-xl bg-stone-950 border border-stone-800">
          <div className="flex items-center gap-1.5 text-slate-400 mb-1">
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
            <span className="text-[10px]">Color Profile</span>
          </div>
          <span className="text-xs font-bold text-amber-300 font-mono">Rec.709 HDR</span>
        </div>
      </div>

      {/* Completion or In-Progress Action Buttons */}
      {isComplete ? (
        <div className="space-y-2 pt-2">
          <div className="p-3 rounded-2xl bg-emerald-950/60 border border-emerald-500/40 text-emerald-200 text-xs flex items-center gap-2.5">
            <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />
            <div>
              <p className="font-bold">Rendered successfully in {elapsedSeconds} seconds!</p>
              <p className="text-[11px] text-emerald-300/80">
                Encoded {totalFrames} frames at {resolution} {fps}fps. Ready to share or test on Android.
              </p>
            </div>
          </div>

          {videoBlobUrl && (
            <div className="rounded-xl overflow-hidden bg-black border border-stone-800 p-1 flex justify-center">
              <video 
                src={videoBlobUrl} 
                controls 
                className="max-h-48 rounded-lg" 
              />
            </div>
          )}

          <div className="flex flex-col sm:flex-row gap-2">
            <button
              onClick={() => {
                if (onDownload) {
                  onDownload();
                } else if (videoBlobUrl) {
                  const a = document.createElement('a');
                  a.href = videoBlobUrl;
                  a.download = `${projectTitle.replace(/\s+/g, '_')}_${resolution}.mp4`;
                  a.click();
                }
              }}
              className="flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-stone-950 font-bold text-xs flex items-center justify-center gap-2 shadow-lg transition-all"
            >
              <Download className="w-4 h-4" />
              <span>Save Video to Gallery &amp; Device</span>
            </button>

            {onDone && (
              <button
                onClick={onDone}
                className="py-3 px-5 rounded-xl bg-stone-800 hover:bg-stone-750 text-slate-200 text-xs font-bold transition-all"
              >
                Close
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="flex items-center justify-between pt-2 border-t border-stone-800/80">
          <div className="flex items-center gap-2">
            {onPauseToggle && (
              <button
                onClick={handleTogglePause}
                className="px-3 py-2 rounded-xl bg-stone-800 hover:bg-stone-750 text-slate-200 text-xs font-bold flex items-center gap-1.5 transition-all"
              >
                {isPaused ? <Play className="w-3.5 h-3.5 fill-current" /> : <Pause className="w-3.5 h-3.5" />}
                <span>{isPaused ? 'Resume Rendering' : 'Pause'}</span>
              </button>
            )}
          </div>

          {onCancel && (
            <button
              onClick={onCancel}
              className="px-3 py-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/30 text-xs font-bold flex items-center gap-1.5 transition-all"
            >
              <X className="w-3.5 h-3.5" />
              <span>Cancel Rendering</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};
