CREATE TABLE IF NOT EXISTS savings_accounts (
    id                      BIGSERIAL PRIMARY KEY,
    account_number          VARCHAR(255)    NOT NULL UNIQUE,
    user_id                 BIGINT          NOT NULL,
    savings_product_id      BIGINT          NOT NULL,
    current_balance_amount  NUMERIC(19,2)   NOT NULL,
    interest_rate           NUMERIC(5,2)    NOT NULL,
    account_status          VARCHAR(20)     NOT NULL,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,

    CONSTRAINT fk_savings_account_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_savings_account_product
        FOREIGN KEY (savings_product_id) REFERENCES savings_products(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_savings_account_status
        CHECK (account_status IN ('ACTIVE', 'FROZEN', 'CLOSED')),

    CONSTRAINT chk_savings_account_balance
        CHECK (current_balance_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_savings_account_user_id
    ON savings_accounts(user_id);

CREATE INDEX IF NOT EXISTS idx_savings_account_status
    ON savings_accounts(account_status);