package uk.gov.hmcts.cp.subscription.services;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * We use a ClockService to expose the clock time in a simple method to allow mocking in Tests
 */
@Service
public class ClockService {

    private final Clock clock;

    public ClockService(final Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public OffsetDateTime nowOffsetUTC() {
        return clock.instant().atOffset(ZoneOffset.UTC);
    }
}
