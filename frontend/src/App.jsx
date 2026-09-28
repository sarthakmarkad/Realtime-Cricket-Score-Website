import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import MatchDetail from './pages/MatchDetail';
import Players from './pages/Players';
import PlayerProfile from './pages/PlayerProfile';
import { useLiveScores } from './hooks/useLiveScores';

export default function App() {
  const live = useLiveScores();

  return (
    <div className="app">
      <Navbar connected={live.connected} />
      <main>
        <Routes>
          <Route path="/" element={<Dashboard live={live} />} />
          <Route path="/matches/:id" element={<MatchDetail />} />
          <Route path="/players" element={<Players />} />
          <Route path="/players/:id" element={<PlayerProfile />} />
          <Route path="*" element={<Dashboard live={live} />} />
        </Routes>
      </main>
      <footer className="footer">
        <div className="container">
          <div>CricLive — Spring Boot + React real-time cricket score management. Feed updates via Server-Sent Events.</div>
          <div className="footer-credit">Sarthak Markad - CS-H-02 - 12411896</div>
        </div>
      </footer>
    </div>
  );
}
