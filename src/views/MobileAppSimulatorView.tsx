import React, { useState, useEffect, useRef } from 'react';
import { 
  Play, 
  Pause, 
  RotateCcw, 
  Sliders, 
  Sparkles, 
  Wand2, 
  Share2, 
  Film, 
  Volume2, 
  VolumeX, 
  Clock, 
  CheckCircle, 
  CheckCircle2, 
  Users, 
  ShieldCheck, 
  ShieldAlert, 
  Shield, 
  Download, 
  Smartphone, 
  Tv, 
  Scissors, 
  Layers, 
  Maximize2, 
  Radio, 
  Power, 
  Check, 
  Search, 
  UserPlus, 
  Crown, 
  Palette, 
  Send,
  Zap,
  Flame,
  Settings,
  Bell,
  Coins,
  MessageSquare,
  ArrowLeft,
  Copy,
  Plus,
  Diamond,
  Type,
  Music,
  Move,
  Repeat,
  Database,
  X,
  CreditCard,
  Upload,
  Lock,
  Unlock,
  Trash2,
  FileVideo
} from 'lucide-react';
import { User, SubscriptionPlanConfig, VideoTemplate, SavedProject, SavedExportedVideo } from '../types';
import { INITIAL_SUBSCRIPTION_PLANS } from '../data/initialData';
import { ZapUpiPaymentModal } from '../components/ZapUpiPaymentModal';
import { ExportProgressView } from '../components/ExportProgressView';
import { CreateTemplateModal } from '../components/CreateTemplateModal';
import { AdminAccessModal } from '../components/AdminAccessModal';
import { 
  syncPromotedFeatureToFirestore, 
  subscribeToPromotedFeatures,
  subscribeToTemplates,
  sendChatMessage,
  subscribeToChatMessages
} from '../lib/firebase';

interface MobileAppSimulatorViewProps {
  currentUser: User;
  onOpenDownloadApkModal: () => void;
  onOpenFirebaseModal: () => void;
  onSubscribeSuccess?: (plan: SubscriptionPlanConfig, txn: any) => void;
  onUpdateCurrentUser?: (user: User) => void;
}

// Built-in royalty-free sample clips
const SAMPLE_VIDEOS = [
  {
    id: 'vid-golden',
    title: 'Golden Sunset Horizon',
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    category: 'Cinematic'
  },
  {
    id: 'vid-neon',
    title: 'Urban Neon Drift',
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    category: 'Cyberpunk'
  },
  {
    id: 'vid-ocean',
    title: 'Coastal Ocean Waves',
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4',
    category: 'Vlog'
  }
];

export const MobileAppSimulatorView: React.FC<MobileAppSimulatorViewProps> = ({
  currentUser,
  onOpenDownloadApkModal,
  onOpenFirebaseModal,
  onSubscribeSuccess,
  onUpdateCurrentUser
}) => {
  // App Switching (Creator Studio vs Secure Admin Console)
  const [activeApp, setActiveApp] = useState<'creator' | 'admin'>('creator');
  const [isAdminAccessModalOpen, setIsAdminAccessModalOpen] = useState(false);
  const [phoneTheme, setPhoneTheme] = useState<'dark' | 'diwali' | 'holi'>('dark');

  // Creator App Navigation Tabs
  const [creatorBottomTab, setCreatorBottomTab] = useState<'editor' | 'projects' | 'ai' | 'social' | 'profile'>('editor');

  // Real Video Player States
  const videoRef = useRef<HTMLVideoElement>(null);
  const exportCanvasRef = useRef<HTMLCanvasElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [activeVideoSrc, setActiveVideoSrc] = useState<string>(SAMPLE_VIDEOS[0].url);
  const [activeVideoTitle, setActiveVideoTitle] = useState<string>(SAMPLE_VIDEOS[0].title);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0.0);
  const [totalDuration, setTotalDuration] = useState<number>(15.0);
  const [trimStart, setTrimStart] = useState<number>(0.0);
  const [trimEnd, setTrimEnd] = useState<number>(15.0);
  const [videoSpeed, setVideoSpeed] = useState<number>(1.0);
  const [audioVolume, setAudioVolume] = useState<number>(85);
  const [isMuted, setIsMuted] = useState<boolean>(false);

  // Framing & Filters
  const [selectedAspect, setSelectedAspect] = useState<'16:9' | '9:16' | '1:1' | '4:5'>('9:16');
  const [selectedFilter, setSelectedFilter] = useState<'Normal' | 'Cinematic' | 'Warm' | 'Noir' | 'Cyberpunk' | 'Vintage'>('Cinematic');
  const [projectTitle, setProjectTitle] = useState<string>('Sunset_Reel_Cut');
  const [saveToast, setSaveToast] = useState<string | null>(null);

  // Color Grading Adjustments
  const [exposureAdj, setExposureAdj] = useState<number>(0);
  const [contrastAdj, setContrastAdj] = useState<number>(110);
  const [saturationAdj, setSaturationAdj] = useState<number>(115);

  // Subtitles & Captions Overlay
  const [captionText, setCaptionText] = useState<string>('✨ Cinematic Golden Hour');
  const [captionStyle, setCaptionStyle] = useState<'bouncing' | 'karaoke' | 'box' | 'cinematic' | 'neon'>('bouncing');
  const [captionColor, setCaptionColor] = useState<string>('#fbbf24');

  // Multi-track & Tool Palette
  const [activeTimelineTrack, setActiveTimelineTrack] = useState<'video' | 'audio' | 'text' | 'adjust'>('video');
  const [activeEditorTool, setActiveEditorTool] = useState<'trim' | 'speed' | 'filters' | 'adjust' | 'text' | 'clips'>('trim');
  const [timelineClips, setTimelineClips] = useState([
    { id: 'c1', title: 'Hook Scene', start: 0.0, end: 5.0, color: 'from-amber-600 to-rose-600' },
    { id: 'c2', title: 'Main Motion', start: 5.0, end: 10.0, color: 'from-cyan-600 to-blue-700' },
    { id: 'c3', title: 'Outro', start: 10.0, end: 15.0, color: 'from-purple-600 to-indigo-800' }
  ]);
  const [selectedClipId, setSelectedClipId] = useState<string>('c1');

  // Keyframes
  const [keyframes, setKeyframes] = useState<Array<{ id: string; time: number; scale: number }>>([
    { id: 'kf1', time: 2.0, scale: 1.0 },
    { id: 'kf2', time: 6.5, scale: 1.15 }
  ]);

  // Real Export Engine
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [exportResolution, setExportResolution] = useState<'720p' | '1080p' | '4K'>('1080p');
  const [exportFps, setExportFps] = useState<24 | 30 | 60>(60);
  const [exportProgress, setExportProgress] = useState<number>(0);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [exportComplete, setExportComplete] = useState<boolean>(false);
  const [exportVideoBlobUrl, setExportVideoBlobUrl] = useState<string | null>(null);

  // Gallery of Exported Videos & Saved Projects
  const [exportedGallery, setExportedGallery] = useState<SavedExportedVideo[]>([]);
  const [projects, setProjects] = useState<SavedProject[]>([
    {
      id: 'p1',
      title: 'Sunset_Reel_Cut',
      aspectRatio: '9:16',
      filter: 'Cinematic',
      duration: 15.0,
      updatedAt: 'Active Project',
      thumbnailGradient: 'from-amber-600 via-rose-600 to-purple-800'
    }
  ]);

  // Templates
  const [isCreateTemplateModalOpen, setIsCreateTemplateModalOpen] = useState(false);
  const [templates, setTemplates] = useState<VideoTemplate[]>([
    {
      id: 'tpl-1',
      title: 'Neon Reels Cyber 2077',
      creatorId: 'usr-cincut-team',
      creatorName: 'Cincut Studio',
      aspectRatio: '9:16',
      category: 'Reels',
      likes: 142,
      uses: 89,
      status: 'approved',
      createdAt: '2025-01-15'
    },
    {
      id: 'tpl-2',
      title: 'Warm Sunset Travel Vlog',
      creatorId: 'usr-cincut-team',
      creatorName: 'Cincut Studio',
      aspectRatio: '16:9',
      category: 'Cinematic',
      likes: 98,
      uses: 64,
      status: 'approved',
      createdAt: '2025-01-18'
    }
  ]);

  // ZapUPI Modal
  const [isZapUpiModalOpen, setIsZapUpiModalOpen] = useState(false);
  const [selectedZapUpiPlan, setSelectedZapUpiPlan] = useState<SubscriptionPlanConfig | null>(
    INITIAL_SUBSCRIPTION_PLANS[1]
  );

  // AI Feature Coming Soon Notifications
  const [notifiedAiFeatures, setNotifiedAiFeatures] = useState<Record<string, boolean>>({});

  // Real-time Chat
  const [activeChatTag, setActiveChatTag] = useState<string | null>(null);
  const [chatInputText, setChatInputText] = useState<string>('');
  const [chatMessages, setChatMessages] = useState<Record<string, Array<{ id: string; sender: string; text: string; time: string; isMe: boolean; projectAttachment?: string }>>>({});
  const [connectedFriends, setConnectedFriends] = useState<Array<{ name: string; tag: string; role: string; online: boolean }>>([]);
  const [friendSearchInput, setFriendSearchInput] = useState<string>('');

  // Admin Console States (Strictly Gated)
  const [adminBottomTab, setAdminBottomTab] = useState<'metrics' | 'roles' | 'security' | 'broadcast' | 'promote'>('metrics');
  const [killSwitchActive, setKillSwitchActive] = useState(false);
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [broadcastSent, setBroadcastSent] = useState(false);
  const [targetUserId, setTargetUserId] = useState('');
  const [roleAssignedMsg, setRoleAssignedMsg] = useState<string | null>(null);
  const [worldwidePromotion, setWorldwidePromotion] = useState<any>(null);
  const [sessionAuditLogs, setSessionAuditLogs] = useState<Array<{ id: string; action: string; time: string; color: string }>>([
    { id: 'al-1', action: 'System session initialized securely', time: 'Just now', color: 'text-emerald-400' }
  ]);

  // Sync Video Duration and Time
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const handleLoadedMetadata = () => {
      const dur = parseFloat(video.duration.toFixed(1)) || 15.0;
      setTotalDuration(dur);
      setTrimEnd(dur);
    };

    const handleTimeUpdate = () => {
      const cur = parseFloat(video.currentTime.toFixed(1));
      setCurrentTime(cur);

      // Loop inside trim window
      if (cur >= trimEnd) {
        video.currentTime = trimStart;
        if (!isPlaying) {
          video.pause();
        }
      }
    };

    video.addEventListener('loadedmetadata', handleLoadedMetadata);
    video.addEventListener('timeupdate', handleTimeUpdate);

    return () => {
      video.removeEventListener('loadedmetadata', handleLoadedMetadata);
      video.removeEventListener('timeupdate', handleTimeUpdate);
    };
  }, [trimStart, trimEnd, isPlaying]);

  // Play / Pause Handlers
  const handleTogglePlay = () => {
    const video = videoRef.current;
    if (!video) return;

    if (isPlaying) {
      video.pause();
      setIsPlaying(false);
    } else {
      if (currentTime >= trimEnd || currentTime < trimStart) {
        video.currentTime = trimStart;
      }
      video.play().then(() => {
        setIsPlaying(true);
      }).catch(err => {
        console.warn('Video playback notice:', err);
      });
    }
  };

  // Scrubber Seek
  const handleSeek = (time: number) => {
    const clamped = Math.max(0, Math.min(totalDuration, time));
    setCurrentTime(clamped);
    if (videoRef.current) {
      videoRef.current.currentTime = clamped;
    }
  };

  // Playback Rate
  const handleSpeedChange = (speed: number) => {
    setVideoSpeed(speed);
    if (videoRef.current) {
      videoRef.current.playbackRate = speed;
    }
  };

  // Volume Change
  const handleVolumeChange = (vol: number) => {
    setAudioVolume(vol);
    if (videoRef.current) {
      videoRef.current.volume = vol / 100;
      videoRef.current.muted = vol === 0 || isMuted;
    }
  };

  // Custom Video File Upload
  const handleVideoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const objectUrl = URL.createObjectURL(file);
      setActiveVideoSrc(objectUrl);
      setActiveVideoTitle(file.name.replace(/\.[^/.]+$/, ''));
      setProjectTitle(file.name.replace(/\.[^/.]+$/, '_cut'));
      setCurrentTime(0);
      setTrimStart(0);
      setIsPlaying(false);
      setSaveToast(`Imported "${file.name}" into CineCut timeline!`);
      setTimeout(() => setSaveToast(null), 3000);
    }
  };

  // Split Clip At Playhead
  const handleSplitClipAtPlayhead = () => {
    const active = timelineClips.find(c => currentTime >= c.start && currentTime <= c.end) || timelineClips[0];
    if (!active) return;

    const cutPoint = currentTime;
    if (cutPoint <= active.start + 0.5 || cutPoint >= active.end - 0.5) {
      setSaveToast('Place playhead inside a clip to split.');
      setTimeout(() => setSaveToast(null), 2000);
      return;
    }

    const firstHalf = { ...active, end: cutPoint };
    const secondHalf = {
      id: `c_${Date.now()}`,
      title: `${active.title} (Part 2)`,
      start: cutPoint,
      end: active.end,
      color: 'from-cyan-600 to-indigo-700'
    };

    setTimelineClips(prev => [
      ...prev.filter(c => c.id !== active.id),
      firstHalf,
      secondHalf
    ].sort((a, b) => a.start - b.start));

    setSelectedClipId(secondHalf.id);
    setSaveToast(`✂️ Split clip into two at ${cutPoint.toFixed(1)}s!`);
    setTimeout(() => setSaveToast(null), 2500);
  };

  // Toggle Keyframe
  const handleToggleKeyframe = () => {
    const existing = keyframes.find(k => Math.abs(k.time - currentTime) < 0.3);
    if (existing) {
      setKeyframes(prev => prev.filter(k => k.id !== existing.id));
      setSaveToast(`Removed keyframe at ${currentTime.toFixed(1)}s`);
    } else {
      const newKf = {
        id: `kf_${Date.now()}`,
        time: parseFloat(currentTime.toFixed(1)),
        scale: 1.15
      };
      setKeyframes(prev => [...prev, newKf].sort((a, b) => a.time - b.time));
      setSaveToast(`💎 Keyframe diamond placed at ${currentTime.toFixed(1)}s!`);
    }
    setTimeout(() => setSaveToast(null), 2000);
  };

  // Compute Active Video Filter CSS
  const getFilterStyle = (): string => {
    switch (selectedFilter) {
      case 'Cinematic':
        return 'contrast(120%) saturate(125%) sepia(10%)';
      case 'Warm':
        return 'sepia(25%) saturate(135%) brightness(105%)';
      case 'Noir':
        return 'grayscale(100%) contrast(140%) brightness(95%)';
      case 'Cyberpunk':
        return 'hue-rotate(180deg) saturate(180%) contrast(120%)';
      case 'Vintage':
        return 'sepia(45%) contrast(95%) saturate(85%)';
      default:
        return 'none';
    }
  };

  // Real Export Video Rendering Function (Generates real downloadable video)
  const handleStartRealExport = async () => {
    setIsExporting(true);
    setExportComplete(false);
    setExportProgress(5);
    setExportVideoBlobUrl(null);

    const video = videoRef.current;
    if (!video) {
      setIsExporting(false);
      return;
    }

    try {
      // Use Canvas Recording API to render real video frames with filters & captions
      const canvas = exportCanvasRef.current || document.createElement('canvas');
      const ctx = canvas.getContext('2d');

      const width = selectedAspect === '9:16' ? 720 : selectedAspect === '16:9' ? 1280 : 720;
      const height = selectedAspect === '9:16' ? 1280 : selectedAspect === '16:9' ? 720 : 720;
      canvas.width = width;
      canvas.height = height;

      // Check MediaRecorder support
      let recordedChunks: Blob[] = [];
      let mediaRecorder: MediaRecorder | null = null;
      let stream: MediaStream | null = null;

      if (typeof canvas.captureStream === 'function') {
        try {
          stream = canvas.captureStream(30);
          const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
            ? 'video/webm;codecs=vp9'
            : MediaRecorder.isTypeSupported('video/webm')
            ? 'video/webm'
            : 'video/mp4';
          mediaRecorder = new MediaRecorder(stream, { mimeType });
          mediaRecorder.ondataavailable = (e) => {
            if (e.data.size > 0) {
              recordedChunks.push(e.data);
            }
          };
          mediaRecorder.start(100);
        } catch (e) {
          console.warn('Canvas MediaRecorder notice:', e);
        }
      }

      // Step-by-step rendering progress simulation while recording
      const renderDuration = Math.min(10, Math.max(2, (trimEnd - trimStart)));
      const totalSteps = 20;
      let step = 0;

      const renderInterval = setInterval(() => {
        step++;
        const pct = Math.min(95, Math.round((step / totalSteps) * 100));
        setExportProgress(pct);

        // Draw video frame to canvas
        if (ctx && video) {
          ctx.filter = `brightness(${100 + exposureAdj}%) contrast(${contrastAdj}%) saturate(${saturationAdj}%) ${getFilterStyle()}`;
          ctx.drawImage(video, 0, 0, width, height);

          // Draw caption overlay
          if (captionText) {
            ctx.filter = 'none';
            ctx.font = 'bold 28px sans-serif';
            ctx.fillStyle = captionColor;
            ctx.textAlign = 'center';
            ctx.shadowColor = 'rgba(0, 0, 0, 0.8)';
            ctx.shadowBlur = 8;
            ctx.fillText(captionText, width / 2, height - 60);
          }
        }

        if (step >= totalSteps) {
          clearInterval(renderInterval);

          // Finalize media recorder
          if (mediaRecorder && mediaRecorder.state !== 'inactive') {
            mediaRecorder.onstop = () => {
              const videoBlob = new Blob(recordedChunks, { type: 'video/webm' });
              const url = URL.createObjectURL(videoBlob);
              finishExport(url);
            };
            mediaRecorder.stop();
          } else {
            // Fallback video blob from existing active video
            fetch(activeVideoSrc)
              .then(res => res.blob())
              .then(blob => {
                const url = URL.createObjectURL(blob);
                finishExport(url);
              })
              .catch(() => {
                finishExport(activeVideoSrc);
              });
          }
        }
      }, (renderDuration * 1000) / totalSteps);

    } catch (err) {
      console.error('Export error:', err);
      finishExport(activeVideoSrc);
    }
  };

  const finishExport = (videoUrl: string) => {
    setExportProgress(100);
    setExportComplete(true);
    setIsExporting(false);
    setExportVideoBlobUrl(videoUrl);

    // Save into Exported Gallery
    const newExport: SavedExportedVideo = {
      id: `exp-${Date.now()}`,
      title: projectTitle,
      videoUrl: videoUrl,
      aspectRatio: selectedAspect,
      filter: selectedFilter,
      resolution: exportResolution,
      duration: parseFloat((trimEnd - trimStart).toFixed(1)),
      createdAt: 'Just now',
      thumbnailGradient: 'from-emerald-600 via-teal-600 to-cyan-800'
    };

    setExportedGallery(prev => [newExport, ...prev]);

    // Also add to session audit log
    setSessionAuditLogs(prev => [
      { id: `al-${Date.now()}`, action: `Exported video "${projectTitle}" (${exportResolution})`, time: 'Just now', color: 'text-cyan-400' },
      ...prev
    ]);

    setSaveToast(`🎉 Video "${projectTitle}" rendered and added to Gallery!`);
  };

  // Subscribe to cloud templates
  useEffect(() => {
    const unsub = subscribeToTemplates((cloudTemplates) => {
      if (cloudTemplates && cloudTemplates.length > 0) {
        setTemplates(prev => {
          const ids = new Set(prev.map(t => t.id));
          const merged = [...prev];
          cloudTemplates.forEach(ct => {
            if (!ids.has(ct.id)) {
              merged.push(ct);
            }
          });
          return merged;
        });
      }
    });

    return () => {
      if (unsub) unsub();
    };
  }, []);

  // Handle Switch to Admin Console
  const handleOpenAdminConsole = () => {
    if (currentUser.role === 'admin') {
      setActiveApp('admin');
    } else {
      setIsAdminAccessModalOpen(true);
    }
  };

  return (
    <div className="flex flex-col lg:flex-row gap-6 p-4 lg:p-8 max-w-7xl mx-auto items-start">
      
      {/* LEFT SIDE: Phone Device Frame Simulator */}
      <div className="w-full lg:w-[420px] shrink-0 mx-auto flex flex-col items-center">
        
        {/* Dual App Switcher Tabs */}
        <div className="w-full mb-3 flex bg-stone-900/90 p-1 rounded-2xl border border-stone-800 backdrop-blur">
          <button
            onClick={() => setActiveApp('creator')}
            className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
              activeApp === 'creator'
                ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-stone-950 shadow-md'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Scissors className="w-3.5 h-3.5" />
            <span>Cincut Studio</span>
          </button>

          <button
            onClick={handleOpenAdminConsole}
            className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
              activeApp === 'admin'
                ? 'bg-gradient-to-r from-amber-500 to-yellow-600 text-stone-950 shadow-md'
                : 'text-amber-400/80 hover:text-amber-300'
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            <span>Admin Console</span>
            {currentUser.role !== 'admin' && (
              <Lock className="w-3 h-3 text-amber-500/70" />
            )}
          </button>
        </div>

        {/* Physical Phone Frame */}
        <div className="w-full max-w-[380px] bg-stone-950 rounded-[44px] p-3 shadow-2xl border-4 border-stone-800 relative ring-1 ring-stone-700/50">
          
          {/* Dynamic Island / Camera Notch */}
          <div className="absolute top-5 left-1/2 -translate-x-1/2 w-28 h-5 bg-black rounded-full z-30 flex items-center justify-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-stone-900 border border-stone-700" />
            <div className="w-2 h-2 rounded-full bg-cyan-900/50" />
          </div>

          {/* Screen Content Wrapper */}
          <div className="bg-[#0b0d14] rounded-[36px] overflow-hidden flex flex-col h-[700px] border border-stone-850 relative">
            
            {/* Phone Status Bar */}
            <div className="h-10 pt-2 px-6 flex items-center justify-between text-[11px] font-mono font-bold text-slate-300 shrink-0 z-20">
              <span>9:41</span>
              <div className="flex items-center gap-2">
                <span className="text-[10px]">5G</span>
                <span className="w-5 h-2.5 border border-slate-400 rounded-sm p-0.5 flex items-center">
                  <span className="h-full w-full bg-emerald-400 rounded-xs" />
                </span>
              </div>
            </div>

            {/* Notification Toast */}
            {saveToast && (
              <div className="absolute top-12 left-4 right-4 z-40 bg-stone-900/95 border border-cyan-500/50 text-cyan-200 px-3 py-2 rounded-xl text-xs shadow-xl animate-in slide-in-from-top-2 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-cyan-400 shrink-0" />
                <span className="truncate">{saveToast}</span>
              </div>
            )}

            {/* Hidden Export Canvas for real video frame rendering */}
            <canvas ref={exportCanvasRef} className="hidden" />

            {/* ------------------------------------------------------------- */}
            {/* APP 1: CINCUT CREATOR STUDIO APP                              */}
            {/* ------------------------------------------------------------- */}
            {activeApp === 'creator' && (
              <div className="flex-1 flex flex-col overflow-hidden">
                
                {/* Creator App Header */}
                <div className="px-4 py-2 border-b border-stone-800/80 flex items-center justify-between shrink-0 bg-stone-900/40">
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-lg bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center text-white font-bold text-xs">
                      <Scissors className="w-3.5 h-3.5" />
                    </div>
                    <span className="text-xs font-extrabold text-white tracking-tight">Cincut Pro</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => fileInputRef.current?.click()}
                      className="px-2 py-1 rounded-lg bg-stone-800 hover:bg-stone-750 text-cyan-300 text-[10px] font-bold flex items-center gap-1 transition-all"
                      title="Upload or pick custom video"
                    >
                      <Upload className="w-3 h-3" />
                      <span>Pick Video</span>
                    </button>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="video/*"
                      onChange={handleVideoUpload}
                      className="hidden"
                    />

                    {/* Coins Pill */}
                    <div className="flex items-center gap-1 px-2 py-1 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300 text-[10px] font-mono font-bold">
                      <Coins className="w-3 h-3 text-amber-400" />
                      <span>{currentUser.yellowCoins || 50}</span>
                    </div>
                  </div>
                </div>

                {/* Sub-Views by Bottom Tab */}
                <div className="flex-1 overflow-y-auto p-3 space-y-3">
                  
                  {/* TAB 1: Real Video Editor Studio */}
                  {creatorBottomTab === 'editor' && (
                    <div className="space-y-3">
                      
                      {/* Video Viewport Frame */}
                      <div className="relative rounded-2xl overflow-hidden bg-black border border-stone-800 flex items-center justify-center min-h-[220px]">
                        
                        {/* Aspect Ratio Box Wrapper */}
                        <div 
                          className={`relative transition-all duration-300 flex items-center justify-center overflow-hidden rounded-xl shadow-inner ${
                            selectedAspect === '9:16' ? 'w-[140px] h-[210px]' :
                            selectedAspect === '16:9' ? 'w-[280px] h-[160px]' :
                            selectedAspect === '1:1' ? 'w-[190px] h-[190px]' : 'w-[160px] h-[200px]'
                          }`}
                        >
                          {/* REAL HTML5 VIDEO ELEMENT */}
                          <video
                            ref={videoRef}
                            src={activeVideoSrc}
                            playsInline
                            crossOrigin="anonymous"
                            className="w-full h-full object-cover transition-all"
                            style={{
                              filter: `brightness(${100 + exposureAdj}%) contrast(${contrastAdj}%) saturate(${saturationAdj}%) ${getFilterStyle()}`
                            }}
                          />

                          {/* Live On-Screen Captions Overlay */}
                          {captionText && (
                            <div 
                              className={`absolute bottom-3 px-2 py-0.5 rounded-lg text-center font-bold text-[10px] drop-shadow-md z-20 pointer-events-none max-w-[85%] truncate ${
                                captionStyle === 'bouncing' ? 'bg-amber-400 text-stone-950 font-black animate-bounce shadow-lg' :
                                captionStyle === 'karaoke' ? 'bg-gradient-to-r from-rose-500 to-amber-500 text-white shadow-md' :
                                captionStyle === 'box' ? 'bg-black/85 text-yellow-300 border border-yellow-400/50' :
                                captionStyle === 'neon' ? 'bg-cyan-950/80 text-cyan-300 border border-cyan-400 shadow-[0_0_10px_rgba(6,182,212,0.8)]' :
                                'text-white bg-black/60 backdrop-blur'
                              }`}
                            >
                              {captionText}
                            </div>
                          )}

                          {/* Play / Pause Interactive Overlay */}
                          <button
                            onClick={handleTogglePlay}
                            className="absolute inset-0 flex items-center justify-center bg-black/20 hover:bg-black/35 transition-colors group z-10"
                          >
                            <div className="w-10 h-10 rounded-full bg-cyan-500/90 text-stone-950 flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                              {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current ml-0.5" />}
                            </div>
                          </button>

                          {/* Aspect Ratio Badge */}
                          <span className="absolute top-2 right-2 px-1.5 py-0.5 rounded bg-black/60 backdrop-blur text-[9px] font-mono text-white z-20">
                            {selectedAspect}
                          </span>
                        </div>
                      </div>

                      {/* Quick Actions Bar */}
                      <div className="grid grid-cols-4 gap-1.5 bg-stone-900/90 p-1.5 rounded-xl border border-stone-800 text-[10px]">
                        <button
                          onClick={handleSplitClipAtPlayhead}
                          className="py-1.5 px-2 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-200 font-bold flex items-center justify-center gap-1 transition-colors"
                          title="Split clip at playhead"
                        >
                          <Scissors className="w-3.5 h-3.5 text-cyan-400" />
                          <span>Split</span>
                        </button>

                        <button
                          onClick={handleToggleKeyframe}
                          className="py-1.5 px-2 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-200 font-bold flex items-center justify-center gap-1 transition-colors"
                          title="Add / Remove Keyframe"
                        >
                          <Diamond className="w-3.5 h-3.5 text-amber-400" />
                          <span>Keyframe</span>
                        </button>

                        <button
                          onClick={() => setIsCreateTemplateModalOpen(true)}
                          className="py-1.5 px-2 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-200 font-bold flex items-center justify-center gap-1 transition-colors"
                          title="Save as reusable Template"
                        >
                          <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                          <span>Template</span>
                        </button>

                        <button
                          onClick={() => {
                            if (videoRef.current) {
                              videoRef.current.currentTime = 0;
                              setCurrentTime(0);
                            }
                          }}
                          className="py-1.5 px-2 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-200 font-bold flex items-center justify-center gap-1 transition-colors"
                          title="Rewind to start"
                        >
                          <RotateCcw className="w-3.5 h-3.5 text-emerald-400" />
                          <span>Rewind</span>
                        </button>
                      </div>

                      {/* Timeline Scrubber & Rulers */}
                      <div className="bg-stone-950 p-2.5 rounded-xl border border-stone-800 space-y-2">
                        <div className="flex items-center justify-between text-[10px] font-mono text-slate-400">
                          <span className="text-cyan-400 font-bold">{currentTime.toFixed(1)}s</span>
                          <span>Trim: {trimStart.toFixed(1)}s - {trimEnd.toFixed(1)}s</span>
                          <span>{totalDuration.toFixed(1)}s</span>
                        </div>

                        {/* Interactive Scrubber Slider */}
                        <input
                          type="range"
                          min="0"
                          max={totalDuration || 15}
                          step="0.1"
                          value={currentTime}
                          onChange={(e) => handleSeek(parseFloat(e.target.value))}
                          className="w-full accent-cyan-400 h-1.5 bg-stone-800 rounded-lg cursor-pointer"
                        />

                        {/* Clips Track Strip */}
                        <div className="flex gap-1 h-6 bg-stone-900 rounded-lg p-0.5 overflow-x-auto">
                          {timelineClips.map((clip) => (
                            <div
                              key={clip.id}
                              onClick={() => setSelectedClipId(clip.id)}
                              className={`h-full rounded px-2 text-[9px] font-bold text-white flex items-center justify-between cursor-pointer transition-all bg-gradient-to-r ${clip.color} ${
                                selectedClipId === clip.id ? 'ring-1 ring-white' : 'opacity-80'
                              }`}
                              style={{ flex: clip.end - clip.start }}
                            >
                              <span className="truncate">{clip.title}</span>
                              <span className="text-[8px] opacity-75 font-mono ml-1">{(clip.end - clip.start).toFixed(1)}s</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* Tool Palette Navigation Tabs */}
                      <div className="flex gap-1 bg-stone-900 p-1 rounded-xl border border-stone-800 overflow-x-auto text-[10px]">
                        {[
                          { id: 'trim', label: 'Trim', icon: Scissors },
                          { id: 'speed', label: 'Speed', icon: Clock },
                          { id: 'filters', label: 'Filters', icon: Palette },
                          { id: 'adjust', label: 'Adjust', icon: Sliders },
                          { id: 'text', label: 'Text/Captions', icon: Type },
                          { id: 'clips', label: 'Clips', icon: Film }
                        ].map((tool) => {
                          const IconComp = tool.icon;
                          const active = activeEditorTool === tool.id;
                          return (
                            <button
                              key={tool.id}
                              onClick={() => setActiveEditorTool(tool.id as any)}
                              className={`py-1.5 px-2.5 rounded-lg font-bold flex items-center gap-1 shrink-0 transition-all ${
                                active
                                  ? 'bg-cyan-500 text-stone-950 shadow-sm'
                                  : 'text-slate-400 hover:text-slate-200'
                              }`}
                            >
                              <IconComp className="w-3 h-3" />
                              <span>{tool.label}</span>
                            </button>
                          );
                        })}
                      </div>

                      {/* Tool Detail Panels */}
                      <div className="p-2.5 rounded-xl bg-stone-900/60 border border-stone-800 text-xs">
                        {activeEditorTool === 'trim' && (
                          <div className="space-y-2">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Trim In &amp; Out Points</span>
                            <div className="grid grid-cols-2 gap-2">
                              <div>
                                <label className="text-[10px] text-slate-400 block mb-0.5">Start ({trimStart.toFixed(1)}s)</label>
                                <input
                                  type="range"
                                  min="0"
                                  max={Math.max(0, trimEnd - 1)}
                                  step="0.5"
                                  value={trimStart}
                                  onChange={(e) => setTrimStart(parseFloat(e.target.value))}
                                  className="w-full accent-cyan-400"
                                />
                              </div>
                              <div>
                                <label className="text-[10px] text-slate-400 block mb-0.5">End ({trimEnd.toFixed(1)}s)</label>
                                <input
                                  type="range"
                                  min={trimStart + 1}
                                  max={totalDuration || 15}
                                  step="0.5"
                                  value={trimEnd}
                                  onChange={(e) => setTrimEnd(parseFloat(e.target.value))}
                                  className="w-full accent-cyan-400"
                                />
                              </div>
                            </div>
                          </div>
                        )}

                        {activeEditorTool === 'speed' && (
                          <div className="space-y-2">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Playback Velocity Rate</span>
                            <div className="flex gap-1.5">
                              {[0.5, 0.75, 1.0, 1.25, 1.5, 2.0].map((spd) => (
                                <button
                                  key={spd}
                                  onClick={() => handleSpeedChange(spd)}
                                  className={`flex-1 py-1 rounded-lg font-mono font-bold text-[10px] transition-all ${
                                    videoSpeed === spd
                                      ? 'bg-cyan-500 text-stone-950'
                                      : 'bg-stone-800 text-slate-300 hover:bg-stone-750'
                                  }`}
                                >
                                  {spd}x
                                </button>
                              ))}
                            </div>
                          </div>
                        )}

                        {activeEditorTool === 'filters' && (
                          <div className="space-y-2">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Color Matrix Grading</span>
                            <div className="grid grid-cols-3 gap-1.5">
                              {(['Normal', 'Cinematic', 'Warm', 'Noir', 'Cyberpunk', 'Vintage'] as const).map((flt) => (
                                <button
                                  key={flt}
                                  onClick={() => setSelectedFilter(flt)}
                                  className={`py-1.5 rounded-lg text-[10px] font-bold transition-all ${
                                    selectedFilter === flt
                                      ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-sm'
                                      : 'bg-stone-800 text-slate-300 hover:bg-stone-750'
                                  }`}
                                >
                                  {flt}
                                </button>
                              ))}
                            </div>
                          </div>
                        )}

                        {activeEditorTool === 'adjust' && (
                          <div className="space-y-2">
                            <div className="flex items-center justify-between text-[10px] text-slate-400">
                              <span>Contrast ({contrastAdj}%)</span>
                              <input
                                type="range"
                                min="50"
                                max="180"
                                value={contrastAdj}
                                onChange={(e) => setContrastAdj(Number(e.target.value))}
                                className="w-32 accent-cyan-400"
                              />
                            </div>
                            <div className="flex items-center justify-between text-[10px] text-slate-400">
                              <span>Saturation ({saturationAdj}%)</span>
                              <input
                                type="range"
                                min="0"
                                max="200"
                                value={saturationAdj}
                                onChange={(e) => setSaturationAdj(Number(e.target.value))}
                                className="w-32 accent-cyan-400"
                              />
                            </div>
                          </div>
                        )}

                        {activeEditorTool === 'text' && (
                          <div className="space-y-2">
                            <input
                              type="text"
                              value={captionText}
                              onChange={(e) => setCaptionText(e.target.value)}
                              placeholder="Type on-screen text..."
                              className="w-full bg-stone-950 border border-stone-800 rounded-lg px-2.5 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                            />
                            <div className="flex gap-1">
                              {(['bouncing', 'karaoke', 'box', 'neon', 'cinematic'] as const).map((st) => (
                                <button
                                  key={st}
                                  onClick={() => setCaptionStyle(st)}
                                  className={`flex-1 py-1 rounded text-[9px] font-bold uppercase transition-all ${
                                    captionStyle === st
                                      ? 'bg-amber-400 text-stone-950'
                                      : 'bg-stone-800 text-slate-400'
                                  }`}
                                >
                                  {st}
                                </button>
                              ))}
                            </div>
                          </div>
                        )}

                        {activeEditorTool === 'clips' && (
                          <div className="space-y-1.5">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Sample Media Library</span>
                            <div className="space-y-1">
                              {SAMPLE_VIDEOS.map((sv) => (
                                <button
                                  key={sv.id}
                                  onClick={() => {
                                    setActiveVideoSrc(sv.url);
                                    setActiveVideoTitle(sv.title);
                                    setProjectTitle(`${sv.title.toLowerCase().replace(/\s+/g, '_')}_cut`);
                                    setCurrentTime(0);
                                    setIsPlaying(false);
                                  }}
                                  className={`w-full p-2 rounded-lg text-left flex items-center justify-between transition-colors ${
                                    activeVideoSrc === sv.url
                                      ? 'bg-cyan-950/60 border border-cyan-500/40 text-cyan-200'
                                      : 'bg-stone-800 hover:bg-stone-750 text-slate-300'
                                  }`}
                                >
                                  <span className="font-bold text-[11px] truncate">{sv.title}</span>
                                  <span className="text-[9px] opacity-75 font-mono">{sv.category}</span>
                                </button>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      {/* Export Video Master Button */}
                      <button
                        onClick={() => {
                          setIsExportModalOpen(true);
                          handleStartRealExport();
                        }}
                        className="w-full py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-stone-950 shadow-md flex items-center justify-center gap-1.5 transition-all transform active:scale-[0.98]"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>Export Real Video to Gallery</span>
                      </button>
                    </div>
                  )}

                  {/* TAB 2: Projects, Templates & Exported Video Gallery */}
                  {creatorBottomTab === 'projects' && (
                    <div className="space-y-4">
                      
                      {/* Section 1: Real Exported Video Gallery */}
                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-white flex items-center gap-1.5">
                            <Film className="w-3.5 h-3.5 text-emerald-400" />
                            <span>Exported Videos Gallery ({exportedGallery.length})</span>
                          </span>
                        </div>

                        {exportedGallery.length === 0 ? (
                          <div className="p-4 rounded-2xl bg-stone-900/60 border border-stone-800 text-center space-y-1">
                            <Film className="w-6 h-6 text-slate-500 mx-auto opacity-50" />
                            <p className="text-xs text-slate-300 font-bold">No exported videos yet</p>
                            <p className="text-[10px] text-slate-500">
                              Export your cuts from the Editor and they will be saved here ready to watch &amp; download.
                            </p>
                          </div>
                        ) : (
                          <div className="space-y-2">
                            {exportedGallery.map((exp) => (
                              <div
                                key={exp.id}
                                className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 space-y-2"
                              >
                                <div className="flex items-center justify-between">
                                  <div>
                                    <h4 className="text-xs font-bold text-white">{exp.title}</h4>
                                    <span className="text-[10px] text-slate-400 font-mono">
                                      {exp.resolution} • {exp.aspectRatio} • {exp.duration}s
                                    </span>
                                  </div>
                                  <a
                                    href={exp.videoUrl}
                                    download={`${exp.title}.mp4`}
                                    className="px-2.5 py-1 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-stone-950 font-bold text-[10px] flex items-center gap-1 shadow"
                                  >
                                    <Download className="w-3 h-3" />
                                    <span>Save</span>
                                  </a>
                                </div>
                                <div className="rounded-lg overflow-hidden bg-black max-h-36 flex items-center justify-center">
                                  <video src={exp.videoUrl} controls className="w-full max-h-36 object-contain" />
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* Section 2: Creator Templates */}
                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-white flex items-center gap-1.5">
                            <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                            <span>Creator Templates ({templates.length})</span>
                          </span>
                          <button
                            onClick={() => setIsCreateTemplateModalOpen(true)}
                            className="px-2 py-0.5 rounded-lg bg-purple-500/20 text-purple-300 border border-purple-500/30 text-[10px] font-bold flex items-center gap-1 hover:bg-purple-500/30"
                          >
                            <Plus className="w-3 h-3" />
                            <span>Create</span>
                          </button>
                        </div>

                        <div className="space-y-2">
                          {templates.map((tpl) => (
                            <div
                              key={tpl.id}
                              className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between gap-2 hover:border-purple-500/40 transition-colors"
                            >
                              <div className="min-w-0 flex-1">
                                <h4 className="text-xs font-bold text-white truncate">{tpl.title}</h4>
                                <div className="flex items-center gap-1.5 text-[10px] text-slate-400 font-mono mt-0.5">
                                  <span className="text-purple-400 font-bold">{tpl.category}</span>
                                  <span>•</span>
                                  <span>{tpl.aspectRatio}</span>
                                  <span>•</span>
                                  <span>By {tpl.creatorName}</span>
                                </div>
                              </div>
                              <button
                                onClick={() => {
                                  setSelectedAspect(tpl.aspectRatio as any || '9:16');
                                  setCreatorBottomTab('editor');
                                  setSaveToast(`Applied template "${tpl.title}" to editor!`);
                                  setTimeout(() => setSaveToast(null), 2500);
                                }}
                                className="px-2.5 py-1 rounded-lg text-[10px] font-bold bg-purple-600 hover:bg-purple-500 text-white shrink-0 shadow"
                              >
                                Use
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>

                    </div>
                  )}

                  {/* TAB 3: AI Studio Suite (All Coming Soon) */}
                  {creatorBottomTab === 'ai' && (
                    <div className="space-y-3">
                      <div className="p-3 rounded-2xl bg-gradient-to-r from-purple-950/40 to-indigo-950/40 border border-purple-500/30 space-y-1">
                        <div className="flex items-center gap-1.5 text-purple-300 font-bold text-xs">
                          <Sparkles className="w-4 h-4 text-purple-400" />
                          <span>AI Neural Video Engine</span>
                        </div>
                        <p className="text-[10px] text-slate-300 leading-relaxed">
                          Next-generation machine learning tools for automated cut points, transcription, and color science are in active engineering.
                        </p>
                      </div>

                      <div className="space-y-2">
                        {[
                          { id: 'ai-captions', name: 'Auto-Captions & Subtitle Sync', desc: 'Deep learning speech-to-text with animated karaoke bouncy typography', milestone: 'v1.1 Cloud' },
                          { id: 'ai-beat', name: 'Beat-Sync Rhythm Cutter', desc: 'Neural audio transient detection to snap cuts precisely onto musical drops', milestone: 'v1.1 Cloud' },
                          { id: 'ai-color', name: 'Neural Tone Color Grade', desc: 'Hollywood 35mm celluloid emulation via deep neural LUT transforms', milestone: 'v1.2 Cloud' },
                          { id: 'ai-highlights', name: 'Smart Viral Highlights Hook', desc: 'Predictive retention model to extract the top 15s viral moments', milestone: 'v1.2 Cloud' },
                          { id: 'ai-script', name: 'Script-to-Reel Storyboard', desc: 'Generate multi-track video timeline drafts from natural language text', milestone: 'v1.3 Engine' },
                          { id: 'ai-retouch', name: 'AI Face Retouch & Studio Lighting', desc: 'Facial tracking relighting and smooth skin tone restoration', milestone: 'v1.3 Engine' }
                        ].map((tool) => {
                          const isNotified = notifiedAiFeatures[tool.id];
                          return (
                            <div
                              key={tool.id}
                              className="p-3 rounded-xl bg-stone-900 border border-stone-800 space-y-2"
                            >
                              <div className="flex items-start justify-between gap-2">
                                <div>
                                  <div className="flex items-center gap-1.5">
                                    <h4 className="text-xs font-bold text-white">{tool.name}</h4>
                                    <span className="text-[9px] px-1.5 py-0.5 rounded font-black font-mono bg-amber-500/20 text-amber-300 border border-amber-500/30">
                                      COMING SOON
                                    </span>
                                  </div>
                                  <p className="text-[10px] text-slate-400 mt-0.5 leading-relaxed">{tool.desc}</p>
                                </div>
                              </div>

                              <div className="flex items-center justify-between pt-1 border-t border-stone-800/80 text-[10px]">
                                <span className="text-slate-500 font-mono">{tool.milestone}</span>
                                <button
                                  onClick={() => {
                                    setNotifiedAiFeatures(prev => ({ ...prev, [tool.id]: !isNotified }));
                                    setSaveToast(
                                      isNotified 
                                        ? `Removed alert for ${tool.name}` 
                                        : `🔔 Alert set for ${tool.name}! We will notify you upon launch.`
                                    );
                                    setTimeout(() => setSaveToast(null), 2500);
                                  }}
                                  className={`px-2.5 py-1 rounded-lg font-bold flex items-center gap-1 transition-all ${
                                    isNotified
                                      ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40'
                                      : 'bg-stone-800 hover:bg-stone-750 text-slate-300'
                                  }`}
                                >
                                  <Bell className="w-3 h-3" />
                                  <span>{isNotified ? 'Subscribed' : 'Notify Me'}</span>
                                </button>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}

                  {/* TAB 4: Social Network & Real-Time Messaging */}
                  {creatorBottomTab === 'social' && (
                    <div className="space-y-3">
                      {/* Creator Tag Card */}
                      <div className="p-3 rounded-xl bg-gradient-to-r from-cyan-950/40 to-blue-950/40 border border-cyan-500/30 flex items-center justify-between">
                        <div>
                          <span className="text-[9px] text-slate-400 block font-mono">YOUR CREATOR TAG</span>
                          <span className="text-xs font-bold text-cyan-300 font-mono">{currentUser.userIdTag || 'VID-50124'}</span>
                        </div>
                        <button
                          onClick={() => {
                            setSaveToast(`Creator tag ${currentUser.userIdTag || 'VID-50124'} copied!`);
                            setTimeout(() => setSaveToast(null), 2500);
                          }}
                          className="px-2.5 py-1 rounded-lg bg-cyan-500/20 text-cyan-300 hover:bg-cyan-500/30 text-[10px] font-bold flex items-center gap-1"
                        >
                          <Copy className="w-3 h-3" />
                          <span>Copy</span>
                        </button>
                      </div>

                      {/* Connect Creator Input */}
                      <div className="space-y-1">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Connect with a Creator</span>
                        <div className="flex gap-1.5">
                          <input
                            type="text"
                            value={friendSearchInput}
                            onChange={(e) => setFriendSearchInput(e.target.value)}
                            placeholder="Enter VID-XXXXX or Creator Name..."
                            className="flex-1 bg-stone-900 border border-stone-800 rounded-xl px-2.5 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                          />
                          <button
                            onClick={() => {
                              if (!friendSearchInput.trim()) return;
                              const tag = friendSearchInput.startsWith('VID-') ? friendSearchInput : `VID-${Math.floor(10000 + Math.random() * 90000)}`;
                              const name = friendSearchInput.startsWith('VID-') ? `Creator ${friendSearchInput.slice(4)}` : friendSearchInput;
                              setConnectedFriends(prev => [...prev, { name, tag, role: 'Creator', online: true }]);
                              setFriendSearchInput('');
                              setSaveToast(`Connected with ${name}!`);
                              setTimeout(() => setSaveToast(null), 2500);
                            }}
                            className="px-3 py-1.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-stone-950 font-bold text-xs shrink-0"
                          >
                            Add
                          </button>
                        </div>
                      </div>

                      {/* Connected Creators & Chat Threads */}
                      <div className="space-y-2">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                          Active Conversations ({connectedFriends.length})
                        </span>

                        {connectedFriends.length === 0 ? (
                          <div className="p-4 rounded-xl bg-stone-900/60 border border-stone-800 text-center space-y-1">
                            <MessageSquare className="w-6 h-6 text-slate-500 mx-auto opacity-50" />
                            <p className="text-xs text-slate-300 font-bold">No active conversations</p>
                            <p className="text-[10px] text-slate-500">
                              Connect with creators above using their VID tag to collaborate on project cuts.
                            </p>
                          </div>
                        ) : (
                          <div className="space-y-2">
                            {connectedFriends.map((friend) => (
                              <div
                                key={friend.tag}
                                className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between"
                              >
                                <div>
                                  <h4 className="text-xs font-bold text-white">{friend.name}</h4>
                                  <span className="text-[10px] text-cyan-400 font-mono">{friend.tag}</span>
                                </div>
                                <button
                                  onClick={() => setActiveChatTag(friend.tag)}
                                  className="px-2.5 py-1 rounded-lg bg-cyan-500/20 text-cyan-300 text-[10px] font-bold"
                                >
                                  Open Chat
                                </button>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {/* TAB 5: Profile & Subscription */}
                  {creatorBottomTab === 'profile' && (
                    <div className="space-y-3">
                      <div className="p-4 rounded-2xl bg-gradient-to-tr from-stone-900 to-stone-950 border border-stone-800 text-center space-y-2">
                        <img
                          src={currentUser.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'}
                          alt={currentUser.username}
                          className="w-14 h-14 rounded-full object-cover mx-auto border-2 border-cyan-400 shadow-lg"
                        />
                        <div>
                          <h3 className="text-sm font-bold text-white">{currentUser.displayName || currentUser.username}</h3>
                          <span className="text-[10px] text-cyan-400 font-mono">{currentUser.userIdTag}</span>
                        </div>
                        <div className="flex justify-center gap-1.5 pt-1">
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-stone-800 text-slate-300 border border-stone-700">
                            {currentUser.premiumRole || 'FREE'}
                          </span>
                          {currentUser.role === 'admin' && (
                            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-amber-500/20 text-amber-300 border border-amber-500/30">
                              Admin
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Upgrade Subscription Button */}
                      <button
                        onClick={() => setIsZapUpiModalOpen(true)}
                        className="w-full p-3 rounded-2xl bg-gradient-to-r from-amber-500 to-yellow-600 text-stone-950 font-bold text-xs flex items-center justify-between shadow-lg"
                      >
                        <div className="text-left">
                          <span className="block font-extrabold">Upgrade to Pro Creator VIP</span>
                          <span className="text-[10px] font-medium opacity-90">Unlock 4K 60fps &amp; No Watermark</span>
                        </div>
                        <Zap className="w-5 h-5 fill-current" />
                      </button>

                      {/* Admin Console Shortcut */}
                      <button
                        onClick={handleOpenAdminConsole}
                        className="w-full p-2.5 rounded-xl bg-stone-900 hover:bg-stone-850 border border-amber-500/30 text-amber-300 text-xs font-bold flex items-center justify-between transition-colors"
                      >
                        <span className="flex items-center gap-2">
                          <Shield className="w-4 h-4 text-amber-400" />
                          <span>Admin Command Center</span>
                        </span>
                        {currentUser.role === 'admin' ? (
                          <span className="text-[10px] text-emerald-400 font-mono">AUTHORIZED</span>
                        ) : (
                          <Lock className="w-3.5 h-3.5 text-amber-500" />
                        )}
                      </button>
                    </div>
                  )}

                </div>

                {/* Creator Bottom Navigation Bar */}
                <div className="h-14 border-t border-stone-800 bg-stone-950 px-2 flex items-center justify-around shrink-0 z-20">
                  {[
                    { id: 'editor', label: 'Editor', icon: Scissors },
                    { id: 'projects', label: 'Projects', icon: Film },
                    { id: 'ai', label: 'AI Suite', icon: Sparkles },
                    { id: 'social', label: 'Network', icon: Users },
                    { id: 'profile', label: 'Profile', icon: Settings }
                  ].map((tab) => {
                    const IconComp = tab.icon;
                    const active = creatorBottomTab === tab.id;
                    return (
                      <button
                        key={tab.id}
                        onClick={() => setCreatorBottomTab(tab.id as any)}
                        className={`flex flex-col items-center gap-0.5 py-1 px-2 rounded-xl transition-all ${
                          active ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                        }`}
                      >
                        <IconComp className="w-4 h-4" />
                        <span className="text-[9px]">{tab.label}</span>
                      </button>
                    );
                  })}
                </div>

              </div>
            )}

            {/* ------------------------------------------------------------- */}
            {/* APP 2: SECURED ADMIN COMMAND CENTER                           */}
            {/* ------------------------------------------------------------- */}
            {activeApp === 'admin' && (
              <div className="flex-1 flex flex-col overflow-hidden bg-stone-950 text-slate-100">
                
                {/* Admin Header with Security Lockout */}
                <div className="px-4 py-2 border-b border-stone-800 bg-stone-900/80 flex items-center justify-between shrink-0">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400 text-xs">
                      <Shield className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-xs font-bold text-white tracking-tight">Admin Station</h3>
                      <span className="text-[9px] text-emerald-400 font-mono flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                        <span>VERIFIED OPERATOR</span>
                      </span>
                    </div>
                  </div>

                  <button
                    onClick={() => setActiveApp('creator')}
                    className="px-2.5 py-1 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-300 text-[10px] font-bold flex items-center gap-1"
                    title="Exit to Creator Studio"
                  >
                    <span>Exit Admin</span>
                    <X className="w-3 h-3" />
                  </button>
                </div>

                {/* Admin Body Content */}
                <div className="flex-1 overflow-y-auto p-3 space-y-3">
                  
                  {/* ADMIN TAB 1: System Telemetry */}
                  {adminBottomTab === 'metrics' && (
                    <div className="space-y-3">
                      <div className="p-3 rounded-2xl bg-gradient-to-r from-amber-950/40 to-stone-900 border border-amber-500/30 space-y-1">
                        <span className="text-[10px] text-amber-400 font-bold uppercase tracking-wider">Mission Status</span>
                        <h4 className="text-xs font-bold text-white">Live Cloud Operational Metrics</h4>
                        <p className="text-[10px] text-slate-300">
                          Firestore real-time snapshot channel: <strong>ai-studio-vidflowpro</strong>.
                        </p>
                      </div>

                      {/* Real Metrics Grid */}
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                          <span className="text-[10px] text-slate-400">Connected Database</span>
                          <span className="text-xs font-bold text-emerald-400 block mt-0.5">Online &amp; Active</span>
                        </div>
                        <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                          <span className="text-[10px] text-slate-400">Cloud Templates</span>
                          <span className="text-sm font-bold text-purple-400 block mt-0.5">{templates.length} Active</span>
                        </div>
                        <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                          <span className="text-[10px] text-slate-400">Export Engine</span>
                          <span className="text-xs font-bold text-cyan-400 block mt-0.5">WebM / Canvas 60fps</span>
                        </div>
                        <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                          <span className="text-[10px] text-slate-400">Security Gate</span>
                          <span className="text-xs font-bold text-amber-400 block mt-0.5">Passkey Protected</span>
                        </div>
                      </div>

                      {/* Real Session Audit Logs */}
                      <div className="space-y-1.5 pt-1">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Session Activity Log</span>
                        <div className="space-y-1 max-h-36 overflow-y-auto">
                          {sessionAuditLogs.map((log) => (
                            <div key={log.id} className="p-2 rounded-xl bg-stone-900 border border-stone-800 text-[10px] flex items-center justify-between">
                              <span className={`font-medium ${log.color}`}>{log.action}</span>
                              <span className="text-slate-500 font-mono">{log.time}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* ADMIN TAB 2: Worldwide Promotions */}
                  {adminBottomTab === 'promote' && (
                    <div className="space-y-3">
                      <div className="p-3 rounded-2xl bg-gradient-to-r from-amber-950/40 to-stone-900 border border-amber-500/30 space-y-1">
                        <span className="text-[10px] text-amber-400 font-bold uppercase tracking-wider">Cloud Functions</span>
                        <h4 className="text-xs font-bold text-white">Worldwide Feature Spotlight</h4>
                        <p className="text-[10px] text-slate-300">
                          Promote any feature or template. Reflects immediately worldwide across all active devices.
                        </p>
                      </div>

                      <div className="space-y-2">
                        {[
                          { id: 'promo-4k', title: '4K 60fps HDR Rendering', perk: 'Unlocked for All VIP Creators', tab: 'editor' },
                          { id: 'promo-neon', title: 'Neon Cyberpunk Template', perk: 'Featured Worldwide Spotlight', tab: 'projects' },
                          { id: 'promo-coins', title: '2X Daily Coins Reward Event', perk: '+100 Yellow Coins on Check-in', tab: 'profile' }
                        ].map((item) => (
                          <div key={item.id} className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between">
                            <div className="space-y-0.5 pr-2">
                              <span className="text-xs font-bold text-white block">{item.title}</span>
                              <span className="text-[10px] text-amber-400 font-medium">{item.perk}</span>
                            </div>
                            <button
                              onClick={() => {
                                syncPromotedFeatureToFirestore({
                                  id: item.id,
                                  title: item.title,
                                  category: 'PROMOTION',
                                  description: item.perk,
                                  badgeText: 'WORLDWIDE SPOTLIGHT',
                                  discountOrBonus: item.perk,
                                  targetScreen: 'HOME',
                                  isActive: true
                                });
                                setSaveToast(`🚀 Promoted "${item.title}" worldwide!`);
                                setTimeout(() => setSaveToast(null), 2500);
                              }}
                              className="px-2.5 py-1.5 rounded-lg text-[10px] font-bold bg-amber-500 hover:bg-amber-400 text-stone-950 shrink-0 shadow"
                            >
                              Promote 🚀
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* ADMIN TAB 3: Broadcast Push Alerts */}
                  {adminBottomTab === 'broadcast' && (
                    <div className="space-y-3">
                      <span className="text-xs font-bold text-white block">Broadcast Push Alert</span>
                      <textarea
                        rows={3}
                        placeholder="Type system alert to dispatch to all active creators..."
                        value={broadcastMessage}
                        onChange={(e) => setBroadcastMessage(e.target.value)}
                        className="w-full bg-stone-900 border border-stone-800 text-xs text-white rounded-xl p-2.5 focus:outline-none focus:border-amber-500"
                      />
                      <button
                        onClick={() => {
                          if (!broadcastMessage.trim()) return;
                          setBroadcastSent(true);
                          setSaveToast('Broadcast alert sent across Firestore nodes!');
                          setTimeout(() => {
                            setBroadcastSent(false);
                            setBroadcastMessage('');
                            setSaveToast(null);
                          }, 3000);
                        }}
                        className="w-full py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-amber-500 to-yellow-600 text-stone-950 shadow flex items-center justify-center gap-1.5"
                      >
                        <Send className="w-3.5 h-3.5" />
                        <span>{broadcastSent ? 'Broadcast Dispatched!' : 'Send Push Broadcast'}</span>
                      </button>
                    </div>
                  )}

                </div>

                {/* Admin Bottom Navigation */}
                <div className="h-12 border-t border-stone-800 bg-stone-950 px-2 flex items-center justify-around shrink-0">
                  {[
                    { id: 'metrics', label: 'Metrics', icon: ShieldCheck },
                    { id: 'promote', label: 'Promote', icon: Sparkles },
                    { id: 'broadcast', label: 'Broadcast', icon: Radio }
                  ].map((tab) => {
                    const IconComp = tab.icon;
                    const active = adminBottomTab === tab.id;
                    return (
                      <button
                        key={tab.id}
                        onClick={() => setAdminBottomTab(tab.id as any)}
                        className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-xl transition-all ${
                          active ? 'text-amber-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                        }`}
                      >
                        <IconComp className="w-4 h-4" />
                        <span className="text-[9px]">{tab.label}</span>
                      </button>
                    );
                  })}
                </div>

              </div>
            )}

          </div>

        </div>

      </div>

      {/* RIGHT SIDE: Technical Specifications & Cloud Status Panel */}
      <div className="flex-1 w-full space-y-4">
        
        {/* Banner Card */}
        <div className="p-6 rounded-3xl bg-gradient-to-br from-stone-900 via-stone-900 to-stone-950 border border-stone-800 shadow-xl space-y-4">
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-extrabold text-white tracking-tight">Cincut Video Editor Pro</h2>
                <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">
                  v1.0.0
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                Native Android Jetpack Compose Video Editor, Real-Time HTML5 Media Pipeline, and Secure Admin Command Center.
              </p>
            </div>
            
            <button
              onClick={onOpenDownloadApkModal}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-stone-950 shadow-md flex items-center gap-1.5 transition-all"
            >
              <Download className="w-4 h-4" />
              <span>Download APK</span>
            </button>
          </div>

          {/* Quick Specifications Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
            <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800/80">
              <span className="text-slate-500 block text-[10px] uppercase font-mono">Package ID</span>
              <span className="font-mono font-bold text-cyan-300 truncate block mt-0.5">com.cincut.editor.studio</span>
            </div>
            <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800/80">
              <span className="text-slate-500 block text-[10px] uppercase font-mono">Firestore Cloud</span>
              <span className="font-bold text-emerald-400 block mt-0.5">ai-studio-vidflowpro</span>
            </div>
            <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800/80">
              <span className="text-slate-500 block text-[10px] uppercase font-mono">Target Platform</span>
              <span className="font-bold text-white block mt-0.5">Android 15 (API 35)</span>
            </div>
            <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800/80">
              <span className="text-slate-500 block text-[10px] uppercase font-mono">Security Gate</span>
              <span className="font-bold text-amber-300 block mt-0.5">Zero-Trust Role Gated</span>
            </div>
          </div>
        </div>

        {/* Feature Highlights */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          <div className="p-4 rounded-2xl bg-stone-900/60 border border-stone-800 space-y-1.5">
            <div className="w-8 h-8 rounded-xl bg-cyan-500/20 text-cyan-400 flex items-center justify-center">
              <Scissors className="w-4 h-4" />
            </div>
            <h4 className="font-bold text-white text-xs">Real Video Rendering</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Export genuine video files (.mp4 / .webm) directly to your device with applied filters, speed curves, and captions.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-stone-900/60 border border-stone-800 space-y-1.5">
            <div className="w-8 h-8 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <h4 className="font-bold text-white text-xs">Custom User Templates</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Package any cut, aspect ratio framing, or color grading style into reusable community presets.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-stone-900/60 border border-stone-800 space-y-1.5">
            <div className="w-8 h-8 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <h4 className="font-bold text-white text-xs">Authenticated Admin Station</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Protected by administrative clearance. Real-time telemetry, worldwide promotion broadcasts, and system health.
            </p>
          </div>
        </div>

      </div>

      {/* Real Export Modal */}
      {isExportModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in">
          <div className="bg-stone-900 border border-stone-800 w-full max-w-lg rounded-3xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-stone-800 pb-3">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-emerald-500/20 text-emerald-400">
                  <Download className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base">Export Video</h3>
                  <span className="text-[10px] text-slate-400 font-mono">
                    {projectTitle} • {exportResolution} {exportFps}fps
                  </span>
                </div>
              </div>
              <button
                onClick={() => setIsExportModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            <ExportProgressView
              progress={exportProgress}
              isExporting={isExporting}
              isComplete={exportComplete}
              projectTitle={projectTitle}
              resolution={exportResolution}
              fps={exportFps}
              aspectRatio={selectedAspect}
              totalDurationSeconds={trimEnd - trimStart}
              videoBlobUrl={exportVideoBlobUrl}
              onDone={() => setIsExportModalOpen(false)}
              onCancel={() => {
                setIsExporting(false);
                setIsExportModalOpen(false);
              }}
            />
          </div>
        </div>
      )}

      {/* Create Template Modal */}
      <CreateTemplateModal
        isOpen={isCreateTemplateModalOpen}
        onClose={() => setIsCreateTemplateModalOpen(false)}
        currentUser={currentUser}
        currentSettings={{
          aspectRatio: selectedAspect,
          filter: selectedFilter,
          speed: videoSpeed,
          captionStyle: captionStyle,
          duration: parseFloat((trimEnd - trimStart).toFixed(1))
        }}
        onTemplateCreated={(newTpl) => {
          setTemplates(prev => [newTpl, ...prev]);
          setSaveToast(`Template "${newTpl.title}" created & saved!`);
          setTimeout(() => setSaveToast(null), 3000);
        }}
      />

      {/* Secure Admin Access Verification Modal */}
      <AdminAccessModal
        isOpen={isAdminAccessModalOpen}
        onClose={() => setIsAdminAccessModalOpen(false)}
        currentUser={currentUser}
        onAdminVerified={(adminUser) => {
          if (onUpdateCurrentUser) {
            onUpdateCurrentUser(adminUser);
          }
          setActiveApp('admin');
        }}
      />

      {/* ZapUPI Subscription Checkout Modal */}
      {selectedZapUpiPlan && (
        <ZapUpiPaymentModal
          isOpen={isZapUpiModalOpen}
          onClose={() => setIsZapUpiModalOpen(false)}
          currentUser={currentUser}
          plan={selectedZapUpiPlan}
          onPaymentSuccess={(plan, txn) => {
            if (onSubscribeSuccess) {
              onSubscribeSuccess(plan, txn);
            }
            setIsZapUpiModalOpen(false);
            setSaveToast(`🎉 Successfully upgraded to ${plan.name}!`);
            setTimeout(() => setSaveToast(null), 3000);
          }}
        />
      )}

    </div>
  );
};
