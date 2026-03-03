-- Create enum first
CREATE TYPE enum_product_status AS ENUM ('ACTIVE', 'INACTIVE');

-- Create table
CREATE TABLE savings_products (
    savings_product_id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL UNIQUE,

    interest_rate_percent DECIMAL(5,2) NOT NULL,
    minimum_opening_balance_amount DECIMAL(15,2) NOT NULL,
    minimum_maintaining_balance_amount DECIMAL(15,2) NOT NULL,

    interest_application_frequency_code VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    product_status enum_product_status NOT NULL DEFAULT 'ACTIVE',

    effective_from_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_savings_product_interest_rate
        CHECK (interest_rate_percent >= 0),

    CONSTRAINT chk_savings_product_min_open_balance
        CHECK (minimum_opening_balance_amount >= 0),

    CONSTRAINT chk_savings_product_min_maint_balance
        CHECK (minimum_maintaining_balance_amount >= 0),

    CONSTRAINT chk_expiry_after_effective
        CHECK (expiry_date IS NULL OR expiry_date >= effective_from_date)
);
