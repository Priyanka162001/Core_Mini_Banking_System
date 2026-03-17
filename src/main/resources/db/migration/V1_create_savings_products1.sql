CREATE TYPE enum_interest_application_frequency AS ENUM (
    'MONTHLY',
    'QUARTERLY',
    'YEARLY'
);

CREATE TYPE enum_product_status AS ENUM (
    'ACTIVE',
    'INACTIVE'
);

CREATE TABLE savings_products (

    id BIGSERIAL PRIMARY KEY,

    product_code VARCHAR(50) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL UNIQUE,

    interest_rate_percent NUMERIC(5,2) NOT NULL,

    minimum_opening_balance_amount NUMERIC(15,2) NOT NULL,
    minimum_maintaining_balance_amount NUMERIC(15,2) NOT NULL,

    interest_application_frequency_code enum_interest_application_frequency NOT NULL,
    product_status enum_product_status NOT NULL,

    effective_from_date DATE NOT NULL,
    expiry_date DATE,

    min_age INTEGER NOT NULL,
    max_age INTEGER NOT NULL,

    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    created_at TIMESTAMP,
    updated_at TIMESTAMP
);