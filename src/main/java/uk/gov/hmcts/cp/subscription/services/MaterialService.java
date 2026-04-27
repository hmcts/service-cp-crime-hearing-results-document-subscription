package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.subscription.clients.MaterialClient;
import uk.gov.hmcts.cp.subscription.model.MaterialMetadata;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialClient materialClient;

    public MaterialMetadata getMaterialMetadata(final UUID materialId) {
        log.info("getMaterialMetadata materialId:{}", materialId);
        return materialClient.getMetadata(materialId);
    }
}
