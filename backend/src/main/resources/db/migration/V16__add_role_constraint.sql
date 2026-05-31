-- Add explicit CHECK constraint for all valid roles now that GROUP_ADMIN is introduced.
-- V1 created the column without a constraint; this adds data integrity at the DB level.
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('LEARNER', 'TEACHER', 'GROUP_ADMIN', 'ADMIN'));
