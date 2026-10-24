import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { complaintsService } from '../services/complaints';
import { useAuth } from '../context/AuthContext';
import { StatusBadge, PriorityBadge, SeverityBadge, CategoryIcon } from '../components/Badge';
import { formatDate, formatRelative, getApiError, STATUSES } from '../utils/helpers';
import {
  MapPin, Calendar, User, Clock, ChevronLeft, Zap, AlertCircle,
  CheckCircle, Copy, Loader2, Image as ImageIcon
} from 'lucide-react';
import toast from 'react-hot-toast';

function TimelineEntry({ entry }) {
  return (
    <div className="flex gap-4">
      <div className="flex flex-col items-center">
        <div className="w-3 h-3 rounded-full bg-primary-500 border-2 border-surface-900 flex-shrink-0 mt-1" />
        <div className="w-px flex-1 bg-white/10 mt-2" />
      </div>
      <div className="pb-5 flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <StatusBadge status={entry.newStatus} />
          <span className="text-white/30 text-xs">← from</span>
          {entry.previousStatus
            ? <StatusBadge status={entry.previousStatus} />
            : <span className="badge bg-white/10 text-white/30">Initial</span>}
        </div>
        <p className="text-white/60 text-xs mt-1.5">
          by <span className="text-white/80 font-medium">{entry.changedByName}</span> · {formatRelative(entry.createdAt)}
        </p>
        {entry.remarks && <p className="text-white/40 text-xs mt-1 italic">"{entry.remarks}"</p>}
      </div>
    </div>
  );
}

export default function ComplaintDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();

  const [complaint, setComplaint] = useState(null);
  const [history, setHistory] = useState([]);
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [statusForm, setStatusForm] = useState({ status: '', remarks: '' });
  const [officerId, setOfficerId] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [cRes, hRes, iRes] = await Promise.all([
          complaintsService.getById(id),
          complaintsService.getHistory(id),
          complaintsService.getImages(id),
        ]);
        setComplaint(cRes.data?.data);
        setHistory(hRes.data?.data || []);
        setImages(iRes.data?.data || []);
      } catch (e) {
        toast.error(getApiError(e));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  const handleStatusUpdate = async (e) => {
    e.preventDefault();
    if (!statusForm.status) return;
    setUpdating(true);
    try {
      const res = await complaintsService.updateStatus(id, {
        status: statusForm.status,
        remarks: statusForm.remarks || null,
      });
      setComplaint(res.data?.data);
      setHistory((h) => [
        ...h,
        {
          id: Date.now(),
          previousStatus: complaint.status,
          newStatus: statusForm.status,
          changedByName: 'You',
          remarks: statusForm.remarks,
          createdAt: new Date().toISOString(),
        },
      ]);
      setStatusForm({ status: '', remarks: '' });
      toast.success('Status updated successfully');
    } catch (e) {
      toast.error(getApiError(e));
    } finally {
      setUpdating(false);
    }
  };

  const handleAssign = async (e) => {
    e.preventDefault();
    if (!officerId) return;
    setUpdating(true);
    try {
      await complaintsService.assign(id, { officerId, notes: 'Assigned from dashboard' });
      toast.success('Complaint assigned to officer');
      setOfficerId('');
    } catch (e) {
      toast.error(getApiError(e));
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="w-10 h-10 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
    </div>
  );

  if (!complaint) return (
    <div className="flex flex-col items-center justify-center h-64 gap-4 text-white/30">
      <AlertCircle size={40} />
      <p>Complaint not found</p>
    </div>
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in">
      {/* Back button */}
      <button onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-white/40 hover:text-white transition-colors text-sm">
        <ChevronLeft size={16} /> Back
      </button>

      {/* Header */}
      <div className="glass-card p-6">
        <div className="flex items-start gap-4">
          <div className="flex-shrink-0 w-12 h-12 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-2xl">
            <CategoryIcon category={complaint.category} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <StatusBadge status={complaint.status} />
              <PriorityBadge priority={complaint.priority} />
              <SeverityBadge severity={complaint.severity} />
              {complaint.isPotentialDuplicate && (
                <span className="badge bg-amber-500/20 text-amber-300 border border-amber-500/30">
                  ⚠ Potential Duplicate
                </span>
              )}
              {complaint.isOverdue && (
                <span className="badge bg-rose-500/20 text-rose-300 border border-rose-500/30">
                  ⏱ Overdue
                </span>
              )}
            </div>
            <h1 className="text-xl font-display font-bold text-white leading-snug">{complaint.title}</h1>
            <div className="flex flex-wrap items-center gap-4 mt-2 text-white/40 text-xs">
              <span className="flex items-center gap-1"><User size={12} />{complaint.citizenName}</span>
              <span className="flex items-center gap-1"><Calendar size={12} />{formatDate(complaint.createdAt)}</span>
              <span className="flex items-center gap-1"><MapPin size={12} />{complaint.addressText || `${complaint.latitude?.toFixed(4)}, ${complaint.longitude?.toFixed(4)}`}</span>
              {complaint.departmentName && (
                <span className="flex items-center gap-1">🏢 {complaint.departmentName}</span>
              )}
            </div>
          </div>
          <button
            onClick={() => { navigator.clipboard.writeText(complaint.id); toast.success('ID copied!'); }}
            className="flex-shrink-0 text-white/30 hover:text-white/60 transition-colors"
            title="Copy complaint ID"
          >
            <Copy size={16} />
          </button>
        </div>

        <div className="mt-5 p-4 rounded-xl bg-white/3 border border-white/8">
          <p className="text-white/70 text-sm leading-relaxed">{complaint.description}</p>
        </div>

        {/* AI Analysis */}
        <div className="mt-4 p-4 rounded-xl bg-gradient-to-r from-primary-600/10 to-accent-purple/5 border border-primary-500/20">
          <div className="flex items-center gap-2 mb-2">
            <Zap size={14} className="text-primary-400" />
            <p className="text-xs font-semibold text-primary-300 uppercase tracking-wide">AI Analysis</p>
          </div>
          <div className="grid grid-cols-3 gap-4 text-xs">
            <div>
              <p className="text-white/40">Suggested Category</p>
              <p className="text-white font-medium mt-0.5">{complaint.aiSuggestedCategory || '—'}</p>
            </div>
            <div>
              <p className="text-white/40">Confidence Score</p>
              <p className="text-white font-medium mt-0.5">
                {complaint.aiConfidenceScore ? `${Math.round(complaint.aiConfidenceScore * 100)}%` : '—'}
              </p>
            </div>
            <div>
              <p className="text-white/40">SLA Deadline</p>
              <p className={`font-medium mt-0.5 ${complaint.isOverdue ? 'text-rose-400' : 'text-white'}`}>
                {formatDate(complaint.slaDeadline)}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Timeline */}
        <div className="lg:col-span-2 glass-card p-6">
          <h2 className="text-lg font-display font-semibold text-white mb-5 flex items-center gap-2">
            <Clock size={16} className="text-primary-400" /> Status Timeline
          </h2>
          {history.length === 0 ? (
            <p className="text-white/30 text-sm">No history yet</p>
          ) : (
            <div>
              {history.map((h) => <TimelineEntry key={h.id} entry={h} />)}
            </div>
          )}
        </div>

        {/* Actions sidebar */}
        <div className="space-y-4">
          {/* Images */}
          {images.length > 0 && (
            <div className="glass-card p-4">
              <h3 className="text-sm font-semibold text-white/70 mb-3 flex items-center gap-2">
                <ImageIcon size={14} /> Attachments ({images.length})
              </h3>
              <div className="grid grid-cols-2 gap-2">
                {images.slice(0, 4).map((img) => (
                  <a key={img.id}
                    href={`http://localhost:8080/api/v1/complaints/${id}/images/${img.id}`}
                    target="_blank" rel="noreferrer"
                    className="aspect-square rounded-xl bg-white/5 border border-white/10 flex items-center justify-center hover:bg-white/10 transition-colors">
                    <ImageIcon size={20} className="text-white/30" />
                  </a>
                ))}
              </div>
            </div>
          )}

          {/* Officer/Admin: Update Status */}
          {(hasRole('OFFICER') || hasRole('ADMIN')) && (
            <div className="glass-card p-4">
              <h3 className="text-sm font-semibold text-white/70 mb-3">Update Status</h3>
              <form onSubmit={handleStatusUpdate} id="update-status-form" className="space-y-3">
                <select
                  className="input-field text-sm"
                  value={statusForm.status}
                  onChange={(e) => setStatusForm({ ...statusForm, status: e.target.value })}
                  required
                >
                  <option value="">Select new status…</option>
                  {STATUSES.map((s) => (
                    <option key={s.value} value={s.value} disabled={s.value === complaint.status}>
                      {s.label}
                    </option>
                  ))}
                </select>
                <input
                  placeholder="Remarks (optional)"
                  className="input-field text-sm"
                  value={statusForm.remarks}
                  onChange={(e) => setStatusForm({ ...statusForm, remarks: e.target.value })}
                />
                <button type="submit" id="update-status-btn" disabled={updating}
                  className="btn-primary w-full text-sm py-2.5">
                  {updating ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />}
                  Update Status
                </button>
              </form>
            </div>
          )}

          {/* Admin: Assign */}
          {hasRole('ADMIN') && (
            <div className="glass-card p-4">
              <h3 className="text-sm font-semibold text-white/70 mb-3">Assign to Officer</h3>
              <form onSubmit={handleAssign} id="assign-complaint-form" className="space-y-3">
                <input
                  placeholder="Officer User ID (UUID)"
                  className="input-field text-sm font-mono"
                  value={officerId}
                  onChange={(e) => setOfficerId(e.target.value)}
                  required
                />
                <button type="submit" id="assign-btn" disabled={updating}
                  className="btn-secondary w-full text-sm py-2.5">
                  {updating ? <Loader2 size={14} className="animate-spin" /> : null}
                  Assign Officer
                </button>
              </form>
            </div>
          )}

          {/* Resolved info */}
          {complaint.resolvedAt && (
            <div className="glass-card p-4 bg-emerald-500/5 border-emerald-500/20">
              <div className="flex items-center gap-2 text-emerald-400 text-sm font-semibold mb-1">
                <CheckCircle size={16} /> Issue Resolved
              </div>
              <p className="text-white/40 text-xs">{formatDate(complaint.resolvedAt)}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
