-- 1. Создаем новый тип и добавляем колонку в таблицу games
CREATE TYPE game_stage AS ENUM ('qualifying', 'final_round');

ALTER TABLE games
    ADD COLUMN stage game_stage DEFAULT 'qualifying';

-- 2. Удаляем старое представление (чтобы обновить структуру)
DROP VIEW IF EXISTS view_tournament_leaderboard;

CREATE VIEW view_tournament_leaderboard AS
SELECT
    tp.tournament_id,
    tp.user_id,
    u.nickname,
    u.avatar_url,
    COALESCE(SUM(gs.computed_score), 0) AS total_score,
    COUNT(gs.id) AS games_played,

    -- Подсчет побед
    COUNT(CASE
              WHEN (gs.role IN ('civilian', 'sheriff') AND g.winner = 'red') OR
                   (gs.role IN ('mafia', 'don') AND g.winner = 'black') THEN 1
        END) AS total_wins,
    COUNT(CASE
              WHEN gs.role = 'sheriff' AND g.winner = 'red' THEN 1
        END) AS sheriff_wins,
    COUNT(CASE
              WHEN gs.role = 'don' AND g.winner = 'black' THEN 1
        END) AS don_wins,

    -- Подсчет ПУ (Первое Убийство)
    COUNT(CASE WHEN gs.is_first_killed = TRUE THEN 1 END) AS first_killed_count,

    -- Подсчет баллов
    COALESCE(SUM(gs.extra_points_positive), 0) AS extra_points_positive,
    COALESCE(SUM(gs.extra_points_negative), 0) AS extra_points_negative,
    COALESCE(SUM(gs.penalty_points), 0) AS penalty_points,
    COALESCE(SUM(gs.compensation_points), 0) AS compensation_points,
    COALESCE(SUM(bm.points), 0) AS best_move_points
FROM tournament_participants tp
         JOIN users u ON tp.user_id = u.id
         LEFT JOIN games g ON g.tournament_id = tp.tournament_id AND g.status = 'completed'
         LEFT JOIN game_slots gs ON gs.game_id = g.id AND gs.user_id = tp.user_id
         LEFT JOIN best_moves bm ON bm.game_id = g.id AND bm.author_slot_id = gs.id
GROUP BY tp.tournament_id, tp.user_id, u.nickname, u.avatar_url;
