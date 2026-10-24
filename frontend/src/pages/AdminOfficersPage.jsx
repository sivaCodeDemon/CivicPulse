import { useEffect, useState, useCallback } from 'react';
import { adminService } from '../services/complaints';
import { formatRelative, getApiError } from '../utils/helpers';
import {
  Users, Shield, RefreshCw, Search, ChevronLeft, ChevronRight,
  CheckCircle, XCircle, Copy, Loader2, UserCheck, UserX, Crown, User
} from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_FILTERS = [
  { value: '',             label: 'All Users',  icon: Users,       color: 'text-white/60' },
  { value: 'ROLE_CITIZEN', label: 'Citizens',   icon: User,        color: 'text-primary-400' },
  { value: 'ROLE_OFFICER', label: 'Officers',   icon: Shield,      color: 'text-amber-400' },
  { value: 'ROLE_ADMIN',   label: 'Admins',     icon: Crown,       color: 'text-rose-400' },
];

function RolePill({ role }) {
  if (role === 'ROLE_ADMIN')   return <span className="badge bg-rose-500/20 text-rose-300 border border-rose-500/30">Admin</span>;
  if (role === 'ROLE_OFFICER') return <span className="badge bg-amber-500/20 text-amber-300 border border-amber-500/30">Officer</span>;
  return <span className="badge bg-primary-500/20 text-primary-300 border border-primary-500/30">Citizen</span>;
}

function UserRow({ user, onToggleActive }) {
  const [busy, setBusy] = useState(false);

  const handleToggle = async () => {
    setBusy(true);
    try {
      await onToggleActive(user.id, user.active);
    } finally {
      setBusy(false);
    }
  };

  const copyId = () => {
    navigator.clipboard.writeText(user.id);
    toast.success('User ID copied!');
  };

  return (
    <tr className="border-b border-white/5 hover:bg-white/5 transition-colors group">
      <td className="py-3.5 px-5">
        <div className="flex items-center gap-3">
          <div className="flex-shrink-0 w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500/30 to-accent-purple/30 flex items-center justify-center text-sm font-bold text-white/80">
            {user.firstName?.[0]?.toUpperCase() || '?'}
          </div>
          <div>
            <p className="font-medium text-white text-sm">{user.firstName} {user.lastName}</p>
            <p className="text-white/40 text-xs mt-0.5">{user.email}</p>
          </div>
        </div>
      </td>
      <td className="py-3.5 px-3">
        <div className="flex flex-wrap gap-1">
          {user.roles?.map((r) => <RolePill key={r} role={r} />)}
        </div>
      </td>
      <td className="py-3.5 px-3">
        {user.active ? (
          <span className="flex items-center gap-1.5 text-xs text-emerald-400">
            <CheckCircle size={12} /> Active
          </span>
        ) : (
          <span className="flex items-center gap-1.5 text-xs text-rose-400">
            <XCircle size={12} /> Deactivated
          </span>
        )}
      </td>
      <td className="py-3.5 px-3 text-white/40 text-xs">{user.phoneNumber || '—'}</td>
      <td className="py-3.5 px-3 text-white/40 text-xs whitespace-nowrap">{formatRelative(user.createdAt)}</td>
      <td className="py-3.5 px-5">
        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={copyId}
            title="Copy User ID"
            className="p-1.5 rounded-lg text-white/30 hover:text-white/70 hover:bg-white/10 transition-all"
          >
            <Copy size={13} />
          </button>
          <button
            onClick={handleToggle}
            disabled={busy}
            title={user.active ? 'Deactivate account' : 'Reactivate account'}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all
              ${user.active
                ? 'bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 border border-rose-500/20'
                : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 border border-emerald-500/20'
              } disabled:opacity-50`}
          >
            {busy
              ? <Loader2 size={12} className="animate-spin" />
              : user.active ? <UserX size={12} /> : <UserCheck size={12} />}
            {user.active ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      </td>
    </tr>
  );
}

export default function AdminOfficersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [roleFilter, setRoleFilter] = useState('');
  const [search, setSearch] = useState('');

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 15,
        sortBy: 'createdAt',
        direction: 'desc',
        ...(roleFilter && { role: roleFilter }),
      };
      const res = await adminService.listUsers(params);
      const data = res.data?.data;
      setUsers(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (e) {
      toast.error(getApiError(e));
    } finally {
      setLoading(false);
    }
  }, [page, roleFilter]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const handleToggleActive = async (userId, currentlyActive) => {
    try {
      if (currentlyActive) {
        await adminService.deactivateUser(userId);
        toast.success('Account deactivated');
      } else {
        await adminService.activateUser(userId);
        toast.success('Account reactivated');
      }
      setUsers((prev) =>
        prev.map((u) => u.id === userId ? { ...u, active: !currentlyActive } : u)
      );
    } catch (e) {
      toast.error(getApiError(e));
    }
  };

  // Client-side name/email search
  const filtered = search.trim()
    ? users.filter((u) =>
        `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(search.toLowerCase())
      )
    : users;

  const activeCount  = users.filter((u) => u.active).length;
  const officerCount = users.filter((u) => u.roles?.includes('ROLE_OFFICER')).length;
  const adminCount   = users.filter((u) => u.roles?.includes('ROLE_ADMIN')).length;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-start justify-between flex-wrap gap-4">
        <div>
          <h1 className="section-title flex items-center gap-2">
            <Users size={24} className="text-primary-400" />
            User Management
          </h1>
          <p className="text-white/40 text-sm mt-1">
            {totalElements} registered accounts across the platform
          </p>
        </div>
        <button onClick={fetchUsers} className="btn-secondary text-sm py-2 px-4" id="refresh-users-btn">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total Users', value: totalElements, icon: Users,       color: 'text-primary-400', bg: 'bg-primary-500/10' },
          { label: 'Active',      value: activeCount,   icon: CheckCircle, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
          { label: 'Officers',    value: officerCount,  icon: Shield,      color: 'text-amber-400',   bg: 'bg-amber-500/10'   },
          { label: 'Admins',      value: adminCount,    icon: Crown,       color: 'text-rose-400',    bg: 'bg-rose-500/10'    },
        ].map(({ label, value, icon: Icon, color, bg }) => (
          <div key={label} className="glass-card p-5 flex items-center gap-4">
            <div className={`w-10 h-10 rounded-xl ${bg} flex items-center justify-center flex-shrink-0`}>
              <Icon size={18} className={color} />
            </div>
            <div>
              <p className="text-white/40 text-xs font-medium">{label}</p>
              <p className={`text-2xl font-display font-bold mt-0.5 ${color}`}>{value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="glass-card p-4 flex flex-wrap gap-3 items-center">
        <div className="flex items-center gap-1 bg-white/5 rounded-xl p-1">
          {ROLE_FILTERS.map(({ value, label, icon: Icon, color }) => (
            <button
              key={value}
              id={`role-filter-${value || 'all'}`}
              onClick={() => { setRoleFilter(value); setPage(0); }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all
                ${roleFilter === value
                  ? 'bg-white/15 text-white border border-white/20'
                  : 'text-white/50 hover:text-white/80 hover:bg-white/8'
                }`}
            >
              <Icon size={12} className={roleFilter === value ? color : ''} />
              {label}
            </button>
          ))}
        </div>

        <div className="relative ml-auto">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/30" />
          <input
            id="user-search"
            placeholder="Search name or email…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input-field !py-2 pl-8 pr-4 !w-56 text-sm"
          />
        </div>

        <p className="text-white/30 text-xs">
          Page {page + 1} of {totalPages || 1}
        </p>
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <Loader2 size={32} className="animate-spin text-primary-400" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 gap-3 text-white/30">
            <Users size={32} />
            <p className="text-sm">No users found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm" id="users-table">
              <thead>
                <tr className="border-b border-white/8 text-white/40 text-xs uppercase tracking-wide">
                  <th className="text-left py-3 px-5 font-medium">User</th>
                  <th className="text-left py-3 px-3 font-medium">Role</th>
                  <th className="text-left py-3 px-3 font-medium">Status</th>
                  <th className="text-left py-3 px-3 font-medium">Phone</th>
                  <th className="text-left py-3 px-3 font-medium">Joined</th>
                  <th className="py-3 px-5" />
                </tr>
              </thead>
              <tbody>
                {filtered.map((u) => (
                  <UserRow key={u.id} user={u} onToggleActive={handleToggleActive} />
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-3 p-4 border-t border-white/8">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-30 gap-1"
            >
              <ChevronLeft size={14} /> Previous
            </button>
            <span className="text-white/40 text-sm font-medium">{page + 1} / {totalPages}</span>
            <button
              onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              className="btn-secondary text-sm py-1.5 px-3 disabled:opacity-30 gap-1"
            >
              Next <ChevronRight size={14} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
