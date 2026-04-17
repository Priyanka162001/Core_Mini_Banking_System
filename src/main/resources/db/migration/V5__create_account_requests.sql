CREATE TABLE IF NOT EXISTS account_requests (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    product_id          BIGINT          NOT NULL,
    initial_deposit     NUMERIC(19,2),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    reviewed_by         BIGINT,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,

    CONSTRAINT fk_account_request_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_account_request_product
        FOREIGN KEY (product_id) REFERENCES savings_products(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_account_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),

    CONSTRAINT chk_account_request_initial_deposit
        CHECK (initial_deposit IS NULL OR initial_deposit >= 0)
);

CREATE INDEX IF NOT EXISTS idx_account_request_user_id
    ON account_requests(user_id);

CREATE INDEX IF NOT EXISTS idx_account_request_status
    ON account_requests(status);