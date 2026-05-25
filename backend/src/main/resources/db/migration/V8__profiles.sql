CREATE TABLE profiles (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    bio          VARCHAR(500),
    avatar_url   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_profiles PRIMARY KEY (id),
    CONSTRAINT uq_profiles_user_id UNIQUE (user_id)
);

CREATE INDEX idx_profiles_user_id ON profiles (user_id);
