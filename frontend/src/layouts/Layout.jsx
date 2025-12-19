import React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

const Layout = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const isGame = location.pathname.includes('/game');
    const isLogin = location.pathname === '/login';

    // Don't show layout elements on login page
    if (isLogin) {
        return <Outlet />;
    }

    return (
        <div className="min-h-screen bg-black text-white font-sans selection:bg-apple-orange selection:text-white overflow-hidden">
            {/* Background Gradient Mesh (Subtle) */}
            <div className="fixed inset-0 z-0 pointer-events-none opacity-20">
                <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-apple-blue rounded-full blur-[120px]" />
                <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-600 rounded-full blur-[120px]" />
            </div>

            {/* Navigation / Header */}
            {!isGame && (
                <header className="fixed top-0 left-0 right-0 z-50 p-6 flex justify-between items-center">
                    <div className="flex items-center gap-4">
                        {location.pathname !== '/' && (
                            <button
                                onClick={() => navigate(-1)}
                                className="p-2 bg-white/10 hover:bg-white/20 rounded-full backdrop-blur-md transition-all"
                            >
                                <ArrowLeft size={20} />
                            </button>
                        )}
                    </div>

                    <div className="flex items-center gap-6 text-sm font-medium text-gray-400">
                        <button onClick={() => navigate('/login')} className="hover:text-white transition-colors">退出登录</button>
                        <button onClick={() => navigate('/settings')} className="hover:text-white transition-colors">个人设置</button>
                    </div>
                </header>
            )}

            {/* Main Content */}
            <main className="relative z-10 w-full h-full min-h-screen flex flex-col">
                <Outlet />
            </main>
        </div>
    );
};

export default Layout;
