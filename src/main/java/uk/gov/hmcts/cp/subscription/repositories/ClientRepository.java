package uk.gov.hmcts.cp.subscription.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;

<<<<<<< HEAD
import java.util.Optional;
=======
import java.time.OffsetDateTime;
>>>>>>> 57e992df (AMP-197 Storing subscription info in client events db tables. (Part-3))
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

<<<<<<< HEAD
    Optional<ClientEntity> findByIdAndSubscriptionId(UUID id, UUID subscriptionId);
    boolean existsById(UUID clientId);
=======
    @Modifying
    @Query("UPDATE ClientEntity c SET c.callbackUrl = :callbackUrl, c.updatedAt = :updatedAt WHERE c.id = :clientId")
    void updateCallbackUrl(@Param("clientId") UUID clientId,
                           @Param("callbackUrl") String callbackUrl,
                           @Param("updatedAt") OffsetDateTime updatedAt);

>>>>>>> 57e992df (AMP-197 Storing subscription info in client events db tables. (Part-3))
}
