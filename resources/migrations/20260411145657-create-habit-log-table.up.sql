CREATE TABLE IF NOT EXISTS habit_logs
(
    id            SERIAL PRIMARY KEY,
    habit_id      INTEGER    NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    date          DATE       NOT NULL,
    completed     BOOLEAN    NOT NULL DEFAULT false,
    emotion_color VARCHAR(7) NOT NULL DEFAULT '#000000',
    created_at    TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (habit_id, date)
);