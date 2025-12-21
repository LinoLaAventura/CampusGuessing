import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './layouts/Layout';
import Home from './pages/Home';
import Login from './pages/Login';
import ResetPassword from './pages/ResetPassword';
import Dashboard from './pages/Dashboard';
import Settings from './pages/Settings';
import Leaderboard from './pages/Leaderboard';
import GameMenu from './pages/GameMenu';
import Matchmaking from './pages/Matchmaking';
import Game from './pages/Game';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/login" replace />} />
          <Route path="login" element={<Login />} />
          <Route path="reset-password" element={<ResetPassword />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="settings" element={<Settings />} />
          <Route path="leaderboard" element={<Leaderboard />} />
          <Route path="game-menu" element={<GameMenu />} />
          <Route path="matchmaking" element={<Matchmaking />} />
          <Route path="game" element={<Game />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
