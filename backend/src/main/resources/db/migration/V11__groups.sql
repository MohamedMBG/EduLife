-- GROUPS: teacher- or admin-managed cohorts that bundle learners and the courses assigned to them.
-- Each group is owned by the user who created it (created_by). Membership and course attachments
-- are modelled as join tables so the same learner or course can appear in multiple groups.

CREATE TABLE groups (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_by UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_groups PRIMARY KEY (id)
);

CREATE INDEX idx_groups_created_by ON groups (created_by);

CREATE TABLE group_members (
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_group_members PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members (user_id);

CREATE TABLE group_courses (
    group_id    UUID NOT NULL REFERENCES groups(id)   ON DELETE CASCADE,
    course_id   UUID NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,
    attached_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_group_courses PRIMARY KEY (group_id, course_id)
);

CREATE INDEX idx_group_courses_course ON group_courses (course_id);
