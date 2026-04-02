package uk.gov.hmcts.cp.subscription.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientHmacRepository extends JpaRepository<ClientHmacEntity, Long> {
    Optional<ClientHmacEntity> findBySubscriptionId(UUID id);
}
