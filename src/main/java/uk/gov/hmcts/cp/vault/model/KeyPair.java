package uk.gov.hmcts.cp.vault.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeyPair {
    private String keyId;
    private byte[] secret;
}