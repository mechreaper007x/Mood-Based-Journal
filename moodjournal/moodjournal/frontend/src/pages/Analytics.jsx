import {
    ArcElement,
    BarElement,
    CategoryScale,
    Chart as ChartJS,
    Legend,
    LinearScale,
    LineElement,
    PointElement,
    Title,
    Tooltip,
} from 'chart.js';
import { motion } from 'framer-motion';
import { AlertTriangle, BarChart3, Brain, LineChart, Loader2, TrendingDown, TrendingUp } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import api from '../lib/axios';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
);

const moodColors = {
  HAPPY: '#eab308',
  SAD: '#3b82f6',
  ANGRY: '#ef4444',
  CALM: '#14b8a6',
  ANXIOUS: '#f97316',
  ENERGETIC: '#ec4899',
  CONTENT: '#22c55e',
  EXCITED: '#a855f7',
  NEUTRAL: '#6b7280',
  JOYFUL: '#eab308',
  PRODUCTIVE: '#22c55e',
};

const Analytics = () => {
  const [loading, setLoading] = useState(true);
  const [range, setRange] = useState('week');
  const [summary, setSummary] = useState(null);
  const [trajectory, setTrajectory] = useState(null);
  const [moodTrend, setMoodTrend] = useState([]);
  const [distortions, setDistortions] = useState({});
  const [riskHistory, setRiskHistory] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [sumRes, trajRes, trendRes, distRes, riskRes] = await Promise.all([
          api.get('/analytics/summary'),
          api.get('/analytics/trajectory'),
          api.get(`/analytics/mood-trend?range=${range}`),
          api.get('/analytics/distortion-frequency'),
          api.get(`/analytics/risk-history?range=${range}`),
        ]);
        setSummary(sumRes.data);
        setTrajectory(trajRes.data);
        setMoodTrend(trendRes.data);
        setDistortions(distRes.data);
        setRiskHistory(riskRes.data);
      } catch (err) {
        console.error('Failed to fetch analytics:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [range]);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="animate-spin text-primary-DEFAULT" size={40} />
      </div>
    );
  }

  // Mood Distribution Doughnut
  const moodDistData = {
    labels: Object.keys(summary?.moodDistribution || {}),
    datasets: [{
      data: Object.values(summary?.moodDistribution || {}),
      backgroundColor: Object.keys(summary?.moodDistribution || {}).map(m => moodColors[m] || '#6b7280'),
      borderWidth: 0,
    }],
  };

  // Risk History Line Chart
  const riskLineData = {
    labels: riskHistory.map(r => r.date),
    datasets: [{
      label: 'Risk Score',
      data: riskHistory.map(r => r.riskScore),
      borderColor: '#ef4444',
      backgroundColor: 'rgba(239, 68, 68, 0.2)',
      tension: 0.3,
      fill: true,
    }],
  };

  // Distortion Bar Chart
  const distortionData = {
    labels: Object.keys(distortions).slice(0, 6),
    datasets: [{
      label: 'Frequency',
      data: Object.values(distortions).slice(0, 6),
      backgroundColor: 'rgba(245, 158, 11, 0.6)',
      borderColor: '#f59e0b',
      borderWidth: 1,
    }],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#9ca3af' } },
      y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#9ca3af' } },
    },
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold text-white flex items-center gap-3">
            <BarChart3 className="text-primary-DEFAULT" />
            Analytics
          </h2>
          <p className="text-gray-400 mt-1">Track your emotional journey over time</p>
        </div>
        <div className="flex items-center gap-3">
            <select
            value={range}
            onChange={(e) => setRange(e.target.value)}
            className="bg-dark-card border border-white/10 rounded-xl px-4 py-2 text-white focus:border-primary-DEFAULT outline-none"
            >
            <option value="week">Past Week</option>
            <option value="month">Past Month</option>
            <option value="year">Past Year</option>
            </select>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="bg-dark-card border border-white/5 rounded-xl p-4">
          <p className="text-gray-500 text-sm">Total Entries</p>
          <p className="text-3xl font-bold text-white">{summary?.totalEntries || 0}</p>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }} className="bg-dark-card border border-white/5 rounded-xl p-4">
          <p className="text-gray-500 text-sm">This Week</p>
          <p className="text-3xl font-bold text-white">{summary?.entriesThisWeek || 0}</p>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="bg-dark-card border border-white/5 rounded-xl p-4">
          <p className="text-gray-500 text-sm">Avg Risk</p>
          <p className="text-3xl font-bold text-white">{summary?.averageRiskScore || '-'}</p>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }} className="bg-dark-card border border-white/5 rounded-xl p-4">
          <p className="text-gray-500 text-sm">Trajectory</p>
          <div className="flex items-center gap-2">
            {trajectory?.trajectory === 'improving' && <TrendingUp className="text-green-400" />}
            {trajectory?.trajectory === 'declining' && <TrendingDown className="text-red-400" />}
            {trajectory?.trajectory === 'stable' && <LineChart className="text-gray-400" />}
            <p className="text-xl font-bold text-white capitalize">{trajectory?.trajectory || 'N/A'}</p>
          </div>
        </motion.div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Mood Distribution */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="bg-dark-card border border-white/5 rounded-2xl p-6">
          <h3 className="text-xl font-semibold text-white mb-4">Mood Distribution</h3>
          <div className="h-64">
            {Object.keys(summary?.moodDistribution || {}).length > 0 ? (
              <Doughnut data={moodDistData} options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right', labels: { color: '#9ca3af' } } } }} />
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">No data yet</div>
            )}
          </div>
        </motion.div>

        {/* Risk Score History */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.25 }} className="bg-dark-card border border-white/5 rounded-2xl p-6">
          <h3 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <AlertTriangle className="text-red-400" size={20} />
            Risk Score Trend
          </h3>
          <div className="h-64">
            {riskHistory.length > 0 ? (
              <Line data={riskLineData} options={chartOptions} />
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">No risk data yet</div>
            )}
          </div>
        </motion.div>

        {/* Cognitive Distortions */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className="bg-dark-card border border-white/5 rounded-2xl p-6">
          <h3 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <Brain className="text-amber-400" size={20} />
            Common Cognitive Distortions
          </h3>
          <div className="h-64">
            {Object.keys(distortions).length > 0 ? (
              <Bar data={distortionData} options={chartOptions} />
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">No distortions detected yet</div>
            )}
          </div>
        </motion.div>

        {/* Trajectory Details */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.35 }} className="bg-dark-card border border-white/5 rounded-2xl p-6">
          <h3 className="text-xl font-semibold text-white mb-4">Emotional Trajectory</h3>
          <div className="space-y-4">
            <div className={`p-4 rounded-xl ${
              trajectory?.trajectory === 'improving' ? 'bg-green-500/20 border border-green-500/30' :
              trajectory?.trajectory === 'declining' ? 'bg-red-500/20 border border-red-500/30' :
              'bg-gray-500/20 border border-gray-500/30'
            }`}>
              <div className="flex items-center gap-3">
                {trajectory?.trajectory === 'improving' && <TrendingUp className="text-green-400" size={32} />}
                {trajectory?.trajectory === 'declining' && <TrendingDown className="text-red-400" size={32} />}
                {trajectory?.trajectory === 'stable' && <LineChart className="text-gray-400" size={32} />}
                <div>
                  <p className="text-lg font-bold text-white capitalize">{trajectory?.trajectory || 'Unknown'}</p>
                  <p className="text-sm text-gray-400">Based on {trajectory?.entriesAnalyzed || 0} entries</p>
                </div>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-white/5 rounded-lg p-3 text-center">
                <p className="text-xs text-gray-500">Recent Score</p>
                <p className="text-2xl font-bold text-white">{trajectory?.recentScore ?? '-'}</p>
              </div>
              <div className="bg-white/5 rounded-lg p-3 text-center">
                <p className="text-xs text-gray-500">Previous Score</p>
                <p className="text-2xl font-bold text-white">{trajectory?.olderScore ?? '-'}</p>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default Analytics;
