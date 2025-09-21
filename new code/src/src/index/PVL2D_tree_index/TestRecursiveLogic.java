package index.PVL2D_tree_index;

import utils.AdaptiveZOrder;
import utils.ZOrderUtils;
import java.util.Arrays;

public class TestRecursiveLogic {
    
    public static void main(String[] args) {
        System.out.println("=== 测试递归逻辑 ===\n");
        
        // 测试用例1：应该能递归到2位的数据
        System.out.println("测试用例1：应该能递归到2位的数据");
        Point2D[] testData1 = createTestData1();
        testRecursiveLogic(testData1);
        
        // 测试用例2：应该能递归到3位的数据
        System.out.println("\n测试用例2：应该能递归到3位的数据");
        Point2D[] testData2 = createTestData2();
        testRecursiveLogic(testData2);
        
        // 测试用例3：只能到1位的数据
        System.out.println("\n测试用例3：只能到1位的数据");
        Point2D[] testData3 = createTestData3();
        testRecursiveLogic(testData3);
    }
    
    private static void testRecursiveLogic(Point2D[] data) {
        System.out.println("数据点: " + Arrays.toString(data));
        
        // 手动测试每个位数
        for (int bits = 1; bits <= 4; bits++) {
            System.out.println("\n--- 手动测试 " + bits + " 位 ---");
            
            // 计算数据范围
            long minX = Arrays.stream(data).mapToLong(p -> p.x).min().orElse(0);
            long maxX = Arrays.stream(data).mapToLong(p -> p.x).max().orElse(0);
            long minY = Arrays.stream(data).mapToLong(p -> p.y).min().orElse(0);
            long maxY = Arrays.stream(data).mapToLong(p -> p.y).max().orElse(0);
            
            // 分割空间
            int cellCount = 1 << bits;
            long cellWidth = Math.max(1, (maxX - minX + 1) / cellCount);
            long cellHeight = Math.max(1, (maxY - minY + 1) / cellCount);
            
            System.out.println("分割成 " + cellCount + "×" + cellCount + " = " + (cellCount * cellCount) + " 个单元格");
            System.out.println("单元格大小: " + cellWidth + "×" + cellHeight);
            
            // 统计有数据的单元格
            int cellsWithData = 0;
            for (int i = 0; i < cellCount; i++) {
                for (int j = 0; j < cellCount; j++) {
                    long cellMinX = minX + i * cellWidth;
                    long cellMaxX = minX + (i + 1) * cellWidth - 1;
                    long cellMinY = minY + j * cellHeight;
                    long cellMaxY = minY + (j + 1) * cellHeight - 1;
                    
                    boolean hasData = false;
                    for (Point2D point : data) {
                        if (point.x >= cellMinX && point.x <= cellMaxX && 
                            point.y >= cellMinY && point.y <= cellMaxY) {
                            hasData = true;
                            break;
                        }
                    }
                    if (hasData) cellsWithData++;
                }
            }
            
            System.out.println("有数据的单元格: " + cellsWithData);
            
            if (cellsWithData < 3) {
                System.out.println("中心点数量不足，无法进行线性拟合");
                break;
            }
            
            // 计算Z-order值
            System.out.println("Z-order编码:");
            for (Point2D point : data) {
                long zOrder = ZOrderUtils.computeZOrderWithBits(point, bits);
                System.out.println("  点" + point + " -> Z-order: " + zOrder);
            }
        }
        
        // 运行自适应算法
        System.out.println("\n--- 运行自适应算法 ---");
        int optimalBits = AdaptiveZOrder.determineOptimalBits(data);
        System.out.println("最终选择的最优位数: " + optimalBits);
    }
    
    // 创建应该能递归到2位的数据
    private static Point2D[] createTestData1() {
        return new Point2D[]{
            new Point2D(0, 0), new Point2D(1, 0), new Point2D(2, 0), new Point2D(3, 0),
            new Point2D(0, 1), new Point2D(1, 1), new Point2D(2, 1), new Point2D(3, 1),
            new Point2D(0, 2), new Point2D(1, 2), new Point2D(2, 2), new Point2D(3, 2),
            new Point2D(0, 3), new Point2D(1, 3), new Point2D(2, 3), new Point2D(3, 3)
        };
    }
    
    // 创建应该能递归到3位的数据
    private static Point2D[] createTestData2() {
        return new Point2D[]{
            new Point2D(0, 0), new Point2D(1, 0), new Point2D(2, 0), new Point2D(3, 0),
            new Point2D(4, 0), new Point2D(5, 0), new Point2D(6, 0), new Point2D(7, 0),
            new Point2D(0, 1), new Point2D(1, 1), new Point2D(2, 1), new Point2D(3, 1),
            new Point2D(4, 1), new Point2D(5, 1), new Point2D(6, 1), new Point2D(7, 1),
            new Point2D(0, 2), new Point2D(1, 2), new Point2D(2, 2), new Point2D(3, 2),
            new Point2D(4, 2), new Point2D(5, 2), new Point2D(6, 2), new Point2D(7, 2),
            new Point2D(0, 3), new Point2D(1, 3), new Point2D(2, 3), new Point2D(3, 3),
            new Point2D(4, 3), new Point2D(5, 3), new Point2D(6, 3), new Point2D(7, 3)
        };
    }
    
    // 创建只能到1位的数据（稀疏分布）
    private static Point2D[] createTestData3() {
        return new Point2D[]{
            new Point2D(0, 0), new Point2D(100, 100), new Point2D(200, 200)
        };
    }
}

