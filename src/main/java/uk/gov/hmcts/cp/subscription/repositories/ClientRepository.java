package uk.gov.hmcts.cp.subscription.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    @Query("SELECT c FROM ClientEntity c,  ClientEventEntity ce, EventTypeEntity e " +
            "where e.eventName = :eventName " +
            "AND e.id = ce.eventTypeId " +
            "AND ce.subscriptionId = c.subscriptionId")
    List<ClientEntity> findClientsByEventType(@Param("eventName") String eventName);

    Optional<ClientEntity> findByIdAndSubscriptionId(UUID id, UUID subscriptionId);
}
