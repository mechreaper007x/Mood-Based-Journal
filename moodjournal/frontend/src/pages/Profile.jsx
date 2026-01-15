import {
    Chart as ChartJS,
    Filler,
    Legend,
    LineElement,
    PointElement,
    RadialLinearScale,
    Tooltip,
} from 'chart.js';
import { motion } from 'framer-motion';
import { Brain, Heart, Shield, Sparkles, Target, User, Zap } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Radar } from 'react-chartjs-2';
import api from '../lib/axios';

ChartJS.register(RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

// Archetype descriptions
const archetypeInfo = {
  hero: { icon: Shield, color: 'red', desc: 'Brave, determined, driven to prove worth through courageous action' },
  caregiver: { icon: Heart, color: 'pink', desc: 'Nurturing, compassionate, driven to protect and care for others' },
  explorer: { icon: Zap, color: 'amber', desc: 'Independent, adventurous, seeks new experiences and self-discovery' },
  rebel: { icon: Target, color: 'orange', desc: 'Revolutionary, outrageous, disrupts the status quo' },
  lover: { icon: Heart, color: 'rose', desc: 'Passionate, intimate, seeks connection and sensory experience' },
  creator: { icon: Sparkles, color: 'purple', desc: 'Innovative, artistic, brings visions to life' },
  jester: { icon: Sparkles, color: 'yellow', desc: 'Playful, humorous, brings joy and lightness' },
  sage: { icon: Brain, color: 'blue', desc: 'Wise, analytical, seeks truth and understanding' },
  magician: { icon: Sparkles, color: 'violet', desc: 'Transformative, visionary, makes dreams reality' },
  ruler: { icon: Shield, color: 'gold', desc: 'Commanding, responsible, creates order from chaos' },
  innocent: { icon: Heart, color: 'green', desc: 'Optimistic, pure, believes in doing the right thing' },
  everyman: { icon: User, color: 'slate', desc: 'Relatable, authentic, values belonging and connection' },
};

const Profile = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await api.get('/profile');
        setProfile(res.data);
      } catch (err) {
        console.error('Failed to fetch profile:', err);
        setError('Failed to load profile. Complete a Deep Assessment first.');
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-DEFAULT"></div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="flex flex-col h-full items-center justify-center gap-4">
        <Brain className="text-gray-600" size={64} />
        <p className="text-gray-400 text-lg text-center max-w-md">{error || 'No profile data available.'}</p>
        <a
          href="/assessment"
          className="bg-primary-gradient text-white px-6 py-3 rounded-xl font-bold hover:opacity-90 transition-all"
        >
          Take Deep Assessment
        </a>
      </div>
    );
  }

  // Big 5 Radar Chart Data
  const radarData = {
    labels: ['Extraversion', 'Agreeableness', 'Conscientiousness', 'Emotional Stability', 'Openness'],
    datasets: [
      {
        label: 'Your Personality',
        data: [
          profile.extraversion || 4,
          profile.agreeableness || 4,
          profile.conscientiousness || 4,
          profile.emotionalStability || 4,
          profile.openness || 4,
        ],
        backgroundColor: 'rgba(139, 92, 246, 0.3)',
        borderColor: 'rgba(139, 92, 246, 1)',
        borderWidth: 2,
        pointBackgroundColor: 'rgba(139, 92, 246, 1)',
      },
    ],
  };

  const radarOptions = {
    scales: {
      r: {
        min: 1,
        max: 7,
        ticks: { stepSize: 1, color: '#9ca3af' },
        grid: { color: 'rgba(255,255,255,0.1)' },
        pointLabels: { color: '#e5e7eb', font: { size: 12 } },
      },
    },
    plugins: {
      legend: { display: false },
    },
    maintainAspectRatio: false,
  };

  const PrimaryArch = archetypeInfo[profile.primaryArchetype?.toLowerCase()] || archetypeInfo.sage;
  const SecondaryArch = archetypeInfo[profile.secondaryArchetype?.toLowerCase()] || archetypeInfo.explorer;

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Header */}
      <div>
        <h2 className="text-3xl font-bold text-white flex items-center gap-3">
          <User className="text-primary-DEFAULT" />
          My Psychological Profile
        </h2>
        <p className="text-gray-400 mt-1">Your personality insights from deep assessment</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Big 5 Radar Chart */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-dark-card border border-white/5 rounded-2xl p-6"
        >
          <h3 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <Brain className="text-primary-DEFAULT" size={20} />
            Big Five Personality
          </h3>
          <div className="h-72">
            <Radar data={radarData} options={radarOptions} />
          </div>
          <div className="mt-4 grid grid-cols-5 gap-2 text-center text-xs text-gray-400">
            <div>E: {profile.extraversion || 4}/7</div>
            <div>A: {profile.agreeableness || 4}/7</div>
            <div>C: {profile.conscientiousness || 4}/7</div>
            <div>ES: {profile.emotionalStability || 4}/7</div>
            <div>O: {profile.openness || 4}/7</div>
          </div>
        </motion.div>

        {/* Archetypes */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="bg-dark-card border border-white/5 rounded-2xl p-6"
        >
          <h3 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <Sparkles className="text-primary-DEFAULT" size={20} />
            Psychological Archetypes
          </h3>
          <div className="space-y-4">
            {/* Primary */}
            <div className="bg-gradient-to-r from-purple-500/20 to-pink-500/20 rounded-xl p-4 border border-purple-500/30">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-purple-500/30 flex items-center justify-center">
                  <PrimaryArch.icon className="text-purple-400" size={24} />
                </div>
                <div>
                  <span className="text-xs text-purple-400 uppercase tracking-wide">Primary Archetype</span>
                  <h4 className="text-lg font-bold text-white capitalize">{profile.primaryArchetype || 'Unknown'}</h4>
                </div>
              </div>
              <p className="text-gray-400 text-sm mt-3">{PrimaryArch.desc}</p>
            </div>
            {/* Secondary */}
            <div className="bg-white/5 rounded-xl p-4 border border-white/10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-gray-500/30 flex items-center justify-center">
                  <SecondaryArch.icon className="text-gray-400" size={20} />
                </div>
                <div>
                  <span className="text-xs text-gray-500 uppercase tracking-wide">Secondary</span>
                  <h4 className="text-md font-semibold text-white capitalize">{profile.secondaryArchetype || 'Unknown'}</h4>
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Empathy Profile */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-dark-card border border-white/5 rounded-2xl p-6"
        >
          <h3 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <Heart className="text-primary-DEFAULT" size={20} />
            Empathy Profile
          </h3>
          <div className="space-y-4">
            {[
              { label: 'Cognitive', value: profile.cognitiveEmpathy || 5, color: 'bg-blue-500', desc: 'Understanding others\' perspectives' },
              { label: 'Affective', value: profile.affectiveEmpathy || 5, color: 'bg-pink-500', desc: 'Feeling others\' emotions' },
              { label: 'Compassionate', value: profile.compassionateEmpathy || 5, color: 'bg-green-500', desc: 'Moved to help others' },
            ].map((emp) => (
              <div key={emp.label}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-gray-300">{emp.label}</span>
                  <span className="text-gray-500">{emp.value}/10</span>
                </div>
                <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${emp.color} rounded-full transition-all duration-500`}
                    style={{ width: `${emp.value * 10}%` }}
                  />
                </div>
                <p className="text-xs text-gray-500 mt-1">{emp.desc}</p>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Stressors & Context */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-dark-card border border-white/5 rounded-2xl p-6"
        >
          <h3 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <Target className="text-primary-DEFAULT" size={20} />
            Life Context
          </h3>
          <div className="space-y-4">
            {profile.currentStressors && profile.currentStressors.length > 0 && (
              <div>
                <span className="text-sm text-gray-500">Current Stressors</span>
                <div className="flex flex-wrap gap-2 mt-2">
                  {(Array.isArray(profile.currentStressors) 
                    ? profile.currentStressors 
                    : profile.currentStressors.split(',')
                  ).map((s, i) => (
                    <span key={i} className="px-3 py-1 rounded-full text-sm bg-red-500/20 text-red-400 border border-red-500/30">
                      {typeof s === 'string' ? s.trim() : s}
                    </span>
                  ))}
                </div>
              </div>
            )}
            <div className="grid grid-cols-2 gap-4 mt-4">
              <div className="bg-white/5 rounded-lg p-3">
                <span className="text-xs text-gray-500">Baseline Stress</span>
                <p className="text-2xl font-bold text-white">{profile.baselineStressLevel || '-'}/10</p>
              </div>
              <div className="bg-white/5 rounded-lg p-3">
                <span className="text-xs text-gray-500">Baseline Energy</span>
                <p className="text-2xl font-bold text-white">{profile.baselineEnergyLevel || '-'}/10</p>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Retake Assessment */}
      <div className="text-center pt-4">
        <a
          href="/assessment"
          className="inline-flex items-center gap-2 text-primary-DEFAULT hover:text-primary-light transition-colors"
        >
          <Brain size={18} />
          Retake Deep Assessment
        </a>
      </div>
    </div>
  );
};

export default Profile;
