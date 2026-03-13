package uk.gov.hmcts.cp.subscription.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "event_type")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventTypeEntity {

    @Id
    private Long id;

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "category")
    private String category;
}
