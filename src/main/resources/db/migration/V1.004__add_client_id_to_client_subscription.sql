ALTER TABLE client_subscription
    ADD COLUMN client_id VARCHAR(256);

CREATE UNIQUE INDEX idx_client_subscription_client_id_endpoint
    ON client_subscription (client_id, notification_endpoint);

CREATE INDEX idx_client_subscription_client_id
    ON client_subscription (client_id);
