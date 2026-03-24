package uk.gov.hmcts.cp.subscription.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientEventsRepository extends JpaRepository<ClientEventEntity, Long> {

    /**
     * Fetch client events with their associated event type details using joins.
     * Joins ClientEventEntity with ClientEntity and EventTypeEntity.
     * 
     * @param clientId the client ID to filter by
     * @return list of client events joined with client and event type information
     */
    @Query("SELECT ce FROM ClientEventEntity ce " +
           "JOIN ClientEntity c ON ce.subscriptionId = c.subscriptionId " +
           "JOIN EventTypeEntity et ON ce.eventTypeId = et.id " +
           "WHERE c.id = :clientId " +
           "ORDER BY et.eventName ASC")
    List<ClientEventEntity> findClientEventsWithEventTypes(@Param("clientId") UUID clientId);
}
