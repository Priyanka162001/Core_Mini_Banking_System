CREATE TABLE IF NOT EXISTS customer_profiles (
    customer_profile_id     BIGSERIAL PRIMARY KEY,
    customer_id             BIGINT          NOT NULL UNIQUE,
    username                VARCHAR(255)    NOT NULL,
    date_of_birth           DATE,
    gender                  VARCHAR(20),
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,

    CONSTRAINT fk_customer_profile_user
        FOREIGN KEY (customer_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_customer_profile_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'))
);

CREATE INDEX IF NOT EXISTS idx_customer_profile_customer_id
    ON customer_profiles(customer_id);

-- ================================================================

CREATE TABLE IF NOT EXISTS customer_addresses (
    address_id              BIGSERIAL PRIMARY KEY,
    customer_profile_id     BIGINT          NOT NULL,
    address_type            VARCHAR(20)     NOT NULL,
    address_line            VARCHAR(255)    NOT NULL,
    city                    VARCHAR(100),
    state                   VARCHAR(100),
    postal_code             VARCHAR(10),
    country                 VARCHAR(100),
    is_active               BOOLEAN         DEFAULT TRUE,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,

    CONSTRAINT fk_address_customer_profile
        FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(customer_profile_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_address_type
        CHECK (address_type IN ('HOME', 'WORK', 'PERMANENT', 'CURRENT')),

    CONSTRAINT chk_postal_code_india
        CHECK (postal_code ~ '^[1-9][0-9]{5}$')
);

CREATE INDEX IF NOT EXISTS idx_customer_address_profile_id
    ON customer_addresses(customer_profile_id);

CREATE INDEX IF NOT EXISTS idx_customer_address_type
    ON customer_addresses(address_type);

CREATE INDEX IF NOT EXISTS idx_customer_address_active
    ON customer_addresses(is_active);