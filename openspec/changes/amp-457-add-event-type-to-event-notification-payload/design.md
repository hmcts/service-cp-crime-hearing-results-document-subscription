## Context

The service consumes PCR events from Azure Service Bus. Each event carries an `eventType` (e.g. `PRISON_COURT_REGISTER_GENERATED`) in its `EventPayload`. The service generates an `EventNotificationPayload` and sends it to downstream subscribers via a callback HTTP call. Currently the `eventType` is silently dropped during the `NotificationMapper.mapToPayload()` step, so subscribers have no way to identify the originating event type from the notification alone.

Version 2.0.6 of `uk.gov.hmcts.cp:api-cp-crime-hearing-results-document-subscription` adds `eventType` to `EventNotificationPayload`. This design covers the upgrade and the single mapping change required.

## Goals / Non-Goals

**Goals:**
- Bump the API spec library dependency to `2.0.6`
- Populate `eventType` on `EventNotificationPayload` from the source `EventPayload`
- Update tests to assert the new field is correctly mapped

**Non-Goals:**
- Changes to the Service Bus event schema or inbound contract
- Validation or business logic on `eventType` values
- Any changes to subscriber callback authentication or delivery mechanism

## Decisions

### Use the library-provided `eventType` field directly

The new field on `EventNotificationPayload` (from 2.0.6) is of the same type emitted by `EventPayload.getEventType()`. The mapper can pass it through without transformation.

**Alternative considered**: Introduce a local enum to guard against unknown event types. Rejected — the spec library already owns the type contract; adding a local re-mapping would duplicate that concern and break when new event types are added without a code change here.

### Single-site change in `NotificationMapper`

All mapping is centralised in `NotificationMapper.mapToPayload()`. The `eventType` should be added there to keep the pattern consistent.

## Risks / Trade-offs

- **Risk**: 2.0.6 jar is not yet published to the artefact repository → **Mitigation**: The version bump will fail at build time with a clear resolution error; no runtime impact.
- **Risk**: `eventType` is nullable in the incoming `EventPayload` and the outbound payload → **Mitigation**: Pass through as-is; downstream subscribers already handle optional fields per the existing contract.

## Migration Plan

1. Bump `build.gradle` dependency version from `2.0.5` to `2.0.6`.
2. Add `eventType` to the builder call in `NotificationMapper.mapToPayload()`.
3. Add assertion in `NotificationMapperTest` for the new field.
4. Deploy as a normal release — additive payload field, no breaking change to existing subscribers.

Rollback: revert the dependency version and the mapper line; downstream subscribers ignore unknown fields so no subscriber-side rollback is needed.

## Open Questions

_(none)_