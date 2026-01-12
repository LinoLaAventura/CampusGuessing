import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
    ChevronLeft, List, PlusCircle, FileText, Play, MapPin, X, Upload, 
    Image as ImageIcon, Trash2, 
    MessageSquare, Send, Clock, ThumbsUp, User, Heart
} from 'lucide-react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import { motion, AnimatePresence } from 'framer-motion';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { getCurrentUserInfo } from '../api/authStorage';
import { createQuestion, getQuestionDetail, getQuestionList } from '../api/questionApi';
import { uploadImage } from '../api/imageUpload';
import { getCommentList, deleteComment, createComment, likeComment, unlikeComment } from '../api/commentApi'; 
import { getUserInfo } from '../api/userApi';

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

// Leaflet marker icon fix
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});



// Fisher-Yates 洗牌算法
function shuffleArray(array) {
    const arr = [...array];
    for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
}


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

    // 使用用户指定的地图中心坐标
    const mapCenter = [22.3477, 113.5894];

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80"
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

// function toNumberOrUndefined(val) {
//     const trimmed = String(val ?? '').trim();
//     if (!trimmed) return undefined;
//     const num = Number(trimmed);
//     return Number.isFinite(num) ? num : undefined;
// }

function cleanErrorMessage(errorMsg) {
    if (!errorMsg) return '操作失败';
    
    // 截断过长的错误消息
    let msg = String(errorMsg);
    if (msg.length > 200) {
        msg = msg.substring(0, 200) + '...';
    }
    
    // 提取关键错误信息
    if (msg.includes('参数验证失败')) {
        // 尝试从Map中提取第一个错误
        const regex = /=([^,}]+)/;
        const match = regex.exec(msg);
        if (match?.[1]) {
            return match[1].trim();
        }
    }
    
    // 处理SQL约束错误
    if (msg.includes('constraint') && msg.includes('content')) {
        return '题目描述不能为空';
    }
    
    return msg;
}

function getAuthedUserOrRedirect(navigate) {
    const userInfo = getCurrentUserInfo();
    if (!userInfo?.username) {
        navigate('/login');
        return null;
    }
    return userInfo;
}

function filterQuestions(list, campus, difficulty) {
    return (list || []).filter((q) => {
        if (campus && q.campus !== campus) return false;
        if (difficulty && q.difficulty !== difficulty) return false;
        return true;
    });
}

async function loadQuestionList({
    navigate,
    campus,
    difficulty,
    setListLoading,
    setListError,
    setTotal,
    setList,
}) {
    const userInfo = getAuthedUserOrRedirect(navigate);
    if (!userInfo) return;

    try {
        setListLoading(true);
        setListError('');
        const resp = await getQuestionList({ page: 1, size: 50, campus, difficulty });
        setTotal(resp?.total || 0);
        setList(resp?.list || []);
    } catch (e) {
        setListError(cleanErrorMessage('题目列表加载失败'));
    } finally {
        setListLoading(false);
    }
}

async function loadQuestionDetail({
    navigate,
    questionId,
    setDetailLoading,
    setDetailError,
    setDetail,
}) {
    const userInfo = getAuthedUserOrRedirect(navigate);
    if (!userInfo) return;

    try {
        setDetailLoading(true);
        setDetailError('');
        const resp = await getQuestionDetail(questionId);
        setDetail(resp);
    } catch (e) {
        setDetailError(cleanErrorMessage('题目详情加载失败'));
    } finally {
        setDetailLoading(false);
    }
}

async function submitCreateQuestion({
    navigate,
    username,
    createCampus,
    createDifficulty,
    createCoord,
    uploadedImageKey,
    createTitle,
    createContent,
    createAnswer,
    setCreateLoading,
    setActionMsg,
    setActionErr,
    onCreated,
    clearForm,
}) {
    const userInfo = getAuthedUserOrRedirect(navigate);
    if (!userInfo) return;

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

        const resp = await createQuestion(username, {
            campus: createCampus,
            difficulty: createDifficulty,
            key: uploadedImageKey,
            correctCoord: { lon: createCoord.lon, lat: createCoord.lat },
            title: createTitle.trim() || '未命名题目',
            content: createContent.trim() || '无描述',
            answer: createAnswer.trim() || '无答案',
        });

        setActionMsg(`题目创建成功（ID: ${resp?.id ?? '-'}）`);
        clearForm();
        await onCreated();
    } catch (e) {
        setActionErr(cleanErrorMessage('题目创建失败'));
    } finally {
        setCreateLoading(false);
    }
}


function renderQuestionListPanel({
    campus,
    difficulty,
    onChangeCampus,
    onChangeDifficulty,
    total,
    filteredCount,
    listLoading,
    listError,
    filteredList,
    selectedId,
    onSelect,
    onDelete,
    onStartRandomPractice, 
}) {
    let listContent;
    if (listLoading) listContent = <div className="text-gray-500">加载中...</div>;
    else if (listError) listContent = <div className="text-red-400">{listError}</div>;
    else if (filteredList.length === 0) listContent = <div className="text-gray-500">暂无题目</div>;
    else {
        listContent = (
            <div className="space-y-3">
                {filteredList.map((q) => (
                    <div
                        key={q.id}
                        className={`w-full flex justify-between items-center pr-2 rounded-xl transition-all border ${
                            selectedId === q.id
                                ? 'bg-white/10 border-white/20'
                                : 'bg-white/5 border-white/10 hover:bg-white/10 hover:border-white/20'
                        }`}
                    >
                        <button
                            type="button"
                            onClick={() => onSelect(q)}
                            className="flex-1 text-left py-3 px-4 outline-none"
                        >
                            <div className="flex flex-col">
                                <span className="text-white font-medium">{q.title || `题目 #${q.id}`}</span>
                                <span className="text-gray-500 text-xs">{q.campus} · {q.difficulty}</span>
                            </div>
                        </button>

                        <div className="flex items-center gap-2 pl-2 border-l border-white/5">
                            <span className="text-gray-400 text-sm mr-2">#{q.id}</span>
                            <button
                                type="button"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onDelete(q.id);
                                }}
                                className="p-2 rounded-lg text-gray-400 hover:text-red-400 hover:bg-red-500/10 transition-all"
                                title="删除题目"
                            >
                                <Trash2 size={16} />
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    return (
        <div className="glass-dark rounded-3xl p-6 lg:col-span-1 flex flex-col h-full">
            <div className="flex items-center gap-3 mb-4">
                <List className="w-5 h-5 text-gray-400" />
                <h2 className="text-xl font-bold">题目列表</h2>
            </div>

            {/* 新增：随机开始按钮 */}
            <button
                onClick={onStartRandomPractice}
                className="w-full py-3 mb-4 bg-gradient-to-r from-orange-500 to-orange-600 hover:shadow-orange-500/30 text-white font-bold rounded-xl shadow-lg transition-all flex items-center justify-center gap-2"
            >
                <Play size={20} fill="currentColor" />
                开始练习 (随机5题)
            </button>

            <div className="grid grid-cols-2 gap-3 mb-4">
               <select value={campus} onChange={(e) => onChangeCampus(e.target.value)} className="bg-white/5 border border-white/10 rounded-xl px-3 py-3 text-white focus:outline-none focus:border-apple-orange transition-all">
                    {CAMPUS_OPTIONS.map((o) => <option key={o.value} value={o.value} className="bg-gray-900">{o.label}</option>)}
               </select>
               <select value={difficulty} onChange={(e) => onChangeDifficulty(e.target.value)} className="bg-white/5 border border-white/10 rounded-xl px-3 py-3 text-white focus:outline-none focus:border-apple-orange transition-all">
                    {DIFFICULTY_OPTIONS.map((o) => <option key={o.value} value={o.value} className="bg-gray-900">{o.label}</option>)}
               </select>
            </div>

            <div className="text-gray-500 text-sm mb-4">显示 {filteredCount} / {total}</div>

            {/* 列表内容区域 */}
            <div className="flex-1 overflow-y-auto custom-scrollbar">
                {listContent}
            </div>
        </div>
    );
}


function renderQuestionDetailPanel({
    selectedId,
    detailLoading,
    detailError,
    detail,
    commentContent,
    setCommentContent,
    onSubmitComment,
    isSubmittingComment,
    comments,            // 评论列表数据
    commentsLoading,     // 评论加载状态
    currentUsername,     // 当前登录用户名(用于判断是否是自己的评论)
    onDeleteComment,
    onToggleLike,
}) {
    let detailContent;
    if (!selectedId) detailContent = <div className="text-gray-500">请选择一道题目查看详情</div>;
    else if (detailLoading) detailContent = <div className="text-gray-500">加载中...</div>;
    else if (detailError) detailContent = <div className="text-red-400">{detailError}</div>;
    else if (detail === null) detailContent = <div className="text-gray-500">暂无详情</div>;
    else {
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
        const imageUrl = detail?.imageData?.links?.url;
        const coordText = detail?.correctCoord
            ? `${detail.correctCoord.lat ?? '-'}, ${detail.correctCoord.lon ?? '-'}`
            : '-';

        detailContent = (
            <div className="space-y-4">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h3 className="text-white font-semibold text-lg">{detail.title || `题目 #${detail.id}`}</h3>
                        <p className="text-gray-500 text-sm">{detail.campus} · {detail.difficulty}</p>
                    </div>
                    <span className="text-gray-400 text-sm">#{detail.id}</span>
                </div>

                {imageUrl ? (
                    <img
                        src={imageUrl}
                        alt={detail.title || `question-${detail.id}`}
                        className="w-full rounded-2xl border border-white/10 max-h-[400px] object-contain bg-black/20"
                    />
                ) : null}

                <div className="grid gap-2 text-sm">
                    <div className="text-gray-400"><span className="text-gray-500">作者：</span>{detail.authorUsername ?? '-'}</div>
                    {/* <div className="text-gray-400"><span className="text-gray-500">坐标：</span>{coordText}</div> */}
                    <div className="text-gray-400"><span className="text-gray-500">创建时间：</span>{formatTime(detail.createdAt) ?? '-'}</div>
                </div>

                {detail.content ? (
                    <div className="bg-white/5 border border-white/10 rounded-2xl p-4">
                        <div className="text-gray-400 text-xs mb-2">题目内容</div>
                        <div className="text-white whitespace-pre-wrap">{detail.content}</div>
                    </div>
                ) : null}

                {detail.answer ? (
                    <div className="bg-white/5 border border-white/10 rounded-2xl p-4">
                        <div className="text-gray-400 text-xs mb-2">提示</div>
                        <div className="text-white whitespace-pre-wrap">{detail.answer}</div>
                    </div>
                ) : null}

                <div className="pt-6 mt-6 border-t border-white/10">
                                        <div className="flex items-center gap-2 mb-4">
                        <MessageSquare className="w-4 h-4 text-orange-400" />
                        <h3 className="text-white font-semibold">
                            评论区 ({comments ? comments.length : 0})
                        </h3>
                    </div>

                    <div className="space-y-3 mb-6">
                        {commentsLoading ? (
                            <div className="text-gray-500 text-sm py-2">加载评论中...</div>
                        ) : (!comments || comments.length === 0) ? (
                            <div className="text-gray-500 text-sm py-2 italic">暂无评论，快来抢沙发吧~</div>
                        ) : (
                            comments.map((comment) => (
                                <div key={comment.commentId} className="bg-white/5 border border-white/10 rounded-xl p-3">
                                    {/* 评论头部：头像/用户名/ID/时间/删除按钮 */}
                                    <div className="flex justify-between items-start mb-2">
                                        <div className="flex items-center gap-2">
                                            <div className="w-6 h-6 rounded-full bg-gradient-to-tr from-orange-400 to-pink-500 flex items-center justify-center">
                                                <User className="w-3 h-3 text-white" />
                                            </div>
                                            <div className="flex flex-col">
                                                <span className="text-gray-200 text-xs font-bold">
                                                    {comment.username}
                                                </span>
                                                <span className="text-gray-600 text-[10px]">
                                                    ID: {comment.userId}
                                                </span>
                                            </div>
                                        </div>
                                        
                                        {/* 如果当前登录用户名 == 评论用户名，显示删除按钮 */}
                                        {currentUsername && currentUsername === comment.username && (
                                            <button
                                                onClick={() => onDeleteComment(comment.commentId)}
                                                className="text-gray-600 hover:text-red-400 transition-colors p-1"
                                                title="删除我的评论"
                                            >
                                                <Trash2 className="w-3 h-3" />
                                            </button>
                                        )}
                                    </div>

                                    {/* 评论内容 */}
                                    <div className="text-gray-300 text-sm whitespace-pre-wrap ml-8 mb-2">
                                        {comment.content}
                                    </div>

                                    {/* 底部信息：时间和点赞 */}
                                    <div className="flex items-center justify-between ml-8 text-xs text-gray-500">
                                        <div className="flex items-center gap-1">
                                            <Clock className="w-3 h-3" />
                                            {/* 简单格式化时间，去掉T和毫秒 */}
                                            <span>
                                                {comment.createTime ? comment.createTime.replace('T', ' ').split('.')[0] : '-'}
                                            </span>
                                        </div>
                                        <button
                                            onClick={() => onToggleLike(comment)}
                                            className="flex items-center gap-1 group hover:text-red-400 transition-colors"
                                        >
                                            <Heart 
                                                className={`w-4 h-4 transition-all ${
                                                    // 判断是否已点赞：如果是 true，显示红色填充；否则显示空心
                                                    comment.isLiked 
                                                        ? "fill-red-500 text-red-500 scale-110" 
                                                        : "text-gray-400 group-hover:text-red-400 group-hover:scale-110"
                                                }`} 
                                            />
                                            <span className={comment.isLiked ? "text-red-400" : ""}>
                                                {comment.likeCount || 0}
                                            </span>
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                    <div className="flex items-center gap-2 mb-3">
                        <MessageSquare className="w-4 h-4 text-orange-400" />
                        <h3 className="text-white font-semibold">添加评论</h3>
                    </div>
                    
                    <div className="relative">
                        <textarea
                            value={commentContent}
                            onChange={(e) => setCommentContent(e.target.value)}
                            placeholder="写下你的看法..."
                            disabled={isSubmittingComment}
                            className="w-full bg-white/5 border border-white/10 rounded-xl p-4 pr-14 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all resize-none h-24 custom-scrollbar"
                        />
                        <button
                            onClick={onSubmitComment}
                            disabled={isSubmittingComment || !commentContent.trim()}
                            className="absolute bottom-3 right-3 p-2 bg-orange-500 hover:bg-orange-600 text-white rounded-lg transition-all disabled:opacity-50 disabled:bg-gray-700 flex items-center justify-center"
                            title="发送"
                        >
                            {isSubmittingComment ? (
                                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                            ) : (
                                <Send className="w-4 h-4" />
                            )}
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="glass-dark rounded-3xl p-6 lg:col-span-2">
            <div className="flex items-center gap-3 mb-4">
                <FileText className="w-5 h-5 text-gray-400" />
                <h2 className="text-xl font-bold">题目详情</h2>
            </div>
            {detailContent}
        </div>
    );
}



function renderCreateQuestionPanel({
    createCampus,
    setCreateCampus,
    createDifficulty,
    setCreateDifficulty,
    createCoord,
    onOpenMapPicker,
    uploadedImageKey,
    uploadedImageUrl,
    onImageUpload,
    uploadingImage,
    createTitle,
    setCreateTitle,
    createContent,
    setCreateContent,
    createAnswer,
    setCreateAnswer,
    createLoading,
    onCreate,
}) {
    // return (
    //     <div className="glass-dark rounded-3xl p-6 lg:col-span-3">
    //         <div className="flex items-center gap-3 mb-4">
    //             <PlusCircle className="w-5 h-5 text-gray-400" />
    //             <h2 className="text-xl font-bold">创建题目</h2>
    //         </div>

    //         <div className="grid md:grid-cols-2 gap-3 mb-3">
    //             <select
    //                 value={createCampus}
    //                 onChange={(e) => setCreateCampus(e.target.value)}
    //                 className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-apple-orange transition-all"
    //             >
    //                 {CAMPUS_OPTIONS.filter((o) => o.value !== '').map((o) => (
    //                     <option key={o.value} value={o.value} className="bg-gray-900">
    //                         {o.label}
    //                     </option>
    //                 ))}
    //             </select>

    //             <select
    //                 value={createDifficulty}
    //                 onChange={(e) => setCreateDifficulty(e.target.value)}
    //                 className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-apple-orange transition-all"
    //             >
    //                 {DIFFICULTY_OPTIONS.filter((o) => o.value !== '').map((o) => (
    //                     <option key={o.value} value={o.value} className="bg-gray-900">
    //                         {o.label}
    //                     </option>
    //                 ))}
    //             </select>
    //         </div>

    //         <div className="mb-3">
    //             <button
    //                 type="button"
    //                 onClick={onOpenMapPicker}
    //                 className="w-full py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all text-white flex items-center justify-center gap-2"
    //             >
    //                 <MapPin className="w-4 h-4" />
    //                 {createCoord ? `已选位置：${createCoord.lat.toFixed(4)}, ${createCoord.lon.toFixed(4)}` : '在地图上选择正确位置（必填）'}
    //             </button>
    //         </div>

    //         <div className="mb-3">
    //             <label className="w-full block">
    //                 <input
    //                     type="file"
    //                     accept="image/*"
    //                     onChange={onImageUpload}
    //                     disabled={uploadingImage}
    //                     className="hidden"
    //                 />
    //                 <div className="w-full py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all text-white flex items-center justify-center gap-2 cursor-pointer">
    //                     {uploadingImage && (
    //                         <>
    //                             <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
    //                             上传中...
    //                         </>
    //                     )}
    //                     {!uploadingImage && uploadedImageKey && (
    //                         <>
    //                             <ImageIcon className="w-4 h-4 text-green-400" />
    //                             已上传图片
    //                         </>
    //                     )}
    //                     {!uploadingImage && !uploadedImageKey && (
    //                         <>
    //                             <Upload className="w-4 h-4" />
    //                             上传题目图片（必填）
    //                         </>
    //                     )}
    //                 </div>
    //             </label>
    //             {uploadedImageKey && (
    //                 <div className="mt-2">
    //                     <div className="text-xs text-gray-400 mb-2 truncate">图片Key: {uploadedImageKey}</div>
    //                     {uploadedImageUrl && (
    //                         <div className="w-full h-48 rounded-xl overflow-hidden border border-white/10 bg-black">
    //                             <img 
    //                                 src={uploadedImageUrl}
    //                                 alt="题目图片预览" 
    //                                 className="w-full h-full object-contain"
    //                                 onError={(e) => {
    //                                     console.error('图片加载失败:', uploadedImageUrl);
    //                                     e.target.style.display = 'none';
    //                                 }}
    //                             />
    //                         </div>
    //                     )}
    //                 </div>
    //             )}
    //         </div>

    //         <div className="grid gap-3 mb-3">
    //             <input
    //                 type="text"
    //                 value={createTitle}
    //                 onChange={(e) => setCreateTitle(e.target.value)}
    //                 placeholder="标题"
    //                 className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all"
    //             />
    //         </div>

    //         <div className="grid gap-3 mb-3">
    //             <textarea
    //                 value={createContent}
    //                 onChange={(e) => setCreateContent(e.target.value)}
    //                 placeholder="题目内容（可选）"
    //                 rows={3}
    //                 className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all resize-none"
    //             />
    //         </div>

    //         <div className="grid gap-3 mb-4">
    //             <input
    //                 type="text"
    //                 value={createAnswer}
    //                 onChange={(e) => setCreateAnswer(e.target.value)}
    //                 placeholder="答案（可选）"
    //                 className="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-600 focus:outline-none focus:border-apple-orange transition-all"
    //             />
    //         </div>

    //         <button
    //             type="button"
    //             onClick={onCreate}
    //             disabled={createLoading}
    //             className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all disabled:opacity-50 disabled:hover:shadow-none"
    //         >
    //             {createLoading ? '创建中...' : '创建题目'}
    //         </button>

    //         <div className="text-gray-500 text-xs mt-3">
    //             提示：地图选点和图片上传为必填项；图片将上传到 PICUI 图床。请先在代码中配置 Bearer Token。
    //         </div>
    //     </div>
    // );
}

const SoloMode = () => {
    const navigate = useNavigate();

    const [campus, setCampus] = useState('');
    const [difficulty, setDifficulty] = useState('');

    const [listLoading, setListLoading] = useState(true);
    const [listError, setListError] = useState('');
    const [total, setTotal] = useState(0);
    const [list, setList] = useState([]);

    const [selectedId, setSelectedId] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');
    const [detail, setDetail] = useState(null);

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

    const [actionMsg, setActionMsg] = useState('');
    const [actionErr, setActionErr] = useState('');

    const [commentContent, setCommentContent] = useState('');
    const [isSubmittingComment, setIsSubmittingComment] = useState(false);

    const [comments, setComments] = useState([]);
    const [commentsLoading, setCommentsLoading] = useState(false);
    
    const currentUserInfo = getCurrentUserInfo();
    const currentUsername = currentUserInfo?.username;

    const filteredList = useMemo(() => filterQuestions(list, campus, difficulty), [campus, difficulty, list]);

    // const canStartPractice = Boolean(
    //     detail?.id && detail?.correctCoord?.lat != null && detail?.correctCoord?.lon != null,
    // );

    const loadList = () => loadQuestionList({
        navigate,
        campus,
        difficulty,
        setListLoading,
        setListError,
        setTotal,
        setList,
    });

    const loadDetail = (questionId) => loadQuestionDetail({
        navigate,
        questionId,
        setDetailLoading,
        setDetailError,
        setDetail,
    });

    useEffect(() => {
        loadList();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [navigate]);

    const handleRefresh = async () => {
        setActionMsg('');
        setActionErr('');
        await loadList();
    };

    const handleSelect = async (q) => {
        setSelectedId(q.id);
        loadDetail(q.id);
        loadComments(q.id);
    };

    const handleStartRandomPractice = () => {
        console.log("点击了开始练习");
        if (!filteredList || filteredList.length === 0) {
            console.log("失败：列表为空");
            setActionErr('当前列表中没有题目，请先创建或修改筛选条件');
            return;
        }

        // 1. 过滤掉坐标不全的题目（防止练习报错）
        const validQuestions = filteredList.filter(
            q => q.correctCoord && q.correctCoord.lat != null && q.correctCoord.lon != null
        );

        console.log("有效题目数量:", validQuestions.length);

        if (validQuestions.length === 0) {
            setActionErr('当前列表中的题目均缺少正确坐标，无法进行练习');
            return;
        }

        // 2. 随机打乱并取前5个
        const randomQuestions = shuffleArray(validQuestions).slice(0, 5);
        const ids = randomQuestions.map(q => q.id);

        console.log("即将跳转题目ID:", ids);

        // 3. 跳转到 Game 页面
        navigate('/game', {
            state: {
                mode: 'solo',
                questionIds: ids,
                startIndex: 0,
            },
        });
    };

    const handleImageUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        console.log('开始上传图片:', file.name, '大小:', (file.size / 1024 / 1024).toFixed(2), 'MB');

        try {
            setUploadingImage(true);
            setActionErr('');
            setActionMsg('');
            const result = await uploadImage(file);
            console.log('图片上传成功:', result);
            setUploadedImageKey(result.key);
            setUploadedImageUrl(result.url);
            setActionMsg('图片上传成功！');
        } catch (error) {
            console.error('图片上传失败:', error);
            setActionErr('图片上传失败');
            setUploadedImageKey(''); // 失败时清空Key
            setUploadedImageUrl(''); // 失败时清空URL
        } finally {
            setUploadingImage(false);
        }
    };

    const handleDelete = async (questionId) => {
        if (!window.confirm('确定要删除这道题目吗？此操作无法撤销。')) {
            return;
        }

        const userInfo = getCurrentUserInfo();
        if (!userInfo?.username) {
            navigate('/login');
            return;
        }

        try {
            // 根据你提供的图片，API 路径是 /users/{userName}/questions/{questionId}
            // 这里假设你的后端运行在 localhost:8080，如果生产环境不同请修改 base URL
            const response = await fetch(`http://localhost:8080/api/users/${userInfo.username}/questions/${questionId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    // 如果你的后端需要 Token，请在这里添加，例如:
                    // 'Authorization': `Bearer ${localStorage.getItem('token')}` 
                }
            });

            if (response.ok) {
                setActionMsg(`题目 #${questionId} 删除成功`);
                
                // 如果删除的是当前选中的题目，清空详情
                if (selectedId === questionId) {
                    setDetail(null);
                    setSelectedId(null);
                }
                
                // 刷新列表
                await loadList();
            } else {
                const errText = await response.text();
                throw new Error(errText || '删除失败');
            }
        } catch (error) {
            console.error('Delete failed:', error);
            setActionErr('删除题目失败');
            window.location.reload();
        }
    };

    const handleCreate = () => {
        const userInfo = getCurrentUserInfo();
        if (!userInfo?.username) {
            navigate('/login');
            return;
        }

        const clearForm = () => {
            setCreateCoord(null);
            setUploadedImageKey('');
            setUploadedImageUrl('');
            setCreateTitle('');
            setCreateContent('');
            setCreateAnswer('');
        };

        submitCreateQuestion({
            navigate,
            username: userInfo.username,
            createCampus,
            createDifficulty,
            createCoord,
            uploadedImageKey,
            createTitle,
            createContent,
            createAnswer,
            setCreateLoading,
            setActionMsg,
            setActionErr,
            onCreated: loadList,
            clearForm,
        });
    };

    const handleCommentSubmit = async () => {
        const userInfo = getCurrentUserInfo();
        console.log('Submitting comment by user:', userInfo?.username);
        if (!userInfo?.username) {
            navigate('/login');
            return;
        }

        if (!selectedId) return;
        if (!commentContent.trim()) {
            setActionErr('评论内容不能为空');
            return;
        }

        try {
            setIsSubmittingComment(true);
            setActionMsg('');
            setActionErr('');

            // 2.【新增步骤】先调用接口获取完整用户信息（为了拿到 userId）
            // 假设 getUserInfo 返回的对象里包含 id 字段
            let fullUserProfile;
            try {
                fullUserProfile = await getUserInfo(userInfo.username);
            } catch (err) {
                console.error("获取用户信息失败:", err);
                throw new Error("无法验证用户身份，请检查网络或重新登录");
            }


            // 3. 使用获取到的 id 提交评论
            await createComment(selectedId, {
                userId: fullUserProfile.userId, 
                content: commentContent
            });

            setActionMsg('评论发布成功！');
            setCommentContent('');
            loadComments(selectedId);

            // Reload the whole page to ensure latest state across the app
            // (intended behavior: 页面重新加载以刷新所有相关视图)
            // window.location.reload();
        } catch (error) {
            console.error('Comment failed:', error);
        } finally {
            setIsSubmittingComment(false);
        }
    };

    const loadComments = async (questionId) => {
        try {
            setCommentsLoading(true);
            const data = await getCommentList(questionId);
            console.log("加载评论成功:", data);
            setComments(data || []);
        } catch (e) {
            console.error("加载评论失败", e);
            setComments([]);
        } finally {
            setCommentsLoading(false);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!window.confirm('确定要删除这条评论吗？')) return;

        // 获取 ID 用于 API 调用
        if (!currentUsername) {
            navigate('/login');
            return;
        }

        try {
            // 需要获取 userId 才能调用删除接口
            // (优化：如果你在 SoloMode 顶部已经用 useEffect 获取过完整 userInfo 并存了 state，这里可以直接用)
            const userProfile = await getUserInfo(currentUsername);
            
            await deleteComment(commentId, userProfile.userId);

            setActionMsg('评论删除成功');
            // 清空输入框
            setCommentContent('');

            // 直接刷新整页以确保视图一致
            window.location.reload();
            
        } catch (e) {
            console.error("删除失败", e);
            // setActionErr('删除评论失败');
        }
    };

    const handleToggleLike = async (comment) => {
        // 1. 权限校验
        if (!currentUserInfo?.username) {
            navigate('/login');
            return;
        }

        // 为了防止用户疯狂点击，这里最好做一个防抖，或者简单的乐观更新
        // 这里采用：先获取 ID，再调用 API，成功后更新 UI
        
        try {
            // 获取用户真实ID (如果 state 里没有存，就重新获取)
            const userProfile = await getUserInfo(currentUserInfo.username);
            const userId = userProfile.userId;

            // 2. 判断当前是“点赞”还是“取消”
            // 注意：因为后端没返回 isLiked，初始加载时默认是 false。
            // 只有用户在当前页面点击过，isLiked 才会变成 true。
            const isCurrentlyLiked = comment.isLiked || false;
            
            if (isCurrentlyLiked) {
                // 执行取消点赞
                await unlikeComment(comment.commentId, userId);
            } else {
                // 执行点赞
                await likeComment(comment.commentId, userId);
            }

            // 3. 手动更新本地状态 (UI 变色 + 数字变化)
            setComments(prevComments => 
                prevComments.map(c => {
                    if (c.commentId === comment.commentId) {
                        return {
                            ...c,
                            isLiked: !isCurrentlyLiked, // 状态反转
                            likeCount: isCurrentlyLiked 
                                ? (c.likeCount - 1) // 取消点赞：数量-1
                                : (c.likeCount + 1) // 点赞：数量+1
                        };
                    }
                    return c;
                })
            );

        } catch (error) {
            console.error('Like toggle failed:', error);
        }
    };



    return (
        <div className="min-h-screen flex flex-col items-center justify-start p-6 relative overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-b from-black via-gray-900 to-black -z-10" />
            <div className="absolute top-0 left-0 w-full h-full overflow-hidden -z-10 opacity-30">
                <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl animate-pulse" />
                <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl animate-pulse delay-1000" />
            </div>

            <div className="max-w-6xl w-full pt-6">
                <div className="flex items-center justify-between mb-8">
                    {/* <button
                        onClick={() => navigate('/game-menu')}
                        className="p-3 rounded-full glass-dark hover:bg-white/10 transition-all group"
                    >
                        <ChevronLeft className="w-6 h-6 text-gray-400 group-hover:text-white transition-colors" />
                    </button> */}
                    <h1 className="text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-400">
                        独自变强
                    </h1>
                    {/* <button
                        type="button"
                        onClick={handleRefresh}
                        className="px-4 py-2 rounded-full glass-dark hover:bg-white/10 transition-all text-gray-200"
                    >
                        刷新
                    </button> */}
                </div>

                {actionMsg ? (
                    <div className="mb-4 p-4 bg-green-500/10 border border-green-500/30 rounded-xl text-green-400">
                        {actionMsg}
                    </div>
                ) : null}
                {actionErr ? (
                    <div className="mb-4 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400">
                        {actionErr}
                    </div>
                ) : null}

                <div className="grid lg:grid-cols-3 gap-6">
                    {renderQuestionListPanel({
                        campus,
                        difficulty,
                        onChangeCampus: setCampus,
                        onChangeDifficulty: setDifficulty,
                        total,
                        filteredCount: filteredList.length,
                        listLoading,
                        listError,
                        filteredList,
                        selectedId,
                        onSelect: handleSelect,
                        onDelete: handleDelete,
                        onStartRandomPractice: handleStartRandomPractice,
                    })}

                    {renderQuestionDetailPanel({
                        selectedId,
                        detailLoading,
                        detailError,
                        detail,
                        commentContent,
                        setCommentContent,
                        onSubmitComment: handleCommentSubmit,
                        isSubmittingComment,
                        comments,
                        commentsLoading,
                        currentUsername, // 传进去用于判断是否显示删除按钮
                        onDeleteComment: handleDeleteComment,
                        onToggleLike: handleToggleLike
                    })}

                    {renderCreateQuestionPanel({
                        createCampus,
                        setCreateCampus,
                        createDifficulty,
                        setCreateDifficulty,
                        createCoord,
                        onOpenMapPicker: () => setShowMapPicker(true),
                        uploadedImageKey,
                        uploadedImageUrl,
                        onImageUpload: handleImageUpload,
                        uploadingImage,
                        createTitle,
                        setCreateTitle,
                        createContent,
                        setCreateContent,
                        createAnswer,
                        setCreateAnswer,
                        createLoading,
                        onCreate: handleCreate,
                    })}
                </div>
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

export default SoloMode;
