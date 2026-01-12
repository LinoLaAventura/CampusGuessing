import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { login, registerUser } from '../api/userApi';
import { setAuth } from '../api/authStorage';

const Login = () => {
    const navigate = useNavigate();
    const MotionDiv = motion.div;
    const MotionButton = motion.button;
    const [activeTab, setActiveTab] = useState('login'); // 'login' or 'register'
    const [formData, setFormData] = useState({
        username: '',
        password: '',
    });
    const [submitting, setSubmitting] = useState(false);
    const [formError, setFormError] = useState('');

    let submitText;
    if (submitting) submitText = '提交中...';
    else if (activeTab === 'login') submitText = '登录';
    else submitText = '注册';

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.username || !formData.password) return;
        setFormError('');
        try {
            setSubmitting(true);

            if (activeTab === 'register') {
                try {
                    await registerUser({
                        username: formData.username,
                        password: formData.password,
                        role: 'user',
                    });
                } catch (err) {
                    const msg = err?.message || '注册失败';
                    // Detect duplicate/exists messages and show concise提示
                    if (/已存在|exists|duplicate|重复/.test(msg)) {
                        setFormError('不可注册：用户名已存在');
                    } else {
                        setFormError(msg);
                    }
                    setSubmitting(false);
                    return;
                }
            }

            try {
                const auth = await login({
                    username: formData.username,
                    password: formData.password,
                });
                setAuth(auth);
                navigate('/dashboard');
            } catch (err) {
                setFormError(err?.message || '登录失败');
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-black flex items-center justify-center p-6 relative overflow-hidden">
            {/* Background Gradient Mesh */}
            <div className="fixed inset-0 z-0 pointer-events-none opacity-30">
                <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-apple-blue rounded-full blur-[150px]" />
                <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-purple-600 rounded-full blur-[150px]" />
            </div>

            {/* Login Card */}
            <MotionDiv
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6 }}
                className="relative z-10 w-full max-w-md"
            >
                {/* Logo / Title */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold bg-gradient-to-r from-white to-gray-400 bg-clip-text text-transparent mb-2">
                        中大图寻
                    </h1>
                    <p className="text-gray-500 text-sm">探索中山大学的每一个角落</p>
                </div>

                {/* Glass Card */}
                <div className="glass-dark rounded-3xl p-8 shadow-2xl">
                    {/* Tab Switcher */}
                    <div className="flex gap-4 mb-8">
                        <button
                            onClick={() => { setActiveTab('login'); setFormError(''); }}
                            className={`flex-1 py-2 text-sm font-medium transition-all rounded-lg ${activeTab === 'login'
                                ? 'text-apple-orange border-b-2 border-apple-orange'
                                : 'text-gray-500 hover:text-gray-300'
                                }`}
                        >
                            账号登录
                        </button>
                        <button
                            onClick={() => { setActiveTab('register'); setFormError(''); }}
                            className={`flex-1 py-2 text-sm font-medium transition-all rounded-lg ${activeTab === 'register'
                                ? 'text-apple-orange border-b-2 border-apple-orange'
                                : 'text-gray-500 hover:text-gray-300'
                                }`}
                        >
                            账号注册
                        </button>
                    </div>

                    {formError ? (
                        <div className="mb-4 p-3 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400">
                            {formError}
                        </div>
                    ) : null}

                    {/* Form */}
                    <form onSubmit={handleSubmit} className="space-y-5">
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-gray-400 mb-2">
                                用户名/邮箱账号
                            </label>
                            <input
                                id="username"
                                type="text"
                                placeholder="请输入用户名或邮箱号码"
                                value={formData.username}
                                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                                className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange focus:bg-white/10 transition-all"
                            />
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-400 mb-2">
                                密码
                            </label>
                            <input
                                id="password"
                                type="password"
                                placeholder="请输入密码"
                                value={formData.password}
                                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange focus:bg-white/10 transition-all"
                            />
                        </div>

                        {/* Submit Button */}
                        <MotionButton
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                            type="submit"
                            disabled={submitting}
                            className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-medium rounded-xl shadow-lg shadow-orange-500/30 hover:shadow-orange-500/50 transition-all"
                        >
                            {submitText}
                        </MotionButton>
                    </form>

                    {/* Footer Links */}
                    <div className="mt-6 flex justify-center gap-4 text-xs">
                        <button
                            onClick={() => navigate('/reset-password')}
                            className="text-gray-500 hover:text-white transition-colors underline"
                        >
                            忘记密码？
                        </button>
                    </div>
                </div>

                {/* Additional Info */}
                <p className="text-center text-xs text-gray-600 mt-6">
                    登录即表示同意 <span className="text-apple-orange cursor-pointer hover:underline">用户协议</span>
                </p>
            </MotionDiv>
        </div>
    );
};

export default Login;
