import { useMemo, useState } from 'react';
import MatchCard from '../components/MatchCard';
import Spinner from '../components/Spinner';

const FILTERS = [
  { key: 'ALL', label: 'All' },
  { key: 'LIVE', label: 'Live' },
  { key: 'UPCOMING', label: 'Upcoming' },
  { key: 'COMPLETED', label: 'Completed' },
];

export default function Dashboard({ live }) {
  const { summaries, connected, error } = live;
  const [overrides, setOverrides] = useState({});
  const [filter, setFilter] = useState('ALL');

  const matches = useMemo(
    () => Object.values(summaries).map((m) => overrides[m.id] || m),
    [summaries, overrides],
  );

  const counts = useMemo(
    () => ({
      ALL: matches.length,
      LIVE: matches.filter((m) => m.status === 'LIVE' || m.status === 'INNINGS_BREAK').length,
      UPCOMING: matches.filter((m) => m.status === 'UPCOMING').length,
      COMPLETED: matches.filter((m) => m.status === 'COMPLETED').length,
    }),
    [matches],
  );

  const visible = matches.filter((m) => {
    if (filter === 'ALL') return true;
    if (filter === 'LIVE') return m.status === 'LIVE' || m.status === 'INNINGS_BREAK';
    return m.status === filter;
  });

  const applyStart = (id, summary) => setOverrides((prev) => ({ ...prev, [id]: summary }));

  if (error) {
    return (
      <div className="container page">
        <div className="error-state">
          Could not reach the API — is the Spring Boot backend running on port 8080?
          <div className="muted small">{error}</div>
        </div>
      </div>
    );
  }
  if (matches.length === 0) {
    return (
      <div className="container page">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="container page">
      <section className="hero">
        <h1>Live Cricket Scores</h1>
        <p>
          Ball-by-ball match simulation with live scorecards, player statistics and summaries —
          pushed to this dashboard the moment each delivery is bowled.
        </p>
      </section>

      <div className="filters">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            className={`chip-filter ${filter === f.key ? 'active' : ''}`}
            onClick={() => setFilter(f.key)}
          >
            {f.label}
            <span className="count">{counts[f.key]}</span>
            {f.key === 'LIVE' && counts.LIVE > 0 && <span className="mini-dot" />}
          </button>
        ))}
        <span className={`live-pill ${connected ? 'on' : 'off'} right`}>
          <span className="dot" />
          {connected ? 'Streaming live' : 'Reconnecting…'}
        </span>
      </div>

      <div className="match-grid">
        {visible.map((m) => (
          <MatchCard key={m.id} match={m} onStart={applyStart} />
        ))}
      </div>
    </div>
  );
}
