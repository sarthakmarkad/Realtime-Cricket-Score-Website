import { ballToChip, chipClass, chipLabel } from '../format';

export default function CommentaryFeed({ balls }) {
  if (!balls || balls.length === 0) {
    return <p className="muted empty-note">No deliveries yet — the stream starts with the first ball.</p>;
  }
  return (
    <div className="commentary card">
      {balls.map((ball) => {
        const chip = ballToChip(ball);
        const rowClass = ball.wicket
          ? 'comm-row is-wicket'
          : chip === '4' || chip === '6'
            ? 'comm-row is-boundary'
            : 'comm-row';
        return (
          <div key={ball.id} className={rowClass}>
            <span className="comm-over">{ball.overDisplay}</span>
            <span className={chipClass(chip)}>{chipLabel(chip)}</span>
            <div className="comm-text">
              {ball.wicket && <div className="comm-headline">WICKET — {ball.dismissedPlayerName} {ball.wicketType.toLowerCase()}</div>}
              <span>
                <strong>{ball.batsmanName}</strong> · {ball.commentary}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
