package com.campusguess.battle.util;

/**
 * 地理距离计算工具
 * 使用 Haversine 公式计算两点间的大圆距离
 */
public final class DistanceUtil {

    private static final double EARTH_RADIUS = 6371000.0; // 地球半径（米）

    private DistanceUtil() {}

    /**
     * 计算两点间距离（米）
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * 根据距离计算伤害值
     * 采用对数公式，伤害随距离先迅速增长，然后缓慢增长
     * 公式: damage = a * log10(distance/b + 1) + c
     * 其中 a 控制增长速度，b 控制曲线陡峭度，c 是基础伤害
     */
    public static int calculateDamage(double distance) {
        double a = 15.0;
        double b = 100.0;
        double c = 5.0;
        double damage = a * Math.log10(distance / b + 1) + c;
        return (int) Math.round(damage);
    }
}