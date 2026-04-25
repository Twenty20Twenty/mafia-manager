-- Добавляем колонку updated_at, если её нет
ALTER TABLE player_stats
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();