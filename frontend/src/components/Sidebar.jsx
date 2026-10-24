import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, FileText, Bell, LogOut, Menu, X,
  ChevronRight, Shield, Users, MapPin, Zap
} from 'lucide-react';

const citizenLinks = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/complaints/my', icon: FileText, label: 'My Complaints' },
  { to: '/complaints/submit', icon: MapPin, label: 'Submit Issue' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
];

const officerLinks = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Overview' },
  { to: '/complaints/all', icon: FileText, label: 'All Complaints' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
];

const adminLinks = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Admin Overview' },
  { to: '/complaints/all', icon: FileText, label: 'All Complaints' },
  { to: '/admin/officers', icon: Users, label: 'Officers' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
];

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const links = hasRole('ADMIN') ? adminLinks : hasRole('OFFICER') ? officerLinks : citizenLinks;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleDisplay = hasRole('ADMIN') ? 'Admin' : hasRole('OFFICER') ? 'Officer' : 'Citizen';
  const roleColor = hasRole('ADMIN') ? 'text-rose-400' : hasRole('OFFICER') ? 'text-amber-400' : 'text-primary-400';

  return (
    <aside
      className={`
        relative flex flex-col h-full
        bg-surface-800 border-r border-white/8
        transition-all duration-300 ease-in-out
        ${collapsed ? 'w-16' : 'w-64'}
      `}
    >
      {/* Logo */}
      <div className="flex items-center gap-3 p-4 border-b border-white/8">
        <div className="flex-shrink-0 w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-accent-purple flex items-center justify-center shadow-glow-primary animate-pulse-glow">
          <Zap size={18} className="text-white" />
        </div>
        {!collapsed && (
          <div className="overflow-hidden">
            <div className="font-display font-bold text-white text-lg leading-tight">CivicPulse</div>
            <div className="text-xs text-white/40">Smart City Platform</div>
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="ml-auto p-1 rounded-lg text-white/40 hover:text-white hover:bg-white/10 transition-all"
          aria-label="Toggle sidebar"
          id="sidebar-toggle-btn"
        >
          {collapsed ? <ChevronRight size={16} /> : <X size={16} />}
        </button>
      </div>

      {/* Nav Links */}
      <nav className="flex-1 py-4 px-2 space-y-1 overflow-y-auto">
        {links.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `nav-link ${isActive ? 'active' : ''}`
            }
            title={collapsed ? label : undefined}
          >
            <Icon size={18} className="flex-shrink-0" />
            {!collapsed && <span className="text-sm font-medium">{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* User Profile */}
      <div className="border-t border-white/8 p-3">
        <div className={`flex items-center gap-3 px-2 py-2 rounded-xl bg-white/5 ${collapsed ? 'justify-center' : ''}`}>
          <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-accent-purple flex items-center justify-center text-sm font-bold">
            {user?.firstName?.[0]?.toUpperCase() || 'U'}
          </div>
          {!collapsed && (
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">
                {user?.firstName} {user?.lastName}
              </p>
              <p className={`text-xs font-semibold ${roleColor}`}>{roleDisplay}</p>
            </div>
          )}
        </div>
        <button
          onClick={handleLogout}
          id="logout-btn"
          className={`mt-2 w-full flex items-center gap-2 px-3 py-2 rounded-xl text-white/50 hover:text-rose-400 hover:bg-rose-500/10 transition-all text-sm ${collapsed ? 'justify-center' : ''}`}
        >
          <LogOut size={16} />
          {!collapsed && <span>Log Out</span>}
        </button>
      </div>
    </aside>
  );
}
