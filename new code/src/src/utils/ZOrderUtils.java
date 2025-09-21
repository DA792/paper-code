package utils;

import index.PVL2D_tree_index.Point2D;

public class ZOrderUtils {
    
    // 计算Z-order编码
    public static long computeZOrder(long x, long y) {
        long z = 0;
        for (int i = 0; i < 32; i++) {
            z |= ((x & (1L << i)) << i) | ((y & (1L << i)) << (i + 1));
        }
        return z;
    }
    
    // 使用指定位数计算Z-order（按照用户规则：从最高位开始，先x再y交替）
    public static long computeZOrderWithBits(long x, long y, int bits) {
        long z = 0;

        for (int i = bits - 1; i >= 0; i--) {
            // 取 x 的第 i 位，放到 2*i + 1 位置（因为从最高位开始）
            if ((x & (1L << i)) != 0) {
                z |= (1L << (2 * i + 1));
            }

            // 取 y 的第 i 位，放到 2*i 位置
            if ((y & (1L << i)) != 0) {
                z |= (1L << (2 * i));
            }
        }

        return z;
    }
    
    // 使用指定位数计算Z-order（Point2D版本）
    public static long computeZOrderWithBits(Point2D point, int bits) {
        return computeZOrderWithBits(point.x, point.y, bits);
    }
    
    // 优化的Z-order计算（使用位操作）
    public static long computeZOrderFast(long x, long y) {
        x = (x | (x << 16)) & 0x0000FFFF0000FFFFL;
        x = (x | (x << 8)) & 0x00FF00FF00FF00FFL;
        x = (x | (x << 4)) & 0x0F0F0F0F0F0F0F0FL;
        x = (x | (x << 2)) & 0x3333333333333333L;
        x = (x | (x << 1)) & 0x5555555555555555L;
        
        y = (y | (y << 16)) & 0x0000FFFF0000FFFFL;
        y = (y | (y << 8)) & 0x00FF00FF00FF00FFL;
        y = (y | (y << 4)) & 0x0F0F0F0F0F0F0F0FL;
        y = (y | (y << 2)) & 0x3333333333333333L;
        y = (y | (y << 1)) & 0x5555555555555555L;
        
        return x | (y << 1);
    }
    
    // 从Z-order解码
    public static long[] fromZOrder(long zOrder) {
        long x = 0, y = 0;
        for (int i = 0; i < 32; i++) {
            x |= (zOrder & (1L << (2 * i))) >> i;
            y |= (zOrder & (1L << (2 * i + 1))) >> (i + 1);
        }
        return new long[]{x, y};
    }
    
    // 批量计算Z-order
    public static long[] computeZOrdersBatch(long[] xCoords, long[] yCoords) {
        if (xCoords.length != yCoords.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }
        
        long[] zOrders = new long[xCoords.length];
        for (int i = 0; i < xCoords.length; i++) {
            zOrders[i] = computeZOrderFast(xCoords[i], yCoords[i]);
        }
        return zOrders;
    }
    
    // 将二维点数组转换为Z-order数组
    public static long[] pointsToZOrders(Point2D[] points) {
        long[] zOrders = new long[points.length];
        for (int i = 0; i < points.length; i++) {
            zOrders[i] = points[i].zOrder;
        }
        return zOrders;
    }
}