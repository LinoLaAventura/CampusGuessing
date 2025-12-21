import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Swords, Trophy, ChevronLeft } from 'lucide-react';

const GameMenu = () => {
    const navigate = useNavigate();

    const menuItems = [
        {
            title: '开始匹配',
            description: '普通模式，轻松对战',
            icon: Swords,
            color: 'from-blue-500 to-cyan-500',
            path: '/matchmaking?mode=normal',
            delay: 0
        },
        {
            title: '积分排行',
            description: '排位模式，争夺荣耀',
            icon: Trophy,
            color: 'from-orange-500 to-red-500',
            path: '/matchmaking?mode=ranked',
            delay: 0.1
        }
    ];

    return (
        <div className="min-h-screen flex flex-col items-center justify-center p-6 relative overflow-hidden">
            {/* Background Elements */}
            <div className="absolute inset-0 bg-gradient-to-b from-black via-gray-900 to-black -z-10" />
            <div className="absolute top-0 left-0 w-full h-full overflow-hidden -z-10 opacity-30">
                <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-500/20 rounded-full blur-3xl animate-pulse" />
                <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl animate-pulse delay-1000" />
            </div>

            <div className="max-w-4xl w-full">
                {/* Header */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex items-center justify-between mb-12"
                >
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="p-3 rounded-full glass-dark hover:bg-white/10 transition-all group"
                    >
                        <ChevronLeft className="w-6 h-6 text-gray-400 group-hover:text-white transition-colors" />
                    </button>
                    <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-400">
                        选择游戏模式
                    </h1>
                    <div className="w-12" /> {/* Spacer for centering */}
                </motion.div>

                {/* Cards Grid */}
                <div className="grid md:grid-cols-2 gap-8">
                    {menuItems.map((item, index) => (
                        <motion.button
                            key={item.title}
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: item.delay }}
                            whileHover={{ scale: 1.02, translateY: -5 }}
                            whileTap={{ scale: 0.98 }}
                            onClick={() => navigate(item.path)}
                            className="relative group overflow-hidden rounded-3xl glass-dark p-8 text-left transition-all border border-white/5 hover:border-white/20"
                        >
                            {/* Hover Gradient Background */}
                            <div className={`absolute inset-0 bg-gradient-to-br ${item.color} opacity-0 group-hover:opacity-10 transition-opacity duration-500`} />

                            <div className="relative z-10 flex flex-col h-full justify-between gap-8">
                                <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${item.color} flex items-center justify-center shadow-lg`}>
                                    <item.icon className="w-8 h-8 text-white" />
                                </div>

                                <div>
                                    <h3 className="text-2xl font-bold text-white mb-2 group-hover:translate-x-1 transition-transform">
                                        {item.title}
                                    </h3>
                                    <p className="text-gray-400 group-hover:text-gray-200 transition-colors">
                                        {item.description}
                                    </p>
                                </div>
                            </div>
                        </motion.button>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default GameMenu;
