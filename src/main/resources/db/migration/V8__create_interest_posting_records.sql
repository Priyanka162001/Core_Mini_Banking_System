CREATE TABLE IF NOT EXISTS interest_posting_records (
    id                  BIGSERIAL PRIMARY KEY,
    savings_account_id  BIGINT          NOT NULL,
    interest_amount     NUMERIC(19,4)   NOT NULL,
    balance_before      NUMERIC(19,4)   NOT NULL,
    balance_after       NUMERIC(19,4)   NOT NULL,
    annual_interest_rate NUMERIC(5,2)   NOT NULL,
    posting_month       INTEGER         NOT NULL,
    posting_year        INTEGER         NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,

    CONSTRAINT uq_interest_account_period
        UNIQUE (savings_account_id, posting_month, posting_year),

    CONSTRAINT fk_interest_posting_account
        FOREIGN KEY (savings_account_id) REFERENCES savings_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_interest_posting_status
        CHECK (status IN ('SUCCESS', 'FAILED')),

    CONSTRAINT chk_interest_posting_month
        CHECK (posting_month BETWEEN 1 AND 12),

    CONSTRAINT chk_interest_posting_year
        CHECK (posting_year >= 2000)
);

CREATE INDEX IF NOT EXISTS idx_interest_posting_account_id
    ON interest_posting_records(savings_account_id);

CREATE INDEX IF NOT EXISTS idx_interest_posting_period
    ON interest_posting_records(posting_year, posting_month);