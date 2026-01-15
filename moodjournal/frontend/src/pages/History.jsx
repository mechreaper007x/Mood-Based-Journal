import { motion } from 'framer-motion';
import { AlertCircle, BookOpen, Calendar, ChevronRight, Loader2, RefreshCw, Search, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
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

const History = () => {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterMood, setFilterMood] = useState('ALL');

  useEffect(() => {
    const fetchEntries = async () => {
      try {
        console.log('[History] Fetching entries...');
        const res = await api.get('/journal/me');
        console.log('[History] Response:', res.data);
        // Sort by createdAt descending (newest first)
        const sorted = (res.data || []).sort((a, b) => 
          new Date(b.createdAt) - new Date(a.createdAt)
        );
        setEntries(sorted);
        setError(null);
      } catch (err) {
        console.error("[History] Failed to fetch entries:", err);
        console.error("[History] Error response:", err.response?.data);
        setError(err.response?.data?.message || err.message || 'Failed to load entries');
      } finally {
        setLoading(false);
      }
    };
    fetchEntries();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this entry?')) return;
    try {
      await api.delete(`/journal/${id}`);
      setEntries(entries.filter(e => e.id !== id));
    } catch (error) {
      console.error("Failed to delete entry", error);
    }
  };

  const handleReanalyze = async (id) => {
    try {
      const res = await api.post(`/journal/${id}/reanalyze`);
      // Update the entry in state with new mood
      setEntries(entries.map(e => e.id === id ? res.data : e));
      console.log('Reanalyzed entry:', res.data);
    } catch (error) {
      console.error("Failed to reanalyze entry", error);
      alert('Failed to reanalyze. Check console for details.');
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const filteredEntries = entries.filter(entry => {
    const matchesSearch = entry.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          entry.content.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesMood = filterMood === 'ALL' || entry.mood === filterMood;
    return matchesSearch && matchesMood;
  });

  const uniqueMoods = [...new Set(entries.map(e => e.mood))];

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
        <button 
          onClick={() => window.location.reload()} 
          className="bg-primary-DEFAULT/20 text-primary-DEFAULT px-6 py-2 rounded-lg hover:bg-primary-DEFAULT/30"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold text-white flex items-center gap-3">
            <BookOpen className="text-primary-DEFAULT" />
            My Journal
          </h2>
          <p className="text-gray-400 mt-1">{entries.length} entries recorded</p>
        </div>
        <Link
          to="/entry"
          className="bg-primary-gradient text-white px-6 py-3 rounded-xl font-bold hover:opacity-90 transition-all shadow-lg shadow-purple-500/20 text-center"
        >
          + New Entry
        </Link>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        {/* Search */}
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" size={20} />
          <input
            type="text"
            placeholder="Search entries..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-dark-card border border-white/10 rounded-xl pl-12 pr-4 py-3 text-white placeholder-gray-500 focus:border-primary-DEFAULT outline-none transition-colors"
          />
        </div>
        
        {/* Mood Filter */}
        <select
          value={filterMood}
          onChange={(e) => setFilterMood(e.target.value)}
          className="bg-dark-card border border-white/10 rounded-xl px-4 py-3 text-white focus:border-primary-DEFAULT outline-none cursor-pointer min-w-[150px]"
        >
          <option value="ALL">All Moods</option>
          {uniqueMoods.map(mood => (
            <option key={mood} value={mood}>{moodEmojis[mood]} {mood}</option>
          ))}
        </select>
      </div>

      {/* Entries List */}
      {filteredEntries.length === 0 ? (
        <div className="bg-dark-card border border-white/5 rounded-2xl p-12 text-center">
          <BookOpen className="mx-auto text-gray-600 mb-4" size={48} />
          <p className="text-gray-400 text-lg">
            {entries.length === 0 
              ? "No journal entries yet. Start writing!" 
              : "No entries match your search."}
          </p>
          {entries.length === 0 && (
            <Link
              to="/entry"
              className="inline-block mt-4 bg-primary-DEFAULT/20 text-primary-DEFAULT px-6 py-2 rounded-lg hover:bg-primary-DEFAULT/30 transition-colors"
            >
              Write your first entry
            </Link>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredEntries.map((entry, index) => (
            <motion.div
              key={entry.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              className="bg-dark-card border border-white/5 rounded-2xl p-6 hover:border-white/10 transition-all group"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  {/* Title & Mood Badge */}
                  <div className="flex items-center gap-3 flex-wrap">
                    <h3 className="text-xl font-semibold text-white truncate">{entry.title}</h3>
                    <span className={`px-3 py-1 rounded-full text-sm font-medium border ${moodColors[entry.mood] || 'bg-gray-500/20 text-gray-400 border-gray-500/30'}`}>
                      {moodEmojis[entry.mood]} {entry.mood}
                    </span>
                    {entry.analysisEmotion && (
                      <span className="px-2.5 py-1 rounded-md text-xs font-medium bg-primary-DEFAULT/10 text-primary-DEFAULT border border-primary-DEFAULT/20">
                        Feeling: {entry.analysisEmotion}
                      </span>
                    )}
                  </div>
                  
                  {/* Date */}
                  <div className="flex items-center gap-2 text-gray-500 text-sm mt-2">
                    <Calendar size={14} />
                    <span>{formatDate(entry.createdAt)}</span>
                  </div>
                  
                  {/* Content Preview */}
                  <p className="text-gray-400 mt-3 line-clamp-2">{entry.content}</p>

                  {/* Analysis Section */}
                  {(entry.detailedAnalysis || entry.cognitiveDistortions || entry.riskScore) && (
                    <div className="mt-4 pt-4 border-t border-white/5 space-y-3">
                      
                      {/* Risk Score & Trajectory */}
                      {(entry.riskScore || entry.emotionalTrajectory) && (
                        <div className="flex items-center gap-3 flex-wrap">
                          {entry.riskScore && (
                            <span className={`px-2.5 py-1 rounded-md text-xs font-medium border ${
                              entry.riskScore <= 3 ? 'bg-green-500/20 text-green-400 border-green-500/30' :
                              entry.riskScore <= 6 ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' :
                              'bg-red-500/20 text-red-400 border-red-500/30'
                            }`}>
                              Risk: {entry.riskScore}/10
                            </span>
                          )}
                          {entry.emotionalTrajectory && (
                            <span className={`px-2.5 py-1 rounded-md text-xs font-medium border ${
                              entry.emotionalTrajectory === 'improving' ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30' :
                              entry.emotionalTrajectory === 'declining' ? 'bg-orange-500/20 text-orange-400 border-orange-500/30' :
                              'bg-gray-500/20 text-gray-400 border-gray-500/30'
                            }`}>
                              📈 {entry.emotionalTrajectory}
                            </span>
                          )}
                        </div>
                      )}

                      {/* Cognitive Distortions */}
                      {entry.cognitiveDistortions && (
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-xs text-gray-500">Distortions:</span>
                          {entry.cognitiveDistortions.split(',').map((d, i) => (
                            <span key={i} className="px-2 py-0.5 rounded-full text-xs bg-amber-500/20 text-amber-400 border border-amber-500/30">
                              {d.trim()}
                            </span>
                          ))}
                        </div>
                      )}

                      {/* Suggestions */}
                      {entry.suggestions && (
                        <div className="bg-primary-DEFAULT/5 rounded-lg p-3 border-l-2 border-primary-DEFAULT/50">
                          <span className="text-xs text-primary-DEFAULT font-medium block mb-1">💡 Suggestions</span>
                          <p className="text-sm text-gray-300">
                            {(() => {
                              try {
                                const parsed = JSON.parse(entry.suggestions);
                                return Array.isArray(parsed) ? parsed.join(' • ') : entry.suggestions;
                              } catch { return entry.suggestions; }
                            })()}
                          </p>
                        </div>
                      )}
                      
                      {/* Detailed Analysis / Narrative Insight */}
                      {entry.detailedAnalysis && (
                        <p className="text-sm text-gray-300 italic bg-white/5 rounded-lg p-3 border-l-2 border-gray-500/50">
                          {entry.detailedAnalysis}
                        </p>
                      )}
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    onClick={() => handleReanalyze(entry.id)}
                    className="p-2 text-emerald-400 hover:bg-emerald-500/10 rounded-lg transition-colors"
                    title="Re-analyze mood"
                  >
                    <RefreshCw size={18} />
                  </button>
                  <button
                    onClick={() => handleDelete(entry.id)}
                    className="p-2 text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                    title="Delete entry"
                  >
                    <Trash2 size={18} />
                  </button>
                  <Link
                    to={`/entry/${entry.id}`}
                    className="p-2 text-primary-DEFAULT hover:bg-primary-DEFAULT/10 rounded-lg transition-colors"
                    title="View entry"
                  >
                    <ChevronRight size={18} />
                  </Link>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
};

export default History;
