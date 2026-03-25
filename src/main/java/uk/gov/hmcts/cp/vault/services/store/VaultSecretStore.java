package uk.gov.hmcts.cp.vault.services.store;

public interface VaultSecretStore {

    void setSecret(String secretName, String secretValue);

    String getSecret(String secretName);
}