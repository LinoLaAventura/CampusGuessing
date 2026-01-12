import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker } from 'react-leaflet';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft } from 'lucide-react';
import 'leaflet/dist/leaflet.css';

import L from 'leaflet';
import { getUserRecordDetail } from '../api/userApi';
import { getQuestionDetail } from '../api/questionApi';
import { getCurrentUserInfo } from '../api/authStorage';

// Fix Leaflet default marker icon issue
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const correctMarkerIcon = L.divIcon({
  className: 'campusguess-correct-marker',
  html: '<div class="w-4 h-4 rounded-full bg-orange-500 border-2 border-white shadow-lg"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
});

const userMarkerIcon = L.divIcon({
  className: 'campusguess-user-marker',
  html: '<div class="w-4 h-4 rounded-full bg-blue-400 border-2 border-white shadow-lg"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
});

function formatMeters(meters) {
  if (!Number.isFinite(meters)) return '-';
  if (meters >= 1000) return `${(meters / 1000).toFixed(2)} km`;
  return `${Math.round(meters)} m`;
}

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

const Review = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state ?? {};
  const userInfo = getCurrentUserInfo();

  const userId = state.userId ?? userInfo?.userId;
  const recordId = state.recordId;

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (!userId || !recordId) {
      // Missing params - go back
      navigate(-1);
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getUserRecordDetail(userId, recordId);
        if (!cancelled) setDetail(data);
      } catch (e) {
        if (!cancelled) setError(e?.message || '记录加载失败');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => { cancelled = true; };
  }, [userId, recordId, navigate]);

  const question = useMemo(() => {
    return detail?.questionDetails?.[currentIndex] ?? null;
  }, [detail, currentIndex]);

  const resolveImageUrl = (raw) => {
    if (!raw) return '';
    if (raw.startsWith('http')) return raw;
    // Try common PICUI/public patterns for a stored key
    const key = raw;
    return `https://picui.cn/i/${encodeURIComponent(key)}`;
  };

  const [imageSrc, setImageSrc] = useState('');
  const questionImageCache = useMemo(() => ({}), []);

  useEffect(() => {
    const qId = question?.questionBase?.questionId;
    if (!qId) {
      setImageSrc('');
      return;
    }

    let cancelled = false;

    (async () => {
      // If we already fetched this question detail, reuse it
      if (questionImageCache[qId]) {
        setImageSrc(questionImageCache[qId]);
        return;
      }

      try {
        const qDetail = await getQuestionDetail(qId);
        const url = qDetail?.imageData?.links?.url || resolveImageUrl(question?.questionBase?.imageUrl);
        questionImageCache[qId] = url;
        if (!cancelled) setImageSrc(url);
      } catch (e) {
        // fallback to raw key-based resolution
        const url = resolveImageUrl(question?.questionBase?.imageUrl);
        questionImageCache[qId] = url;
        if (!cancelled) setImageSrc(url);
      }
    })();

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [question?.questionBase?.questionId]);

  const correctCoord = question?.questionBase?.correctCoord
    ? { lat: question.questionBase.correctCoord.lat, lng: question.questionBase.correctCoord.lon }
    : null;

  const userCoord = question?.userAnswerInfo?.userCoord
    ? { lat: question.userAnswerInfo.userCoord.lat, lng: question.userAnswerInfo.userCoord.lon }
    : null;

  const mapCenter = useMemo(() => {
    if (correctCoord) return [correctCoord.lat, correctCoord.lng];
    if (userCoord) return [userCoord.lat, userCoord.lng];
    return [22.351484, 113.583680];
  }, [correctCoord, userCoord]);

  const mapZoom = 16;

  const handleBack = () => navigate(-1);

  const distAndScore = useMemo(() => {
    if (!correctCoord || !userCoord) return { meters: NaN, score: 0 };
    const { meters, score } = calculateDistanceAndScore({ lat: correctCoord.lat, lon: correctCoord.lng }, { lat: userCoord.lat, lng: userCoord.lng });
    return { meters, score };
  }, [correctCoord, userCoord]);

  return (
    <div className="min-h-screen bg-black text-white pt-20 pb-12 px-6">
      <div className="max-w-4xl mx-auto relative">
        <div className="absolute top-4 left-4 z-50">
          {/* <button type="button" onClick={handleBack} className="p-2 rounded-full glass-dark">
            <ChevronLeft className="w-6 h-6 text-gray-300" />
          </button> */}
        </div>

        <div className="glass-dark rounded-3xl overflow-hidden">
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <div className="text-xl font-bold">复盘 · {detail?.gameRecordBase?.gameType ?? ''}</div>
                <div className="text-sm text-gray-400">{detail?.gameRecordBase?.createdAt ?? ''}</div>
              </div>
              <div className="text-right">
                <div className="text-sm text-gray-400">题目 {currentIndex + 1}/{detail?.gameRecordBase?.totalQuestionNum ?? detail?.questionDetails?.length ?? 0}</div>
                <div className="text-lg font-bold">总得分：{question?.userAnswerInfo?.singleScore ?? '-'}</div>
              </div>
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-6 p-6">
            <div className="w-full h-96 bg-black rounded-2xl flex items-center justify-center overflow-hidden">
              {loading ? (
                <div className="text-gray-500">加载中...</div>
              ) : imageSrc ? (
                <img
                  src={imageSrc}
                  alt={question?.questionBase?.questionId ?? 'image'}
                  className="max-h-full object-contain"
                  referrerPolicy="no-referrer"
                  onError={(e) => {
                    console.error('图片加载失败:', imageSrc);
                    const raw = question?.questionBase?.imageUrl ?? '';
                    if (!raw) return;
                    const key = raw.replace(/^https?:\/\/.*\//, '');
                    const fallbacks = [
                      `https://picui.cn/images/${encodeURIComponent(key)}`,
                      `https://picui.cn/api/v1/images/${encodeURIComponent(key)}/raw`,
                    ];
                    const current = e.target.src;
                    const next = fallbacks.find((u) => u !== current && !u.includes('undefined'));
                    if (next) setImageSrc(next);
                    else setImageSrc(null);
                  }}
                />
              ) : (
                <div className="text-gray-500">无图片</div>
              )}
            </div>

            <div className="flex flex-col gap-4">
              <div className="w-full h-96 rounded-2xl overflow-hidden shadow-2xl border-2 border-white/20">
                <MapContainer center={mapCenter} zoom={mapZoom} style={{ height: '100%', width: '100%' }} zoomControl={false}>
                  <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>' />
                  {userCoord ? <Marker position={[userCoord.lat, userCoord.lng]} icon={userMarkerIcon} /> : null}
                  {correctCoord ? <Marker position={[correctCoord.lat, correctCoord.lng]} icon={correctMarkerIcon} /> : null}
                </MapContainer>
              </div>

              <div className="text-gray-300">
                <div className="mb-2">题目 ID: {question?.questionBase?.questionId ?? '-'}</div>
                <div className="mb-2">校园: {question?.questionBase?.campus ?? '-'}</div>
                <div className="mb-2">难度: {question?.questionBase?.difficulty ?? '-'}</div>
                <div className="mb-2">距离: {formatMeters(distAndScore.meters)} · 计算得分: {distAndScore.score}</div>
              </div>
            </div>
          </div>

          <div className="p-6">
            <div className="flex gap-3 mt-6">
              <button
                type="button"
                disabled={currentIndex <= 0}
                onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
                className="py-2 px-4 rounded-xl bg-white/5 disabled:opacity-40"
              >上一题</button>
              <button
                type="button"
                disabled={currentIndex >= ((detail?.questionDetails?.length ?? 1) - 1)}
                onClick={() => setCurrentIndex((i) => Math.min((detail?.questionDetails?.length ?? 1) - 1, i + 1))}
                className="py-2 px-4 rounded-xl bg-white/5 disabled:opacity-40"
              >下一题</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Review;
