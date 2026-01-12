import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getCurrentUserInfo, getDisplayName } from '../api/authStorage';
import { getFriendList } from '../api/friendApi';
import { useBattle } from '../battle/BattleContext';

const Matchmaking = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const mode = searchParams.get('mode') || 'normal';
    const displayName = getDisplayName() || '';

    const { connected, inBattle, sendInvite, stateEvents } = useBattle();

    const [friendLoading, setFriendLoading] = useState(true);
    const [friendError, setFriendError] = useState('');
    const [friends, setFriends] = useState([]);

    const [targetUsername, setTargetUsername] = useState('');
    const [sendingTo, setSendingTo] = useState('');
    const [actionMsg, setActionMsg] = useState('');
    const [actionErr, setActionErr] = useState('');

    useEffect(() => {
        const userInfo = getCurrentUserInfo();
        if (!userInfo?.username) {
            navigate('/login');
            return;
        }

        let cancelled = false;
        (async () => {
            try {
                setFriendLoading(true);
                setFriendError('');
                const resp = await getFriendList(userInfo.username, { page: 1, size: 50 });
                if (cancelled) return;
                setFriends(resp?.friendList || []);
            } catch (e) {
                if (cancelled) return;
                setFriendError(e?.message || '好友列表加载失败');
            } finally {
                if (!cancelled) setFriendLoading(false);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [navigate]);

    const lastHandledIdRef = React.useRef(0);

    useEffect(() => {
        if (!stateEvents?.length) return;
        const newEvents = stateEvents.filter((e) => e.id > lastHandledIdRef.current);
        if (!newEvents.length) return;

        for (const ev of newEvents) {
            const msg = ev.msg;
            if (msg?.type === 'INVITE_REJECTED') {
                setSendingTo('');
                setActionErr(msg?.message || '邀请失败');
            }
            if (msg?.type === 'GAME_START') {
                setSendingTo('');
                setActionMsg('对方已接受，正在进入对战...');
            }
            lastHandledIdRef.current = Math.max(lastHandledIdRef.current, ev.id);
        }
    }, [stateEvents]);

    const getTitle = () => {
        return mode === 'ranked' ? '排位对战（好友邀请）' : '普通对战（好友邀请）';
    };

    const connectionText = useMemo(() => {
        if (!connected) return '未连接（正在重连或未登录）';
        return '已连接（可收发邀请）';
    }, [connected]);

    const friendsContent = useMemo(() => {
        if (friendLoading) return <div className="text-gray-500 text-sm">加载中...</div>;
        if (friendError) return <div className="text-red-400 text-sm">{friendError}</div>;
        if (friends.length === 0) return <div className="text-gray-500 text-sm">暂无好友</div>;

        return (
            <div className="space-y-2">
                {friends.map((f) => (
                    <div
                        key={f.friendId}
                        className="flex items-center justify-between gap-3 px-4 py-3 bg-white/5 rounded-xl"
                    >
                        <div className="flex flex-col">
                            <div className="text-white">{f.friendUsername}</div>
                            <div className="text-xs text-gray-600">积分 {f.friendPoints ?? 0}</div>
                        </div>
                        <button
                            type="button"
                            onClick={() => handleInvite(f.friendUsername)}
                            disabled={sendingTo !== '' || inBattle}
                            className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded-xl transition-all text-white disabled:opacity-50"
                        >
                            邀请对战
                        </button>
                    </div>
                ))}
            </div>
        );
    }, [friendError, friendLoading, friends, inBattle, sendingTo]);

    const handleInvite = async (toUsername) => {
        const target = String(toUsername || '').trim();
        if (!target) return;

        try {
            setActionErr('');
            setActionMsg('');
            setSendingTo(target);
            await sendInvite(target);
            setActionMsg(`已发送邀请给：${target}，等待对方响应...`);
        } catch (e) {
            setActionErr(e?.message || '发送邀请失败');
            setSendingTo('');
        }
    };

    return (
        <div className="min-h-screen bg-black flex items-center justify-center p-6 relative overflow-hidden">
            {/* Dramatic Background */}
            <div className="fixed inset-0 z-0 bg-gradient-to-b from-black via-gray-900 to-black" />

            {/* Mode Indicator Background */}
            <div className={`fixed inset-0 z-0 opacity-20 bg-gradient-to-br ${mode === 'ranked' ? 'from-orange-900/40' : 'from-blue-900/40'} to-transparent`} />

            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="relative z-10 text-center"
            >
                {/* Match Found Title */}
                <motion.h1
                    initial={{ y: -50, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    transition={{ delay: 0.2 }}
                    className="text-4xl font-bold mb-4 text-white"
                >
                    {getTitle()}
                </motion.h1>

                <motion.p
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.4 }}
                    className="text-gray-500 mb-12"
                >
                    {mode === 'ranked' ? '选择好友发起邀请（排位）' : '选择好友发起邀请（普通）'}
                </motion.p>

                <div className="max-w-2xl w-full mx-auto text-left">
                    <div className="glass-dark rounded-3xl p-6 border border-white/10">
                        <div className="flex items-center justify-between gap-4 mb-4">
                            <div>
                                <div className="text-white font-semibold">当前用户：{displayName || '-'}</div>
                                <div className="text-gray-500 text-sm">连接状态：{connectionText}</div>
                            </div>
                            <button
                                type="button"
                                onClick={() => navigate('/friends')}
                                className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded-xl transition-all text-white"
                            >
                                好友管理
                            </button>
                        </div>

                        {inBattle ? (
                            <div className="text-orange-300 text-sm mb-4">你正在对战中，无法发起新邀请</div>
                        ) : null}

                        <div className="flex gap-3 mb-4">
                            <input
                                value={targetUsername}
                                onChange={(e) => setTargetUsername(e.target.value)}
                                placeholder="输入对方用户名（或从好友列表选择）"
                                className="flex-1 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder:text-gray-600 focus:outline-none focus:ring-2 focus:ring-orange-500/40"
                                disabled={sendingTo !== '' || inBattle}
                            />
                            <button
                                type="button"
                                onClick={() => handleInvite(targetUsername)}
                                disabled={!targetUsername.trim() || sendingTo !== '' || inBattle}
                                className="px-5 py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all disabled:opacity-50 disabled:hover:shadow-none"
                            >
                                {sendingTo ? '邀请中...' : '发起邀请'}
                            </button>
                        </div>

                        {actionErr ? <div className="text-red-400 text-sm mb-2">{actionErr}</div> : null}
                        {actionMsg ? <div className="text-gray-300 text-sm mb-2">{actionMsg}</div> : null}

                        <div className="mt-5">
                            <div className="text-sm text-gray-400 mb-3">好友列表</div>
                            {friendsContent}
                        </div>
                    </div>
                </div>
            </motion.div>
        </div>
    );
};

export default Matchmaking;
