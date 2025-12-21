import { motion } from 'framer-motion';
import { MapPin, Compass, Trophy } from 'lucide-react';

const Home = () => {
    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
            <div className="text-center max-w-3xl mx-auto mb-16">
                <motion.h1
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-5xl font-bold tracking-tight text-gray-900 mb-6"
                >
                    Explore SYSU Zhuhai Campus
                </motion.h1>
                <motion.p
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                    className="text-xl text-gray-600 mb-8"
                >
                    Test your knowledge of the campus layout. Guess the location from images and compete with others.
                </motion.p>
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                    className="flex justify-center gap-4"
                >
                    <button className="px-8 py-3 bg-blue-600 text-white font-semibold rounded-full hover:bg-blue-700 transition-all shadow-lg hover:shadow-blue-500/25">
                        Start Game
                    </button>
                    <button className="px-8 py-3 bg-white text-gray-900 font-semibold rounded-full border border-gray-200 hover:bg-gray-50 transition-all">
                        View Leaderboard
                    </button>
                </motion.div>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
                <FeatureCard
                    icon={<Compass className="w-6 h-6 text-blue-600" />}
                    title="Explore"
                    description="Navigate through immersive panoramas of the beautiful Zhuhai campus."
                />
                <FeatureCard
                    icon={<MapPin className="w-6 h-6 text-blue-600" />}
                    title="Pinpoint"
                    description="Mark the exact location on the map and get scored based on accuracy."
                />
                <FeatureCard
                    icon={<Trophy className="w-6 h-6 text-blue-600" />}
                    title="Compete"
                    description="Climb the leaderboard and challenge your friends in real-time."
                />
            </div>
        </div>
    );
};

const FeatureCard = ({ icon, title, description }) => (
    <div className="p-6 bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
        <div className="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center mb-4">
            {icon}
        </div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-600">{description}</p>
    </div>
);

export default Home;
