CREATE TABLE teacher_requests (
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    motivation   TEXT,
    admin_note   TEXT,
    reviewed_by  UUID        REFERENCES users(id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at  TIMESTAMPTZ,
    CONSTRAINT chk_teacher_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_teacher_requests_user_id ON teacher_requests(user_id);
CREATE INDEX idx_teacher_requests_status  ON teacher_requests(status);
