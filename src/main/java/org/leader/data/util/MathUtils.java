package org.leader.data.util;

/**
 * 计算工具类
 *
 * @author ldh
 * @since 2017-02-20 10:27
 */
public class MathUtils {

    /**
     * 对数运算
     * @param value
     * @param base 底数
     * @return
     */
    public static double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }
}
