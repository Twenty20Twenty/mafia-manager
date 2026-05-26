-- V14__update_recalculate_player_ci.sql
-- Обновление функции recalculate_player_ci:
--   добавлен фильтр по tournament_id при подсчёте total_games,
--   чтобы счётчик игр относился только к конкретному турниру,
--   а не ко всем турнирам игрока.

CREATE OR REPLACE FUNCTION recalculate_player_ci(
    target_tournament_id BIGINT,
    target_user_id       BIGINT
)
RETURNS VOID AS $$
DECLARE
    _total_games      INTEGER;
    _threshold_b      NUMERIC;
    _first_killed_cnt INTEGER;
    _ci_base          NUMERIC;
    _rec              RECORD;
    _points_to_add    NUMERIC;
    _has_black_in_lh  BOOLEAN;
    _lh_points        NUMERIC;
BEGIN
    SELECT COUNT(*)
    INTO _total_games
    FROM tournament_participants tp
             JOIN game_slots gs ON gs.user_id = tp.user_id
             JOIN games g ON g.id = gs.game_id
    WHERE tp.tournament_id = target_tournament_id
      AND g.tournament_id  = target_tournament_id
      AND tp.user_id       = target_user_id
      AND g.status         = 'completed';

    IF _total_games = 0 OR _total_games IS NULL THEN
        RETURN;
    END IF;

    _threshold_b := ROUND(_total_games * 0.4);
    IF _threshold_b < 4 THEN
        _threshold_b := 4;
    END IF;

    SELECT COUNT(*)
    INTO _first_killed_cnt
    FROM game_slots gs
             JOIN games g ON g.id = gs.game_id
    WHERE g.tournament_id    = target_tournament_id
      AND gs.user_id         = target_user_id
      AND g.status           = 'completed'
      AND gs.is_first_killed = TRUE
      AND gs.role IN ('civilian', 'sheriff');

    IF _first_killed_cnt <= _threshold_b THEN
        _ci_base := (_first_killed_cnt * 0.4) / _threshold_b;
    ELSE
        _ci_base := 0.4;
    END IF;

    FOR _rec IN
        SELECT gs.id             AS slot_id,
               gs.is_first_killed,
               gs.role,
               gs.extra_points_positive,
               gs.extra_points_negative,
               gs.penalty_points,
               g.winner,
               g.id             AS game_id
        FROM game_slots gs
                 JOIN games g ON g.id = gs.game_id
        WHERE g.tournament_id = target_tournament_id
          AND gs.user_id      = target_user_id
          AND g.status        = 'completed'
    LOOP
        _points_to_add := 0;

        IF _rec.is_first_killed AND _rec.role IN ('civilian', 'sheriff') THEN
            SELECT EXISTS (
                SELECT 1
                FROM best_moves bm
                         JOIN game_slots candidate_slot
                              ON candidate_slot.game_id = bm.game_id
                             AND candidate_slot.slot_number IN (
                                 bm.candidate_1_slot,
                                 bm.candidate_2_slot,
                                 bm.candidate_3_slot
                             )
                WHERE bm.game_id        = _rec.game_id
                  AND bm.author_slot_id = _rec.slot_id
                  AND candidate_slot.role IN ('mafia', 'don')
            )
            INTO _has_black_in_lh;

            IF _has_black_in_lh THEN
                IF _rec.winner = 'black' THEN
                    _points_to_add := _ci_base;
                ELSIF _rec.winner = 'red' THEN
                    _points_to_add := _ci_base / 2;
                ELSE
                    _points_to_add := 0;
                END IF;
            ELSE
                _points_to_add := 0;
            END IF;
        END IF;

        SELECT COALESCE(points, 0)
        INTO _lh_points
        FROM best_moves
        WHERE game_id        = _rec.game_id
          AND author_slot_id = _rec.slot_id;

        IF _lh_points IS NULL THEN
            _lh_points := 0;
        END IF;

        UPDATE game_slots
        SET compensation_points = _points_to_add,
            computed_score      = (
                CASE
                    WHEN (_rec.role IN ('civilian', 'sheriff') AND _rec.winner = 'red')  THEN 1.0
                    WHEN (_rec.role IN ('mafia', 'don')        AND _rec.winner = 'black') THEN 1.0
                    ELSE 0.0
                END
                + extra_points_positive
                - extra_points_negative
                - penalty_points
                + _lh_points
                + _points_to_add
            )
        WHERE id = _rec.slot_id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
