-- ACCOUNT ANONYMIZATION (P8 #276): allow email and firebase_uid to be NULL so a self-deleted
-- user can have PII stripped while audit-bound rows (certificates, enrollments, exam_attempts)
-- keep a stable user_id reference. UNIQUE constraints stay in place because Postgres treats
-- NULL values as distinct, so multiple anonymized users coexist without collision.

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL,
    ALTER COLUMN firebase_uid DROP NOT NULL;
