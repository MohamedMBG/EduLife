-- Supports countByUserIdAndExamIdAndPassedFalse and the cooldown last-failure lookup
-- without a full table scan on exam_attempts.
CREATE INDEX idx_exam_attempts_user_passed
    ON exam_attempts (user_id, exam_id, passed);
