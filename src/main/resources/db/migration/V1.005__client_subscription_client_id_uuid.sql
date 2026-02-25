ALTER TABLE client_subscription
    ALTER COLUMN client_id TYPE uuid USING client_id::uuid;
