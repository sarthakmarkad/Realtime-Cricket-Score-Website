import { useEffect, useState } from 'react';
import { fetchMatches, STREAM_URL } from '../api';

/**
 * Subscribes to the live SSE stream and keeps a live map of matchId -> summary.
 * The initial snapshot comes from GET /api/matches; every simulated delivery and
 * status change pushed over /api/stream then overrides the matching entry.
 * EventSource reconnects on its own if the connection drops.
 */
export function useLiveScores() {
  const [summaries, setSummaries] = useState({});
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    fetchMatches()
      .then((list) => {
        if (cancelled) return;
        setSummaries(Object.fromEntries(list.map((m) => [m.id, m])));
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });

    const source = new EventSource(STREAM_URL);
    source.onopen = () => setConnected(true);
    source.onerror = () => setConnected(false);
    source.addEventListener('update', (e) => {
      try {
        const payload = JSON.parse(e.data);
        setSummaries((prev) => ({ ...prev, [payload.matchId]: payload.summary }));
      } catch {
        /* ignore malformed payloads */
      }
    });

    return () => {
      cancelled = true;
      source.close();
    };
  }, []);

  return { summaries, connected, error };
}
