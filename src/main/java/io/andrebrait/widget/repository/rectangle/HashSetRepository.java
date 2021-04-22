package io.andrebrait.widget.repository.rectangle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HashSetRepository implements RectangleRepository {

    private final Set<InternalRectangle> repository = new HashSet<>();

    @Override
    public boolean add(InternalRectangle rectangle) {
        return repository.add(rectangle);
    }

    @Override
    public boolean remove(InternalRectangle rectangle) {
        return repository.remove(rectangle);
    }

    @Override
    public List<InternalRectangle> findAllInside(InternalRectangle rectangle) {
        return repository.stream().filter(rectangle::contains).collect(Collectors.toList());
    }
}
