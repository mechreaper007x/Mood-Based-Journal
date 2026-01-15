import { motion } from 'framer-motion';
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight } from 'lucide-react';
import { useState } from 'react';

const MOOD_COLORS = {
  HAPPY: 'bg-green-500',
  SAD: 'bg-blue-500',
  ANGRY: 'bg-red-500',
  ANXIOUS: 'bg-orange-500',
  CALM: 'bg-teal-400',
  EXCITED: 'bg-yellow-400',
  NEUTRAL: 'bg-gray-500',
  DEFAULT: 'bg-dark-input border border-white/5'
};

const MoodCalendar = ({ entries = [] }) => {
  // Get current date
  const [currentDate, setCurrentDate] = useState(new Date());

  // Helper to get days in month
  const getDaysInMonth = (year, month) => {
    return new Date(year, month + 1, 0).getDate();
  };

  // Helper to get day of week for 1st of month (0 = Sunday)
  const getFirstDayOfMonth = (year, month) => {
    return new Date(year, month, 1).getDay();
  };

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const daysInMonth = getDaysInMonth(year, month);
  const firstDay = getFirstDayOfMonth(year, month);
  
  // Generate calendar grid
  const days = [];
  // Add empty slots for days before 1st
  for (let i = 0; i < firstDay; i++) {
    days.push(null);
  }
  // Add actual days
  for (let i = 1; i <= daysInMonth; i++) {
    days.push(new Date(year, month, i));
  }

  // Helper to find dominant mood for a specific date
  const getMoodForDate = (date) => {
    if (!date) return null;
    
    // Normalize date for comparison (YYYY-MM-DD)
    const dateStr = date.toISOString().split('T')[0];
    
    // Find entry for this date
    // Note: Assuming entries are sorted or we just take the first/last one for the day
    // For a more accurate "daily mood", we might average them, but taking the latest is a good heuristic
    const entry = entries.find(e => {
        if (!e.createdAt) return false;
        return e.createdAt.startsWith(dateStr);
    });

    return entry ? entry.mood : null;
  };

  const changeMonth = (delta) => {
    setCurrentDate(new Date(year, month + delta, 1));
  };

  const MONTH_NAMES = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  return (
    <div className="bg-dark-card border border-white/10 rounded-2xl p-6 shadow-xl">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
            <CalendarIcon className="text-primary-DEFAULT" size={20} />
            <h3 className="text-white font-bold text-lg">Mood Calendar</h3>
        </div>
        <div className="flex items-center gap-2">
            <button onClick={() => changeMonth(-1)} className="p-1 hover:bg-white/10 rounded-full text-gray-400 hover:text-white transition-colors">
                <ChevronLeft size={20} />
            </button>
            <span className="text-white font-medium min-w-[100px] text-center">{MONTH_NAMES[month]} {year}</span>
            <button onClick={() => changeMonth(1)} className="p-1 hover:bg-white/10 rounded-full text-gray-400 hover:text-white transition-colors">
                <ChevronRight size={20} />
            </button>
        </div>
      </div>

      {/* Weekday Headers */}
      <div className="grid grid-cols-7 gap-1 mb-2">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
            <div key={day} className="text-center text-xs text-gray-500 font-medium py-1">
                {day}
            </div>
        ))}
      </div>

      {/* Days Grid */}
      <div className="grid grid-cols-7 gap-1">
        {days.map((date, index) => {
            if (!date) {
                return <div key={`empty-${index}`} className="aspect-square" />;
            }
            
            const mood = getMoodForDate(date);
            const colorClass = mood ? MOOD_COLORS[mood] || MOOD_COLORS.DEFAULT : MOOD_COLORS.DEFAULT;
            const isToday = new Date().toDateString() === date.toDateString();

            return (
                <motion.div
                    key={date.toISOString()}
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: index * 0.01 }}
                    className={`
                        aspect-square rounded-lg flex items-center justify-center text-sm font-medium relative group cursor-default
                        ${colorClass}
                        ${!mood ? 'bg-dark-input/50 text-gray-600' : 'text-white shadow-lg'}
                        ${isToday ? 'border-2 border-primary-DEFAULT' : ''}
                    `}
                >
                    {date.getDate()}
                    
                    {/* Tooltip */}
                    {mood && (
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 bg-black/90 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10 pointer-events-none">
                            {mood}
                        </div>
                    )}
                </motion.div>
            );
        })}
      </div>

      {/* Legend */}
      <div className="flex flex-wrap items-center justify-center gap-3 mt-6 text-xs text-gray-400">
        {Object.entries(MOOD_COLORS).filter(([key]) => key !== 'DEFAULT' && key !== 'NEUTRAL').map(([mood, color]) => (
            <div key={mood} className="flex items-center gap-1.5">
                <div className={`w-3 h-3 rounded-full ${color}`} />
                <span className="capitalize">{mood.toLowerCase()}</span>
            </div>
        ))}
      </div>
    </div>
  );
};

export default MoodCalendar;
