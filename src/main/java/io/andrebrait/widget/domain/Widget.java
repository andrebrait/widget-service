package io.andrebrait.widget.domain;

import io.andrebrait.widget.rtree.Rectangle;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Min;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Widget {

    @Id
    @Column(name = "ID", nullable = false, updatable = false, unique = true)
    @EqualsAndHashCode.Include
    private UUID id;
    @Column(name = "X", nullable = false)
    private long x;
    @Column(name = "Y", nullable = false)
    private long y;
    @Column(name = "Z", nullable = false, unique = true)
    private long z;
    @Min(value = 1, message = "'width' must be greater than or equal to {value}")
    @Column(name = "WIDTH", nullable = false)
    private long width;
    @Min(value = 1, message = "'height' must be greater than or equal to {value}")
    @Column(name = "HEIGHT", nullable = false)
    private long height;
}
