package io.andrebrait.widget.rtree;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.math.BigInteger;
import java.util.Comparator;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rectangle implements Comparable<Rectangle> {

    public static final Rectangle GRID =
            Rectangle.of(Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

    private static final Comparator<Rectangle> RECTANGLE_COMPARATOR =
            Comparator.comparingLong(Rectangle::getX)
                    .thenComparingLong(Rectangle::getX2)
                    .thenComparingLong(Rectangle::getY)
                    .thenComparingLong(Rectangle::getY2);

    long x;
    long y;
    long x2;
    long y2;

    public static Rectangle of(long x, long y, long x2, long y2) {
        if (x2 <= x) {
            throw new IllegalArgumentException("'x2' must ge greater than 'x'");
        }
        if (y2 <= y) {
            throw new IllegalArgumentException("'y2' must ge greater than 'y'");
        }
        return new Rectangle(x, y, x2, y2);
    }

    public boolean contains(Rectangle o) {
        return x <= o.x
                && x2 >= o.x2
                && y <= o.y
                && y2 >= o.y2;
    }

    public boolean intersects(Rectangle o) {
        return x < o.x2
                && x2 > o.x
                && y < o.y2
                && y2 > o.y;
    }

    public BigInteger getArea() {
        BigInteger width = BigInteger.valueOf(x2).subtract(BigInteger.valueOf(x));
        BigInteger height = BigInteger.valueOf(y2).subtract(BigInteger.valueOf(y));
        return width.multiply(height);
    }

    @Override
    public int compareTo(Rectangle o) {
        return RECTANGLE_COMPARATOR.compare(this, o);
    }
}
