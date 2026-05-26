CREATE TABLE certificates (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    course_id          UUID         NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    certificate_number VARCHAR(50)  NOT NULL,
    issued_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_certificates PRIMARY KEY (id),
    CONSTRAINT uq_certificates_user_course  UNIQUE (user_id, course_id),
    CONSTRAINT uq_certificates_number       UNIQUE (certificate_number)
);

CREATE INDEX idx_certificates_user ON certificates (user_id);
