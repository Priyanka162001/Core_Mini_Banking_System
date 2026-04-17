CREATE TABLE IF NOT EXISTS savings_products (
    id                                      BIGSERIAL PRIMARY KEY,
    product_code                            VARCHAR(50)     NOT NULL UNIQUE,
    product_name                            VARCHAR(100)    NOT NULL UNIQUE,
    interest_rate_percent                   NUMERIC(5,2)    NOT NULL,
    minimum_opening_balance_amount          NUMERIC(15,2)   NOT NULL,
    minimum_maintaining_balance_amount      NUMERIC(15,2)   NOT NULL,
    interest_application_frequency_code     VARCHAR(20)     NOT NULL,
    product_status                          VARCHAR(20)     NOT NULL,
    effective_from_date                     DATE            NOT NULL,
    expiry_date                             DATE,
    min_age                                 INTEGER         NOT NULL,
    max_age                                 INTEGER         NOT NULL,
    created_at                              TIMESTAMP,
    updated_at                              TIMESTAMP,
    created_by                              BIGINT,
    updated_by                              BIGINT,

    CONSTRAINT chk_product_interest_rate
        CHECK (interest_rate_percent >= 0),

    CONSTRAINT chk_product_min_open_balance
        CHECK (minimum_opening_balance_amount >= 0),

    CONSTRAINT chk_product_min_maint_balance
        CHECK (minimum_maintaining_balance_amount >= 0),

    CONSTRAINT chk_product_expiry_after_effective
        CHECK (expiry_date IS NULL OR expiry_date >= effective_from_date),

    CONSTRAINT chk_product_age_range
        CHECK (min_age >= 0 AND max_age >= min_age),

    CONSTRAINT chk_product_frequency
        CHECK (interest_application_frequency_code IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),

    CONSTRAINT chk_product_status
        CHECK (product_status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);