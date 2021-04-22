package io.andrebrait.widget.domain;

import java.math.BigInteger;

public interface Rectangle {

    long getX();

    long getY();

    long getX2();

    long getY2();

    default boolean equalDimensions(Rectangle o) {
        return equalDimensions(o.getX(), o.getY(), o.getX2(), o.getY2());
    }

    default boolean equalDimensions(long x, long y, long x2, long y2) {
        return this.getX() == x
                && this.getX2() == x2
                && this.getY() == y
                && this.getY2() == y2;
    }

    default boolean contains(Rectangle o) {
        return contains(o.getX(), o.getY(), o.getX2(), o.getY2());
    }

    default boolean contains(long x, long y, long x2, long y2) {
        return this.getX() <= x
                && this.getX2() >= x2
                && this.getY() <= y
                && this.getY2() >= y2;
    }

    default boolean intersects(Rectangle o) {
        return intersects(o.getX(), o.getY(), o.getX2(), o.getY2());
    }

    default boolean intersects(long x, long y, long x2, long y2) {
        return this.getX() < x2
                && this.getX2() > x
                && this.getY() < y2
                && this.getY2() > y;
    }

    default BigInteger area() {
        return area(getX(), getY(), getX2(), getY2());
    }

    default BigInteger joinedArea(Rectangle o) {
        return joinedArea(o.getX(), o.getY(), o.getX2(), o.getY2());
    }

    default BigInteger joinedArea(long x, long y, long x2, long y2) {
        return area(
                Math.min(this.getX(), x),
                Math.min(this.getY(), y),
                Math.max(this.getX2(), x2),
                Math.max(this.getY2(), y2));
    }

    default BigInteger intersectionArea(Rectangle o) {
        return intersectionArea(o.getX(), o.getY(), o.getX2(), o.getY2());
    }

    default BigInteger intersectionArea(long x, long y, long x2, long y2) {
        if (!intersects(x, y, x2, y2)) {
            return BigInteger.ZERO;
        }
        return area(
                Math.max(this.getX(), x),
                Math.max(this.getY(), y),
                Math.min(this.getX2(), x2),
                Math.min(this.getY2(), y2));
    }

    private static BigInteger area(long x, long y, long x2, long y2) {
        BigInteger width = BigInteger.valueOf(x2).subtract(BigInteger.valueOf(x));
        BigInteger height = BigInteger.valueOf(y2).subtract(BigInteger.valueOf(y));
        return width.multiply(height);
    }
}
