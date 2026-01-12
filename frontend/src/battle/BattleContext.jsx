import React, { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { battleSocket } from './battleSocket';
import { getCurrentUserInfo } from '../api/authStorage';

const BattleContext = createContext(null);

function normalizeInvite(msg) {
  if (!msg || typeof msg !== 'object') return null;
  if (!msg.roomCode) return null;
  return {
    roomCode: String(msg.roomCode),
    from: String(msg.playerA || ''),
    to: String(msg.playerB || ''),
    message: String(msg.message || ''),
    raw: msg,
  };
}

function ToastStack({ toasts, onDismiss }) {
  if (!toasts.length) return null;
  return (
    <div className="fixed top-20 right-6 z-[60] space-y-2 w-[320px]">
      {toasts.map((t) => (
        <div
          key={t.id}
          className="glass-dark border border-white/10 rounded-2xl px-4 py-3 text-sm"
        >
          <div className="flex items-start justify-between gap-3">
            <div className={t.level === 'error' ? 'text-red-300' : 'text-gray-200'}>
              {t.text}
            </div>
            <button
              type="button"
              onClick={() => onDismiss(t.id)}
              className="text-gray-500 hover:text-gray-200 transition-colors"
              aria-label="关闭提示"
            >
              ×
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

function InviteModal({ invite, onAccept, onReject }) {
  if (!invite) return null;
  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center p-4 bg-black/80">
      <div className="glass-dark rounded-3xl p-6 w-full max-w-md border border-white/10">
        <div className="text-xl font-bold mb-2">收到对战邀请</div>
        <div className="text-gray-400 text-sm mb-5">
          {invite.message || `${invite.from} 邀请你进行对战`}
        </div>

        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => onReject(invite.roomCode)}
            className="flex-1 py-3 bg-white/10 hover:bg-white/20 rounded-xl transition-all text-white"
          >
            拒绝
          </button>
          <button
            type="button"
            onClick={() => onAccept(invite.roomCode)}
            className="flex-1 py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all"
          >
            接受
          </button>
        </div>
      </div>
    </div>
  );
}

export function BattleProvider({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const userInfo = getCurrentUserInfo();
  const username = userInfo?.username || '';

  const pathnameRef = useRef(location.pathname);

  useEffect(() => {
    pathnameRef.current = location.pathname;
  }, [location.pathname]);

  const [connected, setConnected] = useState(false);
  const [inBattle, setInBattle] = useState(false);
  const [activeRoomCode, setActiveRoomCode] = useState('');

  const stateEventIdRef = useRef(1);
  const [stateEvents, setStateEvents] = useState([]);

  const lastStateMessage = useMemo(() => {
    return stateEvents.at(-1)?.msg ?? null;
  }, [stateEvents]);

  const [invites, setInvites] = useState([]);
  const [activeInviteRoomCode, setActiveInviteRoomCode] = useState('');

  const [toasts, setToasts] = useState([]);
  const toastIdRef = useRef(1);

  const addToast = (text, level = 'info') => {
    const id = toastIdRef.current++;
    setToasts((prev) => [{ id, text, level }, ...prev].slice(0, 4));
    globalThis.setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4500);
  };

  const dismissToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  useEffect(() => {
    if (!username) {
      battleSocket.disconnect();
      setConnected(false);
      setInBattle(false);
      setActiveRoomCode('');
      setInvites([]);
      setActiveInviteRoomCode('');
      return undefined;
    }

    battleSocket.connect(username);

    const unsubConn = battleSocket.subscribeConnection((ok) => {
      setConnected(ok);
    });

    const unsubInvite = battleSocket.subscribeInvite((msg) => {
      const inv = normalizeInvite(msg);
      if (!inv) return;

      setInvites((prev) => {
        if (prev.some((x) => x.roomCode === inv.roomCode)) return prev;
        return [inv, ...prev];
      });
      setActiveInviteRoomCode(inv.roomCode);
      addToast('📬 收到对战邀请', 'info');
    });

    const unsubState = battleSocket.subscribeState((msg) => {
      const id = stateEventIdRef.current++;
      setStateEvents((prev) => {
        const next = [...prev, { id, msg }];
        return next.length > 80 ? next.slice(-80) : next;
      });

      const type = msg?.type;
      if (type === 'INVITE_REJECTED') {
        addToast(msg?.message || '邀请失败', 'error');
      }

      if (type === 'GAME_START') {
        setInBattle(true);
        setActiveRoomCode(String(msg?.roomCode || ''));
        setInvites([]);
        setActiveInviteRoomCode('');

        const roomCode = String(msg?.roomCode || '');
        const isAlreadyInBattleRoute = pathnameRef.current.startsWith('/battle/');
        if (roomCode && !isAlreadyInBattleRoute) {
          navigate(`/battle/${encodeURIComponent(roomCode)}`, {
            replace: false,
            state: { initial: msg },
          });
        }
      }

      if (type === 'GAME_OVER') {
        setInBattle(false);
        setActiveRoomCode('');
        addToast(msg?.message || '对战结束', 'info');
      }
    });

    return () => {
      unsubConn?.();
      unsubInvite?.();
      unsubState?.();
      battleSocket.disconnect();
    };
  }, [navigate, username]);

  const activeInvite = useMemo(() => {
    if (!activeInviteRoomCode) return null;
    return invites.find((i) => i.roomCode === activeInviteRoomCode) || null;
  }, [activeInviteRoomCode, invites]);

  const removeInvite = (roomCode) => {
    setInvites((prev) => prev.filter((i) => i.roomCode !== roomCode));
    setActiveInviteRoomCode((prev) => (prev === roomCode ? '' : prev));
  };

  const sendInvite = async (toUsername) => {
    const target = String(toUsername || '').trim();
    if (!target) throw new Error('请输入对方用户名');
    if (inBattle) throw new Error('你正在对战中');

    await battleSocket.invite(target);
    addToast(`📨 已发送邀请给：${target}`, 'info');
  };

  const acceptInvite = async (roomCode) => {
    try {
      await battleSocket.respond(roomCode, true);
      removeInvite(roomCode);
      addToast('✅ 已接受邀请，等待游戏开始...', 'info');
    } catch (e) {
      addToast(e?.message || '接受邀请失败', 'error');
    }
  };

  const rejectInvite = async (roomCode) => {
    try {
      await battleSocket.respond(roomCode, false);
      removeInvite(roomCode);
      addToast('已拒绝邀请', 'info');
    } catch (e) {
      addToast(e?.message || '拒绝邀请失败', 'error');
    }
  };

  const submitAnswer = async (roomCode, coord) => {
    await battleSocket.answer(roomCode, coord.lon, coord.lat);
  };

  const quitBattle = async () => {
    await battleSocket.quit();
  };

  const value = useMemo(
    () => ({
      username,
      connected,
      inBattle,
      activeRoomCode,
      stateEvents,
      lastStateMessage,
      sendInvite,
      acceptInvite,
      rejectInvite,
      submitAnswer,
      quitBattle,
    }),
    [
      username,
      connected,
      inBattle,
      activeRoomCode,
      stateEvents,
      lastStateMessage,
    ]
  );

  return (
    <BattleContext.Provider value={value}>
      {children}
      <ToastStack toasts={toasts} onDismiss={dismissToast} />
      <InviteModal invite={activeInvite} onAccept={acceptInvite} onReject={rejectInvite} />
    </BattleContext.Provider>
  );
}

export function useBattle() {
  const ctx = useContext(BattleContext);
  if (!ctx) {
    throw new Error('useBattle 必须在 BattleProvider 内使用');
  }
  return ctx;
}
