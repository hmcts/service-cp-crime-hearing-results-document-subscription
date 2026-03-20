package uk.gov.hmcts.cp.subscription.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "client")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClientEntity {

    @Id
    @Column(name = "client_id")
    private UUID id;
    private UUID subscriptionId;
    private String callbackUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
