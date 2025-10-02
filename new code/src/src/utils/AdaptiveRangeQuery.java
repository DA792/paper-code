package utils;

import index.PVL2D_tree_index.Point2D;
import utils.AdaptiveZOrder.*;
import java.util.*;

/**
 * 基于学习索引的自适应Z-order空间范围查询系统
 * 实现了您提出的查询思路：
 * 1. 给定查询矩形的左下角和右上角坐标
 * 2. 从根节点开始，使用最优位数计算查询边界的Z地址
 * 3. 通过学习模型预测起始位置
 * 4. 使用二分搜索在预测位置±误差范围内找到精确位置
 */
public class AdaptiveRangeQuery {
    
    private AdaptiveZOrderTree tree;
    private boolean enableDebugOutput = false; // 性能优化：控制调试输出
    
    public AdaptiveRangeQuery(AdaptiveZOrderTree tree) {
        this.tree = tree;
    }
    
    /**
     * 设置是否启用调试输出（影响性能）
     */
    public void setDebugOutput(boolean enable) {
        this.enableDebugOutput = enable;
    }
    
    /**
     * 执行范围查询
     * @param bottomLeft 查询矩形的左下角点
     * @param topRight 查询矩形的右上角点
     * @return 查询结果
     */
    public RangeQueryResult rangeQuery(Point2D bottomLeft, Point2D topRight) {
        if (tree == null || tree.getRoot() == null) {
            return new RangeQueryResult();
        }
        
        System.out.println("\n=== 执行基于学习索引的范围查询 ===");
        System.out.println("查询范围: 左下角" + bottomLeft + " -> 右上角" + topRight);
        
        long queryStartTime = System.nanoTime();
        
        // 从根节点开始，在每一层使用该层的最优位数计算Z地址
        AdaptiveZOrderNode root = tree.getRoot();
        
        System.out.println("采用分层计算策略：在每一层使用该层的最优位数计算Z地址");
        System.out.println("根节点最优位数: " + root.optimalBits + " 位");
        
        RangeQueryResult result = new RangeQueryResult();
        
        // 从根节点开始递归查询，动态计算每层的Z地址
        performLayeredRangeQuery(root, bottomLeft, topRight, result, 0, tree);
        
        long queryEndTime = System.nanoTime();
        result.setQueryTime((queryEndTime - queryStartTime) / 1000000); // 转换为毫秒
        
        System.out.println("查询完成: " + result);
        return result;
    }
    
    /**
     * 分层范围查询：在每一层使用该层的最优位数计算Z地址
     * 核心思想：每个节点都有自己的最优位数，查询时需要根据当前节点的最优位数重新计算Z地址范围
     */
    private void performLayeredRangeQuery(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                        RangeQueryResult result, int depth, AdaptiveZOrderTree tree) {
        if (node == null) return;
        
        String indent = "  ".repeat(depth);
        if (enableDebugOutput) {
            System.out.println(indent + "=== 第" + depth + "层查询 ===");
            System.out.println(indent + "查询节点: " + node);
            System.out.println(indent + "节点存储信息: " + node.getStorageInfo());
        }
        result.addNodeAccess();
        
        // 关键步骤：使用当前节点的最优位数重新计算Z地址范围
        // 1. 获取数据集的全局最大位数
        int maxBitsInDataset = calculateMaxBitsNeeded(tree.getRoot());
        
        // 2. 使用当前节点的最优位数计算查询范围的Z地址
        // 这里实现您的逻辑：先扩展到最大位数，再截取高位到当前节点的最优位数
        long queryMinZ = computeAdaptiveZAddress(bottomLeft, maxBitsInDataset, node.optimalBits, "左下角");
        long queryMaxZ = computeAdaptiveZAddress(topRight, maxBitsInDataset, node.optimalBits, "右上角");
        
        System.out.println(indent + "  数据集最大位数: " + maxBitsInDataset + " 位");
        System.out.println(indent + "  当前节点最优位数: " + node.optimalBits + " 位");
        System.out.println(indent + "  自适应Z地址范围: [" + queryMinZ + ", " + queryMaxZ + "]");
        
        // 3. 根据节点类型执行相应的查询策略
        if (node.isLeaf) {
            // 叶子节点：执行类似PVL-tree的学习索引查询
            performPVLStyleLeafQuery(node, bottomLeft, topRight, queryMinZ, queryMaxZ, result, indent);
        } else {
            // 内部节点：确定需要递归查询的子节点
            performPVLStyleInternalQuery(node, bottomLeft, topRight, result, indent, depth, tree);
        }
    }
    
    /**
     * 计算自适应Z地址：先扩展到最大位数，再截取高位到指定位数
     */
    private long computeAdaptiveZAddress(Point2D point, int maxBits, int optimalBits, String label) {
        // 优化版本：减少调试输出以提高性能
        // System.out.println("    " + label + " " + point + " 的Z地址计算:");
        
        // 步骤1：扩展坐标到最大位数（跳过字符串格式化以提高性能）
        // String xMaxBin = String.format("%" + maxBits + "s", Long.toBinaryString(point.x)).replace(' ', '0');
        // String yMaxBin = String.format("%" + maxBits + "s", Long.toBinaryString(point.y)).replace(' ', '0');
        // System.out.println("      扩展到" + maxBits + "位: x=" + xMaxBin + "(" + point.x + "), y=" + yMaxBin + "(" + point.y + ")");
        
        // 步骤2：截取高位到最优位数
        long xTruncated = point.x >> (maxBits - optimalBits);
        long yTruncated = point.y >> (maxBits - optimalBits);
        
        // 跳过调试输出以提高性能
        // String xTruncBin = String.format("%" + optimalBits + "s", Long.toBinaryString(xTruncated)).replace(' ', '0');
        // String yTruncBin = String.format("%" + optimalBits + "s", Long.toBinaryString(yTruncated)).replace(' ', '0');
        // System.out.println("      截取前" + optimalBits + "位: x=" + xTruncBin + "(" + xTruncated + "), y=" + yTruncBin + "(" + yTruncated + ")");
        
        // 步骤3：使用ZOrderUtils计算Z地址（从高位开始交错）
        long zAddress = utils.ZOrderUtils.computeZOrderWithBits(xTruncated, yTruncated, optimalBits);
        // System.out.println("      最终Z地址: " + zAddress);
        
        return zAddress;
    }
    
    /**
     * PVL-tree风格的叶子节点查询
     * 实现类似Algorithm 1中的叶子节点处理逻辑
     * 简化版：只检查Z地址范围，不进行空间坐标解码验证
     */
    private void performPVLStyleLeafQuery(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                        long ql, long qr, RangeQueryResult result, String indent) {
        System.out.println(indent + "  执行PVL风格叶子节点查询...");
        System.out.println(indent + "  查询范围: [" + ql + ", " + qr + "]");
        
        if (!node.hasLearnedModel || node.zOrderArray == null) {
            System.out.println(indent + "  节点没有学习模型，跳过查询");
            return;
        }
        
        // 新增：利用叶子节点存储的二进制位模式进行空间范围优化
        if (!node.xBinaryPattern.isEmpty() && !node.yBinaryPattern.isEmpty()) {
            int maxBitsInDataset = calculateMaxBitsNeeded(node);
            
            // 计算相交区域
            long[] intersectionRange = node.calculateIntersectionRange(bottomLeft.x, topRight.x, 
                                                                     bottomLeft.y, topRight.y, 
                                                                     maxBitsInDataset);
            
            if (intersectionRange == null) {
                long[] nodeRange = node.calculateLeafNodeSpatialRange(maxBitsInDataset);
                System.out.println(indent + "  ✗ 叶子节点空间范围预筛选: 不相交，跳过");
                System.out.println(indent + "    节点空间范围: X[" + nodeRange[0] + "," + nodeRange[1] + 
                                 "] Y[" + nodeRange[2] + "," + nodeRange[3] + "]");
                System.out.println(indent + "    查询矩形: X[" + bottomLeft.x + "," + topRight.x + 
                                 "] Y[" + bottomLeft.y + "," + topRight.y + "]");
                return; // 空间范围不相交，直接跳过
            } else {
                // 基于相交区域计算更精确的Z地址范围
                long[] intersectionZRange = node.calculateIntersectionZRange(intersectionRange, maxBitsInDataset);
                if (intersectionZRange != null) {
                    // 使用相交区域的Z地址范围替换原始查询范围
                    ql = Math.max(ql, intersectionZRange[0]);
                    qr = Math.min(qr, intersectionZRange[1]);
                    
                    System.out.println(indent + "  ✓ 叶子节点空间范围优化: 相交区域精确化");
                    System.out.println(indent + "    相交区域: X[" + intersectionRange[0] + "," + intersectionRange[1] + 
                                     "] Y[" + intersectionRange[2] + "," + intersectionRange[3] + "]");
                    System.out.println(indent + "    优化后Z地址范围: [" + ql + ", " + qr + "]");
                    System.out.println(indent + "    二进制模式: x:" + node.xBinaryPattern + " y:" + node.yBinaryPattern);
                }
            }
        }
        
        // 步骤1：使用学习模型预测起始位置 (类似Algorithm 1 line 1)
        int predictedPos = predictStartPosition(node, ql, indent);
        
        // 步骤2：初始化边界位置 (类似Algorithm 1 line 2)
        int pl = predictedPos;
        int pr = node.zOrderArray.length - 1;
        
        System.out.println(indent + "    预测起始位置: " + predictedPos);
        System.out.println(indent + "    初始边界: pl=" + pl + ", pr=" + pr);
        
        // 步骤3：主循环扫描 (类似Algorithm 1 line 3-15)
        int p = pl;
        while (p < node.zOrderArray.length) {
            long kp = node.zOrderArray[p]; // 当前位置的Z地址
            
            // 检查Z地址是否在查询范围内 (类似Algorithm 1 line 5-6)
            if (kp >= ql && kp <= qr) {
                // Z地址在范围内，直接添加对应的数据点
                Point2D dataPoint = node.sortedPoints[p];
                result.addPoint(dataPoint);
                // 注释掉频繁的调试输出以提高性能
                // System.out.println(indent + "    ✓ 找到匹配Z地址: " + dataPoint + " (Z=" + kp + ")");
            }
            
            // 如果超出查询上界，退出循环 (类似Algorithm 1 line 7-8)
            if (kp >= qr) {
                System.out.println(indent + "    到达查询上界，退出循环");
                break;
            }
            
            p++;
        }
        
        System.out.println(indent + "    叶子节点查询完成，找到 " + result.points.size() + " 个匹配点");
    }
    
    /**
     * PVL-tree风格的内部节点查询
     * 实现类似Algorithm 1中的内部节点处理逻辑
     */
    private void performPVLStyleInternalQuery(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                            RangeQueryResult result, String indent, int depth, AdaptiveZOrderTree tree) {
        System.out.println(indent + "  执行PVL风格内部节点查询...");
        
        // 对每个子节点进行检查和递归查询
        for (int i = 0; i < node.children.size(); i++) {
            AdaptiveZOrderNode child = node.children.get(i);
            
            // 检查子节点是否可能包含查询结果
            if (childMayContainResults(child, bottomLeft, topRight)) {
                System.out.println(indent + "    递归查询子节点 " + i + ": " + child);
                // 递归查询子节点 (类似Algorithm 1 line 11)
                performLayeredRangeQuery(child, bottomLeft, topRight, result, depth + 1, tree);
            } else {
                System.out.println(indent + "    跳过子节点 " + i + " (不包含查询结果)");
            }
        }
    }
    
    /**
     * 使用学习模型预测起始位置
     */
    private int predictStartPosition(AdaptiveZOrderNode node, long queryZ, String indent) {
        if (!node.hasLearnedModel || node.fitResult == null) {
            return 0;
        }
        
        // 使用线性模型预测位置: position = slope * z_address + intercept
        double predictedPos = node.fitResult.slope * queryZ + node.fitResult.intercept;
        int pos = Math.max(0, Math.min((int)Math.round(predictedPos), node.zOrderArray.length - 1));
        
        System.out.println(indent + "    学习模型预测: position = " + 
                         String.format("%.4f", node.fitResult.slope) + " × " + queryZ + " + " + 
                         String.format("%.4f", node.fitResult.intercept) + " = " + 
                         String.format("%.2f", predictedPos) + " → " + pos);
        
        return pos;
    }
    
    /**
     * 检查子节点是否可能包含查询结果
     */
    private boolean childMayContainResults(AdaptiveZOrderNode child, Point2D bottomLeft, Point2D topRight) {
        // 检查子节点的空间边界是否与查询矩形相交
        return !(child.maxX < bottomLeft.x || child.minX > topRight.x ||
                 child.maxY < bottomLeft.y || child.minY > topRight.y);
    }
    
    /**
     * 计算数据集需要的最大位数
     */
    private int calculateMaxBitsNeeded(AdaptiveZOrderNode node) {
        // 向上遍历到根节点，获取全局最大位数
        AdaptiveZOrderNode root = node;
        while (root.parent != null) {
            root = root.parent;
        }
        
        // 计算根节点空间范围需要的位数
        long maxCoord = Math.max(root.maxX, root.maxY);
        return (int) Math.ceil(Math.log(maxCoord + 1) / Math.log(2));
    }
    
    /**
     * 递归执行范围查询（保留原方法作为备用）
     */
    private void performRangeQuery(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                 long queryMinZ, long queryMaxZ, RangeQueryResult result, int depth) {
        if (node == null) return;
        
        String indent = "  ".repeat(depth);
        System.out.println(indent + "查询节点: " + node);
        result.addNodeAccess();
        
        if (node.isLeaf) {
            // 叶子节点：使用学习模型进行精确查询
            queryLeafNode(node, bottomLeft, topRight, queryMinZ, queryMaxZ, result, indent);
        } else {
            // 内部节点：确定需要查询的子节点
            queryInternalNode(node, bottomLeft, topRight, queryMinZ, queryMaxZ, result, indent, depth);
        }
    }
    
    /**
     * 使用当前层Z地址查询叶子节点
     */
    private void queryLeafNodeWithLayerZ(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                       long queryMinZ, long queryMaxZ, RangeQueryResult result, String indent) {
        
        if (!node.hasLearnedModel || node.zOrderArray == null) {
            System.out.println(indent + "  叶子节点无学习模型，执行线性扫描");
            linearScanLeafNode(node, bottomLeft, topRight, result);
            return;
        }
        
        System.out.println(indent + "  使用学习模型查询叶子节点 (使用" + node.optimalBits + "位Z地址)");
        System.out.println(indent + "    节点包含 " + node.zOrderArray.length + " 个数据点");
        System.out.println(indent + "    查询Z地址范围: [" + queryMinZ + ", " + queryMaxZ + "]");
        
        // 步骤1: 使用学习模型预测查询范围的起始和结束位置
        int startPos = predictPosition(node, queryMinZ, "起始", indent);
        int endPos = predictPosition(node, queryMaxZ, "结束", indent);
        
        // 确保范围有效
        if (startPos > endPos) {
            int temp = startPos;
            startPos = endPos;
            endPos = temp;
        }
        
        // 步骤2: 在预测范围内进行二分搜索找到精确边界
        int[] exactBounds = binarySearchBounds(node, queryMinZ, queryMaxZ, startPos, endPos, indent);
        int exactStart = exactBounds[0];
        int exactEnd = exactBounds[1];
        
        // 步骤3: 扫描精确范围内的所有点，进行空间过滤
        System.out.println(indent + "    扫描范围: [" + exactStart + ", " + exactEnd + "]");
        int foundInRange = 0;
        
        for (int i = exactStart; i <= exactEnd && i < node.zOrderArray.length; i++) {
            Point2D point = node.sortedPoints[i];
            if (isPointInRange(point, bottomLeft, topRight)) {
                result.addPoint(point);
                foundInRange++;
                if (foundInRange <= 5) { // 只显示前5个找到的点
                    System.out.println(indent + "      找到点: " + point + " (Z=" + node.zOrderArray[i] + ")");
                }
            }
        }
        
        if (foundInRange > 5) {
            System.out.println(indent + "      ... 还有" + (foundInRange - 5) + "个点");
        }
        
        System.out.println(indent + "    叶子节点查询完成，找到 " + foundInRange + " 个点");
    }
    
    /**
     * 使用当前层查询内部节点
     */
    private void queryInternalNodeWithLayerZ(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                           RangeQueryResult result, String indent, int depth, AdaptiveZOrderTree tree) {
        
        System.out.println(indent + "  查询内部节点，共 " + node.children.size() + " 个子节点");
        
        // 对每个子节点，判断是否与查询范围相交
        int intersectingChildren = 0;
        for (AdaptiveZOrderNode child : node.children) {
            if (nodeIntersectsQuery(child, bottomLeft, topRight)) {
                intersectingChildren++;
                System.out.println(indent + "    子节点 " + child.zOrderValue + " 与查询范围相交，继续查询");
                
                // 递归查询子节点，使用子节点的最优位数
                performLayeredRangeQuery(child, bottomLeft, topRight, result, depth + 1, tree);
            } else {
                System.out.println(indent + "    子节点 " + child.zOrderValue + " 与查询范围不相交，跳过");
            }
        }
        
        System.out.println(indent + "  内部节点查询完成，访问了 " + intersectingChildren + "/" + node.children.size() + " 个子节点");
    }
    
    /**
     * 查询叶子节点 - 核心的学习索引查询逻辑
     */
    private void queryLeafNode(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                              long queryMinZ, long queryMaxZ, RangeQueryResult result, String indent) {
        
        if (!node.hasLearnedModel || node.zOrderArray == null) {
            System.out.println(indent + "  叶子节点无学习模型，执行线性扫描");
            linearScanLeafNode(node, bottomLeft, topRight, result);
            return;
        }
        
        System.out.println(indent + "  使用学习模型查询叶子节点");
        System.out.println(indent + "    节点包含 " + node.zOrderArray.length + " 个数据点");
        
        // 步骤1: 使用学习模型预测查询范围的起始和结束位置
        int startPos = predictPosition(node, queryMinZ, "起始", indent);
        int endPos = predictPosition(node, queryMaxZ, "结束", indent);
        
        // 确保范围有效
        if (startPos > endPos) {
            int temp = startPos;
            startPos = endPos;
            endPos = temp;
        }
        
        // 步骤2: 在预测范围内进行二分搜索找到精确边界
        int[] exactBounds = binarySearchBounds(node, queryMinZ, queryMaxZ, startPos, endPos, indent);
        int exactStart = exactBounds[0];
        int exactEnd = exactBounds[1];
        
        // 步骤3: 扫描精确范围内的所有点，进行空间过滤
        System.out.println(indent + "    扫描范围: [" + exactStart + ", " + exactEnd + "]");
        int foundInRange = 0;
        
        for (int i = exactStart; i <= exactEnd && i < node.zOrderArray.length; i++) {
            Point2D point = node.sortedPoints[i];
            if (isPointInRange(point, bottomLeft, topRight)) {
                result.addPoint(point);
                foundInRange++;
                if (foundInRange <= 5) { // 只显示前5个找到的点
                    System.out.println(indent + "      找到点: " + point + " (Z=" + node.zOrderArray[i] + ")");
                }
            }
        }
        
        if (foundInRange > 5) {
            System.out.println(indent + "      ... 还有" + (foundInRange - 5) + "个点");
        }
        
        System.out.println(indent + "    叶子节点查询完成，找到 " + foundInRange + " 个点");
    }
    
    /**
     * 使用学习模型预测位置
     */
    private int predictPosition(AdaptiveZOrderNode node, long queryZ, String type, String indent) {
        if (!node.hasLearnedModel || node.fitResult == null) {
            return type.equals("起始") ? 0 : node.zOrderArray.length - 1;
        }
        
        double predictedPos = node.fitResult.slope * queryZ + node.fitResult.intercept;
        int predicted = Math.max(0, Math.min((int)Math.round(predictedPos), node.zOrderArray.length - 1));
        
        System.out.println(indent + "    预测" + type + "位置: Z=" + queryZ + " -> 位置=" + predicted + 
                         " (置信度R²=" + String.format("%.3f", node.fitResult.rSquared) + ")");
        return predicted;
    }
    
    /**
     * 二分搜索精确边界
     */
    private int[] binarySearchBounds(AdaptiveZOrderNode node, long queryMinZ, long queryMaxZ,
                                   int startPos, int endPos, String indent) {
        
        // 根据模型质量设置误差范围
        int errorRange;
        if (node.fitResult.rSquared > 0.95) {
            errorRange = 2;  // 高质量模型，小误差范围
        } else if (node.fitResult.rSquared > 0.8) {
            errorRange = 5;  // 中等质量模型
        } else {
            errorRange = 10; // 低质量模型，大误差范围
        }
        
        int searchStart = Math.max(0, Math.min(startPos, endPos) - errorRange);
        int searchEnd = Math.min(node.zOrderArray.length - 1, Math.max(startPos, endPos) + errorRange);
        
        System.out.println(indent + "    二分搜索范围: [" + searchStart + ", " + searchEnd + 
                         "] (误差范围=" + errorRange + ")");
        
        // 找到 >= queryMinZ 的第一个位置
        int exactStart = binarySearchFirst(node.zOrderArray, queryMinZ, searchStart, searchEnd);
        
        // 找到 <= queryMaxZ 的最后一个位置  
        int exactEnd = binarySearchLast(node.zOrderArray, queryMaxZ, searchStart, searchEnd);
        
        System.out.println(indent + "    精确边界: [" + exactStart + ", " + exactEnd + "]");
        return new int[]{exactStart, exactEnd};
    }
    
    /**
     * 二分搜索第一个 >= target 的位置
     */
    private int binarySearchFirst(long[] array, long target, int left, int right) {
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (array[mid] >= target) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return left < array.length ? left : array.length;
    }
    
    /**
     * 二分搜索最后一个 <= target 的位置
     */
    private int binarySearchLast(long[] array, long target, int left, int right) {
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (array[mid] <= target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return right >= 0 ? right : -1;
    }
    
    /**
     * 查询内部节点
     */
    private void queryInternalNode(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                 long queryMinZ, long queryMaxZ, RangeQueryResult result, String indent, int depth) {
        
        System.out.println(indent + "  查询内部节点，共 " + node.children.size() + " 个子节点");
        
        // 对每个子节点，判断是否与查询范围相交
        int intersectingChildren = 0;
        for (AdaptiveZOrderNode child : node.children) {
            if (nodeIntersectsQuery(child, bottomLeft, topRight)) {
                intersectingChildren++;
                System.out.println(indent + "    子节点 " + child.zOrderValue + " 与查询范围相交，继续查询");
                
                // 递归查询子节点
                performRangeQuery(child, bottomLeft, topRight, queryMinZ, queryMaxZ, result, depth + 1);
            } else {
                System.out.println(indent + "    子节点 " + child.zOrderValue + " 与查询范围不相交，跳过");
            }
        }
        
        System.out.println(indent + "  内部节点查询完成，访问了 " + intersectingChildren + "/" + node.children.size() + " 个子节点");
    }
    
    /**
     * 判断节点是否与查询范围相交
     */
    private boolean nodeIntersectsQuery(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight) {
        // 检查节点的空间边界是否与查询矩形相交
        return !(node.maxX < bottomLeft.x || node.minX > topRight.x ||
                node.maxY < bottomLeft.y || node.minY > topRight.y);
    }
    
    /**
     * 判断点是否在查询范围内
     */
    private boolean isPointInRange(Point2D point, Point2D bottomLeft, Point2D topRight) {
        return point.x >= bottomLeft.x && point.x <= topRight.x &&
               point.y >= bottomLeft.y && point.y <= topRight.y;
    }
    
    /**
     * 线性扫描叶子节点（备用方案）
     */
    private void linearScanLeafNode(AdaptiveZOrderNode node, Point2D bottomLeft, Point2D topRight,
                                   RangeQueryResult result) {
        int found = 0;
        for (Point2D point : node.points) {
            if (isPointInRange(point, bottomLeft, topRight)) {
                result.addPoint(point);
                found++;
            }
        }
        System.out.println("      线性扫描找到 " + found + " 个点");
    }
    
    /**
     * 范围查询结果类
     */
    public static class RangeQueryResult {
        public List<Point2D> points;
        public int nodeAccesses;
        public long queryTime;
        
        public RangeQueryResult() {
            this.points = new ArrayList<>();
            this.nodeAccesses = 0;
            this.queryTime = 0;
        }
        
        public void addPoint(Point2D point) {
            points.add(point);
        }
        
        public void addNodeAccess() {
            nodeAccesses++;
        }
        
        public void setQueryTime(long time) {
            this.queryTime = time;
        }
        
        @Override
        public String toString() {
            return String.format("RangeQueryResult{找到%d个点, 访问%d个节点, 耗时%dms}", 
                               points.size(), nodeAccesses, queryTime);
        }
    }
    
    /**
     * 计算数据集需要的最大位数
     */
    private int calculateMaxBitsNeeded(AdaptiveZOrderNode root) {
        // 基于根节点的空间范围计算最大坐标
        long maxCoord = Math.max(Math.max(root.maxX, root.maxY), 
                               Math.max(Math.abs(root.minX), Math.abs(root.minY)));
        
        // 计算需要的位数
        if (maxCoord == 0) return 1;
        return (int) Math.ceil(Math.log(maxCoord + 1) / Math.log(2));
    }
    
    /**
     * Z地址工具类
     */
    public static class ZOrderUtils {
        
        public static long computeZOrderWithBits(Point2D point, int bits) {
            return utils.ZOrderUtils.computeZOrderWithBits(point.x, point.y, bits);
        }
        
        public static long computeZOrderWithBits(long x, long y, int bits) {
            return utils.ZOrderUtils.computeZOrderWithBits(x, y, bits);
        }
        
        /**
         * 正确的Z地址计算：先扩展到最大位数，再截取前面的最优位数
         */
        public static long computeZOrderWithTruncation(Point2D point, int maxBits, int optimalBits) {
            return computeZOrderWithTruncation(point.x, point.y, maxBits, optimalBits);
        }
        
        public static long computeZOrderWithTruncation(long x, long y, int maxBits, int optimalBits) {
            System.out.println("    Z地址计算详情: (" + x + "," + y + ")");
            
            // 显示原始二进制
            String xBin = String.format("%" + maxBits + "s", Long.toBinaryString(x)).replace(' ', '0');
            String yBin = String.format("%" + maxBits + "s", Long.toBinaryString(y)).replace(' ', '0');
            System.out.println("    扩展到" + maxBits + "位: x=" + xBin + ", y=" + yBin);
            
            // 第1步：将坐标扩展到最大位数表示
            // 第2步：截取前面的最优位数
            long xTruncated = x >> (maxBits - optimalBits);
            long yTruncated = y >> (maxBits - optimalBits);
            
            // 显示截取后的结果
            String xTruncBin = String.format("%" + optimalBits + "s", Long.toBinaryString(xTruncated)).replace(' ', '0');
            String yTruncBin = String.format("%" + optimalBits + "s", Long.toBinaryString(yTruncated)).replace(' ', '0');
            System.out.println("    截取前" + optimalBits + "位: x=" + xTruncBin + "(" + xTruncated + "), y=" + yTruncBin + "(" + yTruncated + ")");
            
            // 第3步：用截取后的坐标计算Z地址
            long zOrder = computeZOrderWithBits(xTruncated, yTruncated, optimalBits);
            System.out.println("    计算得Z地址: " + zOrder);
            
            return zOrder;
        }
    }
    
    /**
     * 测试范围查询功能
     */
    public static void testRangeQuery(Point2D[] dataset, String datasetName) {
        System.out.println("\n=== " + datasetName + " 范围查询测试 ===");
        
        // 构建自适应Z-order树
        System.out.println("构建自适应Z-order树...");
        long buildStart = System.nanoTime();
        AdaptiveZOrderTree tree = new AdaptiveZOrderTree(dataset);
        long buildEnd = System.nanoTime();
        System.out.println("树构建完成，耗时: " + (buildEnd - buildStart) / 1000000.0 + "ms");
        
        // 创建查询系统
        AdaptiveRangeQuery querySystem = new AdaptiveRangeQuery(tree);
        
        // 计算数据边界
        long minX = Arrays.stream(dataset).mapToLong(p -> p.x).min().orElse(0);
        long maxX = Arrays.stream(dataset).mapToLong(p -> p.x).max().orElse(0);
        long minY = Arrays.stream(dataset).mapToLong(p -> p.y).min().orElse(0);
        long maxY = Arrays.stream(dataset).mapToLong(p -> p.y).max().orElse(0);
        
        // 测试不同大小的查询窗口
        testQueryWindow(querySystem, minX, maxX, minY, maxY, 0.1, "小窗口(10%)");
        testQueryWindow(querySystem, minX, maxX, minY, maxY, 0.25, "中窗口(25%)");
        testQueryWindow(querySystem, minX, maxX, minY, maxY, 0.5, "大窗口(50%)");
    }
    
    /**
     * 测试指定大小的查询窗口
     */
    private static void testQueryWindow(AdaptiveRangeQuery querySystem, long minX, long maxX, long minY, long maxY,
                                      double windowRatio, String windowName) {
        
        System.out.println("\n--- " + windowName + "查询测试 ---");
        
        // 计算查询窗口大小
        long rangeX = maxX - minX;
        long rangeY = maxY - minY;
        long windowWidth = Math.max(1, (long)(rangeX * windowRatio));
        long windowHeight = Math.max(1, (long)(rangeY * windowRatio));
        
        // 随机选择查询窗口位置
        long startX = minX + (long)(Math.random() * Math.max(1, rangeX - windowWidth));
        long startY = minY + (long)(Math.random() * Math.max(1, rangeY - windowHeight));
        
        Point2D bottomLeft = new Point2D(startX, startY);
        Point2D topRight = new Point2D(startX + windowWidth, startY + windowHeight);
        
        System.out.println("查询窗口: " + bottomLeft + " -> " + topRight);
        System.out.println("窗口大小: " + windowWidth + " × " + windowHeight);
        
        // 执行范围查询
        RangeQueryResult result = querySystem.rangeQuery(bottomLeft, topRight);
        
        // 显示找到的点（最多显示10个）
        if (!result.points.isEmpty()) {
            System.out.println("找到的点:");
            for (int i = 0; i < Math.min(10, result.points.size()); i++) {
                System.out.println("  " + result.points.get(i));
            }
            if (result.points.size() > 10) {
                System.out.println("  ... 还有" + (result.points.size() - 10) + "个点");
            }
        }
    }
    
    /**
     * 加载数据集的方法
     */
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
        
        // 尝试每个可能的路径
        for (String path : possiblePaths) {
            try {
                br = new java.io.BufferedReader(new java.io.FileReader(path));
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
                
                // 支持逗号分隔和空格分隔
                String[] parts = line.split("[,\\s]+");
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
    
    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== 基于学习索引的自适应Z-order范围查询测试 ===\n");
        
        // 创建简单测试数据集
        Point2D[] testData = {
            new Point2D(0, 0), new Point2D(1, 0), new Point2D(2, 0),
            new Point2D(0, 1), new Point2D(1, 1), new Point2D(2, 1),
            new Point2D(0, 2), new Point2D(1, 2), new Point2D(2, 2),
            new Point2D(3, 0), new Point2D(3, 1), new Point2D(3, 2),
            new Point2D(4, 4), new Point2D(5, 5), new Point2D(6, 6)
        };
        
        System.out.println("测试数据集包含 " + testData.length + " 个点:");
        for (int i = 0; i < testData.length; i++) {
            System.out.println("  点" + (i+1) + ": " + testData[i]);
        }
        
        // 构建自适应Z-order树
        System.out.println("\n构建自适应Z-order树...");
        AdaptiveZOrderTree tree = new AdaptiveZOrderTree(testData);
        
        // 创建查询系统
        AdaptiveRangeQuery querySystem = new AdaptiveRangeQuery(tree);
        
        // 执行指定的查询范围 [(0,0), (2,1)]
        System.out.println("\n=== 执行指定查询: 范围[(0,0), (2,1)] ===");
        Point2D bottomLeft = new Point2D(0, 0);
        Point2D topRight = new Point2D(2, 1);
        
        RangeQueryResult result = querySystem.rangeQuery(bottomLeft, topRight);
        
        // 显示查询结果
        System.out.println("\n=== 查询结果总结 ===");
        System.out.println("查询范围: [" + bottomLeft + ", " + topRight + "]");
        System.out.println("找到的点数量: " + result.points.size());
        System.out.println("访问的树节点数: " + result.nodeAccesses);
        System.out.println("查询耗时: " + result.queryTime + "ms");
        
        System.out.println("\n在查询范围内的所有点:");
        for (int i = 0; i < result.points.size(); i++) {
            System.out.println("  结果" + (i+1) + ": " + result.points.get(i));
        }
        
        // 验证结果正确性
        System.out.println("\n=== 验证查询结果 ===");
        verifySpecificQuery(testData, bottomLeft, topRight, result);
        
        // 如果有外部数据集文件，也可以测试
        System.out.println("\n=== 外部数据集测试 ===");
        Point2D[] externalData = loadDataset("src/data/grid_10x10_sparse_dataset.csv", 1000);
        if (externalData != null && externalData.length > 0) {
            System.out.println("加载外部数据集成功，包含 " + externalData.length + " 个点");
            
            // 构建外部数据的树
            AdaptiveZOrderTree externalTree = new AdaptiveZOrderTree(externalData);
            AdaptiveRangeQuery externalQuerySystem = new AdaptiveRangeQuery(externalTree);
            
            // 对外部数据集执行相同的查询
            System.out.println("对外部数据集执行查询范围 [(0,0), (2,1)]:");
            RangeQueryResult externalResult = externalQuerySystem.rangeQuery(bottomLeft, topRight);
            
            System.out.println("外部数据集查询结果:");
            System.out.println("  找到点数: " + externalResult.points.size());
            System.out.println("  访问节点数: " + externalResult.nodeAccesses);
            System.out.println("  查询耗时: " + externalResult.queryTime + "ms");
            
            if (!externalResult.points.isEmpty()) {
                System.out.println("  找到的点:");
                for (int i = 0; i < Math.min(10, externalResult.points.size()); i++) {
                    System.out.println("    " + externalResult.points.get(i));
                }
                if (externalResult.points.size() > 10) {
                    System.out.println("    ... 还有" + (externalResult.points.size() - 10) + "个点");
                }
            }
        } else {
            System.out.println("未找到外部数据集文件");
        }
    }
    
    /**
     * 验证特定查询的结果
     */
    private static void verifySpecificQuery(Point2D[] dataset, Point2D bottomLeft, Point2D topRight, 
                                          RangeQueryResult result) {
        
        System.out.println("手动验证查询范围 [" + bottomLeft + ", " + topRight + "]:");
        System.out.println("条件: " + bottomLeft.x + " ≤ x ≤ " + topRight.x + 
                         " 且 " + bottomLeft.y + " ≤ y ≤ " + topRight.y);
        
        int expectedCount = 0;
        System.out.println("应该包含的点:");
        
        for (Point2D point : dataset) {
            if (point.x >= bottomLeft.x && point.x <= topRight.x &&
                point.y >= bottomLeft.y && point.y <= topRight.y) {
                expectedCount++;
                System.out.println("  ✓ " + point + " (x=" + point.x + " 在[" + bottomLeft.x + "," + topRight.x + "], " +
                                 "y=" + point.y + " 在[" + bottomLeft.y + "," + topRight.y + "])");
            }
        }
        
        System.out.println("\n验证结果:");
        System.out.println("期望找到: " + expectedCount + " 个点");
        System.out.println("实际找到: " + result.points.size() + " 个点");
        
        if (expectedCount == result.points.size()) {
            System.out.println("✓ 查询结果完全正确！");
        } else {
            System.out.println("✗ 查询结果不匹配");
            
            // 详细分析差异
            Set<Point2D> expected = new HashSet<>();
            Set<Point2D> actual = new HashSet<>(result.points);
            
            for (Point2D point : dataset) {
                if (point.x >= bottomLeft.x && point.x <= topRight.x &&
                    point.y >= bottomLeft.y && point.y <= topRight.y) {
                    expected.add(point);
                }
            }
            
            // 找出遗漏的点
            Set<Point2D> missed = new HashSet<>(expected);
            missed.removeAll(actual);
            if (!missed.isEmpty()) {
                System.out.println("遗漏的点:");
                for (Point2D p : missed) {
                    System.out.println("  - " + p);
                }
            }
            
            // 找出多余的点
            Set<Point2D> extra = new HashSet<>(actual);
            extra.removeAll(expected);
            if (!extra.isEmpty()) {
                System.out.println("多余的点:");
                for (Point2D p : extra) {
                    System.out.println("  + " + p);
                }
            }
        }
    }
}
