import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, Flag } from 'lucide-react';
import 'leaflet/dist/leaflet.css';

import L from 'leaflet';
import { useBattle } from '../battle/BattleContext';
import { getQuestionDetail } from '../api/questionApi';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

function safeNumber(n) {
  return Number.isFinite(n) ? n : null;
}

function formatMeters(meters) {
  if (!Number.isFinite(meters)) return '-';
  if (meters >= 1000) return `${(meters / 1000).toFixed(2)} km`;
  return `${Math.round(meters)} m`;
}

function getQuestionImageUrl(q) {
  const url = q?.imageData?.links?.url;
  if (typeof url === 'string' && url) return url;
  return '';
}

const BattleGame = () => {
  const navigate = useNavigate();
  const { roomCode: roomCodeParam } = useParams();
  const roomCode = decodeURIComponent(roomCodeParam || '');

  const { username, stateEvents, submitAnswer, quitBattle } = useBattle();

  const [playerA, setPlayerA] = useState('');
  const [playerB, setPlayerB] = useState('');
  const [playerAHealth, setPlayerAHealth] = useState(100);
  const [playerBHealth, setPlayerBHealth] = useState(100);
  const [currentRound, setCurrentRound] = useState(1);

  const [question, setQuestion] = useState(null);
  const [imageUrl, setImageUrl] = useState('');

  const [guessPosition, setGuessPosition] = useState(null);
  const [hasSubmitted, setHasSubmitted] = useState(false);

  const [playerAAnswered, setPlayerAAnswered] = useState(false);
  const [playerBAnswered, setPlayerBAnswered] = useState(false);

  const [statusText, setStatusText] = useState('等待对战开始...');
  const [countdown, setCountdown] = useState(null);

  const [roundResult, setRoundResult] = useState(null);
  const [showRoundResult, setShowRoundResult] = useState(false);

  const [gameOver, setGameOver] = useState(null);

  const countdownTimerRef = useRef(null);
  const submitInFlightRef = useRef(false);

  const isPlayerA = username && playerA && username === playerA;
  const myHealth = isPlayerA ? playerAHealth : playerBHealth;
  const oppHealth = isPlayerA ? playerBHealth : playerAHealth;
  const opponentName = isPlayerA ? playerB : playerA;
  const myAnswered = isPlayerA ? playerAAnswered : playerBAnswered;
  const oppAnswered = isPlayerA ? playerBAnswered : playerAAnswered;

  const tileLayer = useMemo(
    () => ({
      url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 18,
    }),
    []
  );

  const mapCenter = useMemo(() => [22.255, 113.541], []);

  const lastHandledIdRef = useRef(0);

  useEffect(() => {
    if (!stateEvents?.length) return;

    const newEvents = stateEvents.filter((e) => e.id > lastHandledIdRef.current);
    if (!newEvents.length) return;

    for (const ev of newEvents) {
      const msg = ev.msg;
      if (msg?.roomCode !== roomCode) {
        lastHandledIdRef.current = Math.max(lastHandledIdRef.current, ev.id);
        continue;
      }

      const type = msg.type;

      switch (type) {
        case 'GAME_START':
          setPlayerA(msg.playerA || '');
          setPlayerB(msg.playerB || '');
          setPlayerAHealth(safeNumber(msg.playerAHealth) ?? 100);
          setPlayerBHealth(safeNumber(msg.playerBHealth) ?? 100);
          setCurrentRound(safeNumber(msg.currentRound) ?? 1);
          setQuestion(msg.question || null);
          setGuessPosition(null);
          setHasSubmitted(false);
          setPlayerAAnswered(Boolean(msg.playerAAnswered));
          setPlayerBAnswered(Boolean(msg.playerBAnswered));
          setRoundResult(null);
          setShowRoundResult(false);
          setGameOver(null);
          setStatusText(msg.message || '对战开始！');
          setCountdown(null);
          break;
        case 'NEW_QUESTION':
          setCurrentRound(safeNumber(msg.currentRound) ?? 1);
          setQuestion(msg.question || null);
          setPlayerAHealth(safeNumber(msg.playerAHealth) ?? 100);
          setPlayerBHealth(safeNumber(msg.playerBHealth) ?? 100);
          setGuessPosition(null);
          setHasSubmitted(false);
          setPlayerAAnswered(false);
          setPlayerBAnswered(false);
          setRoundResult(null);
          setShowRoundResult(false);
          setStatusText(msg.message || '新回合开始');
          setCountdown(null);
          break;
        case 'PLAYER_ANSWERED':
          setPlayerAAnswered(Boolean(msg.playerAAnswered));
          setPlayerBAnswered(Boolean(msg.playerBAnswered));
          setStatusText(msg.message || '对方已作答');
          setCountdown(safeNumber(msg.countdown) ?? 30);
          break;
        case 'ROUND_RESULT':
          setPlayerAHealth(safeNumber(msg.playerAHealth) ?? 100);
          setPlayerBHealth(safeNumber(msg.playerBHealth) ?? 100);
          setCurrentRound(safeNumber(msg.currentRound) ?? 1);
          setRoundResult(msg.roundResult || null);
          setShowRoundResult(true);
          setStatusText(msg.message || '回合结束');
          setCountdown(null);
          break;
        case 'GAME_OVER':
          setPlayerAHealth(safeNumber(msg.playerAHealth) ?? 100);
          setPlayerBHealth(safeNumber(msg.playerBHealth) ?? 100);
          setGameOver({ winner: msg.winner || '', message: msg.message || '游戏结束' });
          setStatusText(msg.message || '游戏结束');
          setCountdown(null);
          break;
        default:
          setStatusText(msg?.message || '');
      }

      lastHandledIdRef.current = Math.max(lastHandledIdRef.current, ev.id);
    }
  }, [roomCode, stateEvents]);

  useEffect(() => {
    // 倒计时仅做 UI 提示（后端不强制）
    if (!countdown || countdown <= 0) return undefined;
    if (countdownTimerRef.current) globalThis.clearInterval(countdownTimerRef.current);

    countdownTimerRef.current = globalThis.setInterval(() => {
      setCountdown((prev) => {
        const next = (prev ?? 0) - 1;
        return next <= 0 ? null : next;
      });
    }, 1000);

    return () => {
      if (countdownTimerRef.current) globalThis.clearInterval(countdownTimerRef.current);
      countdownTimerRef.current = null;
    };
  }, [countdown]);

  useEffect(() => {
    // battle WS 的 QuestionResponse 可能缺少 imageData；这里用 REST 再拉一次补全图片 URL
    const id = question?.id;
    if (!id) {
      setImageUrl('');
      return;
    }

    const urlFromWs = getQuestionImageUrl(question);
    if (urlFromWs) {
      setImageUrl(urlFromWs);
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const detail = await getQuestionDetail(id);
        if (cancelled) return;
        setImageUrl(getQuestionImageUrl(detail) || '');
      } catch {
        if (!cancelled) setImageUrl('');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [question]);

  const canSubmit = Boolean(guessPosition && !hasSubmitted && !gameOver);

  const handleSubmit = async () => {
    if (!canSubmit) return;
    if (submitInFlightRef.current) return;
    submitInFlightRef.current = true;
    // 先锁定，避免 await 期间被连点造成重复提交
    setHasSubmitted(true);
    try {
      await submitAnswer(roomCode, { lat: guessPosition.lat, lon: guessPosition.lng });
      if (username && playerA && username === playerA) setPlayerAAnswered(true);
      if (username && playerB && username === playerB) setPlayerBAnswered(true);
      setStatusText('已提交答案，等待对方作答...');
    } catch (e) {
      setHasSubmitted(false);
      setStatusText(e?.message || '提交失败');
    } finally {
      submitInFlightRef.current = false;
    }
  };

  const handleQuit = async () => {
    try {
      await quitBattle();
      navigate('/game-menu');
    } catch (e) {
      setStatusText(e?.message || '退出失败');
    }
  };

  function MapClickHandler() {
    useMapEvents({
      click(e) {
        if (hasSubmitted || gameOver) return;
        setGuessPosition(e.latlng);
      },
    });
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black">
      {/* Top Bar */}
      <div className="absolute top-0 left-0 right-0 z-50 flex items-center justify-between p-4">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="p-3 rounded-full glass-dark hover:bg-white/10 transition-all group"
          aria-label="返回"
        >
          <ChevronLeft className="w-6 h-6 text-gray-400 group-hover:text-white transition-colors" />
        </button>

        <div className="glass-dark px-4 py-2 rounded-full text-sm text-gray-200">
          房间 {roomCode || '-'} · 第 {currentRound} 回合
        </div>

        <button
          type="button"
          onClick={handleQuit}
          className="p-3 rounded-full glass-dark hover:bg-white/10 transition-all group"
          aria-label="退出对战"
        >
          <Flag className="w-6 h-6 text-gray-400 group-hover:text-white transition-colors" />
        </button>
      </div>

      {/* Main */}
      <div className="w-full h-full relative">
        {/* Image */}
        <div className="absolute inset-0 bg-black">
          {imageUrl ? (
            <img
              src={imageUrl}
              alt={question?.title || 'battle'}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <div className="glass-dark px-5 py-4 rounded-2xl border border-white/10 text-gray-400 text-sm">
                暂无图片
              </div>
            </div>
          )}
        </div>

        {/* Health + Status */}
        <div className="absolute top-20 left-6 right-6 z-40 flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-4">
            <div className="glass-dark px-5 py-4 rounded-2xl border border-white/10">
              <div className="text-sm text-gray-400 mb-2">你：{username || '-'}</div>
              <div className="w-full h-3 bg-white/10 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-emerald-500 to-green-500"
                  style={{ width: `${Math.max(0, Math.min(100, myHealth))}%` }}
                />
              </div>
              <div className="text-xs text-gray-500 mt-2">血量 {myHealth}</div>
            </div>

            <div className="glass-dark px-5 py-4 rounded-2xl border border-white/10">
              <div className="text-sm text-gray-400 mb-2">对手：{opponentName || '-'}</div>
              <div className="w-full h-3 bg-white/10 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-orange-500 to-red-500"
                  style={{ width: `${Math.max(0, Math.min(100, oppHealth))}%` }}
                />
              </div>
              <div className="text-xs text-gray-500 mt-2">血量 {oppHealth}</div>
            </div>
          </div>

          <div className="glass-dark px-4 py-3 rounded-2xl border border-white/10 text-sm flex items-center justify-between">
            <div className="text-gray-200">{statusText}</div>
            <div className="flex items-center gap-3">
              <div className="text-gray-500">你{myAnswered ? '已作答' : '未作答'} · 对手{oppAnswered ? '已作答' : '未作答'}</div>
              {countdown ? <div className="text-orange-300 font-semibold">倒计时 {countdown}s</div> : null}
            </div>
          </div>
        </div>

        {/* Map */}
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="absolute bottom-6 right-6 w-[420px] h-[420px] rounded-2xl overflow-hidden shadow-2xl border-2 border-white/20"
        >
          <MapContainer
            center={mapCenter}
            zoom={15}
            style={{ height: '100%', width: '100%' }}
            zoomControl={false}
            maxZoom={tileLayer.maxZoom}
            minZoom={12}
          >
            <TileLayer attribution={tileLayer.attribution} url={tileLayer.url} maxZoom={tileLayer.maxZoom} />
            <MapClickHandler />
            {guessPosition ? <Marker position={guessPosition} /> : null}
          </MapContainer>

          <div className="absolute bottom-4 left-4 right-4 z-[1000]">
            <button
              type="button"
              onClick={handleSubmit}
              disabled={!canSubmit}
              className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all disabled:opacity-50 disabled:hover:shadow-none"
            >
              {hasSubmitted ? '已提交' : '提交答案'}
            </button>
          </div>
        </motion.div>

        {/* Round Result */}
        <AnimatePresence>
          {showRoundResult && roundResult ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-[80] flex items-center justify-center p-4 bg-black/80"
              onClick={() => setShowRoundResult(false)}
            >
              <motion.div
                initial={{ scale: 0.96, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.96, opacity: 0 }}
                className="glass-dark rounded-3xl p-6 w-full max-w-lg border border-white/10"
                onClick={(e) => e.stopPropagation()}
              >
                <div className="text-xl font-bold mb-3">回合结算</div>
                <div className="text-sm text-gray-400 mb-4">{statusText}</div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-400">玩家A距离</span>
                    <span className="text-gray-200">{formatMeters(roundResult.playerADistance)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">玩家B距离</span>
                    <span className="text-gray-200">{formatMeters(roundResult.playerBDistance)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">扣血方</span>
                    <span className="text-orange-300">{roundResult.damagedPlayer || '-'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">伤害</span>
                    <span className="text-red-300">-{roundResult.damage ?? '-'}</span>
                  </div>
                </div>

                <div className="mt-5">
                  <button
                    type="button"
                    onClick={() => setShowRoundResult(false)}
                    className="w-full py-3 bg-white/10 hover:bg-white/20 rounded-xl transition-all text-white"
                  >
                    继续
                  </button>
                </div>
              </motion.div>
            </motion.div>
          ) : null}
        </AnimatePresence>

        {/* Game Over */}
        <AnimatePresence>
          {gameOver ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-[90] flex items-center justify-center p-4 bg-black/80"
            >
              <motion.div
                initial={{ scale: 0.96, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.96, opacity: 0 }}
                className="glass-dark rounded-3xl p-6 w-full max-w-lg border border-white/10"
              >
                <div className="text-2xl font-bold mb-2">对战结束</div>
                <div className="text-gray-400 text-sm mb-5">{gameOver.message}</div>

                <div className="glass-dark px-4 py-3 rounded-2xl border border-white/10 text-sm mb-6">
                  <div className="flex justify-between">
                    <span className="text-gray-400">获胜者</span>
                    <span className="text-emerald-300 font-semibold">{gameOver.winner || '-'}</span>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => navigate('/game-menu')}
                  className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all"
                >
                  返回菜单
                </button>
              </motion.div>
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default BattleGame;
