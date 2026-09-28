const LABELS = {
  LIVE: 'Live',
  INNINGS_BREAK: 'Innings break',
  UPCOMING: 'Upcoming',
  COMPLETED: 'Completed',
};

const CLASSES = {
  LIVE: 'badge badge-live',
  INNINGS_BREAK: 'badge badge-break',
  UPCOMING: 'badge badge-upcoming',
  COMPLETED: 'badge badge-completed',
};

export default function StatusBadge({ status }) {
  return <span className={CLASSES[status] || 'badge'}>{LABELS[status] || status}</span>;
}
