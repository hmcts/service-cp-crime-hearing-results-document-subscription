package uk.gov.hmcts.cp.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class CorrelationIdService {

    public UUID random() {
        return UUID.randomUUID();
    }

    public String randomString() {
        return UUID.randomUUID().toString();
    }
}
