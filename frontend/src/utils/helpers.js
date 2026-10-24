/**
 * Maps backend enum values to display-friendly labels and CSS classes.
 */

export const STATUS_MAP = {
  REPORTED:    { label: 'Reported',     className: 'badge-reported',   dot: 'bg-blue-400' },
  VERIFIED:    { label: 'Verified',     className: 'badge-verified',   dot: 'bg-cyan-400' },
  ASSIGNED:    { label: 'Assigned',     className: 'badge-assigned',   dot: 'bg-violet-400' },
  IN_PROGRESS: { label: 'In Progress',  className: 'badge-inprogress', dot: 'bg-amber-400' },
  RESOLVED:    { label: 'Resolved',     className: 'badge-resolved',   dot: 'bg-emerald-400' },
  REOPENED:    { label: 'Reopened',     className: 'badge-reopened',   dot: 'bg-rose-400' },
};

export const PRIORITY_MAP = {
  CRITICAL: { label: 'Critical', className: 'badge-critical' },
  HIGH:     { label: 'High',     className: 'badge-high' },
  MEDIUM:   { label: 'Medium',   className: 'badge-medium' },
  LOW:      { label: 'Low',      className: 'badge-low' },
};

export const SEVERITY_MAP = {
  EMERGENCY: { label: 'Emergency', className: 'badge-emergency' },
  SEVERE:    { label: 'Severe',    className: 'badge-severe' },
  MODERATE:  { label: 'Moderate',  className: 'badge-moderate' },
  MINOR:     { label: 'Minor',     className: 'badge-minor' },
};

export const CATEGORY_MAP = {
  POTHOLE:           { label: 'Pothole',            icon: '🕳️' },
  GARBAGE_OVERFLOW:  { label: 'Garbage Overflow',   icon: '🗑️' },
  WATER_LEAKAGE:     { label: 'Water Leakage',      icon: '💧' },
  BROKEN_STREETLIGHT:{ label: 'Broken Streetlight', icon: '💡' },
  FALLEN_TREE:       { label: 'Fallen Tree',        icon: '🌳' },
  OTHER:             { label: 'Other',              icon: '⚠️' },
};

export const CATEGORIES = Object.entries(CATEGORY_MAP).map(([value, meta]) => ({
  value,
  ...meta,
}));

export const PRIORITIES = Object.entries(PRIORITY_MAP).map(([value, meta]) => ({
  value,
  ...meta,
}));

export const STATUSES = Object.entries(STATUS_MAP).map(([value, meta]) => ({
  value,
  ...meta,
}));

export const formatDate = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const formatRelative = (isoString) => {
  if (!isoString) return '—';
  const now = new Date();
  const then = new Date(isoString);
  const diff = Math.floor((now - then) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
};

export const getApiError = (error) => {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message
    || 'An unexpected error occurred';
};
