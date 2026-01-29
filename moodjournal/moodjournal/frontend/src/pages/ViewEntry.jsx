import { motion } from 'framer-motion';
import {
    AlertCircle,
    ArrowLeft,
    BedDouble,
    Calendar,
    Flame,
    Loader2,
    RefreshCw,
    Trash2,
    Zap
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../lib/axios';

// Mood color mapping
const moodColors = {
  HAPPY: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  SAD: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  ANGRY: 'bg-red-500/20 text-red-400 border-red-500/30',
  CALM: 'bg-teal-500/20 text-teal-400 border-teal-500/30',
  ANXIOUS: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
  ENERGETIC: 'bg-pink-500/20 text-pink-400 border-pink-500/30',
  CONTENT: 'bg-green-500/20 text-green-400 border-green-500/30',
  EXCITED: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
  NEUTRAL: 'bg-gray-500/20 text-gray-400 border-gray-500/30',
};

const moodEmojis = {
  HAPPY: '😊',
  SAD: '😢',
  ANGRY: '😠',
  CALM: '😌',
  ANXIOUS: '😰',
  ENERGETIC: '⚡',
  CONTENT: '☺️',
  EXCITED: '🎉',
  NEUTRAL: '😐',
};

const CONTEXT_TAGS = {
  work: { label: 'Work', icon: '💼' },
  family: { label: 'Family', icon: '👨‍👩‍👧' },
  health: { label: 'Health', icon: '🏥' },
  relationships: { label: 'Relationships', icon: '💕' },
  self: { label: 'Self', icon: '🪞' },
  money: { label: 'Finances', icon: '💰' },
  academic: { label: 'Academic', icon: '📚' },
  social: { label: 'Social', icon: '👥' },
};

const ViewEntry = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [entry, setEntry] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchEntry = async () => {
      try {
        console.log('[ViewEntry] Fetching entry:', id);
        const res = await api.get(`/journal/${id}`);
        console.log('[ViewEntry] Response:', res.data);
        setEntry(res.data);
        setError(null);
      } catch (err) {
        console.error("[ViewEntry] Failed to fetch entry:", err);
        setError(err.response?.data?.message || err.message || 'Failed to load entry');
      } finally {
        setLoading(false);
      }
    };
    fetchEntry();
  }, [id]);

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this entry?')) return;
    try {
      await api.delete(`/journal/${id}`);
      navigate('/journal');
    } catch (error) {
      console.error("Failed to delete entry", error);
      alert('Failed to delete entry');
    }
  };

  const handleReanalyze = async () => {
    try {
      const res = await api.post(`/journal/${id}/reanalyze`);
      setEntry(res.data);
      console.log('Reanalyzed entry:', res.data);
    } catch (error) {
      console.error("Failed to reanalyze entry", error);
      alert('Failed to reanalyze. Check console for details.');
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="animate-spin text-primary-DEFAULT" size={40} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col h-full items-center justify-center gap-4">
        <AlertCircle className="text-red-400" size={48} />
        <p className="text-red-400 text-lg">{error}</p>
        <Link 
          to="/journal" 
          className="bg-primary-DEFAULT/20 text-primary-DEFAULT px-6 py-2 rounded-lg hover:bg-primary-DEFAULT/30"
        >
          Back to Journal
        </Link>
      </div>
    );
  }

  if (!entry) {
    return (
      <div className="flex flex-col h-full items-center justify-center gap-4">
        <AlertCircle className="text-yellow-400" size={48} />
        <p className="text-yellow-400 text-lg">Entry not found</p>
        <Link 
          to="/journal" 
          className="bg-primary-DEFAULT/20 text-primary-DEFAULT px-6 py-2 rounded-lg hover:bg-primary-DEFAULT/30"
        >
          Back to Journal
        </Link>
      </div>
    );
  }

  // Parse context tags
  let contextTags = [];
  try {
    if (entry.contextTags) {
      contextTags = typeof entry.contextTags === 'string' 
        ? JSON.parse(entry.contextTags) 
        : entry.contextTags;
    }
  } catch { contextTags = []; }

  // Parse suggestions
  let suggestions = [];
  try {
    if (entry.suggestions) {
      const parsed = JSON.parse(entry.suggestions);
      suggestions = Array.isArray(parsed) ? parsed : [entry.suggestions];
    }
  } catch { 
    if (entry.suggestions) suggestions = [entry.suggestions]; 
  }

  // Parse nuance tags
  let nuanceTags = [];
  try {
    if (entry.nuanceTags) {
      nuanceTags = typeof entry.nuanceTags === 'string' 
        ? JSON.parse(entry.nuanceTags) 
        : entry.nuanceTags;
    }
  } catch { nuanceTags = []; }

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex items-center justify-between">
        <Link 
          to="/journal" 
          className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
        >
          <ArrowLeft size={20} />
          <span>Back to Journal</span>
        </Link>
        
        <div className="flex items-center gap-2">
          <button
            onClick={handleReanalyze}
            className="flex items-center gap-2 px-4 py-2 text-emerald-400 hover:bg-emerald-500/10 rounded-lg transition-colors border border-emerald-500/30"
          >
            <RefreshCw size={18} />
            Re-analyze
          </button>
          <button
            onClick={handleDelete}
            className="flex items-center gap-2 px-4 py-2 text-red-400 hover:bg-red-500/10 rounded-lg transition-colors border border-red-500/30"
          >
            <Trash2 size={18} />
            Delete
          </button>
        </div>
      </div>

      {/* Main Entry Card */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-dark-card border border-white/10 rounded-2xl p-8 shadow-xl"
      >
        {/* Title & Mood */}
        <div className="flex items-start justify-between gap-4 mb-6">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">{entry.title}</h1>
            <div className="flex items-center gap-2 text-gray-500">
              <Calendar size={16} />
              <span>{formatDate(entry.createdAt)}</span>
            </div>
          </div>
          <span className={`px-4 py-2 rounded-full text-lg font-medium border ${moodColors[entry.mood] || 'bg-gray-500/20 text-gray-400 border-gray-500/30'}`}>
            {moodEmojis[entry.mood]} {entry.mood}
          </span>
        </div>

        {/* Quick Check-in Stats */}
        {(entry.stressLevel || entry.energyLevel || entry.sleepQuality) && (
          <div className="grid grid-cols-3 gap-4 mb-6 p-4 bg-dark-bg/50 rounded-xl border border-white/5">
            {entry.stressLevel && (
              <div className="text-center">
                <Flame className="mx-auto text-red-400 mb-2" size={24} />
                <div className="text-2xl font-bold text-white">{entry.stressLevel}/10</div>
                <div className="text-xs text-gray-500">Stress Level</div>
              </div>
            )}
            {entry.energyLevel && (
              <div className="text-center">
                <Zap className="mx-auto text-yellow-400 mb-2" size={24} />
                <div className="text-2xl font-bold text-white">{entry.energyLevel}/10</div>
                <div className="text-xs text-gray-500">Energy Level</div>
              </div>
            )}
            {entry.sleepQuality && (
              <div className="text-center">
                <BedDouble className="mx-auto text-blue-400 mb-2" size={24} />
                <div className="text-2xl font-bold text-white">
                  {'★'.repeat(entry.sleepQuality)}{'☆'.repeat(5 - entry.sleepQuality)}
                </div>
                <div className="text-xs text-gray-500">Sleep Quality</div>
              </div>
            )}
          </div>
        )}

        {/* Context Tags */}
        {contextTags.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-6">
            {contextTags.map(tagId => {
              const tag = CONTEXT_TAGS[tagId];
              if (!tag) return null;
              return (
                <span 
                  key={tagId}
                  className="px-3 py-1.5 rounded-lg bg-primary-DEFAULT/10 border border-primary-DEFAULT/30 text-primary-DEFAULT text-sm"
                >
                  {tag.icon} {tag.label}
                </span>
              );
            })}
          </div>
        )}

        {/* Trigger Description */}
        {entry.triggerDescription && (
          <div className="mb-6 p-4 bg-amber-500/5 rounded-xl border border-amber-500/20">
            <span className="text-xs text-amber-400 font-medium">Trigger:</span>
            <p className="text-gray-300 mt-1">{entry.triggerDescription}</p>
          </div>
        )}

        {/* Main Content */}
        <div className="mb-8">
          <h3 className="text-lg font-medium text-gray-400 mb-3">Journal Entry</h3>
          <p className="text-white text-lg leading-relaxed whitespace-pre-wrap">{entry.content}</p>
        </div>

        {/* Analysis Section */}
        {(entry.detailedAnalysis || entry.cognitiveDistortions || entry.riskScore || entry.primaryEmotion || entry.vadScores) && (
          <div className="border-t border-white/10 pt-6 space-y-6">
            <h3 className="text-lg font-medium text-gray-400">AI Analysis</h3>

            {/* Risk Score & Trajectory */}
            <div className="flex items-center gap-4 flex-wrap">
              {entry.riskScore !== undefined && entry.riskScore !== null && (
                <div className={`px-4 py-2 rounded-lg border ${
                  entry.riskScore <= 3 ? 'bg-green-500/20 text-green-400 border-green-500/30' :
                  entry.riskScore <= 6 ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' :
                  'bg-red-500/20 text-red-400 border-red-500/30'
                }`}>
                  <span className="text-sm font-medium">Risk Score:</span>
                  <span className="ml-2 text-xl font-bold">{entry.riskScore}/10</span>
                </div>
              )}
              {entry.emotionalTrajectory && (
                <div className={`px-4 py-2 rounded-lg border ${
                  entry.emotionalTrajectory === 'improving' ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30' :
                  entry.emotionalTrajectory === 'declining' ? 'bg-orange-500/20 text-orange-400 border-orange-500/30' :
                  'bg-gray-500/20 text-gray-400 border-gray-500/30'
                }`}>
                  📈 Trajectory: <span className="font-bold">{entry.emotionalTrajectory}</span>
                </div>
              )}
              {entry.analysisEmotion && (
                <div className="px-4 py-2 rounded-lg bg-primary-DEFAULT/10 border border-primary-DEFAULT/20 text-primary-DEFAULT">
                  Feeling: <span className="font-bold">{entry.analysisEmotion}</span>
                </div>
              )}
            </div>

            {/* Primary Emotion & Nuance Tags */}
            {(entry.primaryEmotion || nuanceTags.length > 0) && (
              <div className="flex items-center gap-3 flex-wrap">
                {entry.primaryEmotion && (
                  <span className="px-3 py-1.5 rounded-lg text-sm font-bold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                    🎭 Primary: {entry.primaryEmotion}
                  </span>
                )}
                {nuanceTags.map((tag, i) => (
                  <span key={i} className="px-3 py-1 rounded-full text-sm bg-purple-500/20 text-purple-400 border border-purple-500/30">
                    {tag}
                  </span>
                ))}
              </div>
            )}

            {/* VAD Scores */}
            {entry.vadScores && (
              <div className="flex items-center gap-6 p-4 bg-dark-bg/50 rounded-xl">
                <span className="text-gray-500 text-sm">VAD Analysis:</span>
                <div className={`px-3 py-1.5 rounded-lg ${
                  entry.vadScores.valence >= 0.6 ? 'bg-green-500/20 text-green-400' :
                  entry.vadScores.valence <= 0.4 ? 'bg-red-500/20 text-red-400' :
                  'bg-gray-500/20 text-gray-400'
                }`}>
                  <span className="text-xs">Valence:</span>
                  <span className="ml-1 font-bold">{(entry.vadScores.valence * 100).toFixed(0)}%</span>
                </div>
                <div className={`px-3 py-1.5 rounded-lg ${
                  entry.vadScores.arousal >= 0.7 ? 'bg-orange-500/20 text-orange-400' :
                  'bg-gray-500/20 text-gray-400'
                }`}>
                  <span className="text-xs">Arousal:</span>
                  <span className="ml-1 font-bold">{(entry.vadScores.arousal * 100).toFixed(0)}%</span>
                </div>
                <div className={`px-3 py-1.5 rounded-lg ${
                  entry.vadScores.dominance >= 0.6 ? 'bg-blue-500/20 text-blue-400' :
                  entry.vadScores.dominance <= 0.4 ? 'bg-amber-500/20 text-amber-400' :
                  'bg-gray-500/20 text-gray-400'
                }`}>
                  <span className="text-xs">Dominance:</span>
                  <span className="ml-1 font-bold">{(entry.vadScores.dominance * 100).toFixed(0)}%</span>
                </div>
              </div>
            )}

            {/* Cognitive Distortions */}
            {entry.cognitiveDistortions && (
              <div className="p-4 bg-amber-500/5 rounded-xl border border-amber-500/20">
                <span className="text-sm text-amber-400 font-medium block mb-2">⚠️ Cognitive Distortions Detected:</span>
                <div className="flex flex-wrap gap-2">
                  {entry.cognitiveDistortions.split(',').map((d, i) => (
                    <span key={i} className="px-3 py-1 rounded-full text-sm bg-amber-500/20 text-amber-400 border border-amber-500/30">
                      {d.trim()}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Suggestions */}
            {suggestions.length > 0 && (
              <div className="p-4 bg-primary-DEFAULT/5 rounded-xl border-l-4 border-primary-DEFAULT/50">
                <span className="text-sm text-primary-DEFAULT font-medium block mb-2">💡 Suggestions</span>
                <ul className="space-y-2">
                  {suggestions.map((suggestion, i) => (
                    <li key={i} className="text-gray-300 flex items-start gap-2">
                      <span className="text-primary-DEFAULT">•</span>
                      {suggestion}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Detailed Analysis */}
            {entry.detailedAnalysis && (
              <div className="p-4 bg-white/5 rounded-xl border-l-4 border-gray-500/50">
                <span className="text-sm text-gray-400 font-medium block mb-2">📝 Detailed Analysis</span>
                <p className="text-gray-300 italic leading-relaxed">{entry.detailedAnalysis}</p>
              </div>
            )}
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default ViewEntry;
