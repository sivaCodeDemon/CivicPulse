import { useEffect, useState } from 'react';
import { notificationsService } from '../services/complaints';
import { formatRelative } from '../utils/helpers';
import { Bell, BellOff, Check, CheckCheck, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

const TYPE_ICONS = {
  COMPLAINT_SUBMITTED: '📋',
  STATUS_UPDATED:      '🔄',
  COMPLAINT_ASSIGNED:  '👤',
  COMPLAINT_RESOLVED:  '✅',
  SLA_WARNING:         '⚠️',
};

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [marking, setMarking] = useState(false);

  const fetchNotifs = async () => {
    setLoading(true);
    try {
      const res = await notificationsService.getAll({ page: 0, size: 50 });
      setNotifications(res.data?.data?.content || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchNotifs(); }, []);

  const handleMarkAllRead = async () => {
    setMarking(true);
    try {
      await notificationsService.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      toast.success('All notifications marked as read');
    } catch {
      toast.error('Failed to mark notifications as read');
    } finally {
      setMarking(false);
    }
  };

  const handleMarkOne = async (id) => {
    try {
      await notificationsService.markRead(id);
      setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));
    } catch {}
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="section-title">Notifications</h1>
          <p className="text-white/40 text-sm mt-1">{unreadCount} unread</p>
        </div>
        {unreadCount > 0 && (
          <button onClick={handleMarkAllRead} disabled={marking}
            className="btn-secondary text-sm py-2 px-4" id="mark-all-read-btn">
            {marking ? <Loader2 size={14} className="animate-spin" /> : <CheckCheck size={14} />}
            Mark all read
          </button>
        )}
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-48">
          <Loader2 size={28} className="animate-spin text-primary-400" />
        </div>
      ) : notifications.length === 0 ? (
        <div className="glass-card flex flex-col items-center justify-center h-48 gap-3 text-white/30">
          <BellOff size={32} />
          <p>No notifications yet</p>
        </div>
      ) : (
        <div className="space-y-2" id="notifications-list">
          {notifications.map((n) => (
            <div
              key={n.id}
              className={`glass-card p-4 flex items-start gap-4 transition-all cursor-pointer
                ${!n.read ? 'border-primary-500/30 bg-primary-500/5' : ''}`}
              onClick={() => !n.read && handleMarkOne(n.id)}
            >
              <div className="flex-shrink-0 w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-xl">
                {TYPE_ICONS[n.type] || '🔔'}
              </div>
              <div className="flex-1 min-w-0">
                <p className={`text-sm font-medium ${!n.read ? 'text-white' : 'text-white/60'}`}>{n.title}</p>
                <p className="text-white/40 text-xs mt-0.5 leading-relaxed">{n.message}</p>
                <p className="text-white/25 text-xs mt-1.5">{formatRelative(n.createdAt)}</p>
              </div>
              {!n.read ? (
                <div className="flex-shrink-0 w-2 h-2 rounded-full bg-primary-400 mt-2 animate-pulse" />
              ) : (
                <Check size={14} className="flex-shrink-0 text-white/20 mt-1" />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
