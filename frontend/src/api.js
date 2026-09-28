const BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body && body.message) message = body.message;
    } catch {
      /* non-JSON error body */
    }
    const error = new Error(message);
    error.status = res.status;
    throw error;
  }
  return res.json();
}

export const fetchMatches = () => request('/matches');
export const fetchMatchDetail = (id) => request(`/matches/${id}`);
export const fetchScorecard = (id) => request(`/matches/${id}/scorecard`);
export const fetchCommentary = (id, limit = 40) => request(`/matches/${id}/commentary?limit=${limit}`);
export const fetchMatchSummary = (id) => request(`/matches/${id}/summary`);
export const fetchLeaderboard = () => request('/players/leaders');
export const fetchPlayer = (id) => request(`/players/${id}`);
export const fetchTeams = () => request('/teams');
export const startMatch = (id) => request(`/matches/${id}/start`, { method: 'POST' });
export const STREAM_URL = `${BASE}/stream`;
