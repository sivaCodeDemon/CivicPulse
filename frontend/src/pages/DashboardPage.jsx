import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { complaintsService, notificationsService } from '../services/complaints';
import { StatusBadge, PriorityBadge, CategoryIcon } from '../components/Badge';
import { formatRelative, formatDate } from '../utils/helpers';
import {
  TrendingUp, FileText, CheckCircle, Clock, AlertTriangle,
  Bell, ArrowRight, Plus, Zap, MapPin, Star
} from 'lucide-react';

function StatCard({ label, value, icon: Icon, color, sublabel }) {
  return (
    <div className="stat-card glass-card-hover">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-white/50 font-medium">{label}</p>
          <p className={`text-3xl font-display font-bold mt-1 ${color}`}>{value ?? '—'}</p>
          {sublabel && <p className="text-xs text-white/30 mt-1">{sublabel}</p>}
        </div>
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color.replace('text-', 'bg-').replace('-400', '-500/20').replace('-300', '-500/20')}`}>
          <Icon size={22} className={color} />
        </div>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const [complaints, setComplaints] = useState([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [cRes, nRes] = await Promise.all([
          hasRole('OFFICER') || hasRole('ADMIN')
            ? complaintsService.getAll({ page: 0, size: 8, sortBy: 'createdAt', direction: 'desc' })
            : complaintsService.getMy({ page: 0, size: 8, sortBy: 'createdAt', direction: 'desc' }),
          notificationsService.getUnreadCount().catch(() => ({ data: { data: 0 } })),
        ]);
        setComplaints(cRes.data?.data?.content || []);
        setUnread(nRes.data?.data ?? 0);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [hasRole]);

  const total = complaints.length;
  const resolved = complaints.filter((c) => c.status === 'RESOLVED').length;
  const inProgress = complaints.filter((c) => c.status === 'IN_PROGRESS').length;
  const overdue = complaints.filter((c) => c.isOverdue).length;

  const greeting = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex items-start justify-between flex-wrap gap-4">
        <div>
          <p className="text-white/40 text-sm font-medium">{greeting()},</p>
          <h1 className="text-3xl font-display font-bold text-white mt-0.5">
            {user?.firstName} {user?.lastName} 👋
          </h1>
          <p className="text-white/40 text-sm mt-1">
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        </div>
        <div className="flex items-center gap-3">
          {unread > 0 && (
            <Link to="/notifications" className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-amber-500/20 border border-amber-500/30 text-amber-300 hover:bg-amber-500/30 transition-all text-sm font-medium">
              <Bell size={16} />
              {unread} unread
            </Link>
          )}
          {(hasRole('CITIZEN') || hasRole('OFFICER') || hasRole('ADMIN')) && (
            <Link to="/complaints/submit" id="submit-complaint-btn" className="btn-primary">
              <Plus size={16} /> Submit Issue
            </Link>
          )}
        </div>
      </div>

      {/* AI insight banner */}
      <div className="glass-card p-5 border-primary-500/20 bg-gradient-to-r from-primary-600/10 to-accent-purple/10">
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-accent-purple flex items-center justify-center flex-shrink-0">
            <Zap size={20} className="text-white" />
          </div>
          <div>
            <p className="font-semibold text-white text-sm">AI Engine Active</p>
            <p className="text-white/50 text-xs mt-0.5">
              CivicPulse AI is automatically categorizing issues, detecting duplicates, and triaging priority — reducing manual effort by up to 60%.
            </p>
          </div>
          <Star size={16} className="text-primary-400 ml-auto flex-shrink-0 animate-pulse" />
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Issues" value={total} icon={FileText} color="text-primary-400" sublabel="This view" />
        <StatCard label="Resolved" value={resolved} icon={CheckCircle} color="text-accent-emerald" sublabel={`${total ? Math.round((resolved / total) * 100) : 0}% resolution rate`} />
        <StatCard label="In Progress" value={inProgress} icon={Clock} color="text-amber-400" sublabel="Being handled" />
        <StatCard label="Overdue" value={overdue} icon={AlertTriangle} color="text-rose-400" sublabel="Past SLA deadline" />
      </div>

      {/* Recent Complaints Table */}
      <div className="glass-card overflow-hidden">
        <div className="flex items-center justify-between p-5 border-b border-white/8">
          <h2 className="section-title text-lg">
            {hasRole('OFFICER') || hasRole('ADMIN') ? 'Recent Complaints' : 'My Recent Issues'}
          </h2>
          <Link
            to={hasRole('OFFICER') || hasRole('ADMIN') ? '/complaints/all' : '/complaints/my'}
            className="flex items-center gap-1.5 text-primary-400 hover:text-primary-300 text-sm font-medium transition-colors"
          >
            View all <ArrowRight size={14} />
          </Link>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 gap-3 text-white/30">
            <MapPin size={32} />
            <p className="text-sm">No complaints found</p>
            <Link to="/complaints/submit" className="btn-primary text-sm py-2 px-4">
              <Plus size={14} /> Submit your first issue
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm" id="dashboard-complaints-table">
              <thead>
                <tr className="border-b border-white/8 text-white/40 text-xs uppercase tracking-wide">
                  <th className="text-left py-3 px-5 font-medium">Issue</th>
                  <th className="text-left py-3 px-3 font-medium">Status</th>
                  <th className="text-left py-3 px-3 font-medium">Priority</th>
                  <th className="text-left py-3 px-3 font-medium">AI Score</th>
                  <th className="text-left py-3 px-5 font-medium">Submitted</th>
                </tr>
              </thead>
              <tbody>
                {complaints.map((c, i) => (
                  <tr key={c.id}
                      className="border-b border-white/5 hover:bg-white/5 transition-colors"
                      style={{ animationDelay: `${i * 50}ms` }}>
                    <td className="py-3.5 px-5">
                      <Link to={`/complaints/${c.id}`} className="flex items-center gap-2.5 hover:text-primary-300 transition-colors group">
                        <CategoryIcon category={c.category} />
                        <div>
                          <p className="font-medium text-white group-hover:text-primary-300 line-clamp-1">{c.title}</p>
                          <p className="text-white/30 text-xs mt-0.5 flex items-center gap-1">
                            <MapPin size={10} /> {c.addressText || `${c.latitude?.toFixed(4)}, ${c.longitude?.toFixed(4)}`}
                          </p>
                        </div>
                      </Link>
                    </td>
                    <td className="py-3.5 px-3"><StatusBadge status={c.status} /></td>
                    <td className="py-3.5 px-3"><PriorityBadge priority={c.priority} /></td>
                    <td className="py-3.5 px-3">
                      <span className="text-white/60">
                        {c.aiConfidenceScore ? `${Math.round(c.aiConfidenceScore * 100)}%` : '—'}
                      </span>
                    </td>
                    <td className="py-3.5 px-5 text-white/40 whitespace-nowrap">{formatRelative(c.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
