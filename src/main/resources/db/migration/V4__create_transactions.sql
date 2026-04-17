CREATE TABLE IF NOT EXISTS transactions (
    id                          BIGSERIAL PRIMARY KEY,
    account_id                  BIGINT          NOT NULL,
    type                        VARCHAR(20),
    status                      VARCHAR(20),
    amount                      NUMERIC(19,2),
    currency                    VARCHAR(10),
    payment_mode                VARCHAR(20),
    description                 VARCHAR(255),
    balance_before_transaction  NUMERIC(19,2),
    balance_after_transaction   NUMERIC(19,2),
    interest_posting            NUMERIC(19,2),
    interest_posted_at          TIMESTAMP,
    transaction_date            TIMESTAMP,
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  BIGINT,
    updated_by                  BIGINT,

    CONSTRAINT fk_transaction_account
        FOREIGN KEY (account_id) REFERENCES savings_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_transaction_type
        CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'INTEREST')),

    CONSTRAINT chk_transaction_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),

    CONSTRAINT chk_transaction_currency
        CHECK (currency IN ('INR', 'USD', 'EUR')),

    CONSTRAINT transactions_payment_mode_check
        CHECK (payment_mode IN ('CASH', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'CARD', 'SYSTEM')),

    CONSTRAINT chk_transaction_amount
        CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_transaction_account_id
    ON transactions(account_id);

CREATE INDEX IF NOT EXISTS idx_transaction_type
    ON transactions(type);

CREATE INDEX IF NOT EXISTS idx_transaction_date
    ON transactions(transaction_date);

CREATE INDEX IF NOT EXISTS idx_transaction_status
    ON transactions(status);