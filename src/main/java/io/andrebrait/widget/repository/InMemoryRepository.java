package io.andrebrait.widget.repository;

import io.andrebrait.widget.domain.Widget;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@Profile("!sql")
public class InMemoryRepository implements WidgetRepository {

    private final HashMap<UUID, Widget> widgetDatabase = new HashMap<>();
    private final TreeMap<Long, Widget> zIndexMap = new TreeMap<>();

}
