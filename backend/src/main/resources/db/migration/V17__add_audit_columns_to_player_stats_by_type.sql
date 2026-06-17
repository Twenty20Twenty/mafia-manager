-- V17__add_audit_columns_to_player_stats_by_type.sql
-- Добавляем недостающие поля аудита из BaseEntity для таблицы статистики

ALTER TABLE player_stats_by_type
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();