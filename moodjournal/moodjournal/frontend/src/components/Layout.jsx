import { BookOpen, ChartNoAxesCombined, Compass, LayoutDashboard, LogOut, Menu, PenLine, Target, X, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';

const navigation = [
  { label: 'Overview', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Write a note', to: '/entry', icon: PenLine },
  { label: 'My journal', to: '/journal', icon: BookOpen },
];
const explore = [
  { label: 'Patterns', to: '/analytics', icon: ChartNoAxesCombined },
  { label: 'Personal growth', to: '/assessment', icon: Compass },
  { label: 'Goals', to: '/goals', icon: Target },
  { label: 'My profile', to: '/profile', icon: UserRound },
];

const NavLink = ({ item, active, onNavigate }) => {
  const Icon = item.icon;
  return <Link to={item.to} onClick={onNavigate} className={`app-nav-link ${active ? 'active' : ''}`}>
    <Icon size={17} strokeWidth={1.8} /><span>{item.label}</span>
  </Link>;
};

const Layout = ({ children }) => {
  const { logout, user } = useAuth();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const active = (to) => location.pathname === to || (to === '/journal' && location.pathname.startsWith('/entry/'));
  const displayName = user?.username || user?.email?.split('@')[0] || 'Your space';

  return (
    <div className="app-shell">
      {sidebarOpen && <button aria-label="Close navigation" className="fixed inset-0 z-20 bg-black/20 lg:hidden" onClick={() => setSidebarOpen(false)} />}
      <aside className={`app-sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="brand-lockup">
          <div className="brand-mark">m</div>
          <div><div className="brand-name">MoodJournal</div><div className="brand-subtitle">a little space for you</div></div>
          <button className="ml-auto lg:hidden text-stone-500" onClick={() => setSidebarOpen(false)} aria-label="Close menu"><X size={19}/></button>
        </div>

        <div className="nav-caption">Your space</div>
        <nav className="app-nav">
          {navigation.map(item => <NavLink key={item.to} item={item} active={active(item.to)} onNavigate={() => setSidebarOpen(false)} />)}
        </nav>

        <div className="nav-caption mt-8">Look a little closer</div>
        <nav className="app-nav">
          {explore.map(item => <NavLink key={item.to} item={item} active={active(item.to)} onNavigate={() => setSidebarOpen(false)} />)}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-note"><strong>A page for the in-between.</strong><span>No perfect words needed. Just show up as you are.</span></div>
          <div className="sidebar-user">
            <div className="user-avatar">{displayName.charAt(0).toUpperCase()}</div>
            <div className="user-meta"><strong>{displayName}</strong><span>Your private journal</span></div>
            <button className="logout-button" onClick={logout} aria-label="Log out" title="Log out"><LogOut size={16}/></button>
          </div>
        </div>
      </aside>

      <main className="app-main">
        <div className="app-content">
          <div className="topbar">
            <button className="mobile-menu" onClick={() => setSidebarOpen(true)} aria-label="Open navigation"><Menu size={18}/></button>
            <div className="flex items-center gap-3"><span>Take a breath. You're here.</span><NotificationBell /></div>
          </div>
          {children}
        </div>
      </main>
    </div>
  );
};

export default Layout;
