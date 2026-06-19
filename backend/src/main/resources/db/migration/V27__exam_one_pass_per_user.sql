-- Guarantees at most one passing attempt per (user, exam). Two concurrent exam submissions
-- could both pass the application-level "already passed?" check before either committed,
-- double-awarding XP and issuing two certificates. This partial unique index makes the second
-- concurrent passing insert fail at the database, which ExamService translates into the normal
-- "already passed" response. Failing attempts are unaffected (the index covers passed = true only).

-- Collapse any pre-existing duplicate passing attempts (from before this guard) down to the
-- earliest one per (user, exam) so the unique index can be created without error. The certificate
-- and XP records already reference the kept attempt; later duplicate passes are demoted to failed
-- rather than deleted to preserve the attempt audit trail.
UPDATE exam_attempts a
SET passed = false
WHERE passed = true
  AND EXISTS (
        SELECT 1 FROM exam_attempts b
        WHERE b.user_id = a.user_id
          AND b.exam_id = a.exam_id
          AND b.passed = true
          AND (b.taken_at < a.taken_at OR (b.taken_at = a.taken_at AND b.id < a.id))
  );

CREATE UNIQUE INDEX idx_exam_attempts_one_pass
    ON exam_attempts (user_id, exam_id)
    WHERE passed = true;
