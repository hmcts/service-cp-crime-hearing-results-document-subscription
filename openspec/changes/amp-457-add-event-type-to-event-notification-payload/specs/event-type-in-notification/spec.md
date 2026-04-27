## ADDED Requirements

### Requirement: eventType is included in EventNotificationPayload
The system SHALL populate the `eventType` field on `EventNotificationPayload` with the value from the source `EventPayload.eventType` when constructing the outbound notification.

#### Scenario: eventType is forwarded from source event
- **WHEN** `NotificationMapper.mapToPayload()` is called with an `EventPayload` that has a non-null `eventType`
- **THEN** the returned `EventNotificationPayload` SHALL contain the same `eventType` value


### Requirement: API spec library version is 2.0.6
The project SHALL declare `uk.gov.hmcts.cp:api-cp-crime-hearing-results-document-subscription:2.0.6` as its implementation dependency in `build.gradle`.

#### Scenario: Dependency version matches 2.0.6
- **WHEN** the project is built
- **THEN** the resolved `api-cp-crime-hearing-results-document-subscription` jar version SHALL be `2.0.6`