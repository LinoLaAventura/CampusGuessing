import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Trophy, Clock, Users, Map } from 'lucide-react';
import { getCurrentUserInfo, getDisplayName } from '../api/authStorage';
import { getFriendCount } from '../api/friendApi';
import { getUserRecords } from '../api/userApi';

const Dashboard = () => {
    const navigate = useNavigate();
    const [friendCount, setFriendCount] = useState('0');
    const [records, setRecords] = useState([]);
    const userInfo = getCurrentUserInfo();
    const displayName = getDisplayName() || userInfo?.username || '';
    const userId = userInfo?.userId;

    const formatTime = (isoString) => {
        if (!isoString) return '-';
        const date = new Date(isoString);
        const year = String(date.getFullYear()).slice(2);
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day} ${hour}:${minute}`;
    };

    // 预留：点击记录进行复盘
    const handleRecordClick = (recordId) => {
        console.log(`Maps to review for record: ${recordId}`);
        // 示例跳转： navigate(`/review/${recordId}`);
    };

    useEffect(() => {
        const currentUserInfo = getCurrentUserInfo();
        if (!userInfo?.username) {
            setFriendCount('0');
            setRecords([]);
            return;
        }

        let cancelled = false;
        (async () => {
            try {
                const count = await getFriendCount(userInfo.username);
                if (!cancelled) setFriendCount(String(count ?? 0));
            } catch {
                if (!cancelled) setFriendCount('0');
            }
        })();

        (async () => {
            if (!currentUserInfo.userId) return;
            try {
                const data = await getUserRecords(currentUserInfo.userId);
                // 假设 unwrapApiResponse 已经处理了 code === 200 并返回了 data 数组
                if (!cancelled && data) {
                    setRecords(data);
                }
            } catch (error) {
                console.error("Failed to fetch records:", error);
                // 出错时保持空数组或之前的状态
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    const stats = [
        { label: '总积分', value: '0', icon: Trophy },
        { label: '游戏场次', value: String(records.length), icon: Clock },
        { label: '好友', value: friendCount, icon: Users, onClick: () => navigate('/friends') },
        { label: '胜场数', value: '0', icon: Map },
    ];

    // const matchHistory = [
    //     { date: '25-04-15 00:57', result: '排位赛' },
    //     { date: '25-04-15 00:47', result: '排位赛' },
    //     { date: '25-04-15 00:48', result: '排位赛' },
    //     { date: '25-04-15 00:38', result: '排位赛' },
    //     { date: '25-04-15 00:28', result: '排位赛' },
    // ];

    return (
        <div className="min-h-screen pt-24 pb-12 px-6">
            <div className="max-w-4xl mx-auto">
                {/* User Profile Card */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="glass-dark rounded-3xl p-8 mb-8"
                >
                    <div className="flex items-center gap-6 mb-8">
                        {/* Avatar */}
                        <div className="w-20 h-20 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center text-2xl font-bold">
                            {(displayName || 'U').charAt(0).toUpperCase()}
                        </div>
                        <div>
                            <h2 className="text-2xl font-bold mb-1">{displayName || '-'}</h2>
                            <p className="text-gray-500 text-sm">uid: {userInfo?.userId ?? '-'}</p>
                        </div>
                    </div>

                    {/* Stats Section */}
                    <div>
                        <h3 className="text-lg font-semibold mb-4">中大积分</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            {stats.map((stat, index) => (
                                stat.onClick ? (
                                    <button
                                        key={stat.label}
                                        type="button"
                                        onClick={stat.onClick}
                                        className="text-center cursor-pointer"
                                    >
                                        <div className="flex justify-center mb-2">
                                            <stat.icon className="w-5 h-5 text-gray-500" />
                                        </div>
                                        <div className="text-sm text-gray-500 mb-1">{stat.label}</div>
                                        <div className="text-2xl font-bold">{stat.value}</div>
                                    </button>
                                ) : (
                                    <div key={stat.label || index} className="text-center">
                                        <div className="flex justify-center mb-2">
                                            <stat.icon className="w-5 h-5 text-gray-500" />
                                        </div>
                                        <div className="text-sm text-gray-500 mb-1">{stat.label}</div>
                                        <div className="text-2xl font-bold">{stat.value}</div>
                                    </div>
                                )
                            ))}
                        </div>
                    </div>
                </motion.div>

                {/* Match History */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                    className="glass-dark rounded-3xl p-8"
                >
                    <h3 className="text-lg font-semibold mb-6">对战记录</h3>
                    <div className="space-y-3">
                        {records.length > 0 ? (
                            records.map((record) => (
                                <div
                                    key={record.recordId}
                                    onClick={() => handleRecordClick(record.recordId)}
                                    className="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl hover:bg-white/10 transition-all cursor-pointer"
                                >
                                    <span className="text-gray-400 text-sm">
                                        {formatTime(record.createdAt)}
                                    </span>
                                    <div className="flex items-center gap-4">
                                        {/* 如果想显示积分，可以在这里加 */}
                                        {/* <span className="text-yellow-500 text-sm">+{record.earnPoints}分</span> */}
                                        <span className="text-white font-medium">{record.gameType}</span>
                                    </div>
                                </div>
                            ))
                        ) : (
                            <div className="text-center text-gray-500 py-4">
                                暂无对战记录
                            </div>
                        )}
                    </div>
                </motion.div>

                {/* Game Menu Buttons */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                    className="mt-8 flex flex-col gap-4"
                >
                    <button
                        onClick={() => navigate('/game-menu')}
                        className="w-full py-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-2xl shadow-lg shadow-orange-500/30 hover:shadow-orange-500/50 hover:scale-[1.02] transition-all"
                    >
                        开始游戏
                    </button>
                    <button
                        onClick={() => navigate('/leaderboard')}
                        className="w-full py-4 glass-dark text-white font-semibold rounded-2xl hover:bg-white/20 transition-all"
                    >
                        查看排行榜
                    </button>
                </motion.div>
            </div>
        </div>
    );
};

export default Dashboard;
