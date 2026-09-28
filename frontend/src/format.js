export function formatDateTime(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString([], {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatClock(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/** Maps a ball chip string ("0","1","4","6","W","wd","nb") to its CSS class. */
export function chipClass(chip) {
  if (chip === 'W') return 'chip chip-wicket';
  if (chip === 'wd' || chip === 'nb') return 'chip chip-extra';
  if (chip === '4') return 'chip chip-four';
  if (chip === '6') return 'chip chip-six';
  return 'chip chip-dot';
}

export const chipLabel = (chip) => (chip === '0' ? '•' : chip);

/** Maps a CommentaryBall API object to the same chip vocabulary. */
export function ballToChip(ball) {
  if (ball.wicket) return 'W';
  if (ball.extraType === 'WIDE') return 'wd';
  if (ball.extraType === 'NO_BALL') return 'nb';
  return String(ball.runsScored);
}

export const roleLabel = (role) =>
  (role || '')
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
