import React, { useState, useEffect } from 'react';
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
  Clock, 
  CheckCircle, 
  Users, 
  ShieldCheck, 
  ShieldAlert, 
  Download, 
  Smartphone, 
  Tv, 
  Scissors, 
  Layers, 
  Maximize2, 
  VolumeX, 
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
  CreditCard
} from 'lucide-react';
import { User, SubscriptionPlanConfig } from '../types';
import { INITIAL_SUBSCRIPTION_PLANS } from '../data/initialData';
import { ZapUpiPaymentModal } from '../components/ZapUpiPaymentModal';
import { ExportProgressView } from '../components/ExportProgressView';
import { syncPromotedFeatureToFirestore } from '../lib/firebase';

interface MobileAppSimulatorViewProps {
  currentUser: User;
  onOpenDownloadApkModal: () => void;
  onOpenFirebaseModal: () => void;
  onSubscribeSuccess?: (plan: SubscriptionPlanConfig, txn: any) => void;
}

interface SavedProject {
  id: string;
  title: string;
  aspectRatio: string;
  filter: string;
  duration: number;
  updatedAt: string;
  thumbnailGradient: string;
}

export const MobileAppSimulatorView: React.FC<MobileAppSimulatorViewProps> = ({
  currentUser,
  onOpenDownloadApkModal,
  onOpenFirebaseModal,
  onSubscribeSuccess
}) => {
  // Simulator Device State
  const [activeApp, setActiveApp] = useState<'creator' | 'admin'>('creator');
  const [phoneTheme, setPhoneTheme] = useState<'dark' | 'diwali' | 'holi'>('dark');

  // Creator App States
  const [creatorBottomTab, setCreatorBottomTab] = useState<'editor' | 'projects' | 'ai' | 'social' | 'profile'>('editor');
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(4.2);
  const [totalDuration, setTotalDuration] = useState(15.0);
  const [trimStart, setTrimStart] = useState(0.0);
  const [trimEnd, setTrimEnd] = useState(15.0);
  const [selectedAspect, setSelectedAspect] = useState<'16:9' | '9:16' | '1:1' | '4:5'>('9:16');
  const [selectedFilter, setSelectedFilter] = useState<'Normal' | 'Cinematic' | 'Warm' | 'Noir' | 'Cyberpunk'>('Cinematic');
  const [videoSpeed, setVideoSpeed] = useState<number>(1.0);
  const [audioVolume, setAudioVolume] = useState<number>(85);
  const [projectTitle, setProjectTitle] = useState('Reel_Sunset_Cut');
  const [saveToast, setSaveToast] = useState<string | null>(null);

  // Pro CapCut Multi-Track & Tool Palette States
  const [activeTimelineTrack, setActiveTimelineTrack] = useState<'video' | 'audio' | 'text' | 'effects'>('video');
  const [activeEditorTool, setActiveEditorTool] = useState<'trim' | 'speed' | 'keyframes' | 'filters' | 'adjust' | 'text' | 'transitions'>('trim');
  const [timelineClips, setTimelineClips] = useState([
    { id: 'c1', title: 'Hook Scene', start: 0.0, end: 4.5, color: 'from-amber-600 to-rose-600' },
    { id: 'c2', title: 'Action Drop', start: 4.5, end: 10.2, color: 'from-cyan-600 to-blue-700' },
    { id: 'c3', title: 'Outro Climax', start: 10.2, end: 15.0, color: 'from-purple-600 to-indigo-800' }
  ]);
  const [selectedClipId, setSelectedClipId] = useState<string>('c2');
  
  // Speed Curve & Keyframes
  const [speedCurvePreset, setSpeedCurvePreset] = useState<'normal' | 'montage' | 'hero' | 'bullet' | 'flash'>('normal');
  const [keyframes, setKeyframes] = useState<Array<{ id: string; time: number; scale: number; rotation: number }>>([
    { id: 'kf1', time: 2.0, scale: 1.0, rotation: 0 },
    { id: 'kf2', time: 6.5, scale: 1.25, rotation: 8 }
  ]);
  const [keyframeScale, setKeyframeScale] = useState<number>(1.15);
  const [keyframeRotation, setKeyframeRotation] = useState<number>(0);

  // Color Grading Adjustments
  const [exposureAdj, setExposureAdj] = useState<number>(0);
  const [contrastAdj, setContrastAdj] = useState<number>(108);
  const [saturationAdj, setSaturationAdj] = useState<number>(115);
  const [vignetteAdj, setVignetteAdj] = useState<number>(25);
  const [tempAdj, setTempAdj] = useState<number>(5);

  // Subtitles & Captions
  const [captionText, setCaptionText] = useState('✨ Epic Golden Hour Reel');
  const [captionStyle, setCaptionStyle] = useState<'bouncing' | 'karaoke' | 'box' | 'cinematic'>('bouncing');
  const [captionFont, setCaptionFont] = useState<'sans' | 'serif' | 'bold' | 'script'>('bold');

  // Transitions
  const [selectedTransition, setSelectedTransition] = useState<'none' | 'dissolve' | 'fade_black' | 'whip_pan' | 'zoom' | 'glitch'>('dissolve');

  // Export Modal & Progress
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [exportResolution, setExportResolution] = useState<'720p' | '1080p' | '4K' | '8K'>('1080p');
  const [exportFps, setExportFps] = useState<24 | 30 | 60>(60);
  const [exportProgress, setExportProgress] = useState<number>(0);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [exportComplete, setExportComplete] = useState<boolean>(false);
  const [isExportPaused, setIsExportPaused] = useState<boolean>(false);
  const exportIntervalRef = React.useRef<any>(null);

  // ZapUPI Subscription Checkout
  const [isZapUpiModalOpen, setIsZapUpiModalOpen] = useState(false);
  const [selectedZapUpiPlan, setSelectedZapUpiPlan] = useState<SubscriptionPlanConfig | null>(
    INITIAL_SUBSCRIPTION_PLANS.find(p => p.id === 'plan-subscribed-creator') || INITIAL_SUBSCRIPTION_PLANS[1]
  );

  // Projects list
  const [projects, setProjects] = useState<SavedProject[]>([
    {
      id: 'p1',
      title: 'Reel_Sunset_Cut',
      aspectRatio: '9:16',
      filter: 'Cinematic',
      duration: 15.0,
      updatedAt: 'Just now',
      thumbnailGradient: 'from-amber-600 via-rose-600 to-purple-800'
    },
    {
      id: 'p2',
      title: 'Mumbai_Street_Vlog',
      aspectRatio: '16:9',
      filter: 'Warm',
      duration: 42.5,
      updatedAt: '2 hours ago',
      thumbnailGradient: 'from-cyan-600 via-blue-600 to-indigo-900'
    },
    {
      id: 'p3',
      title: 'Tech_Launch_Teaser',
      aspectRatio: '1:1',
      filter: 'Cyberpunk',
      duration: 8.2,
      updatedAt: 'Yesterday',
      thumbnailGradient: 'from-fuchsia-600 via-pink-600 to-rose-900'
    }
  ]);

  // AI Generation Sim
  const [aiRunning, setAiRunning] = useState(false);
  const [aiResult, setAiResult] = useState<string | null>(null);

  // Admin App States
  const [adminBottomTab, setAdminBottomTab] = useState<'telemetry' | 'roles' | 'security' | 'broadcast' | 'promote'>('promote');
  const [killSwitchActive, setKillSwitchActive] = useState(false);
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [broadcastSent, setBroadcastSent] = useState(false);
  const [targetUserId, setTargetUserId] = useState('');
  const [roleAssignedMsg, setRoleAssignedMsg] = useState<string | null>(null);

  // Worldwide Promoted Feature state (Promoted by Admin and synced worldwide)
  const [worldwidePromotion, setWorldwidePromotion] = useState<{
    id: string;
    title: string;
    badge: string;
    perk: string;
    description: string;
    targetTab: string;
  } | null>({
    id: 'promo-ai-captions',
    title: 'AI Auto-Captions & Subtitle Sync',
    badge: 'WORLDWIDE SPOTLIGHT',
    perk: 'Free 0 Blue Coins Unlocked',
    description: 'Deep learning speech-to-text with karaoke bounce animations.',
    targetTab: 'ai'
  });

  // Phone Simulator Live Coin Economy
  const [simYellowCoins, setSimYellowCoins] = useState<number>(currentUser.yellowCoins || 150);
  const [simBlueCoins, setSimBlueCoins] = useState<number>(currentUser.blueCoins || 25);
  const [simStreak, setSimStreak] = useState<number>(currentUser.streakDays || 5);
  const [showPhoneCoinsModal, setShowPhoneCoinsModal] = useState<boolean>(false);
  const [phonePromoInput, setPhonePromoInput] = useState<string>('');
  const [phoneWalletMsg, setPhoneWalletMsg] = useState<string | null>(null);

  // Phone Simulator Live Notifications
  const [showPhoneNotifsModal, setShowPhoneNotifsModal] = useState<boolean>(false);
  const [phoneNotifs, setPhoneNotifs] = useState([
    {
      id: 'pn1',
      title: 'Diwali Festive Gift Unlocked 🪔',
      message: 'Redeem code DIWALI50 in wallet for +50 Yellow Coins & +5 Blue Coins!',
      type: 'CAMPAIGN',
      time: '10m ago',
      unread: true
    },
    {
      id: 'pn2',
      title: 'Collaboration Request',
      message: 'Aarav Sharma (VID-10492) invited you to edit "Mumbai_Night_Reel".',
      type: 'FRIEND',
      time: '1h ago',
      unread: true
    },
    {
      id: 'pn3',
      title: 'Render Engine v2.4 Active',
      message: 'Hardware 1080p 60fps export and multi-track audio mixing is ready.',
      type: 'RENDER',
      time: '2h ago',
      unread: false
    }
  ]);

  // Phone Simulator Friends & Direct Chat System
  const [phoneFriends, setPhoneFriends] = useState([
    { name: 'Aarav Sharma', tag: 'VID-10492', role: 'VIP Creator', online: true, lastMsg: 'Did you see the new 4K LUT?' },
    { name: 'Priya Patel', tag: 'VID-20914', role: 'Cinematographer', online: true, lastMsg: 'Want to collaborate on Mumbai Vlog?' },
    { name: 'Devon King', tag: 'VID-90214', role: 'Sound Designer', online: false, lastMsg: 'Audio stems delivered.' }
  ]);
  const [phonePendingRequests, setPhonePendingRequests] = useState([
    { name: 'Rohan Verma', tag: 'VID-30192', role: 'Motion Designer' }
  ]);
  const [activeChatFriend, setActiveChatFriend] = useState<{ name: string; tag: string; role: string; online: boolean } | null>(null);
  const [chatMessages, setChatMessages] = useState<Record<string, Array<{ id: string; sender: string; text: string; time: string; isMe: boolean; projectAttachment?: string }>>>({
    'VID-10492': [
      { id: 'cm1', sender: 'Aarav Sharma', text: 'Hey Robin! Did you see the new Cinematic LUT in CineCut?', time: '12:15 PM', isMe: false },
      { id: 'cm2', sender: 'Robin', text: 'Yes! Used it on my sunset reel at 4K 60fps. Looks incredible!', time: '12:18 PM', isMe: true },
      { id: 'cm3', sender: 'Aarav Sharma', text: 'Can you share the project? Let me tweak the audio fade curves on the beat drop.', time: '12:22 PM', isMe: false }
    ],
    'VID-20914': [
      { id: 'cm4', sender: 'Priya Patel', text: 'Hi Robin! I am editing a Mumbai travel documentary. Want to collaborate?', time: '11:40 AM', isMe: false }
    ],
    'VID-90214': [
      { id: 'cm5', sender: 'Devon King', text: 'Audio stems uploaded. 5.1 surround sound mix is attached.', time: 'Yesterday', isMe: false }
    ]
  });
  const [chatInputText, setChatInputText] = useState<string>('');
  const [friendSearchQuery, setFriendSearchQuery] = useState<string>('');

  // Playback timer
  useEffect(() => {
    let interval: any;
    if (isPlaying) {
      interval = setInterval(() => {
        setCurrentTime((prev) => {
          if (prev >= trimEnd) {
            return trimStart;
          }
          return parseFloat((prev + 0.1 * videoSpeed).toFixed(1));
        });
      }, 100);
    }
    return () => clearInterval(interval);
  }, [isPlaying, trimEnd, trimStart, videoSpeed]);

  const handleSaveProject = () => {
    const newProject: SavedProject = {
      id: `p-${Date.now()}`,
      title: projectTitle || 'Untitled_Cut',
      aspectRatio: selectedAspect,
      filter: selectedFilter,
      duration: parseFloat((trimEnd - trimStart).toFixed(1)),
      updatedAt: 'Just now',
      thumbnailGradient: 'from-emerald-600 via-teal-600 to-cyan-900'
    };
    setProjects([newProject, ...projects]);
    setSaveToast(`Saved "${newProject.title}" to device and synced to Firestore!`);
    setTimeout(() => setSaveToast(null), 3500);
  };

  // Pro Editing Helpers: Split, Keyframe, Export
  const handleSplitClipAtPlayhead = () => {
    const currentActiveClip = timelineClips.find(c => currentTime >= c.start && currentTime <= c.end);
    if (!currentActiveClip || (currentTime - currentActiveClip.start < 0.5) || (currentActiveClip.end - currentTime < 0.5)) {
      setSaveToast("Move playhead inside a clip with > 0.5s margin to split!");
      setTimeout(() => setSaveToast(null), 2500);
      return;
    }

    const firstHalf = {
      ...currentActiveClip,
      end: currentTime
    };
    const secondHalf = {
      id: `c-${Date.now()}`,
      title: `${currentActiveClip.title} (Part 2)`,
      start: currentTime,
      end: currentActiveClip.end,
      color: 'from-emerald-600 to-teal-700'
    };

    setTimelineClips(prev => {
      const idx = prev.findIndex(c => c.id === currentActiveClip.id);
      const updated = [...prev];
      updated.splice(idx, 1, firstHalf, secondHalf);
      return updated;
    });

    setSaveToast(`✂️ Split clip at ${currentTime.toFixed(1)}s into 2 segments!`);
    setTimeout(() => setSaveToast(null), 2500);
  };

  const handleToggleKeyframe = () => {
    const existing = keyframes.find(k => Math.abs(k.time - currentTime) < 0.3);
    if (existing) {
      setKeyframes(prev => prev.filter(k => k.id !== existing.id));
      setSaveToast(`Removed keyframe at ${currentTime.toFixed(1)}s`);
    } else {
      const newKf = {
        id: `kf-${Date.now()}`,
        time: parseFloat(currentTime.toFixed(1)),
        scale: keyframeScale,
        rotation: keyframeRotation
      };
      setKeyframes(prev => [...prev, newKf].sort((a, b) => a.time - b.time));
      setSaveToast(`💎 Placed keyframe diamond at ${currentTime.toFixed(1)}s!`);
    }
    setTimeout(() => setSaveToast(null), 2500);
  };

  const handleExecuteExport = () => {
    setIsExporting(true);
    setExportProgress(0);
    setExportComplete(false);
    setIsExportPaused(false);

    if (exportIntervalRef.current) {
      clearInterval(exportIntervalRef.current);
    }

    exportIntervalRef.current = setInterval(() => {
      setExportProgress((prev) => {
        const next = Math.min(100, prev + Math.floor(Math.random() * 5) + 3);
        if (next >= 100) {
          clearInterval(exportIntervalRef.current);
          setIsExporting(false);
          setExportComplete(true);
          setSaveToast(`🎉 Render complete! ${projectTitle} (${exportResolution} @ ${exportFps}fps) ready.`);
          setTimeout(() => setSaveToast(null), 4000);
          return 100;
        }
        return next;
      });
    }, 280);
  };

  const handlePauseExport = (paused: boolean) => {
    setIsExportPaused(paused);
    if (paused) {
      if (exportIntervalRef.current) {
        clearInterval(exportIntervalRef.current);
      }
    } else {
      exportIntervalRef.current = setInterval(() => {
        setExportProgress((prev) => {
          const next = Math.min(100, prev + Math.floor(Math.random() * 5) + 3);
          if (next >= 100) {
            clearInterval(exportIntervalRef.current);
            setIsExporting(false);
            setExportComplete(true);
            setSaveToast(`🎉 Render complete! ${projectTitle} (${exportResolution} @ ${exportFps}fps) ready.`);
            setTimeout(() => setSaveToast(null), 4000);
            return 100;
          }
          return next;
        });
      }, 280);
    }
  };

  const handleCancelExport = () => {
    if (exportIntervalRef.current) {
      clearInterval(exportIntervalRef.current);
    }
    setIsExporting(false);
    setExportProgress(0);
    setIsExportPaused(false);
    setExportComplete(false);
    setSaveToast("Export rendering cancelled.");
    setTimeout(() => setSaveToast(null), 2500);
  };

  const handleRunAiTool = (toolName: string) => {
    setAiRunning(true);
    setAiResult(null);
    setTimeout(() => {
      setAiRunning(false);
      setAiResult(`✨ ${toolName} completed: 18 auto-cuts applied, 4 captions synced.`);
    }, 1800);
  };

  const handleAssignRole = (roleName: string) => {
    setRoleAssignedMsg(`Assigned role [${roleName}] to ${targetUserId || 'User'}`);
    setTimeout(() => setRoleAssignedMsg(null), 3000);
  };

  const handleSendBroadcast = () => {
    if (!broadcastMessage.trim()) return;
    setBroadcastSent(true);
    setTimeout(() => {
      setBroadcastSent(false);
      setBroadcastMessage('');
    }, 2500);
  };

  // Filter CSS helpers
  const getFilterStyle = () => {
    switch (selectedFilter) {
      case 'Cinematic':
        return 'contrast-125 saturate-110 sepia-[0.2] hue-rotate-[-10deg]';
      case 'Warm':
        return 'sepia-[0.35] saturate-125 contrast-110';
      case 'Noir':
        return 'grayscale contrast-150 brightness-95';
      case 'Cyberpunk':
        return 'hue-rotate-[180deg] saturate-150 contrast-125';
      default:
        return '';
    }
  };

  // Theme styling inside the simulated phone
  const getPhoneThemeClasses = () => {
    if (phoneTheme === 'diwali') {
      return 'bg-gradient-to-b from-amber-950/90 via-stone-900 to-stone-950 text-amber-50';
    }
    if (phoneTheme === 'holi') {
      return 'bg-gradient-to-b from-fuchsia-950/90 via-purple-950 to-stone-950 text-fuchsia-50';
    }
    return 'bg-stone-950 text-slate-100';
  };

  return (
    <div className="p-4 lg:p-6 max-w-7xl mx-auto space-y-6">
      {/* Top Header / Context Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-4 rounded-2xl bg-gradient-to-r from-cyan-950/40 via-studio-850 to-studio-900 border border-cyan-500/30 shadow-lg">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-lg bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">
              <Smartphone className="w-5 h-5" />
            </span>
            <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
              CutMedia Android Apps Simulator
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono border border-emerald-500/40">
                v2.4.0 Production
              </span>
            </h1>
          </div>
          <p className="text-xs text-slate-400">
            Interactive in-browser simulation of both installed Android apps: <span className="text-cyan-300 font-semibold">CutMedia Video Editor</span> and <span className="text-amber-300 font-semibold">CutMedia Admin Console</span>.
          </p>
        </div>

        {/* App Switcher & Download buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="bg-studio-800 p-1 rounded-xl border border-studio-700 flex items-center">
            <button
              onClick={() => setActiveApp('creator')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                activeApp === 'creator'
                  ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Scissors className="w-3.5 h-3.5" />
              <span>CutMedia Video Editor</span>
            </button>
            <button
              onClick={() => setActiveApp('admin')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                activeApp === 'admin'
                  ? 'bg-gradient-to-r from-amber-500 to-rose-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>CutMedia Admin Console</span>
            </button>
          </div>

          <button
            onClick={onOpenDownloadApkModal}
            className="px-3 py-2 rounded-xl text-xs font-bold bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/40 transition-all flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Install on Real Device (.apk)</span>
          </button>
        </div>
      </div>

      {/* Main Dual Pane: Interactive Smartphone + Live Controls */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* Smartphone Mockup Frame (5 cols on lg) */}
        <div className="lg:col-span-6 xl:col-span-5 flex justify-center">
          <div className="relative w-full max-w-[380px] rounded-[44px] p-3.5 bg-gradient-to-b from-stone-800 via-stone-900 to-stone-950 border-4 border-stone-700 shadow-2xl shadow-cyan-950/40 ring-1 ring-white/10">
            {/* Phone Speaker & Dynamic Island / Punch Hole */}
            <div className="absolute top-6 left-1/2 -translate-x-1/2 z-30 flex items-center justify-center">
              <div className="w-20 h-4 bg-stone-950 rounded-full flex items-center justify-between px-2.5 border border-stone-800/80">
                <span className="w-2 h-2 rounded-full bg-cyan-400/80 animate-pulse" />
                <span className="w-2.5 h-2.5 rounded-full bg-stone-800" />
              </div>
            </div>

            {/* Smartphone Inner Screen */}
            <div className={`relative w-full h-[690px] rounded-[34px] overflow-hidden flex flex-col ${getPhoneThemeClasses()} select-none`}>
              
              {/* Android Status Bar */}
              <div className="pt-3 pb-1 px-5 flex items-center justify-between text-[11px] text-slate-300 font-mono shrink-0 z-20">
                <span className="font-bold">12:30</span>
                <div className="flex items-center gap-1.5 opacity-90">
                  <span className="text-[10px] tracking-tight">5G</span>
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-400/90" />
                  <span className="text-[10px]">98%</span>
                </div>
              </div>

              {/* Toast notification inside phone */}
              {saveToast && (
                <div className="absolute top-12 left-4 right-4 z-40 bg-emerald-950/95 border border-emerald-500/50 text-emerald-200 text-xs px-3 py-2 rounded-xl shadow-lg flex items-center gap-2 animate-in fade-in slide-in-from-top-2">
                  <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span className="text-[11px] leading-tight">{saveToast}</span>
                </div>
              )}

              {/* APP 1: CutMedia Video Editor (Creator) */}
              {activeApp === 'creator' && (
                <div className="flex-1 flex flex-col overflow-hidden">
                  
                  {/* Creator App Header */}
                  <div className="px-3 py-2 border-b border-stone-800/80 flex items-center justify-between shrink-0 bg-stone-950/60">
                    <div className="flex items-center gap-1.5">
                      <div className="w-6 h-6 rounded-lg bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center font-bold text-white text-[10px]">
                        CC
                      </div>
                      <div>
                        <h2 className="text-xs font-bold tracking-tight text-white leading-none">CineCut</h2>
                        <span className="text-[8px] text-cyan-400 font-mono">v2.4.0</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      {/* Coins Badges Pill (Clickable Wallet) */}
                      <button
                        onClick={() => setShowPhoneCoinsModal(true)}
                        className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-stone-900 border border-amber-500/40 text-[10px] hover:border-amber-400 transition-all shadow-sm"
                        title="Open Coin Economy Wallet"
                      >
                        <span className="flex items-center gap-0.5 text-amber-400 font-extrabold">
                          <span>🪙</span>
                          <span>{simYellowCoins}</span>
                        </span>
                        <span className="w-px h-2.5 bg-stone-700" />
                        <span className="flex items-center gap-0.5 text-cyan-400 font-extrabold">
                          <span>⚡</span>
                          <span>{simBlueCoins}</span>
                        </span>
                      </button>

                      {/* Notification Bell with Badge */}
                      <button
                        onClick={() => setShowPhoneNotifsModal(true)}
                        className="relative p-1 rounded-lg text-slate-300 hover:text-white bg-stone-900/80 border border-stone-800"
                        title="Open Notifications Center"
                      >
                        <Bell className="w-3.5 h-3.5 text-cyan-400" />
                        {phoneNotifs.filter(n => n.unread).length > 0 && (
                          <span className="absolute -top-1 -right-1 w-3.5 h-3.5 rounded-full bg-cyan-500 text-stone-950 font-black text-[8px] flex items-center justify-center animate-pulse">
                            {phoneNotifs.filter(n => n.unread).length}
                          </span>
                        )}
                      </button>

                      {/* Seasonal theme pill */}
                      <div className="flex items-center gap-0.5 bg-stone-900 p-0.5 rounded-lg border border-stone-800 text-[9px]">
                        <button 
                          onClick={() => setPhoneTheme('dark')}
                          className={`px-1 py-0.5 rounded ${phoneTheme === 'dark' ? 'bg-stone-800 text-cyan-300 font-bold' : 'text-slate-400'}`}
                        >
                          Dark
                        </button>
                        <button 
                          onClick={() => setPhoneTheme('diwali')}
                          className={`px-1 py-0.5 rounded ${phoneTheme === 'diwali' ? 'bg-amber-600 text-white font-bold' : 'text-slate-400'}`}
                          title="Diwali Glow"
                        >
                          🪔
                        </button>
                        <button 
                          onClick={() => setPhoneTheme('holi')}
                          className={`px-1 py-0.5 rounded ${phoneTheme === 'holi' ? 'bg-fuchsia-600 text-white font-bold' : 'text-slate-400'}`}
                          title="Holi Splash"
                        >
                          🎨
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Creator Body by Tab */}
                  <div className="flex-1 overflow-y-auto p-3 space-y-3">
                    
                    {/* Worldwide Promoted Feature Live Banner (Synced across all devices) */}
                    {worldwidePromotion && (
                      <div 
                        onClick={() => setCreatorBottomTab(worldwidePromotion.targetTab as any)}
                        className="p-2.5 rounded-xl bg-gradient-to-r from-amber-500/20 via-cyan-500/20 to-emerald-500/10 border border-amber-400/50 flex items-center justify-between shadow-lg cursor-pointer hover:border-amber-300 transition-all animate-in fade-in"
                        title="Worldwide Promoted Feature — Tap to Open"
                      >
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 rounded-full bg-gradient-to-tr from-amber-400 to-amber-600 text-stone-950 flex items-center justify-center font-bold text-xs shrink-0 shadow">
                            ⭐
                          </div>
                          <div>
                            <div className="flex items-center gap-1.5">
                              <span className="text-[8px] font-black text-amber-300 uppercase tracking-wider">{worldwidePromotion.badge}</span>
                              <span className="text-[8px] font-bold text-emerald-400">• {worldwidePromotion.perk}</span>
                            </div>
                            <p className="text-[11px] font-bold text-white leading-tight">{worldwidePromotion.title}</p>
                          </div>
                        </div>
                        <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-amber-400 hover:bg-amber-300 text-stone-950 shadow shrink-0">
                          Open →
                        </span>
                      </div>
                    )}
                    
                    {/* TAB 1: Main Video Editor Studio */}
                    {creatorBottomTab === 'editor' && (
                      <div className="space-y-3">
                        {/* Video Canvas Preview with Live Keyframes & Adjustments */}
                        <div className="relative rounded-2xl overflow-hidden bg-black border border-stone-800 flex items-center justify-center min-h-[220px]">
                          {/* Aspect Ratio Box Wrapper */}
                          <div 
                            className={`relative transition-all duration-300 flex items-center justify-center overflow-hidden rounded-xl shadow-inner ${
                              selectedAspect === '9:16' ? 'w-[140px] h-[210px]' :
                              selectedAspect === '16:9' ? 'w-[280px] h-[160px]' :
                              selectedAspect === '1:1' ? 'w-[190px] h-[190px]' : 'w-[160px] h-[200px]'
                            }`}
                          >
                            {/* Animated Video Simulation Background with Keyframe transforms & adjustments */}
                            <div 
                              className={`absolute inset-0 bg-gradient-to-tr from-cyan-900 via-indigo-950 to-rose-900 flex items-center justify-center ${getFilterStyle()}`}
                              style={{
                                transform: `scale(${isPlaying ? 1.05 : keyframeScale}) rotate(${keyframeRotation}deg)`,
                                filter: `brightness(${100 + exposureAdj}%) contrast(${contrastAdj}%) saturate(${saturationAdj}%)`,
                                transition: 'transform 0.2s ease-out'
                              }}
                            >
                              {/* Moving visuals representing video playback */}
                              <div className="relative w-full h-full flex flex-col items-center justify-center p-3 text-center">
                                <div className={`w-16 h-16 rounded-full bg-white/10 backdrop-blur-sm border border-white/20 flex items-center justify-center shadow-lg transition-transform ${isPlaying ? 'scale-110 animate-spin-slow' : ''}`}>
                                  <Film className="w-8 h-8 text-cyan-300" />
                                </div>
                                <span className="mt-2 text-xs font-bold text-white drop-shadow-md">{projectTitle}</span>
                                <span className="text-[10px] text-cyan-300/90 font-mono mt-0.5">
                                  {currentTime.toFixed(1)}s / {totalDuration.toFixed(1)}s
                                </span>
                              </div>
                            </div>

                            {/* On-Screen Animated Captions Overlay */}
                            {captionText && (
                              <div className={`absolute bottom-3 px-2.5 py-1 rounded-lg text-center font-bold text-[10px] drop-shadow-md z-20 pointer-events-none max-w-[85%] truncate ${
                                captionStyle === 'bouncing' ? 'bg-amber-400 text-stone-950 font-black animate-bounce shadow-lg' :
                                captionStyle === 'karaoke' ? 'bg-gradient-to-r from-rose-500 to-amber-500 text-white shadow-md' :
                                captionStyle === 'box' ? 'bg-black/85 text-yellow-300 border border-yellow-400/50' :
                                'text-white bg-black/60 backdrop-blur'
                              }`}>
                                {captionText}
                              </div>
                            )}

                            {/* Play / Pause Overlay Button */}
                            <button
                              onClick={() => setIsPlaying(!isPlaying)}
                              className="absolute inset-0 flex items-center justify-center bg-black/20 hover:bg-black/40 transition-colors group z-10"
                            >
                              <div className="w-11 h-11 rounded-full bg-cyan-500/90 hover:bg-cyan-400 text-stone-950 flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                                {isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
                              </div>
                            </button>

                            {/* Aspect badge */}
                            <span className="absolute top-2 right-2 px-1.5 py-0.5 rounded bg-black/60 backdrop-blur text-[9px] font-mono text-white z-20">
                              {selectedAspect}
                            </span>

                            {/* Keyframe Indicator Badge */}
                            {keyframes.some(k => Math.abs(k.time - currentTime) < 0.4) && (
                              <span className="absolute top-2 left-2 px-1.5 py-0.5 rounded bg-amber-500/90 text-stone-950 font-bold text-[9px] flex items-center gap-0.5 z-20">
                                <Diamond className="w-2.5 h-2.5 fill-current" /> KF Active
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Quick Pro Actions Bar (Split, Keyframe, Export) */}
                        <div className="grid grid-cols-4 gap-1.5 bg-stone-900/90 p-1.5 rounded-xl border border-stone-800">
                          <button
                            onClick={handleSplitClipAtPlayhead}
                            className="py-1.5 px-2 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-200 text-[10px] font-bold flex items-center justify-center gap-1 transition-colors"
                            title="Split clip at playhead"
                          >
                            <Scissors className="w-3.5 h-3.5 text-cyan-400" />
                            <span>Split</span>
                          </button>

                          <button
                            onClick={handleToggleKeyframe}
                            className={`py-1.5 px-2 rounded-lg text-[10px] font-bold flex items-center justify-center gap-1 transition-colors ${
                              keyframes.some(k => Math.abs(k.time - currentTime) < 0.4)
                                ? 'bg-amber-500 text-stone-950'
                                : 'bg-stone-800 hover:bg-stone-750 text-slate-200'
                            }`}
                            title="Add / Remove Keyframe"
                          >
                            <Diamond className="w-3.5 h-3.5 text-amber-400" />
                            <span>Keyframe</span>
                          </button>

                          <button
                            onClick={() => {
                              setIsPlaying(false);
                              setCurrentTime(0);
                            }}
                            className="py-1.5 px-2 rounded-lg bg-stone-800 hover:bg-stone-750 text-slate-200 text-[10px] font-bold flex items-center justify-center gap-1 transition-colors"
                            title="Rewind to start"
                          >
                            <RotateCcw className="w-3.5 h-3.5 text-purple-400" />
                            <span>Rewind</span>
                          </button>

                          <button
                            onClick={() => setIsExportModalOpen(true)}
                            className="py-1.5 px-2 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-stone-950 text-[10px] font-extrabold flex items-center justify-center gap-1 transition-all shadow"
                            title="Export Video Project"
                          >
                            <Download className="w-3.5 h-3.5" />
                            <span>Export</span>
                          </button>
                        </div>

                        {/* Real-Time Video Rendering Status Widget in Simulated Phone */}
                        {(isExporting || exportComplete) && (
                          <div 
                            onClick={() => setIsExportModalOpen(true)}
                            className="cursor-pointer transition-transform hover:scale-[1.01]"
                            title="Click to view full Render Engine dashboard"
                          >
                            <ExportProgressView
                              compact={true}
                              progress={exportProgress}
                              isExporting={isExporting}
                              isComplete={exportComplete}
                              projectTitle={projectTitle}
                              resolution={exportResolution}
                              fps={exportFps}
                              aspectRatio={selectedAspect}
                              totalDurationSeconds={parseFloat((trimEnd - trimStart).toFixed(1)) || 15.0}
                              onCancel={handleCancelExport}
                              onPauseToggle={handlePauseExport}
                            />
                          </div>
                        )}

                        {/* CapCut Multi-Track Layers View */}
                        <div className="bg-stone-900/90 p-2.5 rounded-xl border border-stone-800 space-y-2">
                          <div className="flex items-center justify-between text-[10px] text-slate-400 font-mono">
                            <span className="flex items-center gap-1.5 text-cyan-400 font-bold">
                              <Layers className="w-3.5 h-3.5" /> Multi-Track Timeline:
                            </span>
                            <span>{currentTime.toFixed(1)}s / {totalDuration.toFixed(1)}s</span>
                          </div>

                          {/* Scrubber Ruler with Keyframe Diamonds */}
                          <div className="relative h-6 bg-stone-950 rounded-lg border border-stone-800 flex items-center px-1">
                            {/* Keyframe diamonds plotted on scrubber */}
                            {keyframes.map(kf => (
                              <div
                                key={kf.id}
                                className="absolute w-2.5 h-2.5 bg-amber-400 rotate-45 z-10 -ml-1 border border-stone-950 shadow"
                                style={{ left: `${(kf.time / totalDuration) * 100}%` }}
                                title={`Keyframe at ${kf.time}s`}
                              />
                            ))}

                            {/* Scrubber Playhead Needle */}
                            <div 
                              className="absolute w-1.5 h-6 bg-white rounded-full shadow-lg z-20 -ml-0.5 border border-cyan-400"
                              style={{ left: `${(currentTime / totalDuration) * 100}%` }}
                            />

                            <div className="w-full flex justify-between px-1 text-[8px] text-stone-600 font-mono pointer-events-none">
                              <span>0.0s</span>
                              <span>4.0s</span>
                              <span>8.0s</span>
                              <span>12.0s</span>
                              <span>15.0s</span>
                            </div>
                          </div>

                          {/* Track 1: Main Video Track Clips Strip */}
                          <div className="space-y-1">
                            <div className="text-[9px] text-slate-400 font-semibold flex items-center justify-between">
                              <span className="flex items-center gap-1 text-slate-300">
                                <Film className="w-2.5 h-2.5 text-cyan-400" /> Track 1: Video ({timelineClips.length} clips)
                              </span>
                              <span className="text-cyan-400 font-mono text-[9px]">Tap clip to select</span>
                            </div>
                            <div className="flex gap-1 h-8 bg-stone-950 p-1 rounded-lg border border-stone-800 overflow-x-auto">
                              {timelineClips.map((clip) => {
                                const isSelected = selectedClipId === clip.id;
                                const clipDuration = clip.end - clip.start;
                                const widthPct = (clipDuration / totalDuration) * 100;

                                return (
                                  <button
                                    key={clip.id}
                                    onClick={() => setSelectedClipId(clip.id)}
                                    style={{ width: `${Math.max(22, widthPct)}%` }}
                                    className={`h-full rounded bg-gradient-to-r ${clip.color} px-1.5 text-[8px] font-bold text-white flex items-center justify-between truncate transition-all ${
                                      isSelected ? 'ring-2 ring-white shadow-md' : 'opacity-85 hover:opacity-100'
                                    }`}
                                  >
                                    <span className="truncate">{clip.title}</span>
                                    <span className="text-[7px] font-mono opacity-80 shrink-0 ml-1">{clipDuration.toFixed(1)}s</span>
                                  </button>
                                );
                              })}
                            </div>
                          </div>

                          {/* Track 2: Audio Waveform Track */}
                          <div className="space-y-1">
                            <div className="text-[9px] text-slate-400 font-semibold flex items-center justify-between">
                              <span className="flex items-center gap-1 text-emerald-400">
                                <Volume2 className="w-2.5 h-2.5" /> Track 2: Audio ({audioVolume}%)
                              </span>
                              <button
                                onClick={() => setAudioVolume(audioVolume === 0 ? 85 : 0)}
                                className="text-[9px] text-slate-400 hover:text-white"
                              >
                                {audioVolume === 0 ? 'Unmute' : 'Mute'}
                              </button>
                            </div>
                            <div className="h-6 bg-stone-950 p-1 rounded-lg border border-stone-800 flex items-center gap-0.5 overflow-hidden">
                              {Array.from({ length: 32 }).map((_, i) => {
                                const height = Math.min(100, Math.max(20, Math.sin(i * 0.45) * 80 + 30));
                                return (
                                  <div
                                    key={i}
                                    className={`flex-1 rounded-full ${
                                      audioVolume === 0 ? 'bg-stone-800' : 'bg-emerald-500/70'
                                    }`}
                                    style={{ height: `${height}%` }}
                                  />
                                );
                              })}
                            </div>
                          </div>

                          {/* Track 3: Captions / Subtitle Track */}
                          <div className="space-y-1">
                            <div className="text-[9px] text-slate-400 font-semibold flex items-center justify-between">
                              <span className="flex items-center gap-1 text-amber-400">
                                <Type className="w-2.5 h-2.5" /> Track 3: Captions ({captionStyle})
                              </span>
                              <span className="text-slate-400 text-[8px] font-mono">{captionFont}</span>
                            </div>
                            <div className="h-5 bg-stone-950 p-0.5 rounded-lg border border-stone-800 flex items-center">
                              <div className="w-4/5 h-full rounded bg-amber-500/20 border border-amber-500/40 text-amber-300 text-[8px] font-semibold px-2 flex items-center truncate">
                                {captionText}
                              </div>
                            </div>
                          </div>
                        </div>

                        {/* Tool Switcher Tabs */}
                        <div className="flex items-center gap-1 overflow-x-auto pb-1 scrollbar-none border-b border-stone-800 text-[10px]">
                          {[
                            { id: 'trim', label: 'Canvas & Speed', icon: Sliders },
                            { id: 'filters', label: '10 LUT Filters', icon: Palette },
                            { id: 'adjust', label: 'Color Adjust', icon: Wand2 },
                            { id: 'text', label: 'Captions', icon: Type },
                            { id: 'transitions', label: 'Transitions', icon: Repeat },
                          ].map((t) => {
                            const Icon = t.icon;
                            const isActive = activeEditorTool === t.id;
                            return (
                              <button
                                key={t.id}
                                onClick={() => setActiveEditorTool(t.id as any)}
                                className={`px-2.5 py-1 rounded-lg font-bold flex items-center gap-1 whitespace-nowrap transition-all ${
                                  isActive 
                                    ? 'bg-cyan-500 text-stone-950 shadow' 
                                    : 'text-slate-400 hover:text-white bg-stone-900 border border-stone-800'
                                }`}
                              >
                                <Icon className="w-3 h-3" />
                                <span>{t.label}</span>
                              </button>
                            );
                          })}
                        </div>

                        {/* Tool Panel 1: Canvas & Speed */}
                        {activeEditorTool === 'trim' && (
                          <div className="space-y-2.5">
                            {/* Aspect Ratio Selector Pills */}
                            <div className="space-y-1">
                              <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">Canvas Format</span>
                              <div className="grid grid-cols-4 gap-1.5">
                                {(['9:16', '16:9', '1:1', '4:5'] as const).map((ratio) => (
                                  <button
                                    key={ratio}
                                    onClick={() => setSelectedAspect(ratio)}
                                    className={`py-1 rounded-lg text-[10px] font-bold transition-all border ${
                                      selectedAspect === ratio
                                        ? 'bg-cyan-500 text-stone-950 border-cyan-400 shadow'
                                        : 'bg-stone-900 text-slate-300 border-stone-800 hover:border-stone-700'
                                    }`}
                                  >
                                    {ratio}
                                  </button>
                                ))}
                              </div>
                            </div>

                            {/* Speed Curves Preset */}
                            <div className="bg-stone-900/60 p-2 rounded-xl border border-stone-800 space-y-1.5 text-[10px]">
                              <div className="flex items-center justify-between">
                                <span className="text-slate-400 font-bold flex items-center gap-1">
                                  <Zap className="w-3 h-3 text-amber-400" /> Bézier Speed Curve:
                                </span>
                                <span className="text-cyan-400 font-mono font-bold">{videoSpeed}x</span>
                              </div>
                              <div className="grid grid-cols-5 gap-1">
                                {[
                                  { id: 'normal', label: '1.0x', speed: 1.0 },
                                  { id: 'montage', label: 'Montage', speed: 1.5 },
                                  { id: 'hero', label: 'Hero 0.5x', speed: 0.5 },
                                  { id: 'bullet', label: 'Bullet 0.2x', speed: 0.2 },
                                  { id: 'flash', label: 'Flash 3x', speed: 3.0 },
                                ].map((preset) => (
                                  <button
                                    key={preset.id}
                                    onClick={() => {
                                      setSpeedCurvePreset(preset.id as any);
                                      setVideoSpeed(preset.speed);
                                    }}
                                    className={`py-1 rounded text-[9px] font-bold transition-all ${
                                      videoSpeed === preset.speed
                                        ? 'bg-amber-400 text-stone-950 font-black shadow'
                                        : 'bg-stone-800 text-slate-300 hover:bg-stone-750'
                                    }`}
                                  >
                                    {preset.label}
                                  </button>
                                ))}
                              </div>
                            </div>
                          </div>
                        )}

                        {/* Tool Panel 2: 10 LUT Filters */}
                        {activeEditorTool === 'filters' && (
                          <div className="space-y-1.5">
                            <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">Cinematic LUTs</span>
                            <div className="grid grid-cols-3 gap-1.5">
                              {[
                                { name: 'Normal', desc: 'Rec.709 Natural' },
                                { name: 'Cinematic', desc: '35mm Film Grade' },
                                { name: 'Warm', desc: 'Golden Hour' },
                                { name: 'Noir', desc: 'High Contrast B&W' },
                                { name: 'Cyberpunk', desc: 'Neon Blue & Magenta' },
                              ].map((flt) => (
                                <button
                                  key={flt.name}
                                  onClick={() => setSelectedFilter(flt.name as any)}
                                  className={`p-1.5 rounded-lg text-left transition-all border ${
                                    selectedFilter === flt.name
                                      ? 'bg-gradient-to-r from-cyan-600 to-blue-600 text-white border-cyan-400 shadow'
                                      : 'bg-stone-900 text-slate-400 border-stone-800 hover:text-white'
                                  }`}
                                >
                                  <div className="text-[10px] font-bold truncate">{flt.name}</div>
                                  <div className="text-[8px] opacity-75 truncate">{flt.desc}</div>
                                </button>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Tool Panel 3: Color Adjustments */}
                        {activeEditorTool === 'adjust' && (
                          <div className="bg-stone-900/70 p-2.5 rounded-xl border border-stone-800 space-y-2 text-[10px]">
                            <div>
                              <div className="flex justify-between text-slate-400 mb-0.5">
                                <span>Contrast:</span>
                                <span className="font-mono text-cyan-400">{contrastAdj}%</span>
                              </div>
                              <input
                                type="range"
                                min="70"
                                max="150"
                                value={contrastAdj}
                                onChange={(e) => setContrastAdj(Number(e.target.value))}
                                className="w-full accent-cyan-400 h-1 bg-stone-800 rounded"
                              />
                            </div>
                            <div>
                              <div className="flex justify-between text-slate-400 mb-0.5">
                                <span>Saturation:</span>
                                <span className="font-mono text-amber-400">{saturationAdj}%</span>
                              </div>
                              <input
                                type="range"
                                min="50"
                                max="180"
                                value={saturationAdj}
                                onChange={(e) => setSaturationAdj(Number(e.target.value))}
                                className="w-full accent-amber-400 h-1 bg-stone-800 rounded"
                              />
                            </div>
                            <div>
                              <div className="flex justify-between text-slate-400 mb-0.5">
                                <span>Vignette:</span>
                                <span className="font-mono text-purple-400">{vignetteAdj}%</span>
                              </div>
                              <input
                                type="range"
                                min="0"
                                max="60"
                                value={vignetteAdj}
                                onChange={(e) => setVignetteAdj(Number(e.target.value))}
                                className="w-full accent-purple-400 h-1 bg-stone-800 rounded"
                              />
                            </div>
                          </div>
                        )}

                        {/* Tool Panel 4: Captions & Text */}
                        {activeEditorTool === 'text' && (
                          <div className="bg-stone-900/70 p-2.5 rounded-xl border border-stone-800 space-y-2 text-[10px]">
                            <div>
                              <label className="text-slate-400 block mb-1">Caption Text</label>
                              <input
                                type="text"
                                value={captionText}
                                onChange={(e) => setCaptionText(e.target.value)}
                                className="w-full bg-stone-950 border border-stone-800 rounded-lg px-2.5 py-1.5 text-white"
                                placeholder="Enter animated caption..."
                              />
                            </div>
                            <div className="grid grid-cols-2 gap-2">
                              <div>
                                <label className="text-slate-400 block mb-1">Animation Style</label>
                                <select
                                  value={captionStyle}
                                  onChange={(e: any) => setCaptionStyle(e.target.value)}
                                  className="w-full bg-stone-950 border border-stone-800 rounded-lg px-2 py-1 text-white"
                                >
                                  <option value="bouncing">Bouncing Pop</option>
                                  <option value="karaoke">Karaoke Glow</option>
                                  <option value="box">Yellow Box Subtitle</option>
                                  <option value="cinematic">Cinematic Lower Third</option>
                                </select>
                              </div>
                              <div>
                                <label className="text-slate-400 block mb-1">Font Family</label>
                                <select
                                  value={captionFont}
                                  onChange={(e: any) => setCaptionFont(e.target.value)}
                                  className="w-full bg-stone-950 border border-stone-800 rounded-lg px-2 py-1 text-white"
                                >
                                  <option value="bold">Impact Display</option>
                                  <option value="sans">Modern Sans</option>
                                  <option value="serif">Cinema Serif</option>
                                  <option value="script">Retro Script</option>
                                </select>
                              </div>
                            </div>
                          </div>
                        )}

                        {/* Tool Panel 5: Transitions */}
                        {activeEditorTool === 'transitions' && (
                          <div className="space-y-1.5">
                            <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">Clip Transitions</span>
                            <div className="grid grid-cols-3 gap-1.5">
                              {[
                                { id: 'dissolve', name: 'Cross Dissolve' },
                                { id: 'fade_black', name: 'Fade to Black' },
                                { id: 'whip_pan', name: 'Whip Pan' },
                                { id: 'zoom', name: 'Zoom In/Out' },
                                { id: 'glitch', name: 'RGB Glitch' },
                              ].map((tr) => (
                                <button
                                  key={tr.id}
                                  onClick={() => setSelectedTransition(tr.id as any)}
                                  className={`p-1.5 rounded-lg text-left transition-all border ${
                                    selectedTransition === tr.id
                                      ? 'bg-purple-600 text-white border-purple-400 shadow'
                                      : 'bg-stone-900 text-slate-400 border-stone-800 hover:text-white'
                                  }`}
                                >
                                  <div className="text-[10px] font-bold truncate">{tr.name}</div>
                                </button>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Save & Export Master Action Button */}
                        <div className="pt-1">
                          <button
                            onClick={() => setIsExportModalOpen(true)}
                            className="w-full py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-stone-950 shadow-md flex items-center justify-center gap-1.5 transition-all active:scale-[0.98]"
                          >
                            <Download className="w-3.5 h-3.5" />
                            <span>Export Video & Download APK</span>
                          </button>
                        </div>
                      </div>
                    )}

                    {/* TAB 2: Projects List */}
                    {creatorBottomTab === 'projects' && (
                      <div className="space-y-2.5">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-bold text-white">Your Saved Projects ({projects.length})</span>
                          <span className="text-[10px] text-cyan-400 font-mono">Synced to Room DB</span>
                        </div>

                        {projects.map((proj) => (
                          <div 
                            key={proj.id}
                            className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between gap-3 hover:border-cyan-500/40 transition-colors"
                          >
                            <div className={`w-12 h-12 rounded-lg bg-gradient-to-tr ${proj.thumbnailGradient} flex items-center justify-center text-white shrink-0 shadow`}>
                              <Film className="w-5 h-5 opacity-90" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <h4 className="text-xs font-bold text-white truncate">{proj.title}</h4>
                              <div className="flex items-center gap-2 text-[10px] text-slate-400 font-mono mt-0.5">
                                <span>{proj.aspectRatio}</span>
                                <span>•</span>
                                <span>{proj.filter}</span>
                                <span>•</span>
                                <span>{proj.duration}s</span>
                              </div>
                              <span className="text-[9px] text-slate-500">{proj.updatedAt}</span>
                            </div>
                            <button
                              onClick={() => {
                                setProjectTitle(proj.title);
                                setSelectedAspect(proj.aspectRatio as any);
                                setSelectedFilter(proj.filter as any);
                                setCreatorBottomTab('editor');
                              }}
                              className="px-2 py-1 rounded-lg text-[10px] font-bold bg-cyan-500/20 text-cyan-300 hover:bg-cyan-500/30"
                            >
                              Edit
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* TAB 3: AI Studio Suite */}
                    {creatorBottomTab === 'ai' && (
                      <div className="space-y-3">
                        <div className="p-2.5 rounded-xl bg-gradient-to-r from-purple-950/40 to-indigo-950/40 border border-purple-500/30 text-xs">
                          <div className="flex items-center gap-1.5 text-purple-300 font-bold mb-1">
                            <Sparkles className="w-4 h-4" />
                            <span>CutMedia AI Neural Engine</span>
                          </div>
                          <p className="text-[10px] text-slate-300 leading-relaxed">
                            One-tap generative assistance for auto-captions, audio rhythm cuts, and tone grading.
                          </p>
                        </div>

                        <div className="space-y-2">
                          {[
                            { name: 'Auto-Captions & Subtitles', desc: 'Sync speech to animated on-screen captions', cost: '1 Blue Coin' },
                            { name: 'Beat-Sync Auto Cutter', desc: 'Align transitions to background music drops', cost: '2 Blue Coins' },
                            { name: 'Neural Tone Color Grade', desc: 'Emulate Hollywood 35mm film looks', cost: '1 Blue Coin' },
                            { name: 'Smart Viral Highlights', desc: 'Extract best 15-second hook from long video', cost: '3 Blue Coins' }
                          ].map((tool) => (
                            <div 
                              key={tool.name}
                              className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between gap-2"
                            >
                              <div>
                                <h4 className="text-xs font-bold text-white">{tool.name}</h4>
                                <p className="text-[10px] text-slate-400">{tool.desc}</p>
                                <span className="text-[9px] text-cyan-400 font-mono">{tool.cost}</span>
                              </div>
                              <button
                                onClick={() => handleRunAiTool(tool.name)}
                                disabled={aiRunning}
                                className="px-3 py-1.5 rounded-lg text-[10px] font-bold bg-purple-600 hover:bg-purple-500 text-white shrink-0 disabled:opacity-50"
                              >
                                {aiRunning ? 'Running...' : 'Generate'}
                              </button>
                            </div>
                          ))}
                        </div>

                        {aiResult && (
                          <div className="p-2.5 rounded-xl bg-purple-950/80 border border-purple-500/40 text-purple-200 text-xs flex items-center gap-2">
                            <CheckCircle className="w-4 h-4 text-purple-400 shrink-0" />
                            <span>{aiResult}</span>
                          </div>
                        )}
                      </div>
                    )}

                    {/* TAB 4: Social Friends Network & Live Direct Chat */}
                    {creatorBottomTab === 'social' && (
                      <div className="flex-1 flex flex-col h-full -m-3 overflow-hidden">
                        {activeChatFriend ? (
                          /* IN-PHONE DIRECT CHAT ROOM */
                          <div className="flex-1 flex flex-col bg-stone-950 overflow-hidden">
                            {/* Chat Room Top Bar */}
                            <div className="px-3 py-2 bg-stone-900 border-b border-stone-800 flex items-center justify-between shrink-0">
                              <div className="flex items-center gap-2">
                                <button
                                  onClick={() => setActiveChatFriend(null)}
                                  className="p-1 rounded-lg hover:bg-stone-800 text-slate-300"
                                >
                                  <ArrowLeft className="w-4 h-4" />
                                </button>
                                <div className="relative w-7 h-7 rounded-full bg-gradient-to-tr from-cyan-600 to-indigo-600 flex items-center justify-center font-bold text-white text-xs">
                                  {activeChatFriend.name.charAt(0)}
                                  {activeChatFriend.online && (
                                    <span className="absolute bottom-0 right-0 w-2 h-2 rounded-full bg-emerald-400 border border-stone-950" />
                                  )}
                                </div>
                                <div>
                                  <h4 className="text-xs font-bold text-white leading-none">{activeChatFriend.name}</h4>
                                  <span className="text-[9px] text-cyan-400 font-mono">{activeChatFriend.tag}</span>
                                </div>
                              </div>
                              <span className="text-[9px] px-1.5 py-0.5 rounded bg-stone-800 text-slate-300 font-mono">
                                {activeChatFriend.role}
                              </span>
                            </div>

                            {/* Chat Messages Scroll Thread */}
                            <div className="flex-1 overflow-y-auto p-3 space-y-2.5">
                              {(chatMessages[activeChatFriend.tag] || []).map((msg) => (
                                <div
                                  key={msg.id}
                                  className={`flex flex-col ${msg.isMe ? 'items-end' : 'items-start'}`}
                                >
                                  <div
                                    className={`max-w-[82%] px-3 py-1.5 rounded-2xl text-xs leading-relaxed ${
                                      msg.isMe
                                        ? 'bg-gradient-to-r from-cyan-600 to-blue-600 text-white rounded-br-xs shadow-sm'
                                        : 'bg-stone-900 border border-stone-800 text-slate-200 rounded-bl-xs'
                                    }`}
                                  >
                                    {msg.projectAttachment && (
                                      <div className="mb-1 p-1.5 rounded-lg bg-black/30 border border-white/10 flex items-center gap-1.5 text-[10px] font-bold text-cyan-200">
                                        <Film className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                                        <span className="truncate">Clip: {msg.projectAttachment}</span>
                                      </div>
                                    )}
                                    <span>{msg.text}</span>
                                    <span className={`block text-[8px] text-right mt-0.5 ${msg.isMe ? 'text-cyan-200/70' : 'text-slate-400'}`}>
                                      {msg.time}
                                    </span>
                                  </div>
                                </div>
                              ))}
                            </div>

                            {/* Chat Quick Action & Input */}
                            <div className="p-2 bg-stone-900/90 border-t border-stone-800 space-y-1.5 shrink-0">
                              <button
                                onClick={() => {
                                  const text = `🎬 Shared Timeline Cut: "${projectTitle}" (${selectedAspect}, ${totalDuration}s, Filter: ${selectedFilter})`;
                                  const newMsg = {
                                    id: `msg_${Date.now()}`,
                                    sender: currentUser.displayName,
                                    text,
                                    time: 'Just now',
                                    isMe: true,
                                    projectAttachment: projectTitle
                                  };
                                  setChatMessages((prev) => ({
                                    ...prev,
                                    [activeChatFriend.tag]: [...(prev[activeChatFriend.tag] || []), newMsg]
                                  }));
                                  setSaveToast(`Shared "${projectTitle}" to ${activeChatFriend.name}!`);
                                  setTimeout(() => setSaveToast(null), 2500);
                                }}
                                className="w-full py-1 px-2 rounded-lg bg-stone-800/80 hover:bg-stone-800 text-[10px] text-cyan-300 flex items-center justify-center gap-1.5 font-bold transition-all border border-cyan-500/20"
                              >
                                <Film className="w-3 h-3 text-cyan-400" />
                                <span>Share Active Project ("{projectTitle}")</span>
                              </button>

                              <div className="flex items-center gap-1.5">
                                <input
                                  type="text"
                                  value={chatInputText}
                                  onChange={(e) => setChatInputText(e.target.value)}
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter' && chatInputText.trim()) {
                                      const newMsg = {
                                        id: `msg_${Date.now()}`,
                                        sender: currentUser.displayName,
                                        text: chatInputText.trim(),
                                        time: 'Just now',
                                        isMe: true
                                      };
                                      setChatMessages((prev) => ({
                                        ...prev,
                                        [activeChatFriend.tag]: [...(prev[activeChatFriend.tag] || []), newMsg]
                                      }));
                                      setChatInputText('');
                                    }
                                  }}
                                  placeholder="Message friend..."
                                  className="flex-1 bg-stone-950 border border-stone-800 rounded-xl px-2.5 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500"
                                />
                                <button
                                  onClick={() => {
                                    if (chatInputText.trim()) {
                                      const newMsg = {
                                        id: `msg_${Date.now()}`,
                                        sender: currentUser.displayName,
                                        text: chatInputText.trim(),
                                        time: 'Just now',
                                        isMe: true
                                      };
                                      setChatMessages((prev) => ({
                                        ...prev,
                                        [activeChatFriend.tag]: [...(prev[activeChatFriend.tag] || []), newMsg]
                                      }));
                                      setChatInputText('');
                                    }
                                  }}
                                  disabled={!chatInputText.trim()}
                                  className="w-8 h-8 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center text-white disabled:opacity-40"
                                >
                                  <Send className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            </div>
                          </div>
                        ) : (
                          /* FRIENDS LIST & NETWORK */
                          <div className="p-3 space-y-3 overflow-y-auto">
                            {/* Creator ID Card */}
                            <div className="p-2.5 rounded-xl bg-gradient-to-r from-cyan-950/40 to-blue-950/40 border border-cyan-500/30 flex items-center justify-between">
                              <div>
                                <span className="text-[9px] text-slate-400 block font-mono">YOUR CREATOR TAG</span>
                                <span className="text-xs font-bold text-cyan-300 font-mono">{currentUser.userIdTag || 'VID-78291'}</span>
                              </div>
                              <button
                                onClick={() => {
                                  setSaveToast(`Creator Tag ${currentUser.userIdTag || 'VID-78291'} copied to clipboard!`);
                                  setTimeout(() => setSaveToast(null), 2500);
                                }}
                                className="px-2 py-1 rounded-lg bg-cyan-500/20 text-cyan-300 hover:bg-cyan-500/30 text-[10px] font-bold flex items-center gap-1"
                              >
                                <Copy className="w-3 h-3" />
                                <span>Copy</span>
                              </button>
                            </div>

                            {/* Search bar */}
                            <div className="flex items-center gap-2 bg-stone-900 px-2.5 py-1.5 rounded-xl border border-stone-800">
                              <Search className="w-3.5 h-3.5 text-slate-500" />
                              <input 
                                type="text" 
                                value={friendSearchQuery}
                                onChange={(e) => setFriendSearchQuery(e.target.value)}
                                placeholder="Search friends by name or VID-XXXXX..."
                                className="w-full bg-transparent text-xs text-white placeholder-slate-500 focus:outline-none"
                              />
                            </div>

                            {/* Incoming Requests */}
                            {phonePendingRequests.length > 0 && (
                              <div className="space-y-1.5">
                                <span className="text-[10px] font-bold text-amber-400 uppercase tracking-wider flex items-center gap-1">
                                  <span>Pending Requests</span>
                                  <span className="w-4 h-4 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center text-[9px]">
                                    {phonePendingRequests.length}
                                  </span>
                                </span>
                                {phonePendingRequests.map((req) => (
                                  <div key={req.tag} className="p-2 rounded-xl bg-stone-900/90 border border-amber-500/30 flex items-center justify-between">
                                    <div>
                                      <h4 className="text-xs font-bold text-white">{req.name}</h4>
                                      <span className="text-[9px] text-cyan-400 font-mono">{req.tag} • {req.role}</span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                      <button
                                        onClick={() => {
                                          setPhoneFriends((prev) => [
                                            ...prev,
                                            { name: req.name, tag: req.tag, role: req.role, online: true, lastMsg: 'Connected!' }
                                          ]);
                                          setPhonePendingRequests((prev) => prev.filter(r => r.tag !== req.tag));
                                          setSaveToast(`Accepted request from ${req.name}!`);
                                          setTimeout(() => setSaveToast(null), 2500);
                                        }}
                                        className="px-2 py-1 rounded-lg bg-emerald-500 text-stone-950 font-bold text-[10px]"
                                      >
                                        Accept
                                      </button>
                                      <button
                                        onClick={() => {
                                          setPhonePendingRequests((prev) => prev.filter(r => r.tag !== req.tag));
                                        }}
                                        className="px-2 py-1 rounded-lg bg-stone-800 text-slate-400 font-bold text-[10px]"
                                      >
                                        Ignore
                                      </button>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}

                            {/* My Friends & Direct Chat Buttons */}
                            <div className="space-y-2">
                              <div className="flex items-center justify-between">
                                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                                  My Friends ({phoneFriends.length})
                                </span>
                                <span className="text-[10px] text-cyan-400">
                                  {phoneFriends.filter(f => f.online).length} Online
                                </span>
                              </div>

                              {phoneFriends
                                .filter(f => 
                                  !friendSearchQuery || 
                                  f.name.toLowerCase().includes(friendSearchQuery.toLowerCase()) ||
                                  f.tag.toLowerCase().includes(friendSearchQuery.toLowerCase())
                                )
                                .map((f) => (
                                  <div key={f.tag} className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between">
                                    <div className="flex items-center gap-2.5 min-w-0">
                                      <div className="relative w-8 h-8 rounded-full bg-gradient-to-tr from-cyan-600 to-indigo-600 flex items-center justify-center font-bold text-white text-xs shrink-0">
                                        {f.name.charAt(0)}
                                        <span className={`absolute bottom-0 right-0 w-2 h-2 rounded-full border border-stone-950 ${f.online ? 'bg-emerald-400' : 'bg-slate-500'}`} />
                                      </div>
                                      <div className="min-w-0">
                                        <div className="flex items-center gap-1.5">
                                          <h4 className="text-xs font-bold text-white truncate">{f.name}</h4>
                                          <span className="text-[9px] text-cyan-400 font-mono">{f.tag}</span>
                                        </div>
                                        <p className="text-[10px] text-slate-400 truncate">{f.lastMsg || f.role}</p>
                                      </div>
                                    </div>

                                    {/* Direct Chat Button */}
                                    <button
                                      onClick={() => setActiveChatFriend(f)}
                                      className="px-2.5 py-1.5 rounded-lg bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 font-bold text-[10px] flex items-center gap-1 shrink-0 border border-cyan-500/30 transition-all"
                                    >
                                      <MessageSquare className="w-3 h-3" />
                                      <span>Chat</span>
                                    </button>
                                  </div>
                                ))}
                            </div>

                            {/* Suggested Creators */}
                            <div className="space-y-1.5 pt-1">
                              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Suggested Directors</span>
                              {[
                                { name: 'Rohan Verma', tag: 'VID-30192', role: 'Motion Designer' },
                                { name: 'Ananya Roy', tag: 'VID-40182', role: 'Colorist' }
                              ]
                                .filter(s => !phoneFriends.some(f => f.tag === s.tag))
                                .map((creator) => (
                                  <div key={creator.tag} className="p-2 rounded-xl bg-stone-900/60 border border-stone-800/80 flex items-center justify-between">
                                    <div>
                                      <h4 className="text-xs font-bold text-slate-200">{creator.name}</h4>
                                      <span className="text-[9px] text-slate-400 font-mono">{creator.tag} • {creator.role}</span>
                                    </div>
                                    <button
                                      onClick={() => {
                                        setPhoneFriends(prev => [
                                          ...prev,
                                          { name: creator.name, tag: creator.tag, role: creator.role, online: true, lastMsg: 'Connected!' }
                                        ]);
                                        setSaveToast(`Connected with ${creator.name}!`);
                                        setTimeout(() => setSaveToast(null), 2500);
                                      }}
                                      className="px-2 py-1 rounded-lg bg-stone-800 hover:bg-stone-700 text-cyan-300 font-bold text-[10px] flex items-center gap-1"
                                    >
                                      <Plus className="w-3 h-3" />
                                      <span>Connect</span>
                                    </button>
                                  </div>
                                ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}

                    {/* TAB 5: Profile & Settings */}
                    {creatorBottomTab === 'profile' && (
                      <div className="space-y-3">
                        <div className="p-3 rounded-2xl bg-gradient-to-tr from-cyan-950/60 to-stone-900 border border-cyan-500/30 text-center space-y-1.5">
                          <div className="w-14 h-14 rounded-full bg-gradient-to-tr from-cyan-500 to-blue-600 mx-auto flex items-center justify-center text-white text-lg font-bold shadow-lg">
                            {currentUser.displayName.charAt(0)}
                          </div>
                          <h3 className="text-sm font-bold text-white">{currentUser.displayName}</h3>
                          <div className="flex items-center justify-center gap-2 text-xs font-mono text-cyan-400">
                            <span>{currentUser.userIdTag}</span>
                            <span className="px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 text-[10px] border border-amber-500/40">
                              {currentUser.premiumRole}
                            </span>
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-2 text-center text-xs">
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                            <span className="text-[10px] text-slate-400 block">Yellow Coins</span>
                            <span className="text-base font-extrabold text-amber-400">{currentUser.yellowCoins}</span>
                          </div>
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                            <span className="text-[10px] text-slate-400 block">AI Blue Coins</span>
                            <span className="text-base font-extrabold text-cyan-400">{currentUser.blueCoins}</span>
                          </div>
                        </div>

                        {/* Subscription & ZapUPI Gateway */}
                        <div className="p-3 rounded-2xl bg-gradient-to-r from-emerald-950/60 to-studio-900 border border-emerald-500/40 space-y-2">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-2">
                              <Zap className="w-4 h-4 text-emerald-400 fill-emerald-400" />
                              <span className="text-xs font-bold text-white">ZapUPI Gateway</span>
                            </div>
                            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                              Active
                            </span>
                          </div>
                          <p className="text-[11px] text-slate-300">
                            Upgrade to Pro/VIP with instant UPI settlement. 4K 60fps, no watermark & monthly bonus coins.
                          </p>
                          <div className="flex gap-2 pt-1">
                            <button
                              onClick={() => {
                                const plan = INITIAL_SUBSCRIPTION_PLANS.find(p => p.id === 'plan-subscribed-creator') || INITIAL_SUBSCRIPTION_PLANS[1];
                                setSelectedZapUpiPlan(plan);
                                setIsZapUpiModalOpen(true);
                              }}
                              className="flex-1 py-1.5 px-2 rounded-lg bg-emerald-500 text-stone-950 font-bold text-[11px] hover:bg-emerald-400 flex items-center justify-center space-x-1 shadow-md shadow-emerald-500/20"
                            >
                              <CreditCard className="w-3.5 h-3.5" />
                              <span>Pro (₹499)</span>
                            </button>
                            <button
                              onClick={() => {
                                const plan = INITIAL_SUBSCRIPTION_PLANS.find(p => p.id === 'plan-vip') || INITIAL_SUBSCRIPTION_PLANS[3];
                                setSelectedZapUpiPlan(plan);
                                setIsZapUpiModalOpen(true);
                              }}
                              className="flex-1 py-1.5 px-2 rounded-lg bg-amber-500 text-stone-950 font-bold text-[11px] hover:bg-amber-400 flex items-center justify-center space-x-1 shadow-md shadow-amber-500/20"
                            >
                              <Crown className="w-3.5 h-3.5" />
                              <span>VIP (₹1999)</span>
                            </button>
                          </div>
                        </div>

                        <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 space-y-2 text-xs">
                          <span className="font-bold text-white block">Device Information</span>
                          <div className="flex justify-between text-[10px] text-slate-400">
                            <span>Package ID:</span>
                            <span className="font-mono text-cyan-300">com.cutmedia.app</span>
                          </div>
                          <div className="flex justify-between text-[10px] text-slate-400">
                            <span>Target SDK:</span>
                            <span className="font-mono text-slate-200">Android 15 (API 35)</span>
                          </div>
                          <div className="flex justify-between text-[10px] text-slate-400">
                            <span>Local Storage:</span>
                            <span className="font-mono text-emerald-400">Room SQLite Persistent</span>
                          </div>
                        </div>
                      </div>
                    )}

                  </div>

                  {/* Creator Bottom Navigation Bar */}
                  <div className="h-14 border-t border-stone-800/90 bg-stone-950/95 px-3 flex items-center justify-around shrink-0">
                    {[
                      { id: 'editor', label: 'Editor', icon: Scissors },
                      { id: 'projects', label: 'Projects', icon: Film },
                      { id: 'ai', label: 'AI Suite', icon: Sparkles },
                      { id: 'social', label: 'Friends', icon: Users },
                      { id: 'profile', label: 'Profile', icon: Settings }
                    ].map((tab) => {
                      const Icon = tab.icon;
                      const active = creatorBottomTab === tab.id;
                      return (
                        <button
                          key={tab.id}
                          onClick={() => setCreatorBottomTab(tab.id as any)}
                          className={`flex flex-col items-center justify-center gap-1 transition-colors ${
                            active ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                          }`}
                        >
                          <Icon className="w-4 h-4" />
                          <span className="text-[9px]">{tab.label}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* APP 2: CutMedia Admin Console (Master) */}
              {activeApp === 'admin' && (
                <div className="flex-1 flex flex-col overflow-hidden bg-stone-950 text-slate-100">
                  
                  {/* Admin App Header */}
                  <div className="px-4 py-2 border-b border-stone-800/80 flex items-center justify-between shrink-0 bg-stone-900/60">
                    <div className="flex items-center gap-2">
                      <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-amber-500 to-rose-600 flex items-center justify-center font-bold text-white text-xs">
                        🛡️
                      </div>
                      <div>
                        <h2 className="text-xs font-bold tracking-tight text-white">CutMedia Admin</h2>
                        <span className="text-[9px] text-amber-400 font-mono">com.cutmedia.admin</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 text-[10px] font-mono text-emerald-400">
                      <Radio className="w-3 h-3 animate-pulse" />
                      <span>LIVE</span>
                    </div>
                  </div>

                  {/* Admin Body by Tab */}
                  <div className="flex-1 overflow-y-auto p-3 space-y-3">
                    
                    {/* ADMIN TAB 1: Telemetry & Mission Control */}
                    {adminBottomTab === 'telemetry' && (
                      <div className="space-y-3">
                        <div className="p-3 rounded-2xl bg-gradient-to-r from-amber-950/40 to-stone-900 border border-amber-500/30 space-y-1">
                          <span className="text-[10px] text-amber-400 font-bold uppercase tracking-wider">Mission Control</span>
                          <h3 className="text-sm font-bold text-white">Founder System Status</h3>
                          <p className="text-[10px] text-slate-300">
                            Real-time Firestore snapshot listener connected. 0 latency detected.
                          </p>
                        </div>

                        <div className="grid grid-cols-2 gap-2 text-xs">
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                            <span className="text-[10px] text-slate-400">Active Mobile Nodes</span>
                            <span className="text-lg font-bold text-cyan-400 block mt-0.5">2,841</span>
                          </div>
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                            <span className="text-[10px] text-slate-400">Queue Processing</span>
                            <span className="text-lg font-bold text-emerald-400 block mt-0.5">0.14s avg</span>
                          </div>
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                            <span className="text-[10px] text-slate-400">Firewall Blocks</span>
                            <span className="text-lg font-bold text-rose-400 block mt-0.5">14 IPs</span>
                          </div>
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-stone-800">
                            <span className="text-[10px] text-slate-400">Firestore DB</span>
                            <span className="text-lg font-bold text-amber-400 block mt-0.5">Healthy</span>
                          </div>
                        </div>

                        {/* Global Kill Switch */}
                        <div className="p-3 rounded-xl bg-stone-900 border border-rose-500/30 space-y-2">
                          <div className="flex items-center justify-between">
                            <div>
                              <h4 className="text-xs font-bold text-white flex items-center gap-1.5">
                                <Power className="w-3.5 h-3.5 text-rose-400" />
                                <span>Emergency Kill-Switch</span>
                              </h4>
                              <p className="text-[10px] text-slate-400">Freeze all client rendering & uploads</p>
                            </div>
                            <button
                              onClick={() => setKillSwitchActive(!killSwitchActive)}
                              className={`w-12 h-6 rounded-full transition-colors relative ${killSwitchActive ? 'bg-rose-600' : 'bg-stone-800'}`}
                            >
                              <span className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-transform ${killSwitchActive ? 'left-7' : 'left-1'}`} />
                            </button>
                          </div>
                          {killSwitchActive && (
                            <span className="text-[10px] text-rose-400 font-bold block">
                              ⚠️ Emergency lock engaged. All public API requests paused.
                            </span>
                          )}
                        </div>
                      </div>
                    )}

                    {/* ADMIN TAB 2: Roles Station */}
                    {adminBottomTab === 'roles' && (
                      <div className="space-y-3">
                        <span className="text-xs font-bold text-white block">Assign Role to User</span>
                        
                        <div className="space-y-2">
                          <input 
                            type="text" 
                            placeholder="Enter User ID (e.g. VID-10303)"
                            value={targetUserId}
                            onChange={(e) => setTargetUserId(e.target.value)}
                            className="w-full bg-stone-900 border border-stone-800 text-xs text-white rounded-xl px-3 py-2 focus:outline-none focus:border-amber-500"
                          />

                          <div className="grid grid-cols-2 gap-2">
                            {[
                              { name: 'VIP Creator', color: 'from-amber-500 to-yellow-600' },
                              { name: 'Founder Admin', color: 'from-rose-500 to-red-600' },
                              { name: 'Moderator', color: 'from-blue-500 to-cyan-600' },
                              { name: 'Ban User', color: 'from-stone-700 to-stone-900' }
                            ].map((r) => (
                              <button
                                key={r.name}
                                onClick={() => handleAssignRole(r.name)}
                                className={`p-2 rounded-xl text-xs font-bold text-white bg-gradient-to-r ${r.color} shadow hover:opacity-90 transition-opacity`}
                              >
                                {r.name}
                              </button>
                            ))}
                          </div>
                        </div>

                        {roleAssignedMsg && (
                          <div className="p-2.5 rounded-xl bg-amber-950/80 border border-amber-500/40 text-amber-200 text-xs flex items-center gap-2">
                            <CheckCircle className="w-4 h-4 text-amber-400 shrink-0" />
                            <span>{roleAssignedMsg}</span>
                          </div>
                        )}
                      </div>
                    )}

                    {/* ADMIN TAB 3: Push Notification Broadcast */}
                    {adminBottomTab === 'broadcast' && (
                      <div className="space-y-3">
                        <span className="text-xs font-bold text-white block">Broadcast Push Alert</span>
                        <textarea 
                          rows={3}
                          placeholder="Type system alert to broadcast to all Android devices..."
                          value={broadcastMessage}
                          onChange={(e) => setBroadcastMessage(e.target.value)}
                          className="w-full bg-stone-900 border border-stone-800 text-xs text-white rounded-xl p-2.5 focus:outline-none focus:border-amber-500"
                        />

                        <button
                          onClick={handleSendBroadcast}
                          className="w-full py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-amber-500 to-rose-600 text-stone-950 shadow flex items-center justify-center gap-1.5"
                        >
                          <Send className="w-3.5 h-3.5" />
                          <span>{broadcastSent ? 'Broadcast Dispatched!' : 'Send Push to All Phones'}</span>
                        </button>
                      </div>
                    )}

                    {/* ADMIN TAB: Feature Promotion Station (Worldwide sync) */}
                    {adminBottomTab === 'promote' && (
                      <div className="space-y-3">
                        <div className="p-3 rounded-2xl bg-gradient-to-r from-amber-950/50 to-stone-900 border border-amber-500/40 space-y-1">
                          <span className="text-[10px] text-amber-400 font-bold uppercase tracking-wider">Cloud Functions Promotion</span>
                          <h4 className="text-xs font-bold text-white">Worldwide Feature Spotlight</h4>
                          <p className="text-[10px] text-slate-300">
                            Promote any feature, template, or AI tool from this admin panel. It immediately reflects worldwide across all active devices and apps.
                          </p>
                        </div>

                        {/* Currently Active Worldwide Promotion */}
                        {worldwidePromotion && (
                          <div className="p-2.5 rounded-xl bg-stone-900 border border-amber-400/40 space-y-1.5">
                            <div className="flex items-center justify-between">
                              <span className="text-[9px] font-black text-amber-300 bg-amber-500/20 px-1.5 py-0.5 rounded">
                                ACTIVE WORLDWIDE
                              </span>
                              <button
                                onClick={() => {
                                  setWorldwidePromotion(null);
                                  setSaveToast('Feature promotion deactivated worldwide.');
                                }}
                                className="text-[9px] text-rose-400 hover:text-rose-300 font-bold"
                              >
                                End Promotion
                              </button>
                            </div>
                            <p className="text-xs font-bold text-white">{worldwidePromotion.title}</p>
                            <p className="text-[10px] text-emerald-400 font-medium">{worldwidePromotion.perk}</p>
                          </div>
                        )}

                        <span className="text-xs font-bold text-white block pt-1">One-Click Worldwide Feature Promotions</span>
                        <div className="space-y-2">
                          {[
                            { id: 'promo-ai-captions', title: 'AI Auto-Captions & Subtitle Sync', perk: '0 Blue Coins • Free Unlocked', tab: 'ai' },
                            { id: 'promo-4k-upscale', title: '4K HDR AI Upscaler & Clarity', perk: '50% Off (1 Blue Coin)', tab: 'ai' },
                            { id: 'promo-neon-template', title: 'Cyberpunk Neon Reel 2077', perk: 'Free Template Export Included', tab: 'projects' },
                            { id: 'promo-diwali-reel', title: 'Diwali Festive Lights Reel', perk: 'Global Festival Spotlight', tab: 'projects' },
                            { id: 'promo-2x-coins', title: '2X Daily Coin Multiplier Event', perk: 'Double Coins for All Creators', tab: 'editor' }
                          ].map((item) => (
                            <div key={item.id} className="p-2.5 rounded-xl bg-stone-900 border border-stone-800 flex items-center justify-between">
                              <div className="space-y-0.5 pr-2">
                                <span className="text-xs font-bold text-white block">{item.title}</span>
                                <span className="text-[10px] text-amber-400 font-medium">{item.perk}</span>
                              </div>
                              <button
                                onClick={() => {
                                  const promoObj = {
                                    id: item.id,
                                    title: item.title,
                                    badge: 'WORLDWIDE SPOTLIGHT',
                                    perk: item.perk,
                                    description: `Promoted by Master Admin worldwide`,
                                    targetTab: item.tab
                                  };
                                  setWorldwidePromotion(promoObj);
                                  syncPromotedFeatureToFirestore({
                                    id: item.id,
                                    title: item.title,
                                    category: item.tab === 'ai' ? 'AI' : 'TEMPLATE',
                                    description: `Promoted by Master Admin worldwide`,
                                    badgeText: 'WORLDWIDE SPOTLIGHT',
                                    discountOrBonus: item.perk,
                                    targetScreen: item.tab === 'ai' ? 'AI_STUDIO' : 'PROJECTS',
                                    isActive: true
                                  });
                                  setPhoneNotifs(prev => [
                                    {
                                      id: `notif-${Date.now()}`,
                                      title: `🌟 Worldwide Promotion: ${item.title}`,
                                      message: `${item.title} is now promoted with ${item.perk}!`,
                                      type: 'CAMPAIGN',
                                      time: 'Just now',
                                      unread: true
                                    },
                                    ...prev
                                  ]);
                                  setSaveToast(`🚀 "${item.title}" Promoted Worldwide! Live on all devices.`);
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

                    {/* ADMIN TAB 4: Security & Audit */}
                    {adminBottomTab === 'security' && (
                      <div className="space-y-2">
                        <span className="text-xs font-bold text-white block">Recent Security Incidents</span>
                        {[
                          { ip: '103.21.244.12', action: 'DDoS Burst Prevented', time: '1m ago', color: 'text-rose-400' },
                          { ip: '192.168.1.84', action: 'Dual-Account Limit Reached', time: '4m ago', color: 'text-amber-400' },
                          { ip: '185.199.108.153', action: 'Tampered APK Check Blocked', time: '12m ago', color: 'text-rose-400' }
                        ].map((log, idx) => (
                          <div key={idx} className="p-2 rounded-xl bg-stone-900 border border-stone-800 text-[10px] flex items-center justify-between">
                            <div>
                              <span className={`font-bold block ${log.color}`}>{log.action}</span>
                              <span className="text-slate-500 font-mono">{log.ip}</span>
                            </div>
                            <span className="text-slate-500">{log.time}</span>
                          </div>
                        ))}
                      </div>
                    )}

                  </div>

                  {/* Admin Bottom Navigation Bar */}
                  <div className="h-14 border-t border-stone-800/90 bg-stone-950/95 px-3 flex items-center justify-around shrink-0">
                    {[
                      { id: 'promote', label: 'Promote', icon: Sparkles },
                      { id: 'telemetry', label: 'Telemetry', icon: Radio },
                      { id: 'roles', label: 'Roles', icon: Crown },
                      { id: 'broadcast', label: 'Broadcast', icon: Send },
                      { id: 'security', label: 'Security', icon: ShieldAlert }
                    ].map((tab) => {
                      const Icon = tab.icon;
                      const active = adminBottomTab === tab.id;
                      return (
                        <button
                          key={tab.id}
                          onClick={() => setAdminBottomTab(tab.id as any)}
                          className={`flex flex-col items-center justify-center gap-1 transition-colors ${
                            active ? 'text-amber-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                          }`}
                        >
                          <Icon className="w-4 h-4" />
                          <span className="text-[9px]">{tab.label}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Android System Navigation Gesture Bar */}
              <div className="h-4 bg-stone-950 flex items-center justify-center shrink-0">
                <div className="w-28 h-1 rounded-full bg-slate-600 opacity-70" />
              </div>

            </div>
          </div>
        </div>

        {/* Right Feature Panel: Architecture & Quick Actions (7 cols on lg) */}
        <div className="lg:col-span-6 xl:col-span-7 space-y-4">
          
          {/* App Switcher Highlights */}
          <div className="p-5 rounded-2xl bg-studio-850/90 border border-studio-750 shadow-md space-y-3">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Tv className="w-4 h-4 text-cyan-400" />
              <span>Simulated Native Architecture</span>
            </h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              You are viewing the live interactive simulation of CutMedia's Jetpack Compose codebase. Use the phone controls on the left to test video trimming, aspect ratios, filter color matrices, AI generations, and administrative security rules.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <div 
                onClick={() => setActiveApp('creator')}
                className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                  activeApp === 'creator'
                    ? 'bg-cyan-950/30 border-cyan-500/60 shadow-sm ring-1 ring-cyan-500/30'
                    : 'bg-studio-900 border-studio-800 hover:border-studio-700'
                }`}
              >
                <div className="flex items-center gap-2 mb-1.5">
                  <Scissors className="w-4 h-4 text-cyan-400" />
                  <span className="text-xs font-bold text-white">CutMedia Video Editor</span>
                </div>
                <p className="text-[11px] text-slate-400">
                  Zero-permission photo picker, timeline trimming, speed ramping, color grading, AI Studio, Room persistence.
                </p>
                <span className="inline-block mt-2 text-[10px] font-mono text-cyan-400 font-semibold">
                  Package: com.cutmedia.app
                </span>
              </div>

              <div 
                onClick={() => setActiveApp('admin')}
                className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                  activeApp === 'admin'
                    ? 'bg-amber-950/30 border-amber-500/60 shadow-sm ring-1 ring-amber-500/30'
                    : 'bg-studio-900 border-studio-800 hover:border-studio-700'
                }`}
              >
                <div className="flex items-center gap-2 mb-1.5">
                  <ShieldCheck className="w-4 h-4 text-amber-400" />
                  <span className="text-xs font-bold text-white">CutMedia Admin Console</span>
                </div>
                <p className="text-[11px] text-slate-400">
                  Master mission control, role assigner station, emergency kill-switch, push broadcaster, and security firewall.
                </p>
                <span className="inline-block mt-2 text-[10px] font-mono text-amber-400 font-semibold">
                  Package: com.cutmedia.admin
                </span>
              </div>
            </div>
          </div>

          {/* Quick Technical Specs & Download Card */}
          <div className="p-5 rounded-2xl bg-gradient-to-br from-studio-850 to-studio-900 border border-studio-750 shadow-md space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-sm font-bold text-white">Install on Android Hardware</h4>
                <p className="text-xs text-slate-400">Compiled production binaries ready for physical phones</p>
              </div>
              <span className="px-2.5 py-1 rounded-full text-[11px] font-mono font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                v2.4.0 APK Ready
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="p-3 rounded-xl bg-studio-900/90 border border-studio-800">
                <span className="text-[11px] text-slate-400 block mb-0.5">Dual Launcher</span>
                <span className="text-xs font-bold text-white">2 Icons on Home Screen</span>
              </div>
              <div className="p-3 rounded-xl bg-studio-900/90 border border-studio-800">
                <span className="text-[11px] text-slate-400 block mb-0.5">Firebase Project</span>
                <span className="text-xs font-bold text-cyan-400 font-mono truncate block">ai-studio-vidflowpro</span>
              </div>
              <div className="p-3 rounded-xl bg-studio-900/90 border border-studio-800">
                <span className="text-[11px] text-slate-400 block mb-0.5">Minimum Android</span>
                <span className="text-xs font-bold text-white">Android 8.0+ (Oreo - 15)</span>
              </div>
              <div className="p-3 rounded-xl bg-studio-900/90 border border-studio-800">
                <span className="text-[11px] text-slate-400 block mb-0.5">Architecture</span>
                <span className="text-xs font-bold text-emerald-400">Jetpack Compose M3</span>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-2 pt-1">
              <button
                onClick={onOpenDownloadApkModal}
                className="flex-1 py-2.5 px-4 rounded-xl text-xs font-bold bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-stone-950 flex items-center justify-center gap-2 shadow-md transition-all"
              >
                <Download className="w-4 h-4" />
                <span>Download APK Packages</span>
              </button>

              <button
                onClick={onOpenFirebaseModal}
                className="py-2.5 px-4 rounded-xl text-xs font-bold bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center justify-center gap-1.5 transition-all"
              >
                <Database className="w-4 h-4" />
                <span>Live Firestore Status</span>
              </button>
            </div>
          </div>

        </div>

      </div>

      {/* Export Studio Modal */}
      {isExportModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-in fade-in">
          <div className={`bg-stone-900 border border-stone-700 w-full ${(isExporting || exportComplete) ? 'max-w-xl' : 'max-w-md'} rounded-3xl p-6 shadow-2xl space-y-4`}>
            <div className="flex items-center justify-between border-b border-stone-800 pb-3">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-gradient-to-br from-cyan-500/20 to-blue-500/10 text-cyan-400 rounded-xl border border-cyan-500/30">
                  <Download className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base">CineCut Render Engine</h3>
                  <span className="text-[10px] text-cyan-400 font-mono">Hardware Accelerated 1080p / 4K / 8K</span>
                </div>
              </div>
              <button 
                onClick={() => setIsExportModalOpen(false)} 
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {!isExporting && !exportComplete && (
              <div className="space-y-3 text-xs">
                <div>
                  <label className="text-slate-400 block mb-1">Project Name</label>
                  <input
                    type="text"
                    value={projectTitle}
                    onChange={(e) => setProjectTitle(e.target.value)}
                    className="w-full bg-stone-950 border border-stone-800 rounded-xl px-3 py-2 text-white font-semibold"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-slate-400 block mb-1">Export Resolution</label>
                    <div className="grid grid-cols-2 gap-1.5">
                      {(['720p', '1080p', '4K', '8K'] as const).map((res) => (
                        <button
                          key={res}
                          onClick={() => setExportResolution(res)}
                          className={`py-1.5 rounded-lg text-xs font-bold transition-all border ${
                            exportResolution === res
                              ? 'bg-cyan-500 text-stone-950 border-cyan-400 shadow'
                              : 'bg-stone-950 text-slate-300 border-stone-800 hover:border-stone-700'
                          }`}
                        >
                          {res}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="text-slate-400 block mb-1">Framerate</label>
                    <div className="grid grid-cols-3 gap-1">
                      {([24, 30, 60] as const).map((fps) => (
                        <button
                          key={fps}
                          onClick={() => setExportFps(fps)}
                          className={`py-1.5 rounded-lg text-xs font-bold transition-all border ${
                            exportFps === fps
                              ? 'bg-amber-400 text-stone-950 border-amber-300 shadow'
                              : 'bg-stone-950 text-slate-300 border-stone-800 hover:border-stone-700'
                          }`}
                        >
                          {fps}fps
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-stone-950 border border-stone-800 space-y-1 text-[11px] text-slate-300">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Aspect Ratio:</span>
                    <span className="font-mono text-cyan-400">{selectedAspect}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Duration:</span>
                    <span className="font-mono text-white">{(trimEnd - trimStart).toFixed(1)}s</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Color Grade:</span>
                    <span className="font-mono text-amber-300">{selectedFilter} LUT</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Timeline Tracks:</span>
                    <span className="font-mono text-purple-300">{timelineClips.length} Clips + Audio + Captions</span>
                  </div>
                </div>

                <div className="flex items-center justify-end space-x-2 pt-3 border-t border-stone-800">
                  <button
                    onClick={() => setIsExportModalOpen(false)}
                    className="px-4 py-2 rounded-xl bg-stone-800 text-slate-300 hover:bg-stone-750"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleExecuteExport}
                    className="px-5 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-stone-950 font-extrabold shadow-lg"
                  >
                    Start Rendering
                  </button>
                </div>
              </div>
            )}

            {(isExporting || exportComplete) && (
              <ExportProgressView
                progress={exportProgress}
                isExporting={isExporting}
                isComplete={exportComplete}
                projectTitle={projectTitle}
                resolution={exportResolution}
                fps={exportFps}
                aspectRatio={selectedAspect}
                totalDurationSeconds={parseFloat((trimEnd - trimStart).toFixed(1)) || 15.0}
                onPauseToggle={handlePauseExport}
                onCancel={handleCancelExport}
                onDone={() => {
                  setIsExportModalOpen(false);
                  setExportComplete(false);
                  setIsExporting(false);
                }}
                onDownload={() => {
                  const a = document.createElement('a');
                  a.href = '/CineCut-Release-v2.4.0.apk';
                  a.download = `${projectTitle}_${exportResolution}_${exportFps}fps.mp4`;
                  a.click();
                }}
              />
            )}
          </div>
        </div>
      )}

      {/* ZapUPI Payment Checkout Modal */}
      <ZapUpiPaymentModal
        isOpen={isZapUpiModalOpen}
        onClose={() => setIsZapUpiModalOpen(false)}
        plan={selectedZapUpiPlan}
        currentUser={currentUser}
        onPaymentSuccess={(plan, txn) => {
          if (onSubscribeSuccess) {
            onSubscribeSuccess(plan, txn);
          } else {
            alert(`🎉 Success! Paid ₹${txn.amount} via ZapUPI (${txn.gateway}). ${plan.name} is now active!`);
          }
        }}
      />
    </div>
  );
};
