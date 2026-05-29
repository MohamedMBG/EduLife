-- FULL-TEXT SEARCH FOR COURSE CATALOG
-- Adds a tsvector column to courses, a GIN index for fast search, and a trigger to
-- keep the vector current whenever a course row is inserted or updated.
-- Using the 'simple' dictionary (no language-specific stemming) keeps the implementation
-- safe for multilingual course data (fr, en, ar) without requiring per-language config.

ALTER TABLE courses ADD COLUMN search_vector tsvector;

-- Backfill existing rows before the trigger is in place.
-- Concatenating title + description + short_description gives the widest search surface.
UPDATE courses
SET search_vector = to_tsvector(
    'simple',
    coalesce(title, '') || ' ' ||
    coalesce(description, '') || ' ' ||
    coalesce(short_description, '')
);

-- GIN index makes tsvector @@ tsquery lookups O(log n) instead of a full table scan.
CREATE INDEX idx_courses_search_vector ON courses USING GIN(search_vector);

-- Function called by the trigger to recompute the vector on every INSERT or UPDATE.
CREATE OR REPLACE FUNCTION update_course_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'simple',
        coalesce(NEW.title, '') || ' ' ||
        coalesce(NEW.description, '') || ' ' ||
        coalesce(NEW.short_description, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- BEFORE trigger ensures search_vector is always set before the row is committed,
-- so no separate UPDATE is required after a CMS course creation or edit.
CREATE TRIGGER courses_search_vector_update
    BEFORE INSERT OR UPDATE ON courses
    FOR EACH ROW EXECUTE FUNCTION update_course_search_vector();
