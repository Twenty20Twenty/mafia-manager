-- Добавляем колонку created_at, если её нет
ALTER TABLE player_stats
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();