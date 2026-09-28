import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Spinner from '../components/Spinner';
import { fetchLeaderboard } from '../api';

export default function Players() {
  const [board, setBoard] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchLeaderboard()
      .then(setBoard)
      .catch((e) => setError(e.message));
  }, []);

  if (error) {
    return (
      <div className="container page">
        <div className="error-state">{error}</div>
      </div>
    );
  }
  if (!board) {
    return (
      <div className="container page">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="container page">
      <section className="hero">
        <h1>Player Statistics</h1>
        <p>Tournament leaderboards aggregated across every match — updated as the simulation plays on.</p>
      </section>

      <div className="leader-grid">
        <section className="card">
          <h3 className="section-title">Leading run scorers</h3>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th className="ta-left">Player</th>
                  <th>Team</th>
                  <th>M</th>
                  <th>Runs</th>
                  <th>HS</th>
                  <th>SR</th>
                  <th>50+</th>
                </tr>
              </thead>
              <tbody>
                {board.topScorers.map((p) => (
                  <tr key={p.playerId}>
                    <td className="ta-left">
                      <Link className="player-link" to={`/players/${p.playerId}`}>
                        {p.playerName}
                      </Link>
                    </td>
                    <td>
                      <span className="team-short">{p.teamShort}</span>
                    </td>
                    <td>{p.matches}</td>
                    <td className="strong">{p.runs}</td>
                    <td>{p.highest}</td>
                    <td>{p.strikeRate.toFixed(1)}</td>
                    <td>{p.fifties}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card">
          <h3 className="section-title">Leading wicket takers</h3>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th className="ta-left">Player</th>
                  <th>Team</th>
                  <th>M</th>
                  <th>Wkts</th>
                  <th>Overs</th>
                  <th>Econ</th>
                  <th>Best</th>
                </tr>
              </thead>
              <tbody>
                {board.topWicketTakers.map((p) => (
                  <tr key={p.playerId}>
                    <td className="ta-left">
                      <Link className="player-link" to={`/players/${p.playerId}`}>
                        {p.playerName}
                      </Link>
                    </td>
                    <td>
                      <span className="team-short">{p.teamShort}</span>
                    </td>
                    <td>{p.matches}</td>
                    <td className="strong">{p.wickets}</td>
                    <td>{p.overs}</td>
                    <td>{p.economy.toFixed(1)}</td>
                    <td>{p.bestFigures}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}
