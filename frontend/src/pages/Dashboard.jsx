import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Trophy, Clock, Users, Map } from 'lucide-react';

const Dashboard = () => {
    const navigate = useNavigate();

    const stats = [
        { label: '总积分', value: '0', icon: Trophy },
        { label: '游戏场次', value: '0', icon: Clock },
        { label: '好友数量', value: '0', icon: Users },
        { label: '胜场数', value: '0', icon: Map },
    ];

    const matchHistory = [
        { date: '25-04-15 00:57', result: '排位赛' },
        { date: '25-04-15 00:47', result: '排位赛' },
        { date: '25-04-15 00:48', result: '排位赛' },
        { date: '25-04-15 00:38', result: '排位赛' },
        { date: '25-04-15 00:28', result: '排位赛' },
    ];

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
                            L
                        </div>
                        <div>
                            <h2 className="text-2xl font-bold mb-1">Lumosse</h2>
                            <p className="text-gray-500 text-sm">uid: 1099338</p>
                        </div>
                    </div>

                    {/* Stats Section */}
                    <div>
                        <h3 className="text-lg font-semibold mb-4">中大积分</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            {stats.map((stat, index) => (
                                <div key={index} className="text-center">
                                    <div className="flex justify-center mb-2">
                                        <stat.icon className="w-5 h-5 text-gray-500" />
                                    </div>
                                    <div className="text-sm text-gray-500 mb-1">{stat.label}</div>
                                    <div className="text-2xl font-bold">{stat.value}</div>
                                </div>
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
                        {matchHistory.map((match, index) => (
                            <div
                                key={index}
                                className="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl hover:bg-white/10 transition-all cursor-pointer"
                            >
                                <span className="text-gray-400 text-sm">{match.date}</span>
                                <span className="text-white font-medium">{match.result}</span>
                            </div>
                        ))}
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
