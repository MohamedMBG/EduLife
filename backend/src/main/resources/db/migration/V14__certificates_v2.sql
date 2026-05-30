ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS exam_attempt_id UUID,
    ADD COLUMN IF NOT EXISTS student_name    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS course_title    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS issuer_name     VARCHAR(200),
    ADD COLUMN IF NOT EXISTS verification_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS pdf_url         TEXT,
    ADD COLUMN IF NOT EXISTS created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE certificates
    ADD CONSTRAINT uq_certificates_verification_hash UNIQUE (verification_hash);

ALTER TABLE certificates
    ADD CONSTRAINT uq_certificates_exam_attempt UNIQUE (exam_attempt_id);

CREATE INDEX IF NOT EXISTS idx_certificates_verification_hash ON certificates (verification_hash);
