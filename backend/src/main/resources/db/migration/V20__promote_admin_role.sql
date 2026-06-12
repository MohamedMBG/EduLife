-- Re-promote admin@edulife.test to ADMIN.
-- V18 already ran on environments where this account's row did not exist yet
-- (auth-sync creates it as LEARNER on first login, and ADMIN is not
-- self-assignable). This repeats the promotion now that the row exists.
-- No-op if the account is absent or already ADMIN.
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@edulife.test';
