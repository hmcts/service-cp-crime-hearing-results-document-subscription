## Why

The outgoing `EventNotificationPayload` sent to subscribers does not include the `eventType` from the source PCR event, meaning subscribers cannot distinguish the type of event that triggered the notification without additional calls. Version 2.0.6 of the API spec library adds the `eventType` field to `EventNotificationPayload`, and the service must be updated to populate and forward it.

## What Changes

- Bump dependency `uk.gov.hmcts.cp:api-cp-crime-hearing-results-document-subscription` from `2.0.5` to `2.0.6` in `build.gradle`
- Update `NotificationMapper.mapToPayload()` to populate the new `eventType` field on `EventNotificationPayload` from `EventPayload.eventType`

## Capabilities

### New Capabilities

- `event-type-in-notification`: `EventNotificationPayload` now includes `eventType`, forwarding the source event type (e.g. `PRISON_COURT_REGISTER_GENERATED`) to subscribers

### Modified Capabilities

_(none — no existing spec files exist in this project)_

## Impact

- `build.gradle`: dependency version bump `2.0.5` → `2.0.6`
- `NotificationMapper`: populate `eventType` from `EventPayload` when building `EventNotificationPayload`
- `NotificationMapperTest`: add assertion that `eventType` is mapped correctly
- Downstream subscribers will now receive `eventType` in the payload — additive, non-breaking change