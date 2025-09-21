package utils;

import index.PVL2D_tree_index.Point2D;
import java.util.*;

public class AdaptiveZOrder {
    
    // 单元格类
    public static class Cell {
        public long minX, maxX, minY, maxY;
        public List<Point2D> points;
        public Point2D center;
        
        public Cell(long minX, long maxX, long minY, long maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.points = new ArrayList<>();
        }
        
        public boolean hasData() {
            return !points.isEmpty();
        }
        
        public Point2D getCenter() {
            if (center == null && hasData()) {
                long centerX = (minX + maxX) / 2;
                long centerY = (minY + maxY) / 2;
                center = new Point2D(centerX, centerY);
            }
            return center;
        }
        
        public void addPoint(Point2D point) {
            points.add(point);
        }
        
        @Override
        public String toString() {
            return "Cell[" + minX + "," + maxX + "]x[" + minY + "," + maxY + "]";
        }
    }
    
    // 线性拟合结果
    public static class LinearFitResult {
        public double slope;
        public double intercept;
        public double rSquared;  // 决定系数
        public boolean isValid;
        
        public LinearFitResult(double slope, double intercept, double rSquared) {
            this.slope = slope;
            this.intercept = intercept;
            this.rSquared = rSquared;
            this.isValid = rSquared > 0.6; // 降低阈值，允许3个位置的误差容忍度
        }
        
        @Override
        public String toString() {
            return String.format("LinearFit{slope=%.4f, intercept=%.4f, R²=%.4f, valid=%s}", 
                               slope, intercept, rSquared, isValid);
        }
    }
    
    // 主算法：自适应确定Z-order位数
    public static int determineOptimalBits(Point2D[] points) {
        if (points.length == 0) return 1;
        
        // 计算数据范围
        long minX = Arrays.stream(points).mapToLong(p -> p.x).min().orElse(0);
        long maxX = Arrays.stream(points).mapToLong(p -> p.x).max().orElse(0);
        long minY = Arrays.stream(points).mapToLong(p -> p.y).min().orElse(0);
        long maxY = Arrays.stream(points).mapToLong(p -> p.y).max().orElse(0);
        
        // 计算每维最大需要的位数（per-dimension bits）
        // 令每维位数为k，则总格子数为 4^k，需要满足 4^k >= 点数 → k >= ceil(0.5*log2(n))
        int theoreticalMaxBits = (int) Math.ceil(0.5 * (Math.log(points.length) / Math.log(2)));
        theoreticalMaxBits = Math.max(theoreticalMaxBits, 1); // 至少1位
        
        // 根据数据范围限制最大位数，避免过度细分
        long maxRange = Math.max(maxX - minX + 1, maxY - minY + 1);
        int maxBitsForRange = (int) Math.ceil(Math.log(maxRange) / Math.log(2));
        
        // 允许尝试更多位数以获得更好的精度，但不超过数据范围所需的位数
        int maxNeededBits = Math.min(theoreticalMaxBits + 2, Math.min(maxBitsForRange, 8));
        
        System.out.println("=== 自适应Z-order位数选择 ===");
        System.out.println("数据点数量: " + points.length);
        System.out.println("数据范围: X[" + minX + ", " + maxX + "], Y[" + minY + ", " + maxY + "]");
        System.out.println("理论最大需要位数: " + theoreticalMaxBits + " (0.5*log2(" + points.length + ") = " + String.format("%.2f", 0.5 * (Math.log(points.length) / Math.log(2))) + ")");
        System.out.println("数据范围限制位数: " + maxBitsForRange + " (log2(" + maxRange + ") = " + String.format("%.2f", Math.log(maxRange) / Math.log(2)) + ")");
        System.out.println("实际尝试最大位数: " + maxNeededBits);
        
        // 使用基于二进制位递增的新算法
        return determineOptimalBitsByBinaryProgression(points, minX, maxX, minY, maxY, maxBitsForRange);
    }
    
    // 递归分层自适应Z-order算法
    private static int determineOptimalBitsByBinaryProgression(Point2D[] points, 
                                                             long minX, long maxX,
                                                             long minY, long maxY,
                                                             int maxBitsNeeded) {
        return recursiveHierarchicalZOrder(points, minX, maxX, minY, maxY, maxBitsNeeded, 0, "根节点", 1);
    }
    
    // 递归分层Z-order算法实现
    private static int recursiveHierarchicalZOrder(Point2D[] points,
                                                  long minX, long maxX, 
                                                  long minY, long maxY,
                                                  int maxBitsNeeded,
                                                  int depth,
                                                  String prefix,
                                                  int startFromBits) {
        String indent = "  ".repeat(depth);
        System.out.println("\n" + indent + "=== 递归层级 " + depth + " " + prefix + " ===");
        System.out.println(indent + "数据点数量: " + points.length);
        System.out.println(indent + "数据范围: X[" + minX + ", " + maxX + "], Y[" + minY + ", " + maxY + "]");
        
        // 递归深度限制，防止无限递归
        if (depth > 8) {
            System.out.println(indent + "递归深度超过限制(8)，返回默认值: 1");
            return 1;
        }
        
        // 如果数据点很少，直接用线性拟合
        if (points.length <= 4) {
            System.out.println(indent + "数据点过少(≤4)，直接使用线性拟合");
            return testDirectLinearFit(points, depth, indent);
        }
        
        // 如果范围很小，无需进一步细分
        long rangeX = maxX - minX;
        long rangeY = maxY - minY;
        if (rangeX <= 1 && rangeY <= 1) {
            System.out.println(indent + "范围很小(" + rangeX + "x" + rangeY + ")，无需细分，返回: 1");
            return 1;
        }
        
        // 尝试基于二进制位递增的方法，从指定位数开始
        System.out.println(indent + "从 " + startFromBits + " 位开始测试二进制位递增");
        int optimalBits = testBinaryProgression(points, minX, maxX, minY, maxY, maxBitsNeeded, depth, indent, startFromBits);
        
        // 如果找到了可接受的位数，检查是否需要进一步递归
        if (optimalBits > 0) {
            System.out.println(indent + "当前层级找到可接受位数: " + optimalBits + "，检查是否需要递归细分");
            
            // 如果已经达到4位，直接返回，不需要递归
            if (optimalBits >= 4) {
                System.out.println(indent + "✓ 已达到最大位数(4位)，直接返回: " + optimalBits);
                return optimalBits;
            }
            
            // 按找到的最优位数分割空间，对每个单元格递归处理
            Map<Long, List<Point2D>> cellGroups = groupPointsByZOrder(points, minX, maxX, minY, maxY, maxBitsNeeded, optimalBits);
            
            boolean needsRecursion = false;
            int largeGroupsCount = 0;
            for (Map.Entry<Long, List<Point2D>> entry : cellGroups.entrySet()) {
                List<Point2D> cellPoints = entry.getValue();
                if (cellPoints.size() > 4) {
                    largeGroupsCount++;
                    // 测试这个单元格内的点是否能用线性模型拟合
                    if (!canFitWithLinearModel(cellPoints)) {
                        needsRecursion = true;
                        break;
                    }
                }
            }
            
            // 只有当需要递归的组不太多时才递归，避免过度递归
            if (needsRecursion && largeGroupsCount <= Math.max(1, cellGroups.size() / 2)) {
                System.out.println(indent + "发现需要递归细分的单元格，开始递归处理");
                return performRecursiveRefinement(cellGroups, minX, maxX, minY, maxY, maxBitsNeeded, optimalBits, depth, indent);
            } else {
                System.out.println(indent + "✓ 所有单元格都可以用线性模型拟合或递归组过多，选择位数: " + optimalBits);
                return optimalBits;
            }
        }
        
        // 如果没有找到可接受的位数，返回1
        System.out.println(indent + "未找到可接受的位数，使用默认值: 1");
        return 1;
    }
    
    // 测试基于二进制位递增的方法
    private static int testBinaryProgression(Point2D[] points, long minX, long maxX, long minY, long maxY, 
                                           int maxBitsNeeded, int depth, String indent, int startFromBits) {
        int lastGoodBits = Math.max(1, startFromBits - 1);
        
        for (int currentBits = startFromBits; currentBits <= maxBitsNeeded; currentBits++) {
            System.out.println(indent + "--- 测试前 " + currentBits + " 位二进制 ---");
            
            // 为每个数据点计算截取前currentBits位的Z地址
            Map<Long, Integer> zOrderCounts = new HashMap<>();
            
            for (Point2D point : points) {
                // 截取前currentBits位：右移(maxBitsNeeded - currentBits)位
                long truncatedX = point.x >> (maxBitsNeeded - currentBits);
                long truncatedY = point.y >> (maxBitsNeeded - currentBits);
                
                // 计算Z地址
                long zOrder = ZOrderUtils.computeZOrderWithBits(truncatedX, truncatedY, currentBits);
                zOrderCounts.put(zOrder, zOrderCounts.getOrDefault(zOrder, 0) + 1);
            }
            
            // 获取所有不同的Z地址并排序
            List<Long> uniqueZOrders = new ArrayList<>(zOrderCounts.keySet());
            Collections.sort(uniqueZOrders);
            
            System.out.println(indent + "共有 " + uniqueZOrders.size() + " 个不同的Z地址:");
            for (int i = 0; i < uniqueZOrders.size(); i++) {
                long zOrder = uniqueZOrders.get(i);
                String binary = String.format("%" + (currentBits * 2) + "s", Long.toBinaryString(zOrder)).replace(' ', '0');
                System.out.println(indent + "  位置" + i + ": Z地址=" + zOrder + " (二进制:" + binary + ") 包含" + zOrderCounts.get(zOrder) + "个点");
            }
            
            if (uniqueZOrders.size() < 2) {
                System.out.println(indent + "Z地址种类不足，使用上一个位数: " + lastGoodBits);
                return lastGoodBits;
            }
            
            // 进行线性拟合：y = 位置索引, x = Z地址值
            LinearFitResult fitResult = performLinearFitForZOrders(uniqueZOrders);
            
            if (fitResult == null || !fitResult.isValid) {
                System.out.println(indent + "线性拟合失败，使用上一个位数: " + lastGoodBits);
                return lastGoodBits;
            }
            
            // 计算预测误差
            double maxError = calculateMaxError(uniqueZOrders, fitResult);
            
            System.out.println(indent + "最大误差=" + String.format("%.2f", maxError));
            
            // 判断误差是否可接受（阈值设为2）
            boolean errorAcceptable = maxError <= 2.0;
            
            if (errorAcceptable) {
                System.out.println(indent + "✓ 预测质量: 可接受（最大误差≤2个位置）");
                lastGoodBits = currentBits;
                
                // 如果已经达到最大位数（4位），直接返回，不需要继续递归
                if (currentBits >= 4) {
                    System.out.println(indent + "已达到最大位数(4位)，停止测试和递归，返回: " + currentBits);
                    return currentBits;
                }
            } else {
                System.out.println(indent + "✗ 预测质量: 不可接受（最大误差>2个位置）");
                System.out.println(indent + "✓ 回退到上一个可接受位数: " + lastGoodBits + " 位");
                return lastGoodBits;
            }
        }
        
        System.out.println(indent + "✓ 达到最大位数，选择 " + lastGoodBits + " 位");
        return lastGoodBits;
    }
    
    // 计算最大误差
    private static double calculateMaxError(List<Long> uniqueZOrders, LinearFitResult fitResult) {
        double maxError = 0;
        List<Long> sortedZOrders = new ArrayList<>(uniqueZOrders);
        Collections.sort(sortedZOrders);
        
        for (int i = 0; i < uniqueZOrders.size(); i++) {
            long zOrder = uniqueZOrders.get(i);
            int actualPos = sortedZOrders.indexOf(zOrder);
            double predictedPos = fitResult.slope * zOrder + fitResult.intercept;
            double error = Math.abs(predictedPos - actualPos);
            maxError = Math.max(maxError, error);
        }
        return maxError;
    }
    
    // 测试直接线性拟合
    private static int testDirectLinearFit(Point2D[] points, int depth, String indent) {
        // 对于少量点，直接测试能否用线性模型拟合真实坐标
        if (canFitWithLinearModel(Arrays.asList(points))) {
            System.out.println(indent + "✓ 可以直接用线性模型拟合，返回位数: 1");
            return 1;
        } else {
            System.out.println(indent + "✗ 无法直接线性拟合，返回位数: 2");
            return 2;
        }
    }
    
    // 检查是否可以用线性模型拟合
    private static boolean canFitWithLinearModel(List<Point2D> points) {
        if (points.size() <= 2) return true;
        
        // 简单的线性拟合测试：检查点是否大致在一条直线上
        // 计算所有点相对于第一个点的方向向量，看是否大致平行
        Point2D first = points.get(0);
        double[] slopes = new double[points.size() - 1];
        
        for (int i = 1; i < points.size(); i++) {
            Point2D p = points.get(i);
            if (p.x == first.x) {
                slopes[i-1] = Double.POSITIVE_INFINITY; // 垂直线
            } else {
                slopes[i-1] = (double)(p.y - first.y) / (p.x - first.x);
            }
        }
        
        // 检查斜率的变化是否在可接受范围内
        double minSlope = Arrays.stream(slopes).filter(s -> !Double.isInfinite(s)).min().orElse(0);
        double maxSlope = Arrays.stream(slopes).filter(s -> !Double.isInfinite(s)).max().orElse(0);
        
        return Math.abs(maxSlope - minSlope) <= 1.0; // 斜率变化在1以内认为可以线性拟合
    }
    
    // 按Z地址分组点
    private static Map<Long, List<Point2D>> groupPointsByZOrder(Point2D[] points, long minX, long maxX, long minY, long maxY, 
                                                               int maxBitsNeeded, int optimalBits) {
        Map<Long, List<Point2D>> groups = new HashMap<>();
        
        for (Point2D point : points) {
            long truncatedX = point.x >> (maxBitsNeeded - optimalBits);
            long truncatedY = point.y >> (maxBitsNeeded - optimalBits);
            long zOrder = ZOrderUtils.computeZOrderWithBits(truncatedX, truncatedY, optimalBits);
            
            groups.computeIfAbsent(zOrder, k -> new ArrayList<>()).add(point);
        }
        
        return groups;
    }
    
    // 执行递归细分
    private static int performRecursiveRefinement(Map<Long, List<Point2D>> cellGroups, long minX, long maxX, long minY, long maxY,
                                                int maxBitsNeeded, int currentBits, int depth, String indent) {
        int maxRecursiveBits = currentBits;
        int processedCount = 0;
        
        for (Map.Entry<Long, List<Point2D>> entry : cellGroups.entrySet()) {
            Long zOrder = entry.getKey();
            List<Point2D> cellPoints = entry.getValue();
            
            if (cellPoints.size() > 4 && !canFitWithLinearModel(cellPoints)) {
                System.out.println(indent + "递归处理单元格 Z=" + zOrder + " (包含" + cellPoints.size() + "个点)");
                
                // 计算这个单元格的边界
                long cellMinX = cellPoints.stream().mapToLong(p -> p.x).min().orElse(minX);
                long cellMaxX = cellPoints.stream().mapToLong(p -> p.x).max().orElse(maxX);
                long cellMinY = cellPoints.stream().mapToLong(p -> p.y).min().orElse(minY);
                long cellMaxY = cellPoints.stream().mapToLong(p -> p.y).max().orElse(maxY);
                
                // 检查边界是否有效
                if (cellMinX == cellMaxX && cellMinY == cellMaxY) {
                    System.out.println(indent + "单元格边界无效，跳过递归: (" + cellMinX + "," + cellMinY + ")");
                    continue;
                }
                
                // 递归处理这个单元格，从当前位数+1开始
                Point2D[] cellArray = cellPoints.toArray(new Point2D[0]);
                int nextStartBits = currentBits + 1;
                System.out.println(indent + "下一层递归将从 " + nextStartBits + " 位开始");
                int cellBits = recursiveHierarchicalZOrder(cellArray, cellMinX, cellMaxX, cellMinY, cellMaxY, 
                                                         maxBitsNeeded, depth + 1, "单元格Z=" + zOrder, nextStartBits);
                
                maxRecursiveBits = Math.max(maxRecursiveBits, cellBits);
                processedCount++;
                
                // 限制处理的单元格数量，避免过度递归
                if (processedCount >= 5) {
                    System.out.println(indent + "已处理" + processedCount + "个单元格，停止进一步递归");
                    break;
                }
            }
        }
        
        return maxRecursiveBits;
    }
    
    // 递归确定最优位数
    private static int determineOptimalBitsRecursive(Point2D[] points, 
                                                   long minX, long maxX, 
                                                   long minY, long maxY, 
                                                   int currentBits) {
        return determineOptimalBitsRecursiveWithLastGood(points, minX, maxX, minY, maxY, currentBits, -1);
    }
    
    // 递归确定最优位数（带上一个可接受位数记录）
    private static int determineOptimalBitsRecursiveWithLastGood(Point2D[] points,
                                                               long minX, long maxX,
                                                               long minY, long maxY,
                                                               int currentBits,
                                                               int lastGoodBits) {
        
        System.out.println("\n--- 测试 " + currentBits + " 位二进制 ---");
        
        // 1. 将空间分割成 2^currentBits × 2^currentBits 个单元格
        List<Cell> cells = divideSpaceIntoCells(points, minX, maxX, minY, maxY, currentBits);
        
        // 2. 提取有数据的单元格，并计算每个单元格的代表性Z地址
        long maxRange = Math.max(maxX - minX + 1, maxY - minY + 1);
        List<Long> cellZOrders = extractCellZOrders(cells, currentBits, maxRange);
        
        if (cellZOrders.size() < 2) {
            System.out.println("有数据的单元格数量不足(" + cellZOrders.size() + ")，无法评估");
            return Math.max(1, currentBits - 1); // 返回上一个有效的位数
        }
        
        // 2个单元格也可以进行线性拟合（两点确定一条直线）
        if (cellZOrders.size() == 2) {
            System.out.println("2个单元格，可以进行完美线性拟合（两点确定一条直线）");
        }
        
        // 检查数据分布均匀性
        double distributionScore = calculateDistributionScore(cells, currentBits);
        System.out.println("数据分布均匀性得分: " + String.format("%.4f", distributionScore) + " (1.0为完全均匀)");
        
        // 3. 将Z地址转换为数组
        long[] zOrders = new long[cellZOrders.size()];
        for (int i = 0; i < cellZOrders.size(); i++) {
            zOrders[i] = cellZOrders.get(i);
        }
        
        // 4. 尝试线性拟合：拟合Z地址与位置的关系
        System.out.println("进行线性拟合，使用" + currentBits + "位分割，单元格Z地址:");
        for (int i = 0; i < cellZOrders.size(); i++) {
            System.out.println("  单元格" + (i+1) + ": Z地址 = " + zOrders[i]);
        }
        
        // 调试：显示Z-order的二进制表示
        System.out.println("Z地址二进制表示:");
        for (int i = 0; i < cellZOrders.size(); i++) {
            System.out.println("  单元格" + (i+1) + ": " + Long.toBinaryString(zOrders[i]));
        }
        
        LinearFitResult fitResult = performLinearFitZAddresses(cellZOrders);
        
        System.out.println("线性拟合结果: " + fitResult);
        System.out.println("拟合模型: position = " + String.format("%.6f", fitResult.slope) + " × z_address + " + String.format("%.4f", fitResult.intercept));
        
        // 计算预测误差并获取评估结果
        boolean errorAcceptable = calculatePredictionErrors(cellZOrders, fitResult);
        
        // 5. 判断是否继续递归
        int theoreticalMaxBits = (int) Math.ceil(0.5 * (Math.log(points.length) / Math.log(2)));
        theoreticalMaxBits = Math.max(theoreticalMaxBits, 1);
        int maxNeededBits = Math.min(theoreticalMaxBits + 2, 8); // 与主函数保持一致
        
        System.out.println("数据点数量: " + points.length + ", 每维最大尝试位数: " + maxNeededBits);
        
        // 判断是否继续递归
        boolean shouldContinue = errorAcceptable && (currentBits < maxNeededBits);
        
        if (shouldContinue) {
            // 当前位数误差可接受，记录为lastGoodBits，继续尝试下一位数
            return determineOptimalBitsRecursiveWithLastGood(points, minX, maxX, minY, maxY, currentBits + 1, currentBits);
        } else {
            String reason = "";
            if (!errorAcceptable) {
                reason += "预测误差>3个位置 ";
                // 如果当前位数误差不可接受，但有上一个可接受的位数，则回退
                if (lastGoodBits > 0) {
                    System.out.println("✓ 误差过大，回退到上一个可接受位数: " + lastGoodBits + " 位 (" + reason.trim() + ")");
                    return lastGoodBits;
                } else {
                    // 没有可接受的位数，使用当前位数（通常是1位的情况）
                    System.out.println("✓ 选择每维 " + currentBits + " 位作为最优位数 (" + reason.trim() + ")");
                    return currentBits;
                }
            }
            if (currentBits >= maxNeededBits) reason += "达到最大位数 ";
            
            System.out.println("✓ 选择每维 " + currentBits + " 位作为最优位数 (" + reason.trim() + ")");
            return currentBits;
        }
    }
    
    // 将空间分割成单元格
    private static List<Cell> divideSpaceIntoCells(Point2D[] points, 
                                                 long minX, long maxX, 
                                                 long minY, long maxY, 
                                                 int bits) {
        
        int cellCount = 1 << bits; // 2^bits
        long rangeX = maxX - minX + 1;
        long rangeY = maxY - minY + 1;
        
        // 确保单元格大小至少为1，避免除零和过度细分
        long cellWidth = Math.max(1, rangeX / cellCount);
        long cellHeight = Math.max(1, rangeY / cellCount);
        
        // 如果数据范围小于单元格数量，调整单元格数量
        if (rangeX < cellCount || rangeY < cellCount) {
            cellCount = (int) Math.min(cellCount, Math.min(rangeX, rangeY));
            cellWidth = Math.max(1, rangeX / cellCount);
            cellHeight = Math.max(1, rangeY / cellCount);
            System.out.println("调整单元格数量: " + cellCount + "×" + cellCount + 
                             " (原因: 数据范围" + rangeX + "×" + rangeY + "小于理论单元格数)");
        }
        
        // 创建单元格网格
        Cell[][] cellGrid = new Cell[cellCount][cellCount];
        for (int i = 0; i < cellCount; i++) {
            for (int j = 0; j < cellCount; j++) {
                long cellMinX = minX + i * cellWidth;
                long cellMaxX = minX + (i + 1) * cellWidth - 1;
                long cellMinY = minY + j * cellHeight;
                long cellMaxY = minY + (j + 1) * cellHeight - 1;
                
                cellGrid[i][j] = new Cell(cellMinX, cellMaxX, cellMinY, cellMaxY);
            }
        }
        
        // 将点分配到单元格
        for (Point2D point : points) {
            int cellX = (int) ((point.x - minX) / cellWidth);
            int cellY = (int) ((point.y - minY) / cellHeight);
            
            // 边界处理
            cellX = Math.min(cellX, cellCount - 1);
            cellY = Math.min(cellY, cellCount - 1);
            
            cellGrid[cellX][cellY].addPoint(point);
        }
        
        // 收集所有单元格
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < cellCount; i++) {
            for (int j = 0; j < cellCount; j++) {
                cells.add(cellGrid[i][j]);
            }
        }
        
        int cellsWithData = (int) cells.stream().mapToInt(c -> c.hasData() ? 1 : 0).sum();
        System.out.println("分割成 " + cellCount + "×" + cellCount + " = " + (cellCount * cellCount) + " 个单元格");
        System.out.println("有数据的单元格: " + cellsWithData);
        
        return cells;
    }
    
    // 提取有数据单元格的中点
    private static List<Point2D> extractCenters(List<Cell> cells) {
        List<Point2D> centers = new ArrayList<>();
        
        for (Cell cell : cells) {
            if (cell.hasData()) {
                Point2D center = cell.getCenter();
                centers.add(center);
                System.out.println("  单元格中心: " + center + " (包含 " + cell.points.size() + " 个点)");
            }
        }
        
        return centers;
    }
    
    // 提取有数据单元格的Z地址
    private static List<Long> extractCellZOrders(List<Cell> cells, int bits, long maxRange) {
        List<Long> zOrders = new ArrayList<>();
        
        // 计算数据范围需要的位数
        int rangeBits = (int) Math.ceil(Math.log(maxRange) / Math.log(2));
        
        for (Cell cell : cells) {
            if (cell.hasData()) {
                // 直接使用单元格的索引坐标计算Z地址，不需要位移操作
                // 因为我们已经通过单元格分割得到了正确的坐标范围
                
                // 计算单元格在网格中的索引
                long cellsPerDim = 1L << bits;
                long cellWidth = Math.max(1, maxRange / cellsPerDim);
                long cellHeight = cellWidth; // 假设正方形单元格
                
                long cellIndexX = cell.minX / cellWidth;
                long cellIndexY = cell.minY / cellHeight;
                
                // 确保索引在有效范围内
                cellIndexX = Math.min(cellIndexX, cellsPerDim - 1);
                cellIndexY = Math.min(cellIndexY, cellsPerDim - 1);
                
                // 用单元格索引计算Z地址
                long cellZOrder = ZOrderUtils.computeZOrderWithBits(cellIndexX, cellIndexY, bits);
                
                zOrders.add(cellZOrder);
                System.out.println("  单元格" + cell + " -> 索引坐标(" + cellIndexX + "," + cellIndexY + ") -> Z地址: " + cellZOrder + " (包含 " + cell.points.size() + " 个点)");
            }
        }
        
        return zOrders;
    }
    
    // 计算数据分布的均匀性得分
    private static double calculateDistributionScore(List<Cell> cells, int bits) {
        int totalCells = 1 << (2 * bits); // 2^(2*bits)
        int cellsWithData = (int) cells.stream().mapToInt(c -> c.hasData() ? 1 : 0).sum();
        int totalPoints = cells.stream().mapToInt(c -> c.points.size()).sum();
        
        if (cellsWithData == 0) return 0.0;
        
        // 计算理想的每个单元格点数
        double idealPointsPerCell = (double) totalPoints / cellsWithData;
        
        // 计算方差（衡量分布不均匀程度）
        double variance = 0.0;
        int validCells = 0;
        
        for (Cell cell : cells) {
            if (cell.hasData()) {
                double deviation = cell.points.size() - idealPointsPerCell;
                variance += deviation * deviation;
                validCells++;
            }
        }
        
        if (validCells == 0) return 0.0;
        variance /= validCells;
        
        // 转换为0-1的得分，方差越小得分越高
        double maxVariance = idealPointsPerCell * idealPointsPerCell; // 最大可能方差
        double uniformityScore = Math.max(0.0, 1.0 - Math.sqrt(variance / maxVariance));
        
        // 考虑空间利用率
        double spaceUtilization = (double) cellsWithData / totalCells;
        
        // 综合得分
        return uniformityScore * 0.7 + spaceUtilization * 0.3;
    }
    
    // 计算预测误差分析，返回误差是否可接受
    private static boolean calculatePredictionErrors(List<Long> zAddresses, LinearFitResult fitResult) {
        List<Long> sortedZAddresses = new ArrayList<>(zAddresses);
        Collections.sort(sortedZAddresses);
        
        double maxError = 0;
        double totalError = 0;
        
        // 只显示前5个和后5个误差，避免输出过多
        boolean showDetails = zAddresses.size() <= 20;
        if (showDetails) {
            System.out.println("预测误差分析:");
        }
        
        for (int i = 0; i < zAddresses.size(); i++) {
            long zAddr = zAddresses.get(i);
            int actualPosition = sortedZAddresses.indexOf(zAddr);
            double predictedPosition = fitResult.slope * zAddr + fitResult.intercept;
            double error = Math.abs(predictedPosition - actualPosition);
            
            maxError = Math.max(maxError, error);
            totalError += error;
            
            if (showDetails || i < 5 || i >= zAddresses.size() - 5) {
            System.out.println("  Z地址" + zAddr + ": 实际位置=" + actualPosition + 
                             ", 预测位置=" + String.format("%.2f", predictedPosition) + 
                             ", 误差=" + String.format("%.2f", error));
            } else if (i == 5) {
                System.out.println("  ... (省略中间 " + (zAddresses.size() - 10) + " 个误差) ...");
            }
        }
        
        double avgError = totalError / zAddresses.size();
        System.out.println("误差统计: 平均误差=" + String.format("%.2f", avgError) + 
                         ", 最大误差=" + String.format("%.2f", maxError));
        
        // 评估预测质量（误差容忍度设为3个位置）
        boolean acceptable = maxError <= 3.0;
        
        if (maxError <= 1.0) {
            System.out.println("✓ 预测质量: 优秀（最大误差≤1个位置）");
        } else if (maxError <= 2.0) {
            System.out.println("○ 预测质量: 良好（最大误差≤2个位置）");
        } else if (maxError <= 3.0) {
            System.out.println("△ 预测质量: 可接受（最大误差≤3个位置）");
        } else {
            System.out.println("✗ 预测质量: 较差（最大误差>3个位置，不可接受）");
        }
        
        return acceptable;
    }
    
    // 执行线性拟合：拟合Z地址与位置的关系
    private static LinearFitResult performLinearFitZOrder(List<Point2D> centers, long[] zOrders) {
        int n = centers.size();
        if (n < 3) {
            return new LinearFitResult(0, 0, 0);
        }
        
        // 将二维位置映射到一维位置索引
        // 方法1：使用空间填充曲线的顺序作为位置索引
        List<Point2D> sortedCenters = new ArrayList<>(centers);
        sortedCenters.sort((p1, p2) -> {
            // 按Z-order排序，这样位置索引就反映了空间填充曲线的顺序
            long z1 = ZOrderUtils.computeZOrderWithBits(p1, 16); // 使用足够多的位数
            long z2 = ZOrderUtils.computeZOrderWithBits(p2, 16);
            return Long.compare(z1, z2);
        });
        
        // 计算线性回归：位置索引 vs Z-order值
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, sumYY = 0;
        
        for (int i = 0; i < n; i++) {
            // 找到当前中心点在排序后的位置索引
            int positionIndex = sortedCenters.indexOf(centers.get(i));
            double x = positionIndex; // 位置索引作为x坐标
            double y = zOrders[i]; // Z-order值作为y坐标
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
            sumYY += y * y;
        }
        
        // 计算斜率和截距
        double denominator = n * sumXX - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return new LinearFitResult(0, 0, 0);
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        // 计算决定系数 R²
        double yMean = sumY / n;
        double ssTotal = 0, ssResidual = 0;
        
        for (int i = 0; i < n; i++) {
            int positionIndex = sortedCenters.indexOf(centers.get(i));
            double x = positionIndex;
            double y = zOrders[i];
            double yPredicted = slope * x + intercept;
            
            ssTotal += (y - yMean) * (y - yMean);
            ssResidual += (y - yPredicted) * (y - yPredicted);
        }
        
        // 处理除零情况
        double rSquared;
        if (Math.abs(ssTotal) < 1e-10) {
            rSquared = 0;
        } else {
            rSquared = 1 - (ssResidual / ssTotal);
        }
        
        return new LinearFitResult(slope, intercept, rSquared);
    }
    
    // 执行线性拟合：拟合Z地址与位置索引的关系
    private static LinearFitResult performLinearFitZAddresses(List<Long> zAddresses) {
        int n = zAddresses.size();
        if (n < 2) {
            return new LinearFitResult(0, 0, 0);
        }
        
        // 按Z地址排序，确定位置索引
        List<Long> sortedZAddresses = new ArrayList<>(zAddresses);
        Collections.sort(sortedZAddresses);
        
        System.out.println("按Z地址排序后的位置:");
        for (int i = 0; i < sortedZAddresses.size(); i++) {
            System.out.println("  位置" + i + ": Z地址 = " + sortedZAddresses.get(i));
        }
        
        if (n == 2) {
            System.out.println("注意：2个点的线性拟合总是完美的（R²=1.0），因为两点确定一条直线");
        }
        
        // 计算线性回归：Z地址值 vs 位置索引
        // 目标：position = a × z_address + b（用Z地址预测位置）
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        
        for (int i = 0; i < n; i++) {
            // 找到当前Z地址在排序后的位置索引
            int positionIndex = sortedZAddresses.indexOf(zAddresses.get(i));
            double x = zAddresses.get(i); // Z地址值作为x坐标（自变量）
            double y = positionIndex; // 位置索引作为y坐标（因变量）
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        // 计算斜率和截距
        double denominator = n * sumXX - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return new LinearFitResult(0, 0, 0);
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        // 计算决定系数 R²
        double yMean = sumY / n;
        double ssTotal = 0, ssResidual = 0;
        
        for (int i = 0; i < n; i++) {
            int positionIndex = sortedZAddresses.indexOf(zAddresses.get(i));
            double x = zAddresses.get(i); // Z地址值
            double y = positionIndex; // 实际位置
            double yPredicted = slope * x + intercept; // 预测位置
            
            ssTotal += (y - yMean) * (y - yMean);
            ssResidual += (y - yPredicted) * (y - yPredicted);
        }
        
        double rSquared = Math.abs(ssTotal) < 1e-10 ? 0 : 1 - (ssResidual / ssTotal);
        
        return new LinearFitResult(slope, intercept, rSquared);
    }
    
    // 执行线性拟合（保留原方法作为备用）
    private static LinearFitResult performLinearFit(List<Point2D> centers, long[] zOrders) {
        int n = centers.size();
        if (n < 3) {
            return new LinearFitResult(0, 0, 0);
        }
        
        // 计算线性回归 - 使用空间坐标而不是索引
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, sumYY = 0;
        
        for (int i = 0; i < n; i++) {
            // 使用空间坐标的某种组合作为x坐标
            // 这里我们使用x坐标作为x，y坐标作为y，Z-order作为要拟合的目标
            double x = centers.get(i).x; // 使用实际x坐标
            double y = zOrders[i]; // Z-order值作为y坐标
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
            sumYY += y * y;
        }
        
        // 计算斜率和截距
        double denominator = n * sumXX - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return new LinearFitResult(0, 0, 0);
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        // 计算决定系数 R²
        double yMean = sumY / n;
        double ssTotal = 0, ssResidual = 0;
        
        for (int i = 0; i < n; i++) {
            double x = centers.get(i).x;
            double y = zOrders[i];
            double yPredicted = slope * x + intercept;
            
            ssTotal += (y - yMean) * (y - yMean);
            ssResidual += (y - yPredicted) * (y - yPredicted);
        }
        
        // 处理除零情况
        double rSquared;
        if (Math.abs(ssTotal) < 1e-10) {
            // 如果所有y值都相同，R²为0（无变化）
            rSquared = 0;
        } else {
            rSquared = 1 - (ssResidual / ssTotal);
        }
        
        return new LinearFitResult(slope, intercept, rSquared);
    }
    
    // 为新算法专门的线性拟合方法
    private static LinearFitResult performLinearFitForZOrders(List<Long> uniqueZOrders) {
        int n = uniqueZOrders.size();
        if (n < 2) {
            return new LinearFitResult(0, 0, 0);
        }
        
        // 按Z地址排序，确定位置索引
        List<Long> sortedZOrders = new ArrayList<>(uniqueZOrders);
        Collections.sort(sortedZOrders);
        
        // 计算线性回归：Z地址值 vs 位置索引
        // 目标：position = a × z_address + b（用Z地址预测位置）
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        
        for (int i = 0; i < n; i++) {
            // 找到当前Z地址在排序后的位置索引
            int positionIndex = sortedZOrders.indexOf(uniqueZOrders.get(i));
            double x = uniqueZOrders.get(i); // Z地址值作为x坐标（自变量）
            double y = positionIndex; // 位置索引作为y坐标（因变量）
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        // 计算斜率和截距
        double denominator = n * sumXX - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return new LinearFitResult(0, 0, 0);
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        // 计算决定系数 R²
        double yMean = sumY / n;
        double ssTotal = 0, ssResidual = 0;
        
        for (int i = 0; i < n; i++) {
            int positionIndex = sortedZOrders.indexOf(uniqueZOrders.get(i));
            double x = uniqueZOrders.get(i); // Z地址值
            double y = positionIndex; // 实际位置
            double yPredicted = slope * x + intercept; // 预测位置
            
            ssTotal += (y - yMean) * (y - yMean);
            ssResidual += (y - yPredicted) * (y - yPredicted);
        }
        
        double rSquared = Math.abs(ssTotal) < 1e-10 ? 0 : 1 - (ssResidual / ssTotal);
        
        return new LinearFitResult(slope, intercept, rSquared);
    }
    
    // 测试方法
    public static void main(String[] args) {
        System.out.println("=== 使用16×16网格数据集测试自适应Z-order算法 ===\n");
        
        // // 测试8×8网格稀疏分布数据集
        // Point2D[] sparse8x8Dataset = loadDataset("src/data/grid_8x8_sparse_dataset.csv", 50);
        
        // if (sparse8x8Dataset != null) {
        //     System.out.println("=== 8×8网格稀疏分布数据集测试 ===");
        //     testDataset(sparse8x8Dataset, "8×8网格稀疏分布数据集");
        // }
        
        // 测试10×10网格稀疏分布数据集
        Point2D[] sparse10x10Dataset = loadDataset("src/data/grid_10x10_sparse_dataset.csv", 80);
        
        if (sparse10x10Dataset != null) {
            System.out.println("\n=== 10×10网格稀疏分布数据集测试 ===");
            testDataset(sparse10x10Dataset, "10×10网格稀疏分布数据集");
        } else {
            System.out.println("无法加载10×10网格稀疏分布数据集");
        }
        
        // // 测试8×8网格均匀分布数据集
        // Point2D[] uniform8x8Dataset = loadDataset("src/data/grid_8x8_uniform_dataset.csv", 50);
        
        // if (uniform8x8Dataset != null) {
        //     System.out.println("\n=== 8×8网格均匀分布数据集测试 ===");
        //     testDataset(uniform8x8Dataset, "8×8网格均匀分布数据集");
        // }
    }
    
    // 测试数据集的方法
    private static void testDataset(Point2D[] dataset, String datasetName) {
        System.out.println("数据集: " + datasetName);
        System.out.println("数据点数量: " + dataset.length);
        
        // 显示数据范围
        long minX = Arrays.stream(dataset).mapToLong(p -> p.x).min().orElse(0);
        long maxX = Arrays.stream(dataset).mapToLong(p -> p.x).max().orElse(0);
        long minY = Arrays.stream(dataset).mapToLong(p -> p.y).min().orElse(0);
        long maxY = Arrays.stream(dataset).mapToLong(p -> p.y).max().orElse(0);
        
        System.out.println("数据范围: X[" + minX + ", " + maxX + "], Y[" + minY + ", " + maxY + "]");
        System.out.println("数据范围大小: X=" + (maxX - minX) + ", Y=" + (maxY - minY));
        
        // 显示前5个数据点
        System.out.println("前5个数据点:");
        for (int i = 0; i < Math.min(5, dataset.length); i++) {
            System.out.println("  点" + (i+1) + ": " + dataset[i]);
        }
        
        // 运行自适应算法
        long startTime = System.nanoTime();
        int optimalBits = determineOptimalBits(dataset);
        long endTime = System.nanoTime();
        
        System.out.println("\n=== 结果分析 ===");
        System.out.println("最优位数: " + optimalBits);
        System.out.println("计算时间: " + (endTime - startTime) / 1000000.0 + "ms");
        
        // 验证Z-order编码效果
        System.out.println("\n=== Z-order编码验证 ===");
        System.out.println("使用" + optimalBits + "位编码的前5个点:");
        for (int i = 0; i < Math.min(5, dataset.length); i++) {
            long zOrder = ZOrderUtils.computeZOrderWithBits(dataset[i], optimalBits);
            System.out.println("  点" + dataset[i] + " -> Z-order: " + zOrder);
        }
        
        // 测试不同位数的效果（每维位数，按自适应上限）
        int maxBitsForReport = Math.max(1, optimalBits);
        System.out.println("\n=== 不同每维位数的Z-order范围对比 (1.." + maxBitsForReport + ") ===");
        for (int bits = 1; bits <= maxBitsForReport; bits++) {
            long minZ = Long.MAX_VALUE;
            long maxZ = Long.MIN_VALUE;
            
            for (Point2D point : dataset) {
                long zOrder = ZOrderUtils.computeZOrderWithBits(point, bits);
                minZ = Math.min(minZ, zOrder);
                maxZ = Math.max(maxZ, zOrder);
            }
            
            System.out.println("  " + bits + "位: Z-order范围[" + minZ + ", " + maxZ + "], 范围大小=" + (maxZ - minZ));
        }
    }
    
    // 加载数据集的方法
    private static Point2D[] loadDataset(String filename, int maxPoints) {
        List<Point2D> points = new ArrayList<>();
        
        // 尝试多个可能的路径
        String[] possiblePaths = {
            filename,
            "src/" + filename,
            "src/src/" + filename.replace("src/", ""),
            System.getProperty("user.dir") + "/" + filename,
            System.getProperty("user.dir") + "/src/" + filename,
            System.getProperty("user.dir") + "/src/src/" + filename.replace("src/", "")
        };
        
        java.io.BufferedReader br = null;
        String actualFilename = null;
        
        // 尝试每个可能的路径
        for (String path : possiblePaths) {
            try {
                br = new java.io.BufferedReader(new java.io.FileReader(path));
                actualFilename = path;
                System.out.println("成功打开文件: " + path);
                break;
            } catch (java.io.IOException e) {
                // 继续尝试下一个路径
                continue;
            }
        }
        
        if (br == null) {
            System.err.println("读取文件错误: 尝试了所有可能的路径都无法找到文件 " + filename);
            System.err.println("当前工作目录: " + System.getProperty("user.dir"));
            return null;
        }
        
        try {
            String line;
            int count = 0;
            
            while ((line = br.readLine()) != null && count < maxPoints) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
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
