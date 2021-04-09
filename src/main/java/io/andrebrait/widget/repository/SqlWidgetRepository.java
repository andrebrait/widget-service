package io.andrebrait.widget.repository;

import io.andrebrait.widget.domain.Widget;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@Profile("sql")
public interface SqlWidgetRepository extends WidgetRepository, JpaRepository<Widget, UUID> {

}
