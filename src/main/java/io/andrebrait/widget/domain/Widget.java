package io.andrebrait.widget.domain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Positive;
import java.math.BigInteger;
import java.util.UUID;

/**
 * The Widget entity.<br><br>
 *
 * Using Lombok to generate {@link Object#equals(Object)} is not generally a great idea when
 * using JPA because it breaks {@code null}-{@code null} key comparisons, but we're not using an
 * auto-generated IDs and the ID must never be {@code null}, so it's not a huge problem here.
 */
@Entity
@Table(
        name = "WIDGET",
        indexes = {
                @Index(unique = true, columnList = "UUID"),
                @Index(unique = true, columnList = "PARENT"),
                @Index(unique = true, columnList = "Z")
        }
)
@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class Widget implements IdentifiableRectangle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, updatable = false, unique = true)
    @EqualsAndHashCode.Include
    private UUID id;
    @Column(name = "X", nullable = false)
    private long x;
    @Column(name = "Y", nullable = false)
    private long y;
    @Column(name = "Z", nullable = false, unique = true)
    private BigInteger z;
    @Positive
    @Column(name = "WIDTH", nullable = false)
    private long width;
    @Positive
    @Column(name = "HEIGHT", nullable = false)
    private long height;

    @Override
    public long getX2() {
        return x + width;
    }

    @Override
    public long getY2() {
        return y + height;
    }
}
