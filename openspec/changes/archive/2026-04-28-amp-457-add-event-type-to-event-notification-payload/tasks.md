## 1. Dependency Upgrade

- [x] 1.1 In `build.gradle`, change `api-cp-crime-hearing-results-document-subscription` version from `2.0.5` to `2.0.6`
- [x] 1.2 Run `./gradlew dependencies` to confirm `2.0.6` resolves correctly

## 2. Mapper Update

- [x] 2.1 In `NotificationMapper.mapToPayload()`, add `.eventType(eventPayload.getEventType())` to the `EventNotificationPayload.builder()` call

## 3. Test Coverage

- [x] 3.1 In `NotificationMapperTest`, add an assertion that `mapToPayload()` sets `eventType` equal to the source `EventPayload.eventType`

## 4. Verification

- [x] 4.1 Run `./gradlew test` and confirm all tests pass