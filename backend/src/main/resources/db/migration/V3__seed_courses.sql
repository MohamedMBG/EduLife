-- COURSE DISCOVERY SEED DATA
-- Sprint 2 needs stable published catalog data so Android can integrate against the real
-- backend contract instead of long-lived mock APIs.

-- Five published courses give the catalog enough variety for list, detail, and preview states.
INSERT INTO courses (
    id,
    slug,
    title,
    short_description,
    description,
    language_code,
    level,
    status,
    published_at
) VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'math-bac-sm-algebra-foundations',
        'Math Bac SM - Algebra Foundations',
        'A structured algebra refresher for Moroccan Bac Sciences Math students.',
        'Build core confidence in equations, functions, and algebraic methods used across Bac Sciences Math coursework.',
        'fr',
        'BEGINNER',
        'PUBLISHED',
        CURRENT_TIMESTAMP
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'physics-motion-and-forces',
        'Physics - Motion and Forces',
        'Learn the mechanics basics needed for secondary school physics success.',
        'Covers motion graphs, Newtonian mechanics, and common problem-solving patterns in a guided sequence.',
        'fr',
        'INTERMEDIATE',
        'PUBLISHED',
        CURRENT_TIMESTAMP
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'english-communication-essentials',
        'English Communication Essentials',
        'Improve reading, listening, and classroom communication with practical lessons.',
        'Focuses on high-frequency grammar, comprehension, and spoken classroom English for learners building confidence.',
        'en',
        'BEGINNER',
        'PUBLISHED',
        CURRENT_TIMESTAMP
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        'french-expression-and-writing',
        'French Expression and Writing',
        'Strengthen written French through structure, clarity, and revision habits.',
        'Guides students through sentence building, paragraph organization, and exam-ready written expression.',
        'fr',
        'INTERMEDIATE',
        'PUBLISHED',
        CURRENT_TIMESTAMP
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        'digital-skills-study-productivity',
        'Digital Skills for Study Productivity',
        'Use practical digital habits to organize study time and course materials.',
        'Introduces note-taking workflows, file organization, and focused study routines for mobile-first learners.',
        'en',
        'BEGINNER',
        'PUBLISHED',
        CURRENT_TIMESTAMP
    );

-- Section ordering is explicit so course detail screens can render predictable learning paths.
INSERT INTO course_sections (
    id,
    course_id,
    title,
    description,
    display_order
) VALUES
    ('11111111-aaaa-aaaa-aaaa-111111111111', '11111111-1111-1111-1111-111111111111', 'Algebra Basics', 'Start with core algebra language and operations.', 1),
    ('11111111-bbbb-bbbb-bbbb-111111111111', '11111111-1111-1111-1111-111111111111', 'Functions and Equations', 'Move from symbolic manipulation to exam-style equations.', 2),
    ('22222222-aaaa-aaaa-aaaa-222222222222', '22222222-2222-2222-2222-222222222222', 'Kinematics', 'Read motion situations and connect them to formulas.', 1),
    ('22222222-bbbb-bbbb-bbbb-222222222222', '22222222-2222-2222-2222-222222222222', 'Forces in Action', 'Apply Newtonian reasoning to classroom problems.', 2),
    ('33333333-aaaa-aaaa-aaaa-333333333333', '33333333-3333-3333-3333-333333333333', 'Everyday English', 'Build core vocabulary and listening confidence.', 1),
    ('33333333-bbbb-bbbb-bbbb-333333333333', '33333333-3333-3333-3333-333333333333', 'Academic Communication', 'Use English more clearly in study contexts.', 2),
    ('44444444-aaaa-aaaa-aaaa-444444444444', '44444444-4444-4444-4444-444444444444', 'Sentence Construction', 'Write cleaner, more accurate French sentences.', 1),
    ('44444444-bbbb-bbbb-bbbb-444444444444', '44444444-4444-4444-4444-444444444444', 'Structured Writing', 'Turn ideas into organized written responses.', 2),
    ('55555555-aaaa-aaaa-aaaa-555555555555', '55555555-5555-5555-5555-555555555555', 'Digital Study Basics', 'Set up tools and habits for daily learning.', 1),
    ('55555555-bbbb-bbbb-bbbb-555555555555', '55555555-5555-5555-5555-555555555555', 'Productive Workflows', 'Use simple routines to stay organized and focused.', 2);

-- Each course includes at least one preview lesson so catalog detail screens can expose a
-- free entry point before enrollment without leaking the full lesson flow.
INSERT INTO lessons (
    id,
    course_section_id,
    title,
    summary,
    lesson_type,
    estimated_duration_minutes,
    display_order,
    is_preview
) VALUES
    ('11111111-aaaa-0000-0000-111111111111', '11111111-aaaa-aaaa-aaaa-111111111111', 'Understanding Algebraic Expressions', 'Identify variables, constants, and operations in simple expressions.', 'VIDEO', 12, 1, TRUE),
    ('11111111-aaaa-0000-0000-222222222222', '11111111-aaaa-aaaa-aaaa-111111111111', 'Simplifying Expressions', 'Practice grouping like terms and reducing expressions accurately.', 'VIDEO', 15, 2, FALSE),
    ('11111111-bbbb-0000-0000-111111111111', '11111111-bbbb-bbbb-bbbb-111111111111', 'Linear Equations Step by Step', 'Solve one-variable equations using a repeatable method.', 'VIDEO', 18, 1, FALSE),
    ('11111111-bbbb-0000-0000-222222222222', '11111111-bbbb-bbbb-bbbb-111111111111', 'Reading Basic Functions', 'Connect function notation to tables and simple graphs.', 'ARTICLE', 10, 2, FALSE),

    ('22222222-aaaa-0000-0000-111111111111', '22222222-aaaa-aaaa-aaaa-222222222222', 'Distance, Time, and Speed', 'Introduce the core relationship behind motion problems.', 'VIDEO', 14, 1, TRUE),
    ('22222222-aaaa-0000-0000-222222222222', '22222222-aaaa-aaaa-aaaa-222222222222', 'Reading Motion Graphs', 'Interpret slope and direction using simple graph examples.', 'VIDEO', 16, 2, FALSE),
    ('22222222-bbbb-0000-0000-111111111111', '22222222-bbbb-bbbb-bbbb-222222222222', 'Balanced and Unbalanced Forces', 'Describe how forces change an object state.', 'VIDEO', 13, 1, FALSE),
    ('22222222-bbbb-0000-0000-222222222222', '22222222-bbbb-bbbb-bbbb-222222222222', 'Applying Newtons Laws', 'Use Newtonian logic to solve guided classroom exercises.', 'RESOURCE', 9, 2, FALSE),

    ('33333333-aaaa-0000-0000-111111111111', '33333333-aaaa-aaaa-aaaa-333333333333', 'Greetings and Introductions', 'Use common phrases for introductions and simple exchanges.', 'VIDEO', 11, 1, TRUE),
    ('33333333-aaaa-0000-0000-222222222222', '33333333-aaaa-aaaa-aaaa-333333333333', 'Listening for Key Information', 'Pick out names, places, and main ideas from short audio.', 'VIDEO', 14, 2, FALSE),
    ('33333333-bbbb-0000-0000-111111111111', '33333333-bbbb-bbbb-bbbb-333333333333', 'Classroom Questions in English', 'Ask for clarification, repetition, and examples naturally.', 'ARTICLE', 8, 1, FALSE),
    ('33333333-bbbb-0000-0000-222222222222', '33333333-bbbb-bbbb-bbbb-333333333333', 'Writing Short Study Messages', 'Compose short, clear messages to teachers and classmates.', 'VIDEO', 12, 2, FALSE),

    ('44444444-aaaa-0000-0000-111111111111', '44444444-aaaa-aaaa-aaaa-444444444444', 'Construire une phrase claire', 'Renforce la structure sujet verbe complement avec des exemples simples.', 'VIDEO', 12, 1, TRUE),
    ('44444444-aaaa-0000-0000-222222222222', '44444444-aaaa-aaaa-aaaa-444444444444', 'Ponctuation utile', 'Utilise la ponctuation pour rendre une idee plus lisible.', 'ARTICLE', 9, 2, FALSE),
    ('44444444-bbbb-0000-0000-111111111111', '44444444-bbbb-bbbb-bbbb-444444444444', 'Organiser un paragraphe', 'Transformer des idees brutes en paragraphe coherent.', 'VIDEO', 15, 1, FALSE),
    ('44444444-bbbb-0000-0000-222222222222', '44444444-bbbb-bbbb-bbbb-444444444444', 'Corriger avant de rendre', 'Suivre une petite checklist de revision avant soumission.', 'RESOURCE', 7, 2, FALSE),

    ('55555555-aaaa-0000-0000-111111111111', '55555555-aaaa-aaaa-aaaa-555555555555', 'Setting Up a Study Folder', 'Create a simple structure for course files and downloads.', 'VIDEO', 10, 1, TRUE),
    ('55555555-aaaa-0000-0000-222222222222', '55555555-aaaa-aaaa-aaaa-555555555555', 'Taking Useful Digital Notes', 'Capture lessons in a format that is easy to review later.', 'VIDEO', 13, 2, FALSE),
    ('55555555-bbbb-0000-0000-111111111111', '55555555-bbbb-bbbb-bbbb-555555555555', 'Planning a Weekly Study Routine', 'Use a lightweight schedule to reduce missed work.', 'ARTICLE', 8, 1, FALSE),
    ('55555555-bbbb-0000-0000-222222222222', '55555555-bbbb-bbbb-bbbb-555555555555', 'Focus Sessions on Mobile', 'Reduce distraction and finish short study blocks consistently.', 'VIDEO', 11, 2, FALSE);
