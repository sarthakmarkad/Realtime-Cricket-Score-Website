import { useEffect, useRef } from 'react';
import { STREAM_URL } from '../api';

/**
 * Calls onEvent(payload) for every SSE update belonging to the given match.
 * The callback is kept in a ref so the stream is not re-created on re-renders.
 */
export function useMatchEvents(matchId, onEvent) {
  const handlerRef = useRef(onEvent);
  handlerRef.current = onEvent;

  useEffect(() => {
    const source = new EventSource(STREAM_URL);
    source.addEventListener('update', (e) => {
      try {
        const payload = JSON.parse(e.data);
        if (String(payload.matchId) === String(matchId)) {
          handlerRef.current(payload);
        }
      } catch {
        /* ignore malformed payloads */
      }
    });
    return () => source.close();
  }, [matchId]);
}
