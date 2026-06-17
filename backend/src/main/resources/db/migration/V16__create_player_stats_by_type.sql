-- V16__create_player_stats_by_type.sql
-- Статистика игрока в разрезе типа турнира (individual / team / season).
--
-- Зачем отдельная таблица, а не поле в player_stats?
-- player_stats хранит агрегат «за всё время» и «за год» по ВСЕМ турнирам.
-- Здесь мы хотим выбор по типу, чтобы фронтенд мог показывать только
-- «личные», только «командные» или только «рейтинговые» турниры.
-- Срезы ортогональны (год × тип), поэтому выносим в отдельную таблицу.

CREATE TABLE player_stats_by_type (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Тип турнира: 'individual' | 'team' | 'season'
    tournament_type VARCHAR(20) NOT NULL,

    -- NULL = «за всё время», число = конкретный год
    period_year     SMALLINT    NULL,

    -- ─── Общее ───────────────────────────────────────────────────────────────
    total_games         INT NOT NULL DEFAULT 0,

    -- ─── Игры по ролям ───────────────────────────────────────────────────────
    games_civilian  INT NOT NULL DEFAULT 0,
    games_sheriff   INT NOT NULL DEFAULT 0,
    games_mafia     INT NOT NULL DEFAULT 0,
    games_don       INT NOT NULL DEFAULT 0,

    -- ─── Победы по ролям ─────────────────────────────────────────────────────
    wins_civilian   INT NOT NULL DEFAULT 0,
    wins_sheriff    INT NOT NULL DEFAULT 0,
    wins_mafia      INT NOT NULL DEFAULT 0,
    wins_don        INT NOT NULL DEFAULT 0,

    -- ─── Лучшие ходы ─────────────────────────────────────────────────────────
    best_moves_total   INT NOT NULL DEFAULT 0,
    best_moves_perfect INT NOT NULL DEFAULT 0,

    -- ─── Первый убитый ───────────────────────────────────────────────────────
    first_killed_count INT NOT NULL DEFAULT 0,

    -- ─── Дисциплина ──────────────────────────────────────────────────────────
    total_fouls        INT NOT NULL DEFAULT 0,

    -- ─── Технические метаданные ──────────────────────────────────────────────
    last_recalculated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Гарантия: одна запись на (user, тип, период)
    CONSTRAINT uq_player_stats_by_type UNIQUE (user_id, tournament_type, period_year)
);

CREATE INDEX idx_player_stats_by_type_user_id ON player_stats_by_type(user_id);
CREATE INDEX idx_player_stats_by_type_user_type ON player_stats_by_type(user_id, tournament_type);
