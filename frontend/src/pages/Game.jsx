import React, { useState } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import { motion, AnimatePresence } from 'framer-motion';
import { ZoomIn, ZoomOut, Crosshair } from 'lucide-react';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default marker icon issue
import L from 'leaflet';
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Map click handler component
function MapClickHandler({ onMapClick }) {
    const [position, setPosition] = useState(null);

    useMapEvents({
        click(e) {
            setPosition(e.latlng);
            onMapClick(e.latlng);
        },
    });

    return position === null ? null : <Marker position={position} />;
}

const Game = () => {
    const [guessPosition, setGuessPosition] = useState(null);
    const [showMap, setShowMap] = useState(false);

    // Default center: SYSU Zhuhai Campus approximate location
    const defaultCenter = [22.255, 113.541];

    const handleMapClick = (latlng) => {
        setGuessPosition(latlng);
    };

    const handleSubmit = () => {
        if (guessPosition) {
            alert(`已提交猜测位置: ${guessPosition.lat.toFixed(4)}, ${guessPosition.lng.toFixed(4)}`);
            // TODO: Calculate distance and score
        } else {
            alert('请先在地图上标记位置！');
        }
    };

    return (
        <div className="fixed inset-0 bg-black">
            {/* Player Score Headers */}
            <div className="absolute top-0 left-0 right-0 z-50 flex justify-between p-4">
                {/* Player 1 */}
                <div className="flex items-center gap-3 glass-dark px-4 py-2 rounded-full">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center font-bold">
                        用
                    </div>
                    <div className="bg-gradient-to-r from-green-400 to-green-600 h-6 rounded-full px-3 text-sm font-bold flex items-center">
                        6000
                    </div>
                </div>

                {/* Timer */}
                <div className="glass-dark px-6 py-2 rounded-full flex items-center gap-2">
                    <span className="text-sm text-gray-400">倒计时</span>
                    <span className="text-xl font-bold">60</span>
                </div>

                {/* Player 2 */}
                <div className="flex items-center gap-3 glass-dark px-4 py-2 rounded-full">
                    <div className="bg-gradient-to-r from-green-400 to-green-600 h-6 rounded-full px-3 text-sm font-bold flex items-center">
                        6000
                    </div>
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center font-bold">
                        L
                    </div>
                </div>
            </div>

            {/* Main Game Area - Street View Image */}
            <div className="w-full h-full relative">
                <img
                    src="https://images.unsplash.com/photo-1562774053-701939374585?w=1920&h=1080&fit=crop"
                    alt="Game location"
                    className="w-full h-full object-cover"
                />

                {/* Map Overlay - Bottom Right */}
                <AnimatePresence>
                    <motion.div
                        initial={{ scale: 0.8, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        className="absolute bottom-6 right-6 w-[400px] h-[400px] rounded-2xl overflow-hidden shadow-2xl border-2 border-white/20"
                    >
                        <MapContainer
                            center={defaultCenter}
                            zoom={15}
                            style={{ height: '100%', width: '100%' }}
                            zoomControl={false}
                        >
                            <TileLayer
                                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                            />
                            <MapClickHandler onMapClick={handleMapClick} />
                        </MapContainer>

                        {/* Map Controls */}
                        <div className="absolute top-4 right-4 flex flex-col gap-2 z-[1000]">
                            <button className="p-2 glass-dark rounded-lg hover:bg-white/20 transition-all">
                                <ZoomIn size={20} />
                            </button>
                            <button className="p-2 glass-dark rounded-lg hover:bg-white/20 transition-all">
                                <ZoomOut size={20} />
                            </button>
                            <button className="p-2 glass-dark rounded-lg hover:bg-white/20 transition-all">
                                <Crosshair size={20} />
                            </button>
                        </div>

                        {/* Submit Button */}
                        <div className="absolute bottom-4 left-4 right-4 z-[1000]">
                            <button
                                onClick={handleSubmit}
                                className="w-full py-3 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg hover:shadow-orange-500/50 transition-all"
                            >
                                确认位置
                            </button>
                        </div>
                    </motion.div>
                </AnimatePresence>
            </div>

            {/* Bottom Info */}
            <div className="absolute bottom-6 left-6 glass-dark px-4 py-2 rounded-full text-sm">
                <span className="text-gray-400">回合:</span> <span className="font-bold ml-2">1/5</span>
            </div>
        </div>
    );
};

export default Game;
