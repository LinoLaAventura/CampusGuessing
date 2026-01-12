import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
    Trophy, Clock, Users, Map, 
    PlusCircle, MapPin, ImageIcon, Upload, X, ChevronDown, 
    History // 引入 History 图标用于对战记录标题
} from 'lucide-react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { getCurrentUserInfo, getDisplayName } from '../api/authStorage';
import { getFriendCount } from '../api/friendApi';
import { getUserRecords } from '../api/userApi';
import { createQuestion } from '../api/questionApi';
import { uploadImage } from '../api/imageUpload';

// --- 常量定义 ---
const CAMPUS_OPTIONS = [
    { value: '', label: '全部校区' },
    { value: 'zhuhai', label: '珠海' },
    { value: 'shenzhen', label: '深圳' },
    { value: 'south', label: '南校区' },
    { value: 'east', label: '东校区' },
    { value: 'north', label: '北校区' },
];

const DIFFICULTY_OPTIONS = [
    { value: '', label: '全部难度' },
    { value: 'easy', label: 'easy' },
    { value: 'medium', label: 'medium' },
    { value: 'hard', label: 'hard' },
];

// --- Leaflet 图标修复 ---
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// --- 辅助组件 & 函数 ---

function MapPickerModal({ isOpen, onClose, onConfirm, initialCoord }) {
    const [selectedCoord, setSelectedCoord] = useState(initialCoord || null);

    function MapClickHandler() {
        useMapEvents({
            click(e) {
                setSelectedCoord({ lat: e.latlng.lat, lon: e.latlng.lng });
            },
        });
        return null;
    }

    const handleConfirm = () => {
        if (selectedCoord) {
            onConfirm(selectedCoord);
            onClose();
        }
    };

    const mapCenter = [22.3477, 113.5894]; // 默认中心

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80"
                onClick={onClose}
            >
                <motion.div
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    className="glass-dark rounded-3xl p-6 w-full max-w-4xl border border-white/10"
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                            <MapPin className="w-5 h-5 text-orange-400" />
                            <h2 className="text-xl font-bold">在地图上选择正确位置</h2>
                        </div>
                        <button
                            type="button"
                            onClick={onClose}
                            className="p-2 rounded-full hover:bg-white/10 transition-all"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {selectedCoord ? (
                        <div className="mb-3 text-sm text-gray-400">
                            已选坐标：{selectedCoord.lat.toFixed(6)}, {selectedCoord.lon.toFixed(6)}
                        </div>
                    ) : (
                        <div className="mb-3 text-sm text-gray-500">请在地图上点击选择正确位置</div>
                    )}

                    <div className="w-full h-[500px] rounded-2xl overflow-hidden border border-white/10 mb-4">
                        <MapContainer
                            key={isOpen ? 'map-open' : 'map-closed'}
                            center={mapCenter}
                            zoom={15}
                            style={{ height: '100%', width: '100%' }}
                            zoomControl={true}
                        >
                            <TileLayer
                                url="https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}"
                                attribution='&copy; <a href="https://www.amap.com/">高德地图</a>'
                                subdomains={['1', '2', '3', '4']}
                            />
                            <MapClickHandler />
                            {selectedCoord ? (
                                <Marker position={[selectedCoord.lat, selectedCoord.lon]} />
                            ) : null}
                        </MapContainer>
                    </div>

                    <div className="flex gap-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 py-3 bg-white/10 hover:bg-white/20 rounded-xl transition-all text-white"
                        >
                            取消
                        </button>
                        <button
                            type="button"
                            onClick={handleConfirm}
                            disabled={!selectedCoord}
                            className="flex-1 py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all disabled:opacity-50 disabled:hover:shadow-none"
                        >
                            确认位置
                        </button>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}

function cleanErrorMessage(errorMsg) {
    if (!errorMsg) return '操作失败';
    let msg = String(errorMsg);
    if (msg.length > 200) msg = msg.substring(0, 200) + '...';
    if (msg.includes('参数验证失败')) {
        const regex = /=([^,}]+)/;
        const match = regex.exec(msg);
        if (match?.[1]) return match[1].trim();
    }
    if (msg.includes('constraint') && msg.includes('content')) return '题目描述不能为空';
    return msg;
}

// --- 主组件 ---

const Dashboard = () => {
    const navigate = useNavigate();
    const [friendCount, setFriendCount] = useState('0');
    const [records, setRecords] = useState([]);
    const userInfo = getCurrentUserInfo();
    const displayName = getDisplayName() || userInfo?.username || '';
    const userId = userInfo?.userId;

    // 折叠状态控制
    const [isCreateOpen, setIsCreateOpen] = useState(false); // 创建题目默认折叠
    const [isHistoryOpen, setIsHistoryOpen] = useState(false); // 对战记录默认折叠

    // 创建题目表单状态
    const [createCampus, setCreateCampus] = useState('zhuhai');
    const [createDifficulty, setCreateDifficulty] = useState('easy');
    const [createCoord, setCreateCoord] = useState(null);
    const [uploadedImageKey, setUploadedImageKey] = useState('');
    const [uploadedImageUrl, setUploadedImageUrl] = useState('');
    const [uploadingImage, setUploadingImage] = useState(false);
    const [createTitle, setCreateTitle] = useState('');
    const [createContent, setCreateContent] = useState('');
    const [createAnswer, setCreateAnswer] = useState('');
    const [createLoading, setCreateLoading] = useState(false);
    const [showMapPicker, setShowMapPicker] = useState(false);
    
    // 反馈消息
    const [actionMsg, setActionMsg] = useState('');
    const [actionErr, setActionErr] = useState('');

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

    const handleRecordClick = (record) => {
        if (!userId) {
            navigate('/login');
            return;
        }
        navigate('/review', { state: { userId, recordId: record.recordId } });
    };

    useEffect(() => {
        const currentUserInfo = getCurrentUserInfo();
        if (!currentUserInfo?.username) {
            setFriendCount('0');
            setRecords([]);
            return;
        }

        let cancelled = false;
        (async () => {
            try {
                const count = await getFriendCount(currentUserInfo.username);
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

    // --- 创建题目逻辑 ---

    const handleImageUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        try {
            setUploadingImage(true);
            setActionErr('');
            setActionMsg('');
            const result = await uploadImage(file);
            setUploadedImageKey(result.key);
            setUploadedImageUrl(result.url);
            setActionMsg('图片上传成功！');
        } catch (error) {
            console.error('图片上传失败:', error);
            setActionErr(cleanErrorMessage(error.message) || '图片上传失败');
            setUploadedImageKey(''); 
            setUploadedImageUrl('');
        } finally {
            setUploadingImage(false);
        }
    };

    const handleCreate = async () => {
        const currentUser = getCurrentUserInfo();
        if (!currentUser?.username) {
            navigate('/login');
            return;
        }

        if (!createCoord?.lat || !createCoord?.lon) {
            setActionErr('请在地图上选择正确位置');
            return;
        }

        if (!uploadedImageKey) {
            setActionErr('请上传题目图片');
            return;
        }

        try {
            setCreateLoading(true);
            setActionMsg('');
            setActionErr('');

            const resp = await createQuestion(currentUser.username, {
                campus: createCampus,
                difficulty: createDifficulty,
                key: uploadedImageKey,
                correctCoord: { lon: createCoord.lon, lat: createCoord.lat },
                title: createTitle.trim() || '未命名题目',
                content: createContent.trim() || '无描述',
                answer: createAnswer.trim() || '无答案',
            });

            setActionMsg(`题目创建成功（ID: ${resp?.id ?? '-'}）`);
            
            // 清空表单
            setCreateCoord(null);
            setUploadedImageKey('');
            setUploadedImageUrl('');
            setCreateTitle('');
            setCreateContent('');
            setCreateAnswer('');
            // 保持展开状态，方便继续创建

        } catch (e) {
            setActionErr(cleanErrorMessage(e?.message) || '题目创建失败');
        } finally {
            setCreateLoading(false);
        }
    };

    const stats = [
        { label: '总积分', value: userInfo.points, icon: Trophy },
        // { label: '总积分', value: '0', icon: Trophy },
        { label: '游戏场次', value: String(records.length), icon: Clock },
        { label: '好友', value: friendCount, icon: Users, onClick: () => navigate('/friends') },
        // { label: '胜场数', value: '0', icon: Map },
        // { label: '胜场数', value: '0', icon: Map },
    ];

    return (
        <div className="min-h-screen pt-24 pb-12 px-6">
            <div className="max-w-4xl mx-auto">
                {/* 消息提示区 */}
                {(actionMsg || actionErr) && (
                    <div className="mb-6">
                        {actionMsg && (
                            <div className="mb-2 p-4 bg-green-500/10 border border-green-500/30 rounded-xl text-green-400">
                                {actionMsg}
                            </div>
                        )}
                        {actionErr && (
                            <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400">
                                {actionErr}
                            </div>
                        )}
                    </div>
                )}

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
                        {/* <h3 className="text-lg font-semibold mb-4">中大积分</h3> */}
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
                    className="glass-dark rounded-3xl mb-8 overflow-hidden"
                >
                    <button
                        type="button"
                        onClick={() => setIsHistoryOpen(!isHistoryOpen)}
                        className="w-full flex items-center justify-between p-6 hover:bg-white/5 transition-colors cursor-pointer text-left bg-blue-900"
                    >
                        <div className="flex items-center gap-3">
                            <History className="w-5 h-5 text-gray-400" />
                            <h2 className="text-xl font-bold">对战记录</h2>
                        </div>
                        <div className="flex items-center gap-2">
                             {!isHistoryOpen && records.length > 0 && (
                                <span className="text-xs text-gray-500">最近: {formatTime(records[0]?.createdAt)}</span>
                             )}
                            <ChevronDown 
                                className={`w-5 h-5 text-gray-400 transition-transform duration-300 ${
                                    isHistoryOpen ? 'rotate-180' : ''
                                }`} 
                            />
                        </div>
                    </button>

                    <AnimatePresence>
                        {isHistoryOpen && (
                            <motion.div
                                initial={{ height: 0, opacity: 0 }}
                                animate={{ height: 'auto', opacity: 1 }}
                                exit={{ height: 0, opacity: 0 }}
                                transition={{ duration: 0.3, ease: 'easeInOut' }}
                            >
                                <div className="px-6 pb-6 pt-2">
                                    <div className="space-y-3">
                                        {records.length > 0 ? (
                                            records.map((record) => (
                                                <div
                                                    key={record.recordId}
                                                    onClick={() => handleRecordClick(record)}
                                                    className="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl hover:bg-white/10 transition-all cursor-pointer"
                                                >
                                                    <span className="text-gray-400 text-sm">
                                                        {formatTime(record.createdAt)}
                                                    </span>
                                                    <div className="flex items-center gap-4">
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
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </motion.div>

                {/* Create Question Panel (Collapsible) */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                    className="glass-dark rounded-3xl mb-8 overflow-hidden"
                >
                    <button
                        type="button"
                        onClick={() => setIsCreateOpen(!isCreateOpen)}
                        className="w-full flex items-center justify-between p-6 hover:bg-white/5 transition-colors cursor-pointer text-left bg-blue-800"
                    >
                        <div className="flex items-center gap-3">
                            <PlusCircle className="w-5 h-5 text-gray-400" />
                            <h2 className="text-xl font-bold">创建题目</h2>
                        </div>
                        <ChevronDown 
                            className={`w-5 h-5 text-gray-400 transition-transform duration-300 ${
                                isCreateOpen ? 'rotate-180' : ''
                            }`} 
                        />
                    </button>

                    <AnimatePresence>
                        {isCreateOpen && (
                            <motion.div
                                initial={{ height: 0, opacity: 0 }}
                                animate={{ height: 'auto', opacity: 1 }}
                                exit={{ height: 0, opacity: 0 }}
                                transition={{ duration: 0.3, ease: 'easeInOut' }}
                            >
                                <div className="px-6 pb-6 pt-2">
                                    <div className="grid md:grid-cols-2 gap-3 mb-3">
                                        <select
                                            value={createCampus}
                                            onChange={(e) => setCreateCampus(e.target.value)}
                                            className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-apple-orange transition-all"
                                        >
                                            {CAMPUS_OPTIONS.filter((o) => o.value !== '').map((o) => (
                                                <option key={o.value} value={o.value} className="bg-gray-900">
                                                    {o.label}
                                                </option>
                                            ))}
                                        </select>

                                        <select
                                            value={createDifficulty}
                                            onChange={(e) => setCreateDifficulty(e.target.value)}
                                            className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-apple-orange transition-all"
                                        >
                                            {DIFFICULTY_OPTIONS.filter((o) => o.value !== '').map((o) => (
                                                <option key={o.value} value={o.value} className="bg-gray-900">
                                                    {o.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="mb-3">
                                        <button
                                            type="button"
                                            onClick={() => setShowMapPicker(true)}
                                            className="w-full py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all text-white flex items-center justify-center gap-2"
                                        >
                                            <MapPin className="w-4 h-4" />
                                            {createCoord ? `已选位置：${createCoord.lat.toFixed(4)}, ${createCoord.lon.toFixed(4)}` : '在地图上选择正确位置（必填）'}
                                        </button>
                                    </div>

                                    <div className="mb-3">
                                        <label className="w-full block">
                                            <input
                                                type="file"
                                                accept="image/*"
                                                onChange={handleImageUpload}
                                                disabled={uploadingImage}
                                                className="hidden"
                                            />
                                            <div className="w-full py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all text-white flex items-center justify-center gap-2 cursor-pointer">
                                                {uploadingImage && (
                                                    <>
                                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                                                        上传中...
                                                    </>
                                                )}
                                                {!uploadingImage && uploadedImageKey && (
                                                    <>
                                                        <ImageIcon className="w-4 h-4 text-green-400" />
                                                        已上传图片
                                                    </>
                                                )}
                                                {!uploadingImage && !uploadedImageKey && (
                                                    <>
                                                        <Upload className="w-4 h-4" />
                                                        上传题目图片（必填）
                                                    </>
                                                )}
                                            </div>
                                        </label>
                                        {uploadedImageKey && (
                                            <div className="mt-2">
                                                <div className="text-xs text-gray-400 mb-2 truncate">图片Key: {uploadedImageKey}</div>
                                                {uploadedImageUrl && (
                                                    <div className="w-full h-48 rounded-xl overflow-hidden border border-white/10 bg-black">
                                                        <img 
                                                            src={uploadedImageUrl}
                                                            alt="题目图片预览" 
                                                            className="w-full h-full object-contain"
                                                            referrerPolicy="no-referrer"
                                                            onError={(e) => {
                                                                console.error('图片加载失败:', uploadedImageUrl);
                                                                e.target.style.display = 'none';
                                                            }}
                                                        />
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>

                                    <div className="grid gap-3 mb-3">
                                        <input
                                            type="text"
                                            value={createTitle}
                                            onChange={(e) => setCreateTitle(e.target.value)}
                                            placeholder="标题"
                                            className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all"
                                        />
                                    </div>

                                    <div className="grid gap-3 mb-3">
                                        <textarea
                                            value={createContent}
                                            onChange={(e) => setCreateContent(e.target.value)}
                                            placeholder="题目内容（可选）"
                                            rows={3}
                                            className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all resize-none"
                                        />
                                    </div>

                                    <div className="grid gap-3 mb-4">
                                        <input
                                            type="text"
                                            value={createAnswer}
                                            onChange={(e) => setCreateAnswer(e.target.value)}
                                            placeholder="提示（可选）"
                                            className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all"
                                        />
                                    </div>

                                    <button
                                        type="button"
                                        onClick={handleCreate}
                                        disabled={createLoading}
                                        className="w-full py-3 bg-gradient-to-r from-blue-800 to-blue-800 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all disabled:opacity-50 disabled:hover:shadow-none"
                                    >
                                        {createLoading ? '创建中...' : '创建题目'}
                                    </button>

                                    <div className="text-gray-500 text-xs mt-3">
                                        提示：地图选点和图片上传为必填项；图片将上传到 PICUI 图床。
                                    </div>
                                </div>
                            </motion.div>
                        //     ))
                        // ) : (
                        //     <div className="text-center text-gray-500 py-4">
                        //         暂无对战记录
                        //     </div>
                        )}
                    </AnimatePresence>
                    {/* </div> */}
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
            <MapPickerModal
                isOpen={showMapPicker}
                onClose={() => setShowMapPicker(false)}
                onConfirm={setCreateCoord}
                initialCoord={createCoord}
            />
        </div>
    );
};