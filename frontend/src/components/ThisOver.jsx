import { chipClass, chipLabel } from '../format';

export default function ThisOver({ chips }) {
  if (!chips || chips.length === 0) return null;
  return (
    <div className="this-over">
      <span className="this-over-label">This over</span>
      <div className="chips">
        {chips.map((chip, i) => (
          <span key={i} className={chipClass(chip)}>
            {chipLabel(chip)}
          </span>
        ))}
      </div>
    </div>
  );
}
