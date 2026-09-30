import React, { useState } from 'react';
import { 
  Sparkles, 
  X, 
  Layers, 
  Film, 
  Palette, 
  Type, 
  Clock, 
  Tag, 
  CheckCircle2, 
  Sliders
} from 'lucide-react';
import { VideoTemplate, User } from '../types';
import { syncTemplateToFirestore } from '../lib/firebase';

interface CreateTemplateModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User;
  currentSettings: {
    aspectRatio: string;
    filter: string;
    speed: number;
    captionStyle: string;
    duration: number;
  };
  onTemplateCreated: (newTemplate: any) => void;
}

export const CreateTemplateModal: React.FC<CreateTemplateModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  currentSettings,
  onTemplateCreated
}) => {
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState<'Reels' | 'TikTok' | 'Cinematic' | 'Vlog' | 'Festival'>('Reels');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('trending, viral, aesthetic');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    setIsSubmitting(true);
    const newTpl = {
      id: `tpl-${Date.now()}`,
      title: title.trim(),
      category,
      creatorId: currentUser.id,
      creatorName: currentUser.displayName || currentUser.username,
      aspectRatio: currentSettings.aspectRatio,
      filter: currentSettings.filter,
      speed: currentSettings.speed,
      captionStyle: currentSettings.captionStyle,
      duration: currentSettings.duration,
      description: description.trim() || `User-created template featuring ${currentSettings.filter} filter and ${currentSettings.aspectRatio} framing.`,
      tags: tags.split(',').map(t => t.trim()).filter(Boolean),
      likes: 1,
      uses: 0,
      status: 'approved' as const,
      createdAt: new Date().toISOString()
    };

    try {
      await syncTemplateToFirestore(newTpl as any);
      onTemplateCreated(newTpl);
      setSuccessMsg(`Template "${newTpl.title}" published successfully!`);
      setTimeout(() => {
        onClose();
      }, 1000);
    } catch (err) {
      console.error('Failed to create template:', err);
      // Still add locally
      onTemplateCreated(newTpl);
      onClose();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-stone-900 border border-stone-800 w-full max-w-md rounded-3xl overflow-hidden shadow-2xl relative">
        {/* Header */}
        <div className="px-6 pt-6 pb-4 border-b border-stone-800 bg-gradient-to-r from-cyan-950/40 to-stone-900 flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white tracking-tight">Create Creator Template</h3>
              <p className="text-[11px] text-slate-400">Package active project settings into a reusable preset</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-stone-800 hover:bg-stone-750 flex items-center justify-center text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          {successMsg && (
            <div className="p-3 rounded-2xl bg-emerald-950/70 border border-emerald-500/50 text-emerald-200 text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Current Edit Preset Preview */}
          <div className="p-3 rounded-2xl bg-stone-950 border border-stone-800 space-y-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400">Captured Active Settings</span>
            <div className="grid grid-cols-3 gap-2 text-[11px]">
              <div className="p-2 rounded-xl bg-stone-900 border border-stone-800/80">
                <span className="text-slate-500 block text-[9px]">Aspect Ratio</span>
                <span className="font-bold text-white font-mono">{currentSettings.aspectRatio}</span>
              </div>
              <div className="p-2 rounded-xl bg-stone-900 border border-stone-800/80">
                <span className="text-slate-500 block text-[9px]">Filter Style</span>
                <span className="font-bold text-amber-300">{currentSettings.filter}</span>
              </div>
              <div className="p-2 rounded-xl bg-stone-900 border border-stone-800/80">
                <span className="text-slate-500 block text-[9px]">Speed</span>
                <span className="font-bold text-emerald-400 font-mono">{currentSettings.speed}x</span>
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label className="text-[11px] font-bold text-slate-300 block mb-1">Template Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Golden Sunset Viral Reel"
                className="w-full bg-stone-950 border border-stone-800 rounded-xl px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="text-[11px] font-bold text-slate-300 block mb-1">Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value as any)}
                  className="w-full bg-stone-950 border border-stone-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-cyan-400"
                >
                  <option value="Reels">Instagram Reels</option>
                  <option value="TikTok">TikTok Trend</option>
                  <option value="Cinematic">Cinematic</option>
                  <option value="Vlog">Daily Vlog</option>
                  <option value="Festival">Festive Lights</option>
                </select>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-300 block mb-1">Tags</label>
                <input
                  type="text"
                  value={tags}
                  onChange={(e) => setTags(e.target.value)}
                  placeholder="reels, aesthetic"
                  className="w-full bg-stone-950 border border-stone-800 rounded-xl px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                />
              </div>
            </div>

            <div>
              <label className="text-[11px] font-bold text-slate-300 block mb-1">Description (Optional)</label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Give creators tips on how to shoot for this template..."
                className="w-full bg-stone-950 border border-stone-800 rounded-xl px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-400"
              />
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-xl bg-stone-800 hover:bg-stone-750 text-slate-300 font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting || !title.trim()}
                className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-stone-950 font-bold text-xs shadow-lg shadow-cyan-500/20 disabled:opacity-50 transition-all"
              >
                {isSubmitting ? 'Publishing...' : 'Save & Publish Template'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
