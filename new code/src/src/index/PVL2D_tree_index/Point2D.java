package index.PVL2D_tree_index;

public class Point2D {
    public long x, y;
    public long zOrder;  // Z-order编码值
    
    public Point2D(long x, long y) {
        this.x = x;
        this.y = y;
        this.zOrder = 0; // 将在计算时设置
    }
    
    public Point2D(long x, long y, long zOrder) {
        this.x = x;
        this.y = y;
        this.zOrder = zOrder;
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point2D point2D = (Point2D) obj;
        return x == point2D.x && y == point2D.y;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(x) * 31 + Long.hashCode(y);
    }
}
