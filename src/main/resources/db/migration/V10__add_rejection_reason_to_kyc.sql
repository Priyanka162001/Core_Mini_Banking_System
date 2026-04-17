ALTER TABLE kyc_records
ADD COLUMN IF NOT EXISTS rejection_reason TEXT;