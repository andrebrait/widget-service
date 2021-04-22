package io.andrebrait.widget.repository;

import io.andrebrait.widget.repository.rectangle.InternalRectangle;
import io.andrebrait.widget.domain.Widget;
import io.andrebrait.widget.repository.rectangle.RectangleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Repository
@Profile("!sql")
@RequiredArgsConstructor
public class InMemoryRepository implements WidgetRepository {

    private final Map<UUID, Widget> widgetDatabase = new HashMap<>();
    private final SortedMap<BigInteger, Widget> zIndexMap = new TreeMap<>();

    private final RectangleRepository rectangleRepository;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public Widget save(@Valid Widget widget) {
        lock.writeLock().lock();
        try {
            Widget copy = widget.toBuilder().build();
            if (copy.getId() == null) {
                copy.setId(UUID.randomUUID());
            }
            if (widgetDatabase.containsKey(copy.getId())) {
                updateCopyOnDatabase(copy);
            } else {
                addCopyToDatabase(copy);
            }
            Widget databaseCopy = addCopyToDatabase(widget);
            if (databaseCopy.getZ() == null) {
                if (zIndexMap.isEmpty()) {
                    databaseCopy.setZ(BigInteger.ZERO);
                } else {
                    databaseCopy.setZ(zIndexMap.lastKey().add(BigInteger.ONE));
                }
            } else {
                SortedMap<BigInteger, Widget> zIndexItemsAboveIt ()
            }

            widget.setId(databaseCopy.getId());
            widget.setZ(databaseCopy.getZ());
            return widget;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Widget addCopyToDatabase(Widget copy) {
        if (copy.getZ() == null) {

            if (widget)
            Widget currentCopy = widgetDatabase.get(widget.getId());
            if (currentCopy != null && currentCopy.getZ().compareTo(widget.getZ()))
            copy = widget.toBuilder().build();
            widgetDatabase.put(widget.getId(), copy);
        }
        return copy;
    }

    private Widget updateCopyOnDatabase(Widget copy) {

    }

    @Override
    public Optional<Widget> findOne(UUID id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(widgetDatabase.get(id))
                    .map(Widget::toBuilder)
                    .map(Widget.WidgetBuilder::build);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteById(UUID id) {
        lock.writeLock().lock();
        try {
            widgetDatabase.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Iterable<Widget> findAll() {
        lock.readLock().lock();
        try {
            return widgetDatabase.values()
                    .stream()
                    .map(Widget::toBuilder)
                    .map(Widget.WidgetBuilder::build)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /*
     */
    @Override
    public List<Widget> findAllInside(InternalRectangle rectangle) {
        return rectangleRepository.findAllInside(rectangle)
                .stream()
                .map(InternalRectangle::getId)
                .map(widgetDatabase::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
