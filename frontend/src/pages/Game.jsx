import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, Crosshair, ZoomIn, ZoomOut } from 'lucide-react';
import 'leaflet/dist/leaflet.css';

import { getCurrentUserInfo } from '../api/authStorage';
import { getQuestionDetail } from '../api/questionApi';
import { submitGameRecord } from '../api/recordApi';

// Fix Leaflet default marker icon issue
import L from 'leaflet';
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

function formatMeters(meters) {
    if (!Number.isFinite(meters)) return '-';
    if (meters >= 1000) return `${(meters / 1000).toFixed(2)} km`;
    return `${Math.round(meters)} m`;
}

// 与后端 RecordServiceImpl 的计分方式保持一致：max 50，半径 10km 线性衰减
function calculateDistanceAndScore(correct, guess) {
    if (correct?.lat == null || correct?.lon == null) {
        return { meters: Number.NaN, score: 0 };
    }

    const R = 6371000;
    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(guess.lat - correct.lat);
    const dLon = toRad(guess.lng - correct.lon);
    const lat1 = toRad(correct.lat);
    const lat2 = toRad(guess.lat);

    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const meters = R * c;

    const max = 100;
    const radius = 1000;
    const raw = Math.max(0, max * (1 - meters / radius));
    const score = Math.round(raw);
    return { meters, score };
}

function buildSubmitResult({ guessPosition, correctCoord }) {
    if (!guessPosition) {
        return {
            meters: Number.NaN,
            score: 0,
            isCorrect: false,
            message: '请先在地图上标记位置！',
        };
    }

    if (correctCoord?.lat == null || correctCoord?.lon == null) {
        return {
            meters: Number.NaN,
            score: 0,
            isCorrect: false,
            message: '该题目缺少正确坐标，无法练习',
        };
    }

    const { meters, score } = calculateDistanceAndScore(correctCoord, guessPosition);
    const isCorrect = score === 50;
    return {
        meters,
        score,
        isCorrect,
        message: isCorrect ? '正确！' : '未命中正确位置',
    };
}

function getTileLayer(isSolo) {
    if (!isSolo) {
        return {
            url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
            subdomains: undefined,
            maxZoom: 18,
        };
    }

    // 练习模式：使用高德路网图（style=8 更快），缓存友好
    return {
        url: 'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
        attribution: '&copy; <a href="https://www.amap.com/">高德地图</a>',
        subdomains: ['1', '2', '3', '4'],
        maxZoom: 18,
    };
}

function renderTopBar({ isSolo, onBack, currentIndex, total }) {
    if (!isSolo) {
        return (
            <div className="absolute top-0 left-0 right-0 z-50 flex justify-between p-4">
                <div className="flex items-center gap-3 glass-dark px-4 py-2 rounded-full">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center font-bold">
                        用
                    </div>
                    <div className="bg-gradient-to-r from-green-400 to-green-600 h-6 rounded-full px-3 text-sm font-bold flex items-center">
                        6000
                    </div>
                </div>

                <div className="glass-dark px-6 py-2 rounded-full flex items-center gap-2">
                    <span className="text-sm text-gray-400">倒计时</span>
                    <span className="text-xl font-bold">60</span>
                </div>

                <div className="flex items-center gap-3 glass-dark px-4 py-2 rounded-full">
                    <div className="bg-gradient-to-r from-green-400 to-green-600 h-6 rounded-full px-3 text-sm font-bold flex items-center">
                        6000
                    </div>
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center font-bold">
                        L
                    </div>
                </div>
            </div>
        );
    }

    const progress = total > 0 ? ` · ${Math.min(currentIndex + 1, total)}/${total}` : '';
    return (
        <div className="absolute top-0 left-0 right-0 z-50 p-4 flex items-center justify-between">
            <button
                type="button"
                onClick={onBack}
                className="p-3 rounded-full glass-dark hover:bg-white/10 transition-all group"
                aria-label="退出练习"
            >
                <ChevronLeft className="w-6 h-6 text-gray-400 group-hover:text-white transition-colors" />
            </button>

            <div className="glass-dark px-4 py-2 rounded-full text-sm text-gray-200">
                练习模式{progress}
            </div>

            <div className="w-12" />
        </div>
    );
}

function renderSoloInfoPanel({ isSolo, detail, loading, error }) {
    if (!isSolo) return null;
    return (
        <div className="absolute top-20 left-6 z-40 max-w-[520px]">
            <div className="glass-dark px-5 py-4 rounded-2xl border border-white/10">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <div className="text-white font-semibold">
                            {detail?.title || (detail?.id ? `题目 #${detail.id}` : '题目')}
                        </div>
                        <div className="text-gray-400 text-sm">
                            {detail?.campus ? `${detail.campus} · ` : ''}{detail?.difficulty || ''}
                        </div>
                    </div>
                    {detail?.id ? <div className="text-gray-400 text-sm">#{detail.id}</div> : null}
                </div>

                {loading ? <div className="text-gray-500 text-sm mt-2">加载中...</div> : null}
                {error ? <div className="text-red-400 text-sm mt-2">{error}</div> : null}
            </div>
        </div>
    );
}

function renderImageLayer({ isSolo, loading, imageUrl, error, title, imageScale, onWheel }) {
    if (!isSolo) {
        return (
            <img
                src={imageUrl}
                alt={title || 'Game location'}
                className="w-full h-full object-cover"
            />
        );
    }

    let content;
    if (loading) {
        content = <div className="w-full h-full bg-black" />;
    } else if (imageUrl) {
        content = (
            <img
                src={imageUrl}
                alt={title || 'Game location'}
                className="w-full h-full object-contain select-none"
                style={{
                    transform: imageScale === 1 ? undefined : `scale(${imageScale})`,
                    transformOrigin: 'center center',
                    transition: 'transform 80ms linear',
                }}
                draggable={false}
            />
        );
    } else {
        content = (
            <div className="w-full h-full bg-black flex items-center justify-center">
                <div className="glass-dark px-5 py-4 rounded-2xl border border-white/10 text-gray-400 text-sm">
                    {error ? '图片加载失败' : '暂无图片'}
                </div>
            </div>
        );
    }

    return (
        <div className="absolute inset-0 overflow-hidden bg-black" onWheel={onWheel}>
            {content}
        </div>
    );
}

function renderMapOverlay({
    mapCenter,
    mapZoom,
    tileLayer,
    mapClickHandler,
    showResult,
    guessPosition,
    correctLatLng,
    onSubmit,
    canSubmit,
}) {
    const mapKey = `${mapCenter[0]}-${mapCenter[1]}`;
    return (
        <AnimatePresence>
            <motion.div
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className="absolute bottom-6 right-6 w-[400px] h-[400px] rounded-2xl overflow-hidden shadow-2xl border-2 border-white/20"
            >
                <MapContainer
                    key={mapKey}
                    center={mapCenter}
                    zoom={mapZoom}
                    style={{ height: '100%', width: '100%' }}
                    zoomControl={false}
                    maxZoom={tileLayer.maxZoom}
                    minZoom={12}
                >
                    <TileLayer
                        attribution={tileLayer.attribution}
                        url={tileLayer.url}
                        subdomains={tileLayer.subdomains}
                        maxZoom={tileLayer.maxZoom}
                    />
                    {mapClickHandler}

                    {guessPosition ? <Marker position={guessPosition} /> : null}
                    {showResult && correctLatLng ? <Marker position={correctLatLng} icon={correctMarkerIcon} /> : null}
                </MapContainer>

                <div className="absolute top-4 right-4 flex flex-col gap-2 z-[1000]">
                    <button className="p-2 glass-dark rounded-lg hover:bg-white/20 transition-all" type="button">
                        <ZoomIn size={20} />
                    </button>
                    <button className="p-2 glass-dark rounded-lg hover:bg-white/20 transition-all" type="button">
                        <ZoomOut size={20} />
                    </button>
                    <button className="p-2 glass-dark rounded-lg hover:bg-white/20 transition-all" type="button">
                        <Crosshair size={20} />
                    </button>
                </div>

                <div className="absolute bottom-4 left-4 right-4 z-[1000]">
                    <button
                        onClick={onSubmit}
                        disabled={!canSubmit}
                        className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all disabled:opacity-50 disabled:hover:shadow-none"
                        type="button"
                    >
                        确认位置
                    </button>
                </div>
            </motion.div>
        </AnimatePresence>
    );
}

// -----------------------------------------------------------------------------
// 修改点：重构结果弹窗，实现“继续下一题”->“结束”的逻辑，并移除“返回”按钮
// -----------------------------------------------------------------------------
function renderResultOverlay({ isSolo, showResult, result, hasNext, onNext, onFinish }) {
    if (!isSolo || !showResult) return null;
    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 10 }}
                className="absolute inset-0 z-40 flex items-center justify-center px-6"
            >
                <div className="glass-dark rounded-3xl p-6 w-full max-w-lg border border-white/10">
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <div className={`text-xl font-bold ${result?.isCorrect ? 'text-green-400' : 'text-white'}`}>
                                {result?.message || '结果'}
                            </div>
                            <div className="text-gray-400 text-sm mt-1">
                                距离：{formatMeters(result?.meters)} · 得分：{result?.score ?? 0}
                            </div>
                        </div>
                        <div className="text-gray-400 text-sm">
                            {hasNext ? '下一题已准备' : '已是最后一题'}
                        </div>
                    </div>

                    <div className="text-gray-500 text-xs mt-3">
                        提示：地图已标出正确位置（橙色点）。
                    </div>

                    <div className="flex gap-3 mt-5">
                        {/* 如果有下一题，调用onNext；如果没有，调用onFinish进行上传和退出 */}
                        <button
                            type="button"
                            onClick={hasNext ? onNext : onFinish}
                            className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all"
                        >
                            {hasNext 
                                ? (result?.isCorrect ? '立即进入下一题' : '继续下一题') 
                                : '结束并上传成绩'}
                        </button>
                    </div>
                </div>
            </motion.div>
        </AnimatePresence>
    );
}

function renderBottomInfo({ isSolo }) {
    if (isSolo) return null;
    return (
        <div className="absolute bottom-6 left-6 glass-dark px-4 py-2 rounded-full text-sm">
            <span className="text-gray-400">回合:</span> <span className="font-bold ml-2">1/5</span>
        </div>
    );
}

const correctMarkerIcon = L.divIcon({
    className: 'campusguess-correct-marker',
    html: '<div class="w-4 h-4 rounded-full bg-orange-500 border-2 border-white shadow-lg"></div>',
    iconSize: [16, 16],
    iconAnchor: [8, 8],
});

const Game = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const isSolo = location.state?.mode === 'solo';
    const initialQuestionId = location.state?.questionId ?? null;
    const initialIds = location.state?.questionIds ?? null;
    const initialIndex = location.state?.startIndex ?? 0;

    const [guessPosition, setGuessPosition] = useState(null);
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState(null);
    const [showResult, setShowResult] = useState(false);
    const [questionIds, setQuestionIds] = useState(Array.isArray(initialIds) ? initialIds : []);
    const [currentIndex, setCurrentIndex] = useState(Number.isFinite(initialIndex) ? initialIndex : 0);

    const [imageScale, setImageScale] = useState(1);
    const [sessionRecords, setSessionRecords] = useState([]);

    const autoNextTimerRef = useRef(null);

    const correctCoord = useMemo(() => {
        const lat = detail?.correctCoord?.lat;
        const lon = detail?.correctCoord?.lon;
        if (lat == null || lon == null) return null;
        return { lat, lon };
    }, [detail]);

    const correctLatLng = useMemo(() => {
        if (!correctCoord) return null;
        return { lat: correctCoord.lat, lng: correctCoord.lon };
    }, [correctCoord]);

    const canSubmit = Boolean(guessPosition && correctLatLng && !showResult);

    const imageUrl = isSolo ? (detail?.imageData?.links?.url ?? '') : (
        detail?.imageData?.links?.url
            ?? 'https://images.unsplash.com/photo-1562774053-701939374585?w=1920&h=1080&fit=crop'
    );

    const mapCenter = useMemo(() => {
        if (isSolo && correctLatLng) return [correctLatLng.lat, correctLatLng.lng];
        // Default center: SYSU Zhuhai Campus approximate location (fallback)
        return [22.255, 113.541];
    }, [correctLatLng, isSolo]);

    const mapZoom = useMemo(() => {
        if (isSolo && correctLatLng) return 16;
        return 15;
    }, [correctLatLng, isSolo]);

    const tileLayer = useMemo(() => getTileLayer(isSolo), [isSolo]);

    const handleImageWheel = (e) => {
        // 仅用于练习模式查看图片细节
        if (!isSolo) return;
        e.preventDefault();
        const next = imageScale + (e.deltaY < 0 ? 0.12 : -0.12);
        const clamped = Math.min(3, Math.max(1, next));
        setImageScale(clamped);
    };

    const goBackToQuestions = () => {
        navigate(-1);
    };

    const hasNext = isSolo && questionIds.length > 0 && currentIndex < questionIds.length - 1;

    const loadDetail = async (questionId) => {
        const userInfo = getCurrentUserInfo();
        if (!userInfo?.username) {
            navigate('/login');
            return;
        }

        try {
            setLoading(true);
            setError('');
            const resp = await getQuestionDetail(questionId);
            setDetail(resp);
            setGuessPosition(null);
            setResult(null);
            setShowResult(false);
            setImageScale(1);
        } catch (e) {
            setError(e?.message || '题目加载失败');
        } finally {
            setLoading(false);
        }
    };

    const goNext = async () => {
        if (!hasNext) return;
        const nextIndex = currentIndex + 1;
        setCurrentIndex(nextIndex);
        await loadDetail(questionIds[nextIndex]);
    };

    const handleFinishGame = async () => {
        const userInfo = getCurrentUserInfo();
        if (!userInfo?.userId) {
            navigate('/login');
            return;
        }

        try {
            // 构建请求体
            const payload = {
                userId: userInfo.userId,
                gameType: '独自变强', // 对应后端枚举或字符串
                questionRecords: sessionRecords
            };

            // 调用API上传
            await submitGameRecord(payload);
            
            // 上传成功后离开
            navigate(-1);
        } catch (err) {
            console.error("Failed to submit game record:", err);
            // 即使上传失败，也应当允许用户离开，或者提示重试
            alert("成绩上传失败，请检查网络");
            navigate(-1);
        }
    };

    useEffect(() => {
        if (!isSolo) return;

        if (!initialQuestionId && (!initialIds || initialIds.length === 0)) {
            navigate('/solo');
            return;
        }

        // 兼容单题进入和列表进入的情况
        if (!Array.isArray(initialIds) || initialIds.length === 0) {
            if (initialQuestionId) {
                setQuestionIds([initialQuestionId]);
                setCurrentIndex(0);
                loadDetail(initialQuestionId);
            }
        } else {
            // 如果传入了列表，加载当前index的题目
            loadDetail(initialIds[currentIndex]);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isSolo, navigate]); // removed initialIds dependency to avoid loop if it changes ref

    useEffect(() => {
        if (!result?.isCorrect) return undefined;
        if (!hasNext) return undefined;

        if (autoNextTimerRef.current) clearTimeout(autoNextTimerRef.current);
        autoNextTimerRef.current = setTimeout(() => {
            goNext();
        }, 1200);

        return () => {
            if (autoNextTimerRef.current) clearTimeout(autoNextTimerRef.current);
            autoNextTimerRef.current = null;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [result?.isCorrect, hasNext]);

    const handleSubmit = () => {
        const submitResult = buildSubmitResult({ guessPosition, correctCoord });
        setResult(submitResult);
        setShowResult(true);

        // 新增：记录本题答案
        // 注意：Leaflet是 lat,lng; 后端要求 userCoord: { lon, lat }
        if (detail?.id && guessPosition) {
            const currentRecord = {
                questionId: detail.id,
                userCoord: {
                    lon: guessPosition.lng,
                    lat: guessPosition.lat
                }
            };
            setSessionRecords(prev => [...prev, currentRecord]);
        }
    };

    function MapClickHandlerInner() {
        useMapEvents({
            click(e) {
                if (!showResult) setGuessPosition(e.latlng);
            },
        });
        return null;
    }

    return (
        <div className="fixed inset-0 bg-black">
            {renderTopBar({ isSolo, onBack: goBackToQuestions, currentIndex, total: questionIds.length })}

            {/* Main Game Area - Image */}
            <div className="w-full h-full relative">
                {renderImageLayer({
                    isSolo,
                    loading,
                    imageUrl,
                    error,
                    title: detail?.title,
                    imageScale,
                    onWheel: handleImageWheel,
                })}

                {renderSoloInfoPanel({ isSolo, detail, loading, error })}

                {/* Map Overlay - Bottom Right */}
                {renderMapOverlay({
                    mapCenter,
                    mapZoom,
                    tileLayer,
                    mapClickHandler: <MapClickHandlerInner />,
                    showResult,
                    guessPosition,
                    correctLatLng,
                    onSubmit: handleSubmit,
                    canSubmit,
                })}

                {/* Result Overlay */}
                {renderResultOverlay({
                    isSolo,
                    showResult,
                    result,
                    hasNext,
                    onBack: goBackToQuestions,
                    onNext: goNext,
                    onFinish: handleFinishGame,
                })}
            </div>

            {renderBottomInfo({ isSolo })}
        </div>
    );
};

export default Game;