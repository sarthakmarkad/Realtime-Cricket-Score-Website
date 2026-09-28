import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Spinner from '../components/Spinner';
import { fetchPlayer } from '../api';
import { roleLabel } from '../format';

export default function PlayerProfile() {
  const { id } = useParams();
  const [player, setPlayer] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setPlayer(null);
    fetchPlayer(id)
      .then(setPlayer)
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) {
    return (
      <div className="container page">
        <div className="error-state">
          {error}
          <div>
            <Link to="/players" className="card-link">
              ← Back to stats
            </Link>
          </div>
        </div>
      </div>
    );
  }
  if (!player) {
    return (
      <div className="container page">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="container page">
      <Link to="/players" className="card-link back-link">
        ← All players
      </Link>
      <section className="profile-hero card">
        <div>
          <h1>{player.name}</h1>
          <p className="muted">
            {roleLabel(player.role)} · {player.teamName}
          </p>
        </div>
        <div className="stat-cards">
          <div className="stat-card">
            <span className="label">Matches</span>
            <span className="value">{player.matches}</span>
          </div>
          <div className="stat-card">
            <span className="label">Runs</span>
            <span className="value">{player.runs}</span>
          </div>
          <div className="stat-card">
            <span className="label">Bat avg</span>
            <span className="value">{player.battingAverage.toFixed(1)}</span>
          </div>
          <div className="stat-card">
            <span className="label">Wickets</span>
            <span className="value">{player.wickets}</span>
          </div>
          <div className="stat-card">
            <span className="label">Best</span>
            <span className="value small-value">{player.bestBowling}</span>
          </div>
        </div>
      </section>

      <section className="card">
        <h3 className="section-title">Match by match</h3>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th className="ta-left">Opponent</th>
                <th className="ta-left">Status</th>
                <th className="ta-left">Batting</th>
                <th>R</th>
                <th>B</th>
                <th className="ta-left">Bowling</th>
              </tr>
            </thead>
            <tbody>
              {player.recentMatches.map((m, idx) => (
                <tr key={`${m.matchId}-${idx}`}>
                  <td className="ta-left">
                    <span className="team-short">{m.opponentShort}</span>{' '}
                    <Link className="player-link" to={`/matches/${m.matchId}`}>
                      view match
                    </Link>
                  </td>
                  <td className="ta-left muted">{m.matchStatus}</td>
                  <td className="ta-left">
                    {m.ballsFaced > 0 || m.runs > 0 ? (
                      <>
                        <span className="strong">{m.runs}</span> ({m.ballsFaced})
                        <div className="dismissal">{m.out ? m.dismissal : 'not out'}</div>
                      </>
                    ) : (
                      <span className="muted">did not bat</span>
                    )}
                  </td>
                  <td className="strong">{m.runs}</td>
                  <td>{m.ballsFaced}</td>
                  <td className="ta-left">
                    {m.oversBowled !== '0.0' ? (
                      `${m.wickets}/${m.runsConceded} (${m.oversBowled} ov)`
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                </tr>
              ))}
              {player.recentMatches.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted ta-left">
                    No matches played yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
