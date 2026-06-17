-- Align exam pass_score default and seed data to the locked 80% project rule.
-- V9 set DEFAULT 70 and seeded 5 exams with 70. CLAUDE.md/AGENTS.md lock pass score at 80%.

-- 1. Change column default from 70 to 80
ALTER TABLE exams ALTER COLUMN pass_score SET DEFAULT 80;

-- 2. Update the 5 seed/demo exams (well-known UUIDs from V9) from 70 to 80
UPDATE exams
SET pass_score = 80
WHERE id IN (
    '10000001-0000-0000-0000-000000000000',
    '20000001-0000-0000-0000-000000000000',
    '30000001-0000-0000-0000-000000000000',
    '40000001-0000-0000-0000-000000000000',
    '50000001-0000-0000-0000-000000000000'
)
AND pass_score = 70;
