import { AnimatePresence, motion } from 'framer-motion';
import { AlertTriangle, Bell, CheckCheck, TrendingDown, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import api from '../lib/axios';

const alertIcons = {
  HIGH_RISK: AlertTriangle,
  DECLINING_TRAJECTORY: TrendingDown,
  CONSISTENT_DISTORTION: AlertTriangle,
  CRISIS_KEYWORDS: AlertTriangle,
};

const alertColors = {
  HIGH_RISK: 'text-red-400 bg-red-500/20 border-red-500/30',
  DECLINING_TRAJECTORY: 'text-orange-400 bg-orange-500/20 border-orange-500/30',
  CONSISTENT_DISTORTION: 'text-amber-400 bg-amber-500/20 border-amber-500/30',
  CRISIS_KEYWORDS: 'text-red-400 bg-red-500/20 border-red-500/30',
};

const NotificationBell = () => {
  const [alerts, setAlerts] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);

  const fetchAlerts = async () => {
    try {
      const [alertsRes, countRes] = await Promise.all([
        api.get('/alerts'),
        api.get('/alerts/unread-count'),
      ]);
      setAlerts(alertsRes.data);
      setUnreadCount(countRes.data.count || 0);
    } catch (err) {
      console.error('Failed to fetch alerts:', err);
    }
  };

  useEffect(() => {
    fetchAlerts();
    // Poll every 30 seconds
    const interval = setInterval(fetchAlerts, 30000);
    return () => clearInterval(interval);
  }, []);

  const markAsRead = async (id) => {
    try {
      await api.put(`/alerts/${id}/read`);
      setAlerts(alerts.map(a => a.id === id ? { ...a, isRead: true } : a));
      setUnreadCount(Math.max(0, unreadCount - 1));
    } catch (err) {
      console.error('Failed to mark as read:', err);
    }
  };

  const markAllAsRead = async () => {
    try {
      await api.put('/alerts/read-all');
      setAlerts(alerts.map(a => ({ ...a, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  const formatTime = (instant) => {
    if (!instant) return '';
    const date = new Date(instant);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="relative">
      {/* Bell Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
      >
        <Bell size={22} />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 rounded-full text-xs flex items-center justify-center text-white font-bold animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown */}
      <AnimatePresence>
        {isOpen && (
          <>
            {/* Backdrop */}
            <div 
              className="fixed inset-0 z-40" 
              onClick={() => setIsOpen(false)} 
            />
            
            {/* Panel */}
            <motion.div
              initial={{ opacity: 0, y: -10, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -10, scale: 0.95 }}
              className="absolute right-0 top-12 w-80 sm:w-96 bg-dark-card border border-white/10 rounded-xl shadow-2xl z-50 overflow-hidden"
            >
              {/* Header */}
              <div className="flex items-center justify-between p-4 border-b border-white/10">
                <h3 className="font-semibold text-white flex items-center gap-2">
                  <Bell size={18} className="text-primary-DEFAULT" />
                  Notifications
                </h3>
                <div className="flex items-center gap-2">
                  {unreadCount > 0 && (
                    <button
                      onClick={markAllAsRead}
                      className="text-xs text-primary-DEFAULT hover:text-primary-light flex items-center gap-1"
                    >
                      <CheckCheck size={14} />
                      Mark all read
                    </button>
                  )}
                  <button
                    onClick={() => setIsOpen(false)}
                    className="text-gray-500 hover:text-white"
                  >
                    <X size={18} />
                  </button>
                </div>
              </div>

              {/* Alert List */}
              <div className="max-h-80 overflow-y-auto">
                {alerts.length === 0 ? (
                  <div className="p-6 text-center text-gray-500">
                    <Bell size={32} className="mx-auto mb-2 opacity-50" />
                    <p>No notifications yet</p>
                  </div>
                ) : (
                  alerts.slice(0, 10).map((alert) => {
                    const Icon = alertIcons[alert.type] || AlertTriangle;
                    const colorClass = alertColors[alert.type] || alertColors.HIGH_RISK;
                    
                    return (
                      <div
                        key={alert.id}
                        onClick={() => !alert.isRead && markAsRead(alert.id)}
                        className={`p-4 border-b border-white/5 cursor-pointer hover:bg-white/5 transition-colors ${
                          alert.isRead ? 'opacity-60' : ''
                        }`}
                      >
                        <div className="flex gap-3">
                          <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${colorClass}`}>
                            <Icon size={18} />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm text-white mb-1">{alert.message}</p>
                            <p className="text-xs text-gray-500">{formatTime(alert.createdAt)}</p>
                          </div>
                          {!alert.isRead && (
                            <span className="w-2 h-2 bg-primary-DEFAULT rounded-full shrink-0 mt-2" />
                          )}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};

export default NotificationBell;
