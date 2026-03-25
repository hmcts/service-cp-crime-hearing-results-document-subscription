package uk.gov.hmcts.cp.vault.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.vault.model.KeyPair;

import java.security.InvalidKeyException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VaultSigningServiceTest {

    private static final byte[] TEST_SECRET = "Stub string used purely for development purposes. To be secured.".getBytes(UTF_8);
    private static final KeyPair TEST_KEY_PAIR = KeyPair.builder().keyId("kid_test").secret(TEST_SECRET).build();

    private final VaultSigningService vaultSigningService = new VaultSigningService();

    String message = "A message";

    @Test
    void real_signature_and_message_should_match() throws InvalidKeyException {
        // Message sent at 14:30:07
        String longMessage = "{\"cases\":[{\"urn\":\"AAA20713684\"}],\"masterDefendantId\":\"d5ad03d0-5f2d-477b-9aa8-e0beb68f46a3\",\"defendantName\":\"Sarogav Noem\",\"defendantDateOfBirth\":\"2008-12-08\",\"documentId\":\"6deab43e-70f2-4d8e-bc28-04ab39fbeff1\",\"documentGeneratedTimestamp\":\"2026-03-20T14:29:15.453071625Z\",\"prisonEmailAddress\":\"yoiashfield.premiercustody@premier-serco.cjsm.net\"}";
        String signature = vaultSigningService.sign(TEST_KEY_PAIR.getSecret(), longMessage);
        vaultSigningService.validateSignature(TEST_KEY_PAIR.getSecret(), longMessage, signature);
        assertThat(signature).isEqualTo("mevCvmfiXXoHoeRbvgat8PtHjnAvOgnXMMUgZ+BKmTE=");

        String encodedSecret = new Base64EncodingService().encodeWithBase64(TEST_KEY_PAIR.getSecret());
        assertThat(encodedSecret).isEqualTo("U3R1YiBzdHJpbmcgdXNlZCBwdXJlbHkgZm9yIGRldmVsb3BtZW50IHB1cnBvc2VzLiBUbyBiZSBzZWN1cmVkLg==");
    }

    @Test
    void validate_should_verify_ok() throws InvalidKeyException {
        String signature = vaultSigningService.sign(TEST_KEY_PAIR.getSecret(), message);
        assertThat(signature).isEqualTo("TkkWx3X55YaWF5KB2BwSY4LcoDnFqZFOMrB43hkuFkE=");
        vaultSigningService.validateSignature(TEST_KEY_PAIR.getSecret(), message, signature);
        // no exception
    }

    @Test
    void validate_should_throw_if_bad_signature() throws InvalidKeyException {
        assertThrows(InvalidKeyException.class, () ->
                vaultSigningService.validateSignature(TEST_KEY_PAIR.getSecret(), message, "bad-signature"));
    }

    @Test
    void validate_should_throw_if_bad_secret() throws InvalidKeyException {
        String signature = vaultSigningService.sign(TEST_KEY_PAIR.getSecret(), message);
        assertThrows(InvalidKeyException.class, () ->
                vaultSigningService.validateSignature("bad-=secret".getBytes(), message, signature));
    }
}