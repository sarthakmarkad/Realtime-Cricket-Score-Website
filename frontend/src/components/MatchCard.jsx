import { useState } from 'react';
import { Link } from 'react-router-dom';
import { startMatch } from '../api';
import { formatDateTime } from '../format';
import StatusBadge from './StatusBadge';
import ThisOver from './ThisOver';

const scoreFor = (summary, team, innings) => innings.find((i) => i.battingTeam === team.name) || null;

export default function MatchCard({ match, onStart }) {
  const [starting, setStarting] = useState(false);
  const s = match;
  const innings = s.innings || [];
  const isLive = s.status === 'LIVE' || s.status === 'INNINGS_BREAK';
  const isUpcoming = s.status === 'UPCOMING';
  const current = innings.find((i) => !i.completed) || null;

  async function handleStart() {
    setStarting(true);
    try {
      const updated = await startMatch(s.id);
      onStart(s.id, updated);
    } catch (e) {
      console.error('Failed to start match', e);
    } finally {
      setStarting(false);
    }
  }

  return (
    <article className={`card match-card ${isLive ? 'is-live' : ''}`}>
      <div className="card-top">
        <span className="venue">{s.venue}</span>
        <StatusBadge status={s.status} />
      </div>

      <div className="teams">
        {[s.teamOne, s.teamTwo].map((team) => {
          const inn = scoreFor(s, team, innings);
          const isCurrent = current && current.battingTeam === team.name;
          return (
            <div className="team-row" key={team.id}>
              <span className={`team-name ${isCurrent ? 'batting' : ''}`}>
                <span className="team-short">{team.shortName}</span>
                {team.name}
              </span>
              <span className="team-score">
                {inn ? (
                  <>
                    {inn.runs}/{inn.wickets} <small>({inn.overs})</small>
                  </>
                ) : (
                  <span className="muted">—</span>
                )}
              </span>
            </div>
          );
        })}
      </div>

      {s.status === 'LIVE' && current && current.target != null && (
        <p className="chase-line">
          <strong>{s.battingTeamShort}</strong> need <strong>{Math.max(current.target - current.runs, 0)}</strong>{' '}
          runs in <strong>{s.ballsRemaining}</strong> balls
          <span className="rates">
            CRR {s.currentRunRate} · RRR {s.requiredRunRate}
          </span>
        </p>
      )}
      {s.status === 'LIVE' && current && current.target == null && (
        <p className="chase-line">
          <span className="rates">
            CRR {s.currentRunRate} · {current.battingTeam} batting
          </span>
        </p>
      )}
      {s.status === 'INNINGS_BREAK' && (
        <p className="chase-line">
          <span className="rates">Target {innings.length ? innings[0].runs + 1 : '—'} after the break</span>
        </p>
      )}
      {isUpcoming && <p className="chase-line muted">Starts {formatDateTime(s.scheduledAt)}</p>}
      {s.status === 'COMPLETED' && <p className="result-line">{s.result}</p>}

      {isLive && <ThisOver chips={s.thisOver} />}

      {isLive && s.lastEvent && <p className="last-event">{s.lastEvent}</p>}

      <div className="card-footer">
        {isUpcoming ? (
          <button className="btn" onClick={handleStart} disabled={starting}>
            {starting ? 'Starting…' : 'Start match'}
          </button>
        ) : (
          <span className="muted small">{s.status === 'COMPLETED' ? formatDateTime(s.scheduledAt) : ''}</span>
        )}
        <Link className="card-link" to={`/matches/${s.id}`}>
          Scorecard →
        </Link>
      </div>
    </article>
  );
}
