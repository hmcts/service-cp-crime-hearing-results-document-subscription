package uk.gov.hmcts.cp.subscription.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByIdAndSubscriptionId(UUID id, UUID subscriptionId);
    boolean existsById(UUID clientId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ClientEntity c SET c.callbackUrl = :callbackUrl, c.updatedAt = :updatedAt WHERE c.id = :clientId")
    int updateCallbackUrl(@Param("clientId") UUID clientId,
                           @Param("callbackUrl") String callbackUrl,
                           @Param("updatedAt") OffsetDateTime updatedAt);

}
