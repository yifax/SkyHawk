package yifax10.uci.SkyHawk;

import org.opencv.core.Point;

public class Spot {
    private Point leftTop;
    private Point rightBottom;
    private boolean available;

    public Spot(Point p1, Point p2) {
        this.setLeftTop(p1);
        this.setRightBottom(p2);
        this.setAvailable(false);
    }

    public Point getLeftTop() {
        return leftTop;
    }

    public void setLeftTop(Point leftTop) {
        this.leftTop = leftTop;
    }

    public Point getRightBottom() {
        return rightBottom;
    }

    public void setRightBottom(Point rightBottom) {
        this.rightBottom = rightBottom;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
