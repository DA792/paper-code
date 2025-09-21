package index.PVL2D_tree_index;

import utils.AdaptiveZOrder;
import utils.ZOrderUtils;
import java.util.Arrays;

public class DemoAdaptiveZOrder {
    
    public static void main(String[] args) {
        System.out.println("=== 自适应Z-order位数选择算法演示 ===\n");
        
        // 演示1：简单的小范围数据
        System.out.println("演示1：小范围数据 (4x4网格)");
        Point2D[] demoData1 = createDemoData1();
        demonstrateAlgorithm(demoData1);
        
        // 演示2：中等范围数据
        System.out.println("\n演示2：中等范围数据 (随机分布)");
        Point2D[] demoData2 = createDemoData2();
        demonstrateAlgorithm(demoData2);
        
        // 演示3：展示Z-order编码效果
        System.out.println("\n演示3：Z-order编码效果展示");
        demonstrateZOrderEncoding();
    }
    
    private static void demonstrateAlgorithm(Point2D[] data) {
        System.out.println("数据点: " + Arrays.toString(data));
        
        // 运行自适应算法
        int optimalBits = AdaptiveZOrder.determineOptimalBits(data);
        
        System.out.println("最终选择的最优位数: " + optimalBits);
        
        // 显示所有点的Z-order编码
        System.out.println("Z-order编码结果:");
        for (Point2D point : data) {
            long zOrder = ZOrderUtils.computeZOrderWithBits(point, optimalBits);
            System.out.println("  点" + point + " -> Z-order: " + zOrder);
        }
        System.out.println();
    }
    
    private static void demonstrateZOrderEncoding() {
        // 创建一个简单的2x2网格
        Point2D[] grid = {
            new Point2D(0, 0), new Point2D(1, 0),
            new Point2D(0, 1), new Point2D(1, 1)
        };
        
        System.out.println("2x2网格的Z-order编码:");
        System.out.println("网格布局:");
        System.out.println("(0,1) (1,1)");
        System.out.println("(0,0) (1,0)");
        System.out.println();
        
        System.out.println("Z-order遍历顺序:");
        for (int bits = 1; bits <= 2; bits++) {
            System.out.println("使用" + bits + "位编码:");
            for (Point2D point : grid) {
                long zOrder = ZOrderUtils.computeZOrderWithBits(point, bits);
                System.out.println("  点" + point + " -> Z-order: " + zOrder);
            }
        }
    }
    
    // 创建演示数据1：4x4网格中的部分点
    private static Point2D[] createDemoData1() {
        return new Point2D[]{
            new Point2D(0, 0), new Point2D(1, 0), new Point2D(2, 0),
            new Point2D(0, 1), new Point2D(1, 1),
            new Point2D(0, 2), new Point2D(2, 2),
            new Point2D(1, 3), new Point2D(2, 3)
        };
    }
    
    // 创建演示数据2：中等范围的随机数据
    private static Point2D[] createDemoData2() {
        return new Point2D[]{
            new Point2D(10, 20), new Point2D(30, 40), new Point2D(50, 60),
            new Point2D(70, 80), new Point2D(90, 100), new Point2D(110, 120),
            new Point2D(130, 140), new Point2D(150, 160), new Point2D(170, 180)
        };
    }
}
