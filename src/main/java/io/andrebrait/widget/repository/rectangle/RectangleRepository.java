package io.andrebrait.widget.repository.rectangle;

import io.andrebrait.widget.domain.IdentifiableRectangle;
import io.andrebrait.widget.domain.Rectangle;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Small abstraction layer so we can test the difference between using a structure with linear time
 * complexity for queries ({@link HashSetRepository}) and an RTree-based structure ({@link
 * RTreeRepository} with ease.
 */
public interface RectangleRepository<R extends IdentifiableRectangle> {

    /**
     * Adds a rectangle to the repository
     *
     * @param rectangle the rectangle
     * @return true if the repository did not already contain the rectangle
     */
    boolean add(R rectangle);

    /**
     * Removes a rectangle from the repository
     *
     * @param rectangle the rectangle to be removed
     * @return true if the repository contained that rectangle
     */
    @Nullable
    boolean remove(R rectangle);

    /**
     * Finds all rectangles contained inside the one provided as argument.<br>
     *
     * Only rectangles that return {@code true} for {@link Rectangle#contains(Rectangle)} will be
     * returned.
     *
     * @param rectangle the target search rectangle
     * @return a list of rectangles present that are fully contained inside the search rectangle
     */
    List<R> findAllInside(Rectangle rectangle);
}
