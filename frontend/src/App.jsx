import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './layouts/Layout';
import Login from './pages/Login';
import ResetPassword from './pages/ResetPassword';
import Dashboard from './pages/Dashboard';
import Settings from './pages/Settings';
import Leaderboard from './pages/Leaderboard';
import Friends from './pages/Friends';
import GameMenu from './pages/GameMenu';
import Matchmaking from './pages/Matchmaking';
import Game from './pages/Game';
import SoloMode from './pages/SoloMode';
import BattleGame from './pages/BattleGame';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/login" replace />} />
          <Route path="login" element={<Login />} />
          <Route path="reset-password" element={<ResetPassword />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="friends" element={<Friends />} />
          <Route path="settings" element={<Settings />} />
          <Route path="leaderboard" element={<Leaderboard />} />
          <Route path="game-menu" element={<GameMenu />} />
          <Route path="solo" element={<SoloMode />} />
          <Route path="matchmaking" element={<Matchmaking />} />
          <Route path="game" element={<Game />} />
          <Route path="battle/:roomCode" element={<BattleGame />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
