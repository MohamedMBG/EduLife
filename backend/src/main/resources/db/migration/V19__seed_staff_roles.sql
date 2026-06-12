-- Promote seeded staff accounts to their roles (same pattern as V18).
-- Rows are created by auth-sync on first login with LEARNER default (login path
-- sends no intendedRole); this migration corrects the role on environments where
-- the accounts already exist. If an account does not exist yet it is a no-op.
UPDATE users SET role = 'TEACHER'     WHERE email = 'teacher@edulife.test';
UPDATE users SET role = 'GROUP_ADMIN' WHERE email = 'groupadmin@edulife.test';
