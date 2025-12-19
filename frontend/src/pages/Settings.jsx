import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { User, LogOut, Moon, ChevronLeft, Save, Sun } from 'lucide-react';

const Settings = () => {
    const navigate = useNavigate();
    // Initialize state from localStorage or default to true (dark mode)
    const [darkMode, setDarkMode] = useState(() => {
        const savedMode = localStorage.getItem('darkMode');
        return savedMode === null ? true : savedMode === 'true';
    });
    const [nickname, setNickname] = useState('Lumosse');

    // Apply dark/light mode effect
    useEffect(() => {
        if (darkMode) {
            document.body.classList.remove('light-mode');
        } else {
            document.body.classList.add('light-mode');
        }
        localStorage.setItem('darkMode', darkMode);
    }, [darkMode]);

    const handleSave = () => {
        // Mock save functionality
        // In a real app, this would send data to the backend
        alert('设置已保存！');
        navigate(-1); // Go back
    };

    return (
        <div className="min-h-screen pt-24 pb-12 px-6 transition-colors duration-300">
            <div className="max-w-2xl mx-auto">
                {/* Header */}
                <div className="flex items-center gap-4 mb-8">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 rounded-full glass-dark hover:bg-white/20 transition-all"
                    >
                        <ChevronLeft className="w-6 h-6 text-white" />
                    </button>
                    <h1 className="text-2xl font-bold text-white">个人设置</h1>
                </div>

                <div className="space-y-6">
                    {/* Profile Section */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="glass-dark rounded-3xl p-6"
                    >
                        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2 text-white">
                            <User className="w-5 h-5 text-apple-orange" />
                            个人信息
                        </h2>
                        <div className="flex items-center gap-6">
                            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center text-2xl font-bold text-white relative group cursor-pointer">
                                {nickname.charAt(0).toUpperCase()}
                                <div className="absolute inset-0 bg-black/50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-xs text-white">
                                    更换
                                </div>
                            </div>
                            <div className="flex-1">
                                <div className="mb-3">
                                    <label className="text-xs text-gray-500 block mb-1">昵称</label>
                                    <input
                                        type="text"
                                        value={nickname}
                                        onChange={(e) => setNickname(e.target.value)}
                                        className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-apple-orange transition-colors"
                                    />
                                </div>
                                <div>
                                    <label className="text-xs text-gray-500 block mb-1">UID</label>
                                    <div className="text-sm text-gray-400">1099338</div>
                                </div>
                            </div>
                        </div>
                    </motion.div>

                    {/* Preferences Section */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.1 }}
                        className="glass-dark rounded-3xl p-6"
                    >
                        <h2 className="text-lg font-semibold mb-4 text-white">偏好设置</h2>

                        <div className="space-y-4">
                            <div className="flex items-center justify-between p-3 bg-white/5 rounded-xl transition-colors">
                                <div className="flex items-center gap-3">
                                    <div className={`p-2 rounded-lg transition-colors ${darkMode ? 'bg-purple-500/20 text-purple-400' : 'bg-orange-500/20 text-orange-500'}`}>
                                        {darkMode ? <Moon className="w-5 h-5" /> : <Sun className="w-5 h-5" />}
                                    </div>
                                    <span className="text-white">深色模式</span>
                                </div>
                                <button
                                    onClick={() => setDarkMode(!darkMode)}
                                    className={`w-12 h-7 rounded-full transition-colors relative ${darkMode ? 'bg-apple-orange' : 'bg-gray-400'}`}
                                >
                                    <div className={`absolute top-1 w-5 h-5 bg-white rounded-full transition-transform ${darkMode ? 'left-6' : 'left-1'}`} />
                                </button>
                            </div>
                        </div>
                    </motion.div>

                    {/* Action Buttons */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.2 }}
                        className="flex flex-col gap-4"
                    >
                        <button
                            onClick={handleSave}
                            className="w-full py-4 bg-gradient-to-r from-apple-blue to-blue-600 text-white font-semibold rounded-2xl shadow-lg shadow-blue-500/30 hover:shadow-blue-500/50 hover:scale-[1.02] transition-all flex items-center justify-center gap-2"
                        >
                            <Save className="w-5 h-5" />
                            保存修改
                        </button>

                        <button
                            onClick={() => navigate('/login')}
                            className="w-full py-4 bg-red-500/10 text-red-500 font-semibold rounded-2xl border border-red-500/20 hover:bg-red-500/20 transition-all flex items-center justify-center gap-2"
                        >
                            <LogOut className="w-5 h-5" />
                            退出登录
                        </button>
                    </motion.div>
                </div>
            </div>
        </div>
    );
};

export default Settings;
