CREATE TABLE IF NOT EXISTS kyc_records (
    id                      BIGSERIAL PRIMARY KEY,
    customer_id             BIGINT          NOT NULL UNIQUE,
    doc_type                VARCHAR(20)     NOT NULL,
    doc_number              VARCHAR(20)     NOT NULL,
    document_image_url      TEXT            NOT NULL,
    kyc_verification_status VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    verified_at             TIMESTAMP,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,

    CONSTRAINT fk_kyc_user
        FOREIGN KEY (customer_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_kyc_verification_status
        CHECK (kyc_verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_kyc_customer_id
    ON kyc_records(customer_id);

CREATE INDEX IF NOT EXISTS idx_kyc_status
    ON kyc_records(kyc_verification_status);