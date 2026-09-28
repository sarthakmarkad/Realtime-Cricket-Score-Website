import { Link, NavLink } from 'react-router-dom';

export default function Navbar({ connected }) {
  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand">
          <span className="brand-dot" />
          CricLive
        </Link>
        <nav className="nav-links">
          <NavLink to="/" end>
            Matches
          </NavLink>
          <NavLink to="/players">Player Stats</NavLink>
        </nav>
        <span className={`live-pill ${connected ? 'on' : 'off'}`}>
          <span className="dot" />
          {connected ? 'Live feed' : 'Reconnecting…'}
        </span>
      </div>
    </header>
  );
}
