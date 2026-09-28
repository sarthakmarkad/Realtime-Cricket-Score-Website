import { Link } from 'react-router-dom';

function battingStatus(b) {
  if (b.onStrike || b.nonStriker) return 'batting';
  if (b.out) return b.dismissal || 'out';
  if (b.ballsFaced > 0 || b.runs > 0) return 'not out';
  return 'did not bat';
}

export default function InningsScorecard({ innings }) {
  return (
    <section className="scorecard card">
      <h3 className="innings-title">
        {innings.battingTeam} — {innings.runs}/{innings.wickets} ({innings.overs} ov)
        {innings.target != null && <span className="muted"> · Target {innings.target}</span>}
      </h3>

      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th className="ta-left">Batter</th>
              <th className="ta-left status-col" />
              <th>R</th>
              <th>B</th>
              <th>4s</th>
              <th>6s</th>
              <th>SR</th>
            </tr>
          </thead>
          <tbody>
            {innings.batting.map((b) => (
              <tr key={b.playerId} className={b.onStrike ? 'row-strike' : b.nonStriker ? 'row-nonstrike' : ''}>
                <td className="ta-left">
                  <Link className="player-link" to={`/players/${b.playerId}`}>
                    {b.playerName}
                  </Link>
                  {b.onStrike && <span className="strike-mark">*</span>}
                </td>
                <td className="ta-left status-col muted">{battingStatus(b)}</td>
                <td className="strong">{b.runs}</td>
                <td>{b.ballsFaced}</td>
                <td>{b.fours}</td>
                <td>{b.sixes}</td>
                <td>{b.strikeRate.toFixed(1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="table-scroll bowling-table">
        <table>
          <thead>
            <tr>
              <th className="ta-left">Bowler</th>
              <th className="ta-left status-col" />
              <th>O</th>
              <th>R</th>
              <th>W</th>
              <th>Econ</th>
            </tr>
          </thead>
          <tbody>
            {innings.bowling.map((b) => (
              <tr key={b.playerId} className={b.currentBowler ? 'row-strike' : ''}>
                <td className="ta-left">
                  <Link className="player-link" to={`/players/${b.playerId}`}>
                    {b.playerName}
                  </Link>
                </td>
                <td className="ta-left status-col muted">{b.currentBowler ? 'bowling' : ''}</td>
                <td>{b.overs}</td>
                <td>{b.runsConceded}</td>
                <td className="strong">{b.wickets}</td>
                <td>{b.economy.toFixed(1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
