import { AnimatePresence, motion } from 'framer-motion';
import { BookOpen, Brain, LayoutDashboard, LogOut, Menu, PenLine, X } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';

const SidebarItem = ({ icon: Icon, label, to, active }) => (
  <Link
    to={to}
    className={`flex items-center gap-3 px-6 py-4 transition-all duration-300 border-l-4 ${
      active
        ? 'bg-primary-DEFAULT/10 text-white border-primary-DEFAULT'
        : 'text-gray-400 border-transparent hover:bg-white/5 hover:text-white'
    }`}
  >
    <Icon size={20} />
    <span className="font-medium">{label}</span>
  </Link>
);

const Layout = ({ children }) => {
  const { logout, user } = useAuth();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Close sidebar on route change (mobile)
  if (sidebarOpen && window.innerWidth < 1024) {
      // Just a check to ensure we don't lock it open
  }

  return (
    <div className="min-h-screen bg-dark-bg text-white relative flex overflow-hidden">
      {/* Mobile Menu Button */}
      <button 
        onClick={() => setSidebarOpen(true)}
        className="lg:hidden absolute top-6 left-6 z-50 text-gray-400 hover:text-white"
      >
        <Menu size={28} />
      </button>

      {/* Sidebar Overlay for Mobile */}
      <AnimatePresence>
        {sidebarOpen && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setSidebarOpen(false)}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-30 lg:hidden"
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <motion.aside
        className={`fixed lg:relative z-40 h-full w-72 bg-[#16181d] border-r border-white/5 flex flex-col transition-transform duration-300 ease-in-out ${
             sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="p-8 flex items-center justify-between lg:justify-center shrink-0">
          <h1 className="text-2xl font-bold bg-primary-gradient bg-clip-text text-transparent">
            MoodJournal
          </h1>
          <button onClick={() => setSidebarOpen(false)} className="lg:hidden text-gray-400 hover:text-white">
            <X size={24} />
          </button>
        </div>

        <nav className="mt-4 flex flex-col gap-2 px-4 overflow-y-auto flex-1">
          <SidebarItem 
            icon={PenLine} 
            label="Write Entry" 
            to="/entry" 
            active={location.pathname === '/entry'} 
          />
          <SidebarItem 
            icon={BookOpen} 
            label="My Journal" 
            to="/journal" 
            active={location.pathname === '/journal'} 
          />
          <SidebarItem 
            icon={LayoutDashboard} 
            label="Dashboard" 
            to="/dashboard" 
            active={location.pathname === '/dashboard'} 
          />
          <SidebarItem 
            icon={Brain} 
            label="Deep Assessment" 
            to="/assessment" 
            active={location.pathname === '/assessment'} 
          />
        </nav>
        
        {/* Profile, Analytics & Goals Links - Above user info */}
        <div className="px-4 mb-2 space-y-1">
          <Link
            to="/analytics"
            className={`flex items-center gap-3 px-6 py-3 rounded-lg transition-all duration-300 ${
              location.pathname === '/analytics'
                ? 'bg-primary-DEFAULT/10 text-white'
                : 'text-gray-400 hover:bg-white/5 hover:text-white'
            }`}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            <span className="font-medium">Analytics</span>
          </Link>
          <Link
            to="/goals"
            className={`flex items-center gap-3 px-6 py-3 rounded-lg transition-all duration-300 ${
              location.pathname === '/goals'
                ? 'bg-primary-DEFAULT/10 text-white'
                : 'text-gray-400 hover:bg-white/5 hover:text-white'
            }`}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>
            <span className="font-medium">Goals</span>
          </Link>
          <Link
            to="/profile"
            className={`flex items-center gap-3 px-6 py-3 rounded-lg transition-all duration-300 ${
              location.pathname === '/profile'
                ? 'bg-primary-DEFAULT/10 text-white'
                : 'text-gray-400 hover:bg-white/5 hover:text-white'
            }`}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span className="font-medium">My Profile</span>
          </Link>
        </div>

        <div className="p-6 mt-auto shrink-0">
          <div className="bg-white/5 rounded-xl p-4 mb-4">
            <p className="text-sm text-gray-400">Logged in as</p>
            <p className="font-semibold truncate" title={user?.username || user?.email}>{user?.username || user?.email || 'User'}</p>
          </div>
          <button
            onClick={logout}
            className="flex items-center gap-2 w-full px-4 py-3 text-red-400 hover:bg-red-500/10 rounded-xl transition-colors"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </motion.aside>

      {/* Main Content */}
      <main className="flex-1 h-screen overflow-y-auto w-full p-6 lg:p-10 relative">
        {/* Mobile header with notification */}
        <div className="absolute top-4 right-4 z-20 flex items-center gap-2">
          <NotificationBell />
        </div>
        <div className="max-w-6xl mx-auto pt-12 lg:pt-0"> 
             {/* Added pt-12 for mobile menu spacing */}
          {children}
        </div>
      </main>
    </div>
  );
};

export default Layout;
