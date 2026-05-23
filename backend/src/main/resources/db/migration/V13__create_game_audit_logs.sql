-- V13__create_game_audit_logs.sql
-- Аудит-лог операций с игровыми протоколами.
-- Хранит все изменения: создание, сохранение, завершение, удаление, замену игроков, изменение баллов.

CREATE TABLE game_audit_logs (
    id             BIGSERIAL PRIMARY KEY,
    game_id        BIGINT,                     -- nullable при массовом удалении
    tournament_id  BIGINT      NOT NULL,
    action         VARCHAR(32) NOT NULL,        -- GameAuditAction enum
    actor_id       BIGINT,                      -- кто выполнил операцию
    actor_nickname VARCHAR(50),
    details        TEXT,                        -- JSON или plain-text детали
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_game_audit_game_id       ON game_audit_logs(game_id);
CREATE INDEX idx_game_audit_tournament_id ON game_audit_logs(tournament_id);
CREATE INDEX idx_game_audit_actor_id      ON game_audit_logs(actor_id);
CREATE INDEX idx_game_audit_created_at    ON game_audit_logs(created_at DESC);

COMMENT ON TABLE  game_audit_logs           IS 'Аудит-лог всех операций с игровыми протоколами';
COMMENT ON COLUMN game_audit_logs.action    IS 'Тип операции: GAME_CREATED, PROTOCOL_DRAFT_SAVED, GAME_COMPLETED, GAME_DELETED, GAMES_BULK_DELETED, SLOT_PLAYER_SWAPPED, SLOT_SCORES_CHANGED';
COMMENT ON COLUMN game_audit_logs.details   IS 'Детали операции в виде пар ключ=значение через запятую';
