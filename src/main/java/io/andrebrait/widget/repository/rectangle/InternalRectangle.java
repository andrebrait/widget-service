package io.andrebrait.widget.repository.rectangle;

import io.andrebrait.widget.domain.Rectangle;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.lang.Nullable;

import java.math.BigInteger;
import java.util.UUID;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
class InternalRectangle implements Rectangle {

    long x;
    long y;
    long x2;
    long y2;

    public static InternalRectangle of(long x, long y, long x2, long y2) {
        if (x2 <= x) {
            throw new IllegalArgumentException("'x2' must ge greater than 'x'");
        }
        if (y2 <= y) {
            throw new IllegalArgumentException("'y2' must ge greater than 'y'");
        }
        return new InternalRectangle(x, y, x2, y2);
    }

    public InternalRectangle join(Rectangle o) {
        return of(
                Math.min(x, o.getX()),
                Math.min(y, o.getY()),
                Math.max(x2, o.getX2()),
                Math.max(y2, o.getY2()));
    }

    @Nullable
    public InternalRectangle intersect(Rectangle o) {
        return insersect(o.getX(), o.getY(), o.getX2(), o.getY2());
    }

    @Nullable
    public InternalRectangle insersect(long x, long y, long x2, long y2) {
        if (!this.intersects(x, y, x2, y2)) {
            return null;
        }
        return of(
                Math.min(this.x, x),
                Math.min(this.y, y),
                Math.max(this.x2, x2),
                Math.max(this.y2, y2));
    }

}
