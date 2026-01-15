import { motion } from 'framer-motion';
import { Check, Loader2, Plus, Target, Trash2, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import api from '../lib/axios';

const categoryColors = {
  JOURNALING: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  MOOD: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  SLEEP: 'bg-indigo-500/20 text-indigo-400 border-indigo-500/30',
  STRESS: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
  MINDFULNESS: 'bg-green-500/20 text-green-400 border-green-500/30',
  GENERAL: 'bg-gray-500/20 text-gray-400 border-gray-500/30',
};

const Goals = () => {
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: 'GENERAL',
    targetDate: '',
  });

  const fetchGoals = async () => {
    try {
      const res = await api.get('/goals');
      setGoals(res.data);
    } catch (err) {
      console.error('Failed to fetch goals:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGoals();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim()) return;
    
    try {
      const res = await api.post('/goals', formData);
      setGoals([res.data, ...goals]);
      setFormData({ title: '', description: '', category: 'GENERAL', targetDate: '' });
      setShowForm(false);
    } catch (err) {
      console.error('Failed to create goal:', err);
    }
  };

  const updateProgress = async (id, progress) => {
    try {
      const res = await api.put(`/goals/${id}`, { progress });
      setGoals(goals.map(g => g.id === id ? res.data : g));
    } catch (err) {
      console.error('Failed to update progress:', err);
    }
  };

  const completeGoal = async (id) => {
    try {
      const res = await api.put(`/goals/${id}`, { isCompleted: true });
      setGoals(goals.map(g => g.id === id ? res.data : g));
    } catch (err) {
      console.error('Failed to complete goal:', err);
    }
  };

  const deleteGoal = async (id) => {
    try {
      await api.delete(`/goals/${id}`);
      setGoals(goals.filter(g => g.id !== id));
    } catch (err) {
      console.error('Failed to delete goal:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="animate-spin text-primary-DEFAULT" size={40} />
      </div>
    );
  }

  const activeGoals = goals.filter(g => !g.isCompleted);
  const completedGoals = goals.filter(g => g.isCompleted);

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-white flex items-center gap-3">
            <Target className="text-primary-DEFAULT" />
            Goals
          </h2>
          <p className="text-gray-400 mt-1">Track your mental wellness goals</p>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 bg-primary-gradient text-white px-4 py-2 rounded-xl font-medium hover:opacity-90 transition-opacity"
        >
          <Plus size={18} />
          New Goal
        </button>
      </div>

      {/* New Goal Form Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-dark-card border border-white/10 rounded-2xl p-6 w-full max-w-md"
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-white">Create Goal</h3>
              <button onClick={() => setShowForm(false)} className="text-gray-400 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm text-gray-400 mb-1">Title</label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full bg-dark-bg border border-white/10 rounded-xl px-4 py-3 text-white focus:border-primary-DEFAULT outline-none"
                  placeholder="e.g., Write 3 journal entries this week"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full bg-dark-bg border border-white/10 rounded-xl px-4 py-3 text-white focus:border-primary-DEFAULT outline-none resize-none h-20"
                  placeholder="Optional details..."
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full bg-dark-bg border border-white/10 rounded-xl px-4 py-3 text-white focus:border-primary-DEFAULT outline-none"
                  >
                    <option value="GENERAL">General</option>
                    <option value="JOURNALING">Journaling</option>
                    <option value="MOOD">Mood</option>
                    <option value="SLEEP">Sleep</option>
                    <option value="STRESS">Stress</option>
                    <option value="MINDFULNESS">Mindfulness</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Target Date</label>
                  <input
                    type="date"
                    value={formData.targetDate}
                    onChange={(e) => setFormData({ ...formData, targetDate: e.target.value })}
                    className="w-full bg-dark-bg border border-white/10 rounded-xl px-4 py-3 text-white focus:border-primary-DEFAULT outline-none"
                  />
                </div>
              </div>
              <button
                type="submit"
                className="w-full bg-primary-gradient text-white py-3 rounded-xl font-medium hover:opacity-90"
              >
                Create Goal
              </button>
            </form>
          </motion.div>
        </div>
      )}

      {/* Active Goals */}
      <div>
        <h3 className="text-lg font-semibold text-white mb-4">Active Goals ({activeGoals.length})</h3>
        {activeGoals.length === 0 ? (
          <div className="bg-dark-card border border-white/5 rounded-xl p-8 text-center">
            <Target size={48} className="mx-auto text-gray-600 mb-3" />
            <p className="text-gray-500">No active goals. Create one to get started!</p>
          </div>
        ) : (
          <div className="space-y-4">
            {activeGoals.map((goal) => (
              <motion.div
                key={goal.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-dark-card border border-white/5 rounded-xl p-5"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`px-2 py-0.5 rounded-full text-xs border ${categoryColors[goal.category]}`}>
                        {goal.category}
                      </span>
                      {goal.targetDate && (
                        <span className="text-xs text-gray-500">
                          Due: {new Date(goal.targetDate).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                    <h4 className="text-lg font-medium text-white">{goal.title}</h4>
                    {goal.description && <p className="text-sm text-gray-400 mt-1">{goal.description}</p>}
                    
                    {/* Progress Bar */}
                    <div className="mt-3">
                      <div className="flex justify-between text-xs text-gray-500 mb-1">
                        <span>Progress</span>
                        <span>{goal.progress}%</span>
                      </div>
                      <input
                        type="range"
                        min="0"
                        max="100"
                        value={goal.progress}
                        onChange={(e) => updateProgress(goal.id, parseInt(e.target.value))}
                        className="w-full h-2 bg-gray-700 rounded-full appearance-none cursor-pointer accent-primary-DEFAULT"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => completeGoal(goal.id)}
                      className="p-2 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors"
                      title="Mark Complete"
                    >
                      <Check size={18} />
                    </button>
                    <button
                      onClick={() => deleteGoal(goal.id)}
                      className="p-2 rounded-lg bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
                      title="Delete"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>

      {/* Completed Goals */}
      {completedGoals.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-white mb-4">Completed ({completedGoals.length})</h3>
          <div className="space-y-3">
            {completedGoals.map((goal) => (
              <div
                key={goal.id}
                className="bg-dark-card/50 border border-white/5 rounded-xl p-4 opacity-60"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center">
                      <Check size={16} className="text-green-400" />
                    </div>
                    <div>
                      <h4 className="font-medium text-white line-through">{goal.title}</h4>
                      <p className="text-xs text-gray-500">
                        Completed {goal.completedAt ? new Date(goal.completedAt).toLocaleDateString() : ''}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => deleteGoal(goal.id)}
                    className="text-gray-500 hover:text-red-400 transition-colors"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default Goals;
