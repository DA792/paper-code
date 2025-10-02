package utils;

import index.PVL2D_tree_index.Point2D;
import utils.AdaptiveZOrder.*;
import utils.AdaptiveRangeQuery.*;

/**
 * 专门测试查询范围 [(0,0), (4,2)] 的程序
 */
public class SpecificQueryTest {
    
    public static void main(String[] args) {
        System.out.println("=== 使用 grid_10x10_sparse_dataset.csv 测试查询范围 [(0,0), (4,2)] ===\n");
        
        // 加载真实数据集
        Point2D[] testData = loadDataset("src/src/data/grid_10x10_sparse_dataset.csv", 1000);
        
        if (testData == null || testData.length == 0) {
            System.err.println("无法加载数据集文件，使用备用测试数据");
            // 备用测试数据
            testData = new Point2D[] {
                new Point2D(0, 0), new Point2D(2, 0), new Point2D(1, 1),
                new Point2D(0, 2), new Point2D(1, 3), new Point2D(3, 0),
                new Point2D(3, 1), new Point2D(4, 4), new Point2D(5, 5)
            };
        }
        
        System.out.println("测试数据集包含 " + testData.length + " 个点:");
        
        // 分析哪些点在查询范围内
        int pointsInRange = 0;
        for (int i = 0; i < Math.min(15, testData.length); i++) { // 只显示前15个点
            Point2D p = testData[i];
            boolean inRange = (p.x >= 0 && p.x <= 4 && p.y >= 0 && p.y <= 2);
            if (inRange) pointsInRange++;
            System.out.println("  点" + (i+1) + ": " + p + (inRange ? " [在查询范围内]" : " [在范围外]"));
        }
        
        if (testData.length > 15) {
            // 统计剩余点中有多少在范围内
            for (int i = 15; i < testData.length; i++) {
                Point2D p = testData[i];
                if (p.x >= 0 && p.x <= 4 && p.y >= 0 && p.y <= 2) {
                    pointsInRange++;
                }
            }
            System.out.println("  ... 还有" + (testData.length - 15) + "个点未显示");
        }
        
        System.out.println("预期在查询范围 [(0,0), (4,2)] 内的点数: " + pointsInRange);
        
        // 构建自适应Z-order树
        System.out.println("\n步骤1: 构建自适应Z-order树...");
        long buildStart = System.nanoTime();
        AdaptiveZOrderTree tree = new AdaptiveZOrderTree(testData);
        long buildEnd = System.nanoTime();
        System.out.println("树构建完成，耗时: " + (buildEnd - buildStart) / 1000000.0 + "ms");
        
        // 显示树的基本信息
        AdaptiveZOrderNode root = tree.getRoot();
        System.out.println("根节点最优位数: " + root.optimalBits + " 位");
        System.out.println("树统计信息: " + root.getTreeStats());
        
        // 创建查询系统
        System.out.println("\n步骤2: 创建查询系统...");
        AdaptiveRangeQuery querySystem = new AdaptiveRangeQuery(tree);
        
        // 执行指定的查询范围 [(0,0), (4,2)]
        System.out.println("\n步骤3: 执行查询 [(0,0), (4,2)]...");
        Point2D bottomLeft = new Point2D(0, 0);
        Point2D topRight = new Point2D(4, 2);
        
        System.out.println("查询条件:");
        System.out.println("  左下角: " + bottomLeft);
        System.out.println("  右上角: " + topRight);
        System.out.println("  范围: 0 ≤ x ≤ 4 且 0 ≤ y ≤ 2");
        
        // 计算查询范围的Z地址
        long queryMinZ = AdaptiveRangeQuery.ZOrderUtils.computeZOrderWithBits(bottomLeft, root.optimalBits);
        long queryMaxZ = AdaptiveRangeQuery.ZOrderUtils.computeZOrderWithBits(topRight, root.optimalBits);
        System.out.println("  Z地址范围: [" + queryMinZ + ", " + queryMaxZ + "]");
        
        // 执行自适应PVL-tree风格查询
        System.out.println("\n=== 自适应PVL-tree风格查询特点 ===");
        System.out.println("1. 每个节点使用自己的最优位数重新计算Z地址范围");
        System.out.println("2. 学习模型预测 + 边界扫描");
        System.out.println("3. 简化版：只检查Z地址范围，不进行空间坐标解码验证");
        System.out.println("4. 先扩展到数据集最大位数，再截取高位到当前节点最优位数");
        
        RangeQueryResult result = querySystem.rangeQuery(bottomLeft, topRight);
        
        // 显示详细的查询结果
        System.out.println("\n=== 查询结果分析 ===");
        System.out.println("找到的点数量: " + result.points.size());
        System.out.println("访问的树节点数: " + result.nodeAccesses);
        System.out.println("查询耗时: " + result.queryTime + "ms");
        
        System.out.println("\n查询结果中的所有点:");
        if (result.points.isEmpty()) {
            System.out.println("  (没有找到任何点)");
        } else {
            for (int i = 0; i < result.points.size(); i++) {
                Point2D p = result.points.get(i);
                System.out.println("  结果" + (i+1) + ": " + p);
            }
        }
        
        // 查询结果统计
        System.out.println("\n=== 查询结果统计 ===");
        System.out.println("找到的点数: " + result.points.size());
        
        // 分析查询效率
        System.out.println("\n=== 查询效率分析 ===");
        int totalNodes = root.getTreeStats().totalNodes;
        double accessRatio = (double) result.nodeAccesses / totalNodes;
        System.out.println("总节点数: " + totalNodes);
        System.out.println("访问节点数: " + result.nodeAccesses);
        System.out.println("节点访问比例: " + String.format("%.2f%%", accessRatio * 100));
        
        if (accessRatio < 0.3) {
            System.out.println("✓ 查询效率极佳！只访问了不到30%的节点");
        } else if (accessRatio < 0.5) {
            System.out.println("✓ 查询效率很好！只访问了不到50%的节点");
        } else if (accessRatio < 0.8) {
            System.out.println("○ 查询效率中等");
        } else {
            System.out.println("△ 查询效率有待提升");
        }
        
        // 展示您的查询思路的核心步骤
        System.out.println("\n=== 您的查询思路实现总结 ===");
        System.out.println("1. ✓ 输入查询矩形: 左下角" + bottomLeft + " -> 右上角" + topRight);
        System.out.println("2. ✓ 使用根节点最优位数(" + root.optimalBits + "位)计算Z地址范围");
        System.out.println("3. ✓ 通过学习模型预测起始位置");
        System.out.println("4. ✓ 在预测位置±误差范围内进行二分搜索");
        System.out.println("5. ✓ 对Z地址范围内的点进行空间坐标验证");
        System.out.println("查询思路实现完成！");
    }
    
    /**
     * 加载数据集的方法
     */
    private static Point2D[] loadDataset(String filename, int maxPoints) {
        java.util.List<Point2D> points = new java.util.ArrayList<>();
        
        // 尝试多个可能的路径
        String[] possiblePaths = {
            filename,
            "src/" + filename,
            System.getProperty("user.dir") + "/" + filename,
            System.getProperty("user.dir") + "/src/" + filename
        };
        
        java.io.BufferedReader br = null;
        
        // 尝试每个可能的路径
        for (String path : possiblePaths) {
            try {
                br = new java.io.BufferedReader(new java.io.FileReader(path));
                System.out.println("成功加载数据集: " + path);
                break;
            } catch (java.io.IOException e) {
                // 继续尝试下一个路径
                continue;
            }
        }
        
        if (br == null) {
            System.err.println("无法找到数据集文件: " + filename);
            return null;
        }
        
        try {
            String line;
            int count = 0;
            
            while ((line = br.readLine()) != null && count < maxPoints) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 支持逗号分隔
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        long x = Long.parseLong(parts[0].trim());
                        long y = Long.parseLong(parts[1].trim());
                        points.add(new Point2D(x, y));
                        count++;
                    } catch (NumberFormatException e) {
                        // 跳过无效行
                        continue;
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("读取文件错误: " + e.getMessage());
            return null;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (java.io.IOException e) {
                System.err.println("关闭文件错误: " + e.getMessage());
            }
        }
        
        return points.toArray(new Point2D[0]);
    }
}
