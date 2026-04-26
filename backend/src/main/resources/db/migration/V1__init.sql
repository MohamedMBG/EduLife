-- USERS TABLE (core identity mapping)

CREATE TABLE users (
                       id UUID PRIMARY KEY,

                       firebase_uid VARCHAR(128) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,

                       role VARCHAR(20) NOT NULL DEFAULT 'LEARNER',

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);