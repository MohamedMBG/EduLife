CREATE TABLE enrollments (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id),
    course_id   UUID        NOT NULL REFERENCES courses(id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_enrollments_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT uk_enrollments_user_course UNIQUE (user_id, course_id)
);

CREATE INDEX idx_enrollments_user_id   ON enrollments(user_id);
CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);
