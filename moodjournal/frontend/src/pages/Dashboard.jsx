import {
    ArcElement,
    BarElement,
    CategoryScale,
    Chart as ChartJS,
    Legend,
    LinearScale,
    Title,
    Tooltip
} from 'chart.js';
import { motion } from 'framer-motion';
import { BrainCircuit, Calendar, Loader2, TrendingUp } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Bar, Pie } from 'react-chartjs-2';
import MoodCalendar from '../components/MoodCalendar';
import { useAuth } from '../context/AuthContext';
import api from '../lib/axios';

// Register ChartJS components
ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title);

const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState([]);
  const [entries, setEntries] = useState([]); // State for all entries
  const [quote, setQuote] = useState({ text: "Loading inspiration...", author: "Gemini" });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [statsRes, quoteRes, entriesRes] = await Promise.all([
            api.get('/journal/stats'),
            api.get('/ai/daily-quote'),
            api.get('/journal') // Fetch all entries for the calendar
        ]);
        setStats(statsRes.data);
        if (quoteRes.data) {
            setQuote(quoteRes.data);
        }
        setEntries(entriesRes.data);
      } catch (error) {
        console.error("Failed to fetch dashboard data", error);
        setQuote({ text: "Failed to load quote.", author: "System" });
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const pieData = {
    labels: stats.map(s => s.mood),
    datasets: [
      {
        data: stats.map(s => s.count),
        backgroundColor: [
          'rgba(142, 45, 226, 0.7)', // Primary
          'rgba(74, 0, 224, 0.7)',   // Secondary
          'rgba(52, 211, 153, 0.7)', // Green
          'rgba(251, 113, 133, 0.7)', // Red
          'rgba(250, 204, 21, 0.7)',  // Yellow
        ],
        borderColor: [
          '#8E2DE2',
          '#4A00E0',
          '#34d399',
          '#fb7185',
          '#facc15',
        ],
        borderWidth: 1,
      },
    ],
  };

  const chartOptions = {
    plugins: {
      legend: {
        labels: { color: '#E0E0E0', font: { family: 'Inter' } }
      }
    }
  };

  // Mood colors for bar chart
  const moodBarColors = {
    HAPPY: 'rgba(96, 165, 250, 0.8)',    // Blue
    SAD: 'rgba(156, 163, 175, 0.8)',     // Gray
    ANXIOUS: 'rgba(248, 113, 113, 0.8)', // Red/Pink
    ANGRY: 'rgba(239, 68, 68, 0.8)',     // Red
    CALM: 'rgba(52, 211, 153, 0.8)',     // Green
    NEUTRAL: 'rgba(167, 139, 250, 0.8)', // Purple
    JOYFUL: 'rgba(96, 165, 250, 0.8)',   // Blue
    PRODUCTIVE: 'rgba(167, 139, 250, 0.8)', // Purple
  };

  // Calculate percentage for each mood
  const totalEntries = stats.reduce((acc, curr) => acc + curr.count, 0);
  
  const barData = {
    labels: stats.map(s => s.mood),
    datasets: [
      {
        label: 'Mood Score',
        data: stats.map(s => Math.round((s.count / totalEntries) * 100) || 0),
        backgroundColor: stats.map(s => moodBarColors[s.mood] || 'rgba(167, 139, 250, 0.8)'),
        borderRadius: 6,
        barThickness: 30,
      },
    ],
  };

  const barOptions = {
    indexAxis: 'y', // Horizontal bar chart
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
    },
    scales: {
      x: {
        min: 0,
        max: 100,
        grid: { color: 'rgba(255,255,255,0.05)' },
        ticks: { 
          color: '#9CA3AF', 
          font: { family: 'Inter' },
          callback: (value) => value + '%'
        },
      },
      y: {
        grid: { display: false },
        ticks: { 
          color: '#E0E0E0', 
          font: { family: 'Inter', size: 14 } 
        },
      },
    },
  };

  if (loading) {
    return <div className="flex h-full items-center justify-center"><Loader2 className="animate-spin text-primary-DEFAULT" size={40} /></div>;
  }

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-white">Dashboard</h2>
          <p className="text-gray-400">Your emotional landscape overview</p>
        </div>
        <div className="text-right hidden sm:block">
          <p className="text-sm text-gray-400">{new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Stat Card 1 */}
        <motion.div 
          whileHover={{ y: -5 }}
          className="bg-dark-card border border-white/5 p-6 rounded-2xl shadow-xl flex items-center gap-4"
        >
          <div className="p-3 bg-primary-DEFAULT/10 rounded-xl text-primary-DEFAULT">
            <BrainCircuit size={24} />
          </div>
          <div>
            <p className="text-sm text-gray-400">Total Entries</p>
            <p className="text-2xl font-bold text-white">{stats.reduce((acc, curr) => acc + curr.count, 0)}</p>
          </div>
        </motion.div>

        {/* Stat Card 2 */}
        <motion.div 
          whileHover={{ y: -5 }}
          className="bg-dark-card border border-white/5 p-6 rounded-2xl shadow-xl flex items-center gap-4"
        >
          <div className="p-3 bg-green-500/10 rounded-xl text-green-400">
            <TrendingUp size={24} />
          </div>
          <div>
            <p className="text-sm text-gray-400">Dominant Mood</p>
            <p className="text-2xl font-bold text-white">
              {stats.length > 0 ? stats.reduce((a, b) => a.count > b.count ? a : b).mood : 'N/A'}
            </p>
          </div>
        </motion.div>

        {/* Stat Card 3 */}
        <motion.div 
          whileHover={{ y: -5 }}
          className="bg-dark-card border border-white/5 p-6 rounded-2xl shadow-xl flex items-center gap-4"
        >
          <div className="p-3 bg-purple-500/10 rounded-xl text-purple-400">
            <Calendar size={24} />
          </div>
          <div>
            <p className="text-sm text-gray-400">Streak</p>
            <p className="text-2xl font-bold text-white">3 Days</p>
          </div>
        </motion.div>
      </div>

      {/* Mood Calendar Section - New Feature */}
      <MoodCalendar entries={entries} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Chart Section */}
        <div className="bg-dark-card border border-white/5 p-8 rounded-2xl shadow-xl min-h-[400px]">
          <h3 className="text-xl font-semibold mb-6">Mood Distribution</h3>
          {stats.length > 0 ? (
            <div className="h-64 flex items-center justify-center">
              <Pie data={pieData} options={chartOptions} />
            </div>
          ) : (
            <div className="h-64 flex flex-col items-center justify-center text-gray-500">
              <p>No enough data yet.</p>
              <p className="text-sm">Start journaling to see analysis!</p>
            </div>
          )}
        </div>

        {/* Recent Insights / AI Section */}
        <div className="bg-dark-card border border-white/5 p-8 rounded-2xl shadow-xl">
          <h3 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <span className="text-primary-DEFAULT">✨</span> Daily Wisdom
          </h3>
          <div className="space-y-4">
            <div className="p-4 bg-white/5 rounded-xl border-l-4 border-primary-DEFAULT min-h-[120px] flex flex-col justify-center">
              <p className="text-gray-300 italic text-lg leading-relaxed">"{quote.quote || quote.text || quote.content}"</p>
              <p className="text-sm text-primary-DEFAULT/80 mt-4 text-right font-medium">- {quote.author || 'AI Companion'}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Mood Analysis Bar Chart - Full Width */}
      <div className="bg-dark-card border border-white/5 p-8 rounded-2xl shadow-xl">
        <h3 className="text-xl font-semibold mb-6">Your Mood Analysis</h3>
        {stats.length > 0 ? (
          <div className="h-64">
            <Bar data={barData} options={barOptions} />
          </div>
        ) : (
          <div className="h-64 flex flex-col items-center justify-center text-gray-500">
            <p>No mood data yet.</p>
            <p className="text-sm">Start journaling to see your mood spectrum!</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;

