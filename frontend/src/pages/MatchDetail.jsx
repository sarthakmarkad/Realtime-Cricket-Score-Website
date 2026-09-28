import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import CommentaryFeed from '../components/CommentaryFeed';
import InningsScorecard from '../components/InningsScorecard';
import Spinner from '../components/Spinner';
import StatusBadge from '../components/StatusBadge';
import ThisOver from '../components/ThisOver';
import { fetchCommentary, fetchMatchDetail, fetchMatchSummary } from '../api';
import { useMatchEvents } from '../hooks/useMatchEvents';
import { formatDateTime } from '../format';

const TABS = [
  { key: 'scorecard', label: 'Scorecard' },
  { key: 'commentary', label: 'Commentary' },
  { key: 'summary', label: 'Summary' },
];

export default function MatchDetail() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [commentary, setCommentary] = useState([]);
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState(null);
  const [tab, setTab] = useState('scorecard');

  const load = useCallback(async () => {
    try {
      setDetail(await fetchMatchDetail(id));
    } catch (e) {
      setError(e.message);
    }
  }, [id]);

  const loadCommentary = useCallback(async () => {
    try {
      setCommentary(await fetchCommentary(id, 40));
    } catch {
      /* keep previous commentary */
    }
  }, [id]);

  useEffect(() => {
    load();
    loadCommentary();
    fetchMatchSummary(id)
      .then(setSummary)
      .catch(() => {});
  }, [id, load, loadCommentary]);

  useMatchEvents(id, () => {
    load();
    loadCommentary();
  });

  if (error) {
    return (
      <div className="container page">
        <div className="error-state">
          {error}
          <div>
            <Link to="/" className="card-link">
              ← Back to matches
            </Link>
          </div>
        </div>
      </div>
    );
  }
  if (!detail) {
    return (
      <div className="container page">
        <Spinner />
      </div>
    );
  }

  const s = detail.summary;
  const shortNameOf = (teamName) =>
    s.teamOne.name === teamName ? s.teamOne.shortName : s.teamTwo.shortName;
  const current = (s.innings || []).find((i) => !i.completed) || null;
  const isLive = s.status === 'LIVE' || s.status === 'INNINGS_BREAK';

  return (
    <div className="page detail-page">
      <div className="detail-hero">
        <div className="container">
          <Link to="/" className="card-link back-link">
            ← All matches
          </Link>
          <div className="detail-top">
            <span className="venue">{s.venue}</span>
            <StatusBadge status={s.status} />
          </div>
          <div className="detail-score">
            {(s.innings || []).map((inn) => (
              <div className="score-chunk" key={inn.inningsNumber}>
                <span className="score-team">{shortNameOf(inn.battingTeam)}</span>
                <span className="score-big">
                  {inn.runs}/{inn.wickets}
                </span>
                <span className="score-overs">({inn.overs} ov)</span>
              </div>
            ))}
          </div>

          {isLive && current && current.target != null && (
            <p className="chase-line detail-chase">
              <strong>{s.battingTeamShort}</strong> need <strong>{Math.max(current.target - current.runs, 0)}</strong>{' '}
              runs in <strong>{s.ballsRemaining}</strong> balls
              <span className="rates">
                CRR {s.currentRunRate} · RRR {s.requiredRunRate}
              </span>
            </p>
          )}
          {isLive && current && current.target == null && (
            <p className="chase-line detail-chase">
              <span className="rates">
                CRR {s.currentRunRate} · {current.battingTeam} batting
              </span>
            </p>
          )}
          {s.status === 'COMPLETED' && <p className="result-line">{s.result}</p>}
          {s.status === 'UPCOMING' && (
            <p className="chase-line muted">Starts {formatDateTime(s.scheduledAt)}</p>
          )}

          <ThisOver chips={s.thisOver} />
          {isLive && s.lastEvent && <p className="last-event">{s.lastEvent}</p>}
        </div>
      </div>

      {detail.live && (
        <div className="container">
          <div className="live-strip card">
            <div className="live-cell">
              <span className="label">Striker</span>
              <span className="value">
                {detail.live.striker ? detail.live.striker.playerName : '—'}
                {detail.live.striker && detail.live.striker.onStrike ? <span className="strike-mark">*</span> : null}
              </span>
              <span className="sub">
                {detail.live.striker
                  ? `${detail.live.striker.runs} (${detail.live.striker.ballsFaced}) · SR ${detail.live.striker.strikeRate}`
                  : '—'}
              </span>
            </div>
            <div className="live-cell">
              <span className="label">Non-striker</span>
              <span className="value">{detail.live.nonStriker ? detail.live.nonStriker.playerName : '—'}</span>
              <span className="sub">
                {detail.live.nonStriker
                  ? `${detail.live.nonStriker.runs} (${detail.live.nonStriker.ballsFaced})`
                  : '—'}
              </span>
            </div>
            <div className="live-cell">
              <span className="label">Bowler</span>
              <span className="value">{detail.live.bowler ? detail.live.bowler.playerName : '—'}</span>
              <span className="sub">
                {detail.live.bowler
                  ? `${detail.live.bowler.overs} ov · ${detail.live.bowler.runsConceded}/${detail.live.bowler.wickets} · Econ ${detail.live.bowler.economy}`
                  : '—'}
              </span>
            </div>
          </div>
        </div>
      )}

      <div className="container">
        <div className="tabs">
          {TABS.map((t) => (
            <button
              key={t.key}
              className={`tab ${tab === t.key ? 'active' : ''}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'scorecard' && (
          <div className="tab-body">
            {(detail.innings || []).map((inn) => (
              <InningsScorecard key={inn.inningsNumber} innings={inn} />
            ))}
            {(!detail.innings || detail.innings.length === 0) && (
              <p className="muted empty-note">The scorecard appears once the match starts.</p>
            )}
          </div>
        )}

        {tab === 'commentary' && (
          <div className="tab-body">
            <CommentaryFeed balls={commentary} />
          </div>
        )}

        {tab === 'summary' && (
          <div className="tab-body">
            {!summary ? (
              <Spinner />
            ) : (
              <div className="summary card">
                {s.status === 'COMPLETED' && <p className="result-line big">{summary.result || s.result}</p>}
                <div className="summary-innings">
                  {(summary.innings || []).map((inn) => (
                    <div key={inn.inningsNumber} className="summary-line">
                      <span>{inn.battingTeam}</span>
                      <span className="strong">
                        {inn.runs}/{inn.wickets} ({inn.overs})
                      </span>
                    </div>
                  ))}
                </div>
                <div className="performers">
                  {summary.topScorer && (
                    <div className="performer">
                      <span className="label">Top scorer</span>
                      <Link className="player-link strong" to={`/players/${summary.topScorer.playerId}`}>
                        {summary.topScorer.playerName}
                      </Link>
                      <span className="sub">
                        {summary.topScorer.runs} ({summary.topScorer.ballsFaced}) · {summary.topScorer.fours}×4{' '}
                        {summary.topScorer.sixes}×6
                      </span>
                    </div>
                  )}
                  {summary.topBowler && (
                    <div className="performer">
                      <span className="label">Top bowler</span>
                      <Link className="player-link strong" to={`/players/${summary.topBowler.playerId}`}>
                        {summary.topBowler.playerName}
                      </Link>
                      <span className="sub">
                        {summary.topBowler.wickets}/{summary.topBowler.runsConceded} ({summary.topBowler.overs} ov)
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
