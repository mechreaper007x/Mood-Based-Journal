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
  
  const [currentDate, setCurrentDate] = useState(new Date());

  
  const getDaysInMonth = (year, month) => {
    return new Date(year, month + 1, 0).getDate();
  };

  
  const getFirstDayOfMonth = (year, month) => {
    return new Date(year, month, 1).getDay();
  };

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const daysInMonth = getDaysInMonth(year, month);
  const firstDay = getFirstDayOfMonth(year, month);
  
  
  const days = [];
  
  for (let i = 0; i < firstDay; i++) {
    days.push(null);
  }
  
  for (let i = 1; i <= daysInMonth; i++) {
    days.push(new Date(year, month, i));
  }

  
  const getMoodForDate = (date) => {
    if (!date) return null;
    
    
    const dateStr = date.toISOString().split('T')[0];
    
    
    
    
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

      <div className="grid grid-cols-7 gap-1 mb-2">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
            <div key={day} className="text-center text-xs text-gray-500 font-medium py-1">
                {day}
            </div>
        ))}
      </div>

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
                    
                    {mood && (
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 bg-black/90 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10 pointer-events-none">
                            {mood}
                        </div>
                    )}
                </motion.div>
            );
        })}
      </div>

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
