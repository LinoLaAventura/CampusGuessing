import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ChevronLeft, Trophy, Medal } from 'lucide-react';

const Leaderboard = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('friend'); // 'friend' or 'global'

    const rankings = [
        { rank: 1, name: 'Alex', score: 2450, avatar: 'A', color: 'bg-blue-500' },
        { rank: 2, name: 'Sarah', score: 2380, avatar: 'S', color: 'bg-purple-500' },
        { rank: 3, name: 'Mike', score: 2100, avatar: 'M', color: 'bg-green-500' },
        { rank: 4, name: 'Lumosse', score: 1850, avatar: 'L', color: 'bg-orange-500', isMe: true },
        { rank: 5, name: 'Tom', score: 1600, avatar: 'T', color: 'bg-red-500' },
    ];

    return (
        <div className="min-h-screen pt-24 pb-12 px-6">
            <div className="max-w-2xl mx-auto">
                {/* Header */}
                <div className="flex items-center gap-4 mb-8">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 rounded-full glass-dark hover:bg-white/20 transition-all"
                    >
                        <ChevronLeft className="w-6 h-6 text-white" />
                    </button>
                    <h1 className="text-2xl font-bold">排行榜</h1>
                </div>

                {/* Tabs */}
                <div className="flex gap-4 mb-8 bg-white/5 p-1 rounded-2xl">
                    <button
                        onClick={() => setActiveTab('friend')}
                        className={`flex-1 py-3 text-sm font-medium transition-all rounded-xl ${activeTab === 'friend'
                            ? 'bg-white/10 text-white shadow-lg'
                            : 'text-gray-500 hover:text-gray-300'
                            }`}
                    >
                        好友排名
                    </button>
                    <button
                        onClick={() => setActiveTab('global')}
                        className={`flex-1 py-3 text-sm font-medium transition-all rounded-xl ${activeTab === 'global'
                            ? 'bg-white/10 text-white shadow-lg'
                            : 'text-gray-500 hover:text-gray-300'
                            }`}
                    >
                        全服排名
                    </button>
                </div>

                {/* List */}
                <div className="space-y-4">
                    {rankings.map((user, index) => (
                        <motion.div
                            key={index}
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: index * 0.05 }}
                            className={`glass-dark rounded-2xl p-4 flex items-center gap-4 ${user.isMe ? 'border-apple-orange/50 bg-apple-orange/10' : ''}`}
                        >
                            <div className={`w-8 text-center font-bold ${user.rank === 1 ? 'text-yellow-400 text-xl' :
                                    user.rank === 2 ? 'text-gray-300 text-lg' :
                                        user.rank === 3 ? 'text-amber-600 text-lg' :
                                            'text-gray-500'
                                }`}>
                                {user.rank <= 3 ? <Medal className="w-6 h-6 mx-auto" /> : user.rank}
                            </div>

                            <div className={`w-12 h-12 rounded-full ${user.color} flex items-center justify-center text-lg font-bold`}>
                                {user.avatar}
                            </div>

                            <div className="flex-1">
                                <div className="font-bold flex items-center gap-2">
                                    {user.name}
                                    {user.isMe && <span className="text-xs bg-apple-orange px-2 py-0.5 rounded-full text-white">我</span>}
                                </div>
                            </div>

                            <div className="text-right">
                                <div className="text-apple-orange font-bold text-lg">{user.score}</div>
                                <div className="text-xs text-gray-500">积分</div>
                            </div>
                        </motion.div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Leaderboard;
