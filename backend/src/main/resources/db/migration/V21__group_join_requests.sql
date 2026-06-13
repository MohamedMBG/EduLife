-- Teachers who want institute backing request access to a group instead of being
-- added silently. Approval creates the group membership that later scopes course review.
CREATE TABLE group_join_requests (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    group_id            UUID        NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    requester_user_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    motivation          TEXT,
    admin_note          TEXT,
    reviewed_by_user_id UUID        REFERENCES users(id) ON DELETE SET NULL,
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at         TIMESTAMPTZ,
    CONSTRAINT pk_group_join_requests PRIMARY KEY (id),
    CONSTRAINT ck_group_join_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_group_join_requests_group_status ON group_join_requests (group_id, status);
CREATE INDEX idx_group_join_requests_requester ON group_join_requests (requester_user_id, requested_at DESC);

-- A teacher may re-apply after rejection, but only one pending request per group is useful.
CREATE UNIQUE INDEX uq_group_join_requests_pending
    ON group_join_requests (group_id, requester_user_id)
    WHERE status = 'PENDING';
