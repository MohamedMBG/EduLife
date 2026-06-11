-- Promote admin@edulife.test to ADMIN role.
-- The account is created by Firebase on first login (auth-sync inserts with LEARNER default).
-- This migration runs after that insert; if the account does not exist yet it is a no-op.
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@edulife.test';
