CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone_number    VARCHAR(255) NOT NULL UNIQUE,
    country_code    VARCHAR(50),
    password        VARCHAR(255),
    role            VARCHAR(50),
    status          VARCHAR(50),
    kyc_status      VARCHAR(50),
    otp             VARCHAR(10),
    otp_expiry      TIMESTAMP,
    email_verified  BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,

    CONSTRAINT chk_user_role
        CHECK (role IN ('ROLE_ADMIN', 'ROLE_CUSTOMER')),

    CONSTRAINT chk_user_status
        CHECK (status IN ('ACTIVE', 'PENDING', 'BLOCKED')),

    CONSTRAINT chk_user_kyc_status
        CHECK (kyc_status IS NULL OR kyc_status IN ('MIN_KYC', 'FULL_KYC'))
);