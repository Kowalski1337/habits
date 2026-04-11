CREATE TABLE IF NOT EXISTS habits
(
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    color       VARCHAR(7) DEFAULT '#3B82F6',
    order_index INTEGER    DEFAULT 0,
    created_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP
);