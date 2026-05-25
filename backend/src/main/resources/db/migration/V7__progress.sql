CREATE TABLE lesson_progress (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    lesson_id    UUID        NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    course_id    UUID        NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_lesson_progress PRIMARY KEY (id),
    CONSTRAINT uq_lesson_progress_user_lesson UNIQUE (user_id, lesson_id)
);

CREATE INDEX idx_lesson_progress_user_course ON lesson_progress (user_id, course_id);

CREATE TABLE course_progress (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    course_id         UUID        NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    completed_lessons INT         NOT NULL DEFAULT 0,
    total_lessons     INT         NOT NULL DEFAULT 0,
    last_updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_course_progress PRIMARY KEY (id),
    CONSTRAINT uq_course_progress_user_course UNIQUE (user_id, course_id)
);
