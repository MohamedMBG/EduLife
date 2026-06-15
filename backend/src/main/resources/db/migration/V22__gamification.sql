-- Backend is the single source of truth for gamification. Clients display values
-- only; XP, level, streak, and badge unlocks are computed here.

-- Aggregate per-user gamification state. Updated transactionally each time an XP
-- event lands so reads against /gamification/me and /gamification/leaderboard
-- never need to replay events.
CREATE TABLE user_gamification_state (
    user_id                     UUID        NOT NULL,
    total_xp                    INTEGER     NOT NULL DEFAULT 0,
    level                       INTEGER     NOT NULL DEFAULT 1,
    current_streak              INTEGER     NOT NULL DEFAULT 0,
    longest_streak              INTEGER     NOT NULL DEFAULT 0,
    last_activity_date          DATE,
    streak_bonus_3_awarded      BOOLEAN     NOT NULL DEFAULT FALSE,
    streak_bonus_7_awarded      BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_gamification_state PRIMARY KEY (user_id),
    CONSTRAINT fk_user_gamification_state_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_user_gamification_state_total_xp_nonneg CHECK (total_xp >= 0),
    CONSTRAINT ck_user_gamification_state_level_range CHECK (level BETWEEN 1 AND 10),
    CONSTRAINT ck_user_gamification_state_streaks_nonneg
        CHECK (current_streak >= 0 AND longest_streak >= 0)
);

-- Global all-time leaderboard ordering. (total_xp DESC, updated_at ASC) puts the
-- earliest learner ahead on ties.
CREATE INDEX idx_user_gamification_state_leaderboard
    ON user_gamification_state (total_xp DESC, updated_at ASC);

-- Append-only XP ledger. dedup_key is the integrity rail that makes every awardXp
-- call idempotent: repeated emissions for the same lesson/exam/certificate/login
-- collide on the unique index and are dropped.
CREATE TABLE gamification_xp_events (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    xp          INTEGER     NOT NULL,
    source_ref  VARCHAR(255),
    dedup_key   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_gamification_xp_events PRIMARY KEY (id),
    CONSTRAINT fk_gamification_xp_events_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_gamification_xp_events_type CHECK (event_type IN (
        'LESSON_COMPLETED',
        'COURSE_COMPLETED',
        'EXAM_PASSED',
        'CERTIFICATE_EARNED',
        'ENROLLMENT',
        'DAILY_LOGIN',
        'STREAK_BONUS_3',
        'STREAK_BONUS_7'
    ))
);

CREATE UNIQUE INDEX uq_gamification_xp_events_dedup
    ON gamification_xp_events (dedup_key);

CREATE INDEX idx_gamification_xp_events_user_created
    ON gamification_xp_events (user_id, created_at DESC);

-- Unlocked badges per user. Badge definitions (label, rarity, unlock rule) live
-- in code and must stay byte-identical across web + Android; only unlock state
-- is persisted here.
CREATE TABLE user_badges (
    user_id     UUID        NOT NULL,
    badge_id    VARCHAR(40) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_user_badges PRIMARY KEY (user_id, badge_id),
    CONSTRAINT fk_user_badges_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_badges_user ON user_badges (user_id, unlocked_at DESC);
