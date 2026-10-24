import { STATUS_MAP, PRIORITY_MAP, SEVERITY_MAP } from '../utils/helpers';

export function StatusBadge({ status }) {
  const meta = STATUS_MAP[status] || { label: status, className: 'badge bg-white/10 text-white/60' };
  return (
    <span className={meta.className}>
      <span className={`glow-dot ${meta.dot || 'bg-white/40'}`} />
      {meta.label}
    </span>
  );
}

export function PriorityBadge({ priority }) {
  const meta = PRIORITY_MAP[priority] || { label: priority, className: 'badge bg-white/10 text-white/60' };
  return <span className={meta.className}>{meta.label}</span>;
}

export function SeverityBadge({ severity }) {
  const meta = SEVERITY_MAP[severity] || { label: severity, className: 'badge bg-white/10 text-white/60' };
  return <span className={meta.className}>{meta.label}</span>;
}

export function CategoryIcon({ category }) {
  const icons = {
    POTHOLE: '🕳️',
    GARBAGE_OVERFLOW: '🗑️',
    WATER_LEAKAGE: '💧',
    BROKEN_STREETLIGHT: '💡',
    FALLEN_TREE: '🌳',
    OTHER: '⚠️',
  };
  return <span className="text-lg">{icons[category] || '⚠️'}</span>;
}
