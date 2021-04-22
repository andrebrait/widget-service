package io.andrebrait.widget.repository;

import io.andrebrait.widget.repository.rectangle.RectangleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
@Profile("sql")
@RequiredArgsConstructor
public class SqlRepository implements WidgetRepository {

    private final RectangleRepository rectangleRepository;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

}
