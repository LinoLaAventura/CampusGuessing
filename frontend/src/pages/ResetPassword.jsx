import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

const ResetPassword = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [submitted, setSubmitted] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();
        // TODO: Connect to backend API for password reset
        setSubmitted(true);
        setTimeout(() => {
            navigate('/login');
        }, 3000);
    };

    return (
        <div className="min-h-screen bg-black flex items-center justify-center p-6 relative overflow-hidden">
            {/* Background Gradient Mesh */}
            <div className="fixed inset-0 z-0 pointer-events-none opacity-30">
                <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-apple-blue rounded-full blur-[150px]" />
                <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-purple-600 rounded-full blur-[150px]" />
            </div>

            {/* Card */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6 }}
                className="relative z-10 w-full max-w-md"
            >
                {/* Logo / Title */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold bg-gradient-to-r from-white to-gray-400 bg-clip-text text-transparent mb-2">
                        重置密码
                    </h1>
                    <p className="text-gray-500 text-sm">找回您的账号密码</p>
                </div>

                {/* Glass Card */}
                <div className="glass-dark rounded-3xl p-8 shadow-2xl">
                    {!submitted ? (
                        <form onSubmit={handleSubmit} className="space-y-5">
                            <div>
                                <label className="block text-sm font-medium text-gray-400 mb-2">
                                    注册邮箱
                                </label>
                                <input
                                    type="email"
                                    required
                                    placeholder="请输入您的注册邮箱"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange focus:bg-white/10 transition-all"
                                />
                            </div>

                            {/* Submit Button */}
                            <motion.button
                                whileHover={{ scale: 1.02 }}
                                whileTap={{ scale: 0.98 }}
                                type="submit"
                                className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-medium rounded-xl shadow-lg shadow-orange-500/30 hover:shadow-orange-500/50 transition-all"
                            >
                                发送重置链接
                            </motion.button>
                        </form>
                    ) : (
                        <div className="text-center py-8">
                            <div className="text-5xl mb-4">📧</div>
                            <h3 className="text-xl font-bold text-white mb-2">邮件已发送</h3>
                            <p className="text-gray-400 text-sm">
                                重置链接已发送至 {email}，请查收邮件。
                                <br />
                                3秒后自动返回登录页...
                            </p>
                        </div>
                    )}

                    {/* Footer Links */}
                    <div className="mt-6 flex justify-center text-xs">
                        <button
                            onClick={() => navigate('/login')}
                            className="text-gray-500 hover:text-white transition-colors flex items-center gap-1"
                        >
                            <span>←</span> 返回登录
                        </button>
                    </div>
                </div>
            </motion.div>
        </div>
    );
};

export default ResetPassword;
