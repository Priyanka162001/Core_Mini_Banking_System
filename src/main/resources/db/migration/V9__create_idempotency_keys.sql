CREATE TABLE IF NOT EXISTS idempotency_keys (
    key         VARCHAR(255)    PRIMARY KEY,
    response    TEXT,
    status_code INTEGER,
    created_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotency_created_at
    ON idempotency_keys(created_at);