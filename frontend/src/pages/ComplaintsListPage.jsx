import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { complaintsService } from '../services/complaints';
import { useAuth } from '../context/AuthContext';
import { StatusBadge, PriorityBadge, CategoryIcon } from '../components/Badge';
import { formatRelative } from '../utils/helpers';
import { Search, Filter, MapPin, ArrowRight, Loader2, RefreshCw } from 'lucide-react';

const STATUSES_FILTER = [
  { value: '', label: 'All Statuses' },
  { value: 'REPORTED', label: 'Reported' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'ASSIGNED', label: 'Assigned' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'REOPENED', label: 'Reopened' },
];

export default function ComplaintsListPage({ isMyComplaints = false }) {
  const { hasRole } = useAuth();
  const isOfficerOrAdmin = hasRole('OFFICER') || hasRole('ADMIN');

  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [deptFilter, setDeptFilter] = useState('');

  const fetchComplaints = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 15,
        sortBy: 'createdAt',
        direction: 'desc',
        ...(statusFilter && { status: statusFilter }),
        ...(deptFilter && { departmentId: deptFilter }),
      };

      const res = isMyComplaints
        ? await complaintsService.getMy(params)
        : await complaintsService.getAll(params);

      const data = res.data?.data;
      setComplaints(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, deptFilter, isMyComplaints]);

  useEffect(() => { fetchComplaints(); }, [fetchComplaints]);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="section-title">
            {isMyComplaints ? 'My Complaints' : 'All Municipal Complaints'}
          </h1>
          <p className="text-white/40 text-sm mt-1">
            {totalElements} total issues found
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={fetchComplaints} className="btn-secondary text-sm py-2 px-4" id="refresh-btn">
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Refresh
          </button>
          {!isMyComplaints && (
            <Link to="/complaints/submit" className="btn-primary text-sm py-2 px-4">
              + Submit Issue
            </Link>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="glass-card p-4 flex flex-wrap gap-3 items-center">
        <Filter size={16} className="text-white/40" />
        <select
          id="status-filter"
          className="input-field !w-auto !py-2 text-sm"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
        >
          {STATUSES_FILTER.map((s) => (
            <option key={s.value} value={s.value}>{s.label}</option>
          ))}
        </select>
        {statusFilter && (
          <button onClick={() => { setStatusFilter(''); setPage(0); }}
            className="text-white/40 hover:text-white text-sm transition-colors">
            Clear filter
          </button>
        )}
        <p className="ml-auto text-white/30 text-xs">
          Page {page + 1} of {totalPages || 1}
        </p>
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <Loader2 size={32} className="animate-spin text-primary-400" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 gap-3 text-white/30">
            <Search size={32} />
            <p>No complaints found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm" id="complaints-table">
              <thead>
                <tr className="border-b border-white/8 text-white/40 text-xs uppercase tracking-wide">
                  <th className="text-left py-3 px-5 font-medium">Issue</th>
                  <th className="text-left py-3 px-3 font-medium">Status</th>
                  <th className="text-left py-3 px-3 font-medium">Priority</th>
                  {isOfficerOrAdmin && <th className="text-left py-3 px-3 font-medium">Citizen</th>}
                  <th className="text-left py-3 px-3 font-medium">Department</th>
                  <th className="text-left py-3 px-3 font-medium">AI Score</th>
                  <th className="text-left py-3 px-5 font-medium">Submitted</th>
                  <th className="py-3 px-3" />
                </tr>
              </thead>
              <tbody>
                {complaints.map((c) => (
                  <tr key={c.id} className="border-b border-white/5 hover:bg-white/5 transition-colors group">
                    <td className="py-3.5 px-5">
                      <div className="flex items-center gap-2.5">
                        <CategoryIcon category={c.category} />
                        <div>
                          <p className="font-medium text-white group-hover:text-primary-300 transition-colors line-clamp-1">
                            {c.title}
                          </p>
                          <p className="text-white/30 text-xs mt-0.5 flex items-center gap-1">
                            <MapPin size={10} />
                            {c.addressText || `${c.latitude?.toFixed(3)}, ${c.longitude?.toFixed(3)}`}
                          </p>
                          {c.isPotentialDuplicate && (
                            <span className="text-amber-400 text-xs">⚠ Potential duplicate</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-3"><StatusBadge status={c.status} /></td>
                    <td className="py-3.5 px-3"><PriorityBadge priority={c.priority} /></td>
                    {isOfficerOrAdmin && (
                      <td className="py-3.5 px-3">
                        <p className="text-white/60 text-xs">{c.citizenName}</p>
                        <p className="text-white/30 text-xs">{c.citizenEmail}</p>
                      </td>
                    )}
                    <td className="py-3.5 px-3 text-white/50 text-xs">{c.departmentName || '—'}</td>
                    <td className="py-3.5 px-3">
                      <span className={`text-xs font-mono ${c.aiConfidenceScore >= 0.9 ? 'text-emerald-400' : c.aiConfidenceScore >= 0.7 ? 'text-amber-400' : 'text-white/40'}`}>
                        {c.aiConfidenceScore ? `${Math.round(c.aiConfidenceScore * 100)}%` : '—'}
                      </span>
                    </td>
                    <td className="py-3.5 px-5 text-white/40 whitespace-nowrap text-xs">{formatRelative(c.createdAt)}</td>
                    <td className="py-3.5 px-3">
                      <Link to={`/complaints/${c.id}`}
                        className="flex items-center gap-1 text-primary-400/60 hover:text-primary-300 transition-colors text-xs">
                        View <ArrowRight size={12} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 p-4 border-t border-white/8">
            <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}
              className="btn-secondary text-sm py-1.5 px-4 disabled:opacity-30">
              Previous
            </button>
            <span className="text-white/40 text-sm">Page {page + 1} of {totalPages}</span>
            <button onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}
              className="btn-secondary text-sm py-1.5 px-4 disabled:opacity-30">
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
