CREATE TABLE client (
    client_id uuid PRIMARY KEY NOT NULL,
    subscription_id uuid UNIQUE NOT NULL,
    callback_url VARCHAR(2048) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE UNIQUE INDEX idx_client_subscription_id ON client(subscription_id);