package io.andrebrait.widget.repository;

import io.andrebrait.widget.repository.rectangle.InternalRectangle;
import io.andrebrait.widget.domain.Widget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetRepository {

    Widget save(Widget widget);

    Optional<Widget> findOne(UUID id);

    void deleteById(UUID id);

    Iterable<Widget> findAll();

    List<Widget> findAllInside(InternalRectangle rectangle);
}
