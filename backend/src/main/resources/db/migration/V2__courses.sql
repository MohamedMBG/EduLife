-- COURSE CATALOG TABLES
-- This migration stays focused on Sprint 2 discovery data so the learner flow can move
-- forward without locking the project into early CMS or file-hosting decisions.

CREATE TABLE courses (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    short_description VARCHAR(500),
    description TEXT NOT NULL,
    language_code VARCHAR(10) NOT NULL DEFAULT 'fr',
    level VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Course state is constrained in the database so admin approval flow cannot drift
    -- into arbitrary values from future clients or scripts.
    CONSTRAINT chk_courses_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE course_sections (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Section order starts at 1 to keep API ordering predictable and avoid zero/negative gaps.
    CONSTRAINT chk_course_sections_display_order CHECK (display_order > 0),

    -- One section position per course prevents duplicate ordering inside the same outline.
    CONSTRAINT uq_course_sections_course_order UNIQUE (course_id, display_order)
);

CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    course_section_id UUID NOT NULL REFERENCES course_sections(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    lesson_type VARCHAR(20) NOT NULL DEFAULT 'VIDEO',
    estimated_duration_minutes INTEGER,
    display_order INTEGER NOT NULL,
    is_preview BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- MVP lesson types stay narrow so Android and backend contracts remain simple.
    CONSTRAINT chk_lessons_lesson_type CHECK (lesson_type IN ('VIDEO', 'ARTICLE', 'RESOURCE')),

    -- Duration stays optional until all lesson sources are normalized, but invalid negatives
    -- are rejected early so progress and UI summaries do not have to defend against bad data.
    CONSTRAINT chk_lessons_estimated_duration CHECK (
        estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0
    ),

    -- Lesson order starts at 1 to match section ordering semantics in the learner UI.
    CONSTRAINT chk_lessons_display_order CHECK (display_order > 0),

    -- One lesson position per section prevents ambiguous rendering order in the app.
    CONSTRAINT uq_lessons_section_order UNIQUE (course_section_id, display_order)
);

-- Catalog screens will commonly filter by course publication state.
CREATE INDEX idx_courses_status ON courses (status);

-- Published catalog pages sort by release time, so this index keeps the list query simple.
CREATE INDEX idx_courses_published_at ON courses (published_at DESC);

-- Teacher-scoped admin and CMS queries can resolve authored courses without scanning the catalog.
CREATE INDEX idx_courses_created_by_user_id ON courses (created_by_user_id);

-- Section lookups always start from the parent course and then apply display order in the UI.
CREATE INDEX idx_course_sections_course_id ON course_sections (course_id);

-- Lesson lookups always start from the parent section and then apply display order in the UI.
CREATE INDEX idx_lessons_course_section_id ON lessons (course_section_id);
