import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';

const Matchmaking = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const mode = searchParams.get('mode') || 'normal';
    const [countdown, setCountdown] = useState(3);

    useEffect(() => {
        // Simulate matchmaking countdown
        const timer = setInterval(() => {
            setCountdown((prev) => {
                if (prev <= 1) {
                    clearInterval(timer);
                    // Navigate to game after countdown
                    setTimeout(() => navigate('/game'), 1000);
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(timer);
    }, [navigate]);

    const getTitle = () => {
        return mode === 'ranked' ? '排位赛匹配成功' : '普通匹配成功';
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
                    {mode === 'ranked' ? '积分对战即将开始' : '对战即将开始'}
                </motion.p>

                {countdown > 0 && (
                    <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ delay: 0.6 }}
                        className="text-6xl font-bold mb-12 text-white"
                    >
                        {countdown}秒
                    </motion.div>
                )}

                {/* VS Section */}
                <div className="flex items-center justify-center gap-12 mb-8">
                    {/* Player 1 */}
                    <motion.div
                        initial={{ x: -100, opacity: 0 }}
                        animate={{ x: 0, opacity: 1 }}
                        transition={{ delay: 0.3 }}
                        className="text-center"
                    >
                        <div className="w-24 h-24 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center text-3xl font-bold mb-3 mx-auto text-white">
                            用
                        </div>
                        <div className="text-sm text-gray-400">用户79305343</div>
                        <div className="text-xs text-gray-600">中国积分：1444</div>
                    </motion.div>

                    {/* VS Text */}
                    <motion.div
                        initial={{ scale: 0, rotate: -180 }}
                        animate={{ scale: 1, rotate: 0 }}
                        transition={{ delay: 0.5, type: 'spring' }}
                        className="text-5xl font-bold text-apple-orange"
                    >
                        VS
                    </motion.div>

                    {/* Player 2 */}
                    <motion.div
                        initial={{ x: 100, opacity: 0 }}
                        animate={{ x: 0, opacity: 1 }}
                        transition={{ delay: 0.3 }}
                        className="text-center"
                    >
                        <div className="w-24 h-24 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center text-3xl font-bold mb-3 mx-auto text-white">
                            {/* TODO: Use real user initial */}
                            L
                        </div>
                        <div className="text-sm text-gray-400">Lumosse</div>
                        <div className="text-xs text-gray-600">中国积分：1500</div>
                    </motion.div>
                </div>
            </motion.div>
        </div>
    );
};

export default Matchmaking;
