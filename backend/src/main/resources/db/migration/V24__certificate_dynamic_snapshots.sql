-- Certificates must preserve the exact learner, instructor, course title, and
-- level visible at issue time. These snapshot columns prevent later profile or
-- course edits from rewriting historical certificates.
ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS learner_name_snapshot VARCHAR(200),
    ADD COLUMN IF NOT EXISTS teacher_name_snapshot VARCHAR(200),
    ADD COLUMN IF NOT EXISTS course_title_snapshot VARCHAR(255),
    ADD COLUMN IF NOT EXISTS course_level_snapshot VARCHAR(50);

-- Seed catalog courses predate teacher-owned CMS rows. Attach only orphaned
-- seed/demo courses to a real backend teacher record so certificate generation
-- can resolve an instructor through courses.created_by_user_id.
INSERT INTO users (id, firebase_uid, email, role, created_at)
VALUES (
    '66666666-6666-6666-6666-666666666666',
    'seed-instructor-edulife',
    'seed.instructor@edulife.local',
    'TEACHER',
    CURRENT_TIMESTAMP
)
ON CONFLICT (firebase_uid) DO UPDATE
SET role = 'TEACHER';

INSERT INTO profiles (id, user_id, display_name, bio, created_at, updated_at)
VALUES (
    '77777777-7777-7777-7777-777777777777',
    '66666666-6666-6666-6666-666666666666',
    'EduLife Instructor',
    'Seed instructor for published MVP catalog courses.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (user_id) DO UPDATE
SET display_name = EXCLUDED.display_name,
    updated_at = CURRENT_TIMESTAMP;

UPDATE courses
SET created_by_user_id = '66666666-6666-6666-6666-666666666666',
    updated_at = CURRENT_TIMESTAMP
WHERE created_by_user_id IS NULL
  AND id IN (
      '11111111-1111-1111-1111-111111111111',
      '22222222-2222-2222-2222-222222222222',
      '33333333-3333-3333-3333-333333333333',
      '44444444-4444-4444-4444-444444444444',
      '55555555-5555-5555-5555-555555555555'
  );

UPDATE certificates cert
SET learner_name_snapshot = COALESCE(
        NULLIF(cert.learner_name_snapshot, ''),
        NULLIF(cert.student_name, ''),
        NULLIF(learner_profile.display_name, ''),
        learner.email,
        cert.user_id::TEXT
    ),
    teacher_name_snapshot = COALESCE(
        NULLIF(cert.teacher_name_snapshot, ''),
        NULLIF(cert.issuer_name, ''),
        NULLIF(teacher_profile.display_name, ''),
        teacher.email,
        course.created_by_user_id::TEXT
    ),
    course_title_snapshot = COALESCE(
        NULLIF(cert.course_title_snapshot, ''),
        NULLIF(cert.course_title, ''),
        course.title
    ),
    course_level_snapshot = COALESCE(
        NULLIF(cert.course_level_snapshot, ''),
        course.level
    )
FROM users learner
LEFT JOIN profiles learner_profile ON learner_profile.user_id = learner.id,
courses course
LEFT JOIN users teacher ON teacher.id = course.created_by_user_id
LEFT JOIN profiles teacher_profile ON teacher_profile.user_id = teacher.id
WHERE learner.id = cert.user_id
  AND course.id = cert.course_id;
