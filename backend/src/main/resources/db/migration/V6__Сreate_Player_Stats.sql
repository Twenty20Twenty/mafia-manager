-- V5__create_player_stats.sql
-- Статистика игроков, агрегированная по периодам.
-- period_year = NULL означает статистику "за всё время".
-- period_year = 2025, 2026, ... — статистика за конкретный год.
-- Уникальный индекс: (user_id, period_year) гарантирует одну запись на игрока × период.

CREATE TABLE player_stats (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- NULL = "за всё время", число = конкретный год
    period_year     SMALLINT NULL,

    -- Общее количество завершённых игр в этом периоде
    total_games     INT NOT NULL DEFAULT 0,

    -- Игр по ролям
    games_civilian  INT NOT NULL DEFAULT 0,
    games_sheriff   INT NOT NULL DEFAULT 0,
    games_mafia     INT NOT NULL DEFAULT 0,
    games_don       INT NOT NULL DEFAULT 0,

    -- Победы по ролям (для расчёта win rate на фронте)
    wins_civilian   INT NOT NULL DEFAULT 0,
    wins_sheriff    INT NOT NULL DEFAULT 0,
    wins_mafia      INT NOT NULL DEFAULT 0,
    wins_don        INT NOT NULL DEFAULT 0,

    -- Лучшие ходы
    best_moves_total   INT NOT NULL DEFAULT 0,
    best_moves_perfect INT NOT NULL DEFAULT 0,   -- угадал 3 из 3

    -- Первый убитый
    first_killed_count INT NOT NULL DEFAULT 0,

    -- Фолы и дисциплинарные штрафы (сумма за период)
    total_fouls     INT NOT NULL DEFAULT 0,

    -- Технические метаданные
    last_recalculated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_player_stats_user_period UNIQUE (user_id, period_year)
);

CREATE INDEX idx_player_stats_user_id ON player_stats(user_id);
