package uk.gov.hmcts.cp.subscription.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventTypeRepository extends JpaRepository<EventTypeEntity, Long> {

    List<EventTypeEntity> findByEventNameIn(Collection<String> eventNames);

    Optional<EventTypeEntity> findByEventName(String eventName);

    boolean existsByEventName(String eventName);
}
