-- V15__fix_compensation_by_stage.sql
--
-- Полная переработка функции recalculate_player_ci согласно правилам 8.6:
--
-- ИСПРАВЛЕНИЯ:
--   1. Компенсация считается ОТДЕЛЬНО для каждой стадии (qualifying / final_round).
--   2. Дистанция для qualifying/final_round берётся как ОБЩЕЕ число игр на
--      этой стадии в турнире (не только завершённые) — для individual и team-турниров
--      заранее известно итоговое число игр.
--      Для season-турниров (рейтинг) дистанция = фактически сыгранные игры игрока
--      (итог неизвестен), что соответствует п.8.6.
--   3. Если дистанция стадии < 4 игр — компенсации на этой стадии не начисляются (п.8.6.4).
--   4. Устранён некорректный минимальный порог B>=4 (который игнорировал короткие дистанции).
--   5. Исправлен фильтр tournament_id в подсчёте total_games.

CREATE OR REPLACE FUNCTION recalculate_player_ci(
    target_tournament_id BIGINT,
    target_user_id       BIGINT
)
RETURNS VOID AS $$
DECLARE
    _tournament_type  TEXT;

    -- Переменные для текущей стадии в цикле
    _stage            TEXT;
    _total_games      INTEGER;   -- дистанция стадии
    _threshold_b      NUMERIC;
    _first_killed_cnt INTEGER;
    _ci_base          NUMERIC;

    _rec              RECORD;
    _points_to_add    NUMERIC;
    _has_black_in_lh  BOOLEAN;
    _lh_points        NUMERIC;
BEGIN
    -- Определяем тип турнира: individual, team, season
    SELECT type::TEXT
    INTO _tournament_type
    FROM tournaments
    WHERE id = target_tournament_id;

    -- ─── Обрабатываем каждую стадию независимо ───────────────────────────────
    FOR _stage IN VALUES ('qualifying'), ('final_round') LOOP

        -- ── Шаг 1: определяем дистанцию стадии ──────────────────────────────

        IF _tournament_type IN ('individual', 'team') THEN
            -- Для личного и командного турнира дистанция = все игры конкретного игрока
            -- на данной стадии (включая pending/draft), поскольку итоговое число известно
            -- заранее и игрок сыграет во всех запланированных играх своей стадии.
            SELECT COUNT(*)
            INTO _total_games
            FROM game_slots gs
                JOIN games g ON g.id = gs.game_id
            WHERE g.tournament_id = target_tournament_id
              AND g.stage::TEXT    = _stage
              AND gs.user_id       = target_user_id;
        ELSE
            -- Для рейтинга (season) итог неизвестен — считаем по фактически
            -- сыгранным играм конкретного игрока на этой стадии.
            SELECT COUNT(*)
            INTO _total_games
            FROM game_slots gs
                JOIN games g ON g.id = gs.game_id
            WHERE g.tournament_id = target_tournament_id
              AND g.stage::TEXT    = _stage
              AND gs.user_id       = target_user_id
              AND g.status         = 'completed';
        END IF;

        -- Если дистанция < 4 — компенсации не начисляются (п.8.6.4 / п.о финале)
        IF _total_games IS NULL OR _total_games < 4 THEN
            -- Обнуляем compensation_points в слотах этой стадии и пересчитываем computed_score
            FOR _rec IN
                SELECT gs.id                    AS slot_id,
                       gs.role,
                       gs.extra_points_positive,
                       gs.extra_points_negative,
                       gs.penalty_points,
                       g.winner,
                       g.id                     AS game_id
                FROM game_slots gs
                    JOIN games g ON g.id = gs.game_id
                WHERE g.tournament_id = target_tournament_id
                  AND g.stage::TEXT    = _stage
                  AND gs.user_id       = target_user_id
                  AND g.status         = 'completed'
            LOOP
                SELECT COALESCE(points, 0)
                INTO _lh_points
                FROM best_moves
                WHERE game_id        = _rec.game_id
                  AND author_slot_id = _rec.slot_id;

                IF _lh_points IS NULL THEN
                    _lh_points := 0;
                END IF;

                UPDATE game_slots
                SET compensation_points = 0,
                    computed_score = (
                        CASE
                            WHEN (_rec.role IN ('civilian', 'sheriff') AND _rec.winner = 'red')  THEN 1.0
                            WHEN (_rec.role IN ('mafia', 'don')        AND _rec.winner = 'black') THEN 1.0
                            ELSE 0.0
                        END
                        + _rec.extra_points_positive
                        - _rec.extra_points_negative
                        - _rec.penalty_points
                        + _lh_points
                    )
                WHERE id = _rec.slot_id;
            END LOOP;

            CONTINUE;  -- переходим к следующей стадии
        END IF;

        -- ── Шаг 2: порог B = ROUND(total_games * 0.4) ───────────────────────
        _threshold_b := ROUND(_total_games * 0.4);

        -- Защита от деления на ноль (теоретически невозможна при total_games >= 4,
        -- но на случай ROUND() -> 0 при очень малом total_games)
        IF _threshold_b = 0 THEN
            _threshold_b := 1;
        END IF;

        -- ── Шаг 3: количество отстрелов игрока в первую ночь на этой стадии ─
        -- Учитываем только завершённые игры (только в них зафиксирован факт ПУ)
        SELECT COUNT(*)
        INTO _first_killed_cnt
        FROM game_slots gs
            JOIN games g ON g.id = gs.game_id
        WHERE g.tournament_id  = target_tournament_id
          AND g.stage::TEXT     = _stage
          AND gs.user_id        = target_user_id
          AND g.status          = 'completed'
          AND gs.is_first_killed = TRUE
          AND gs.role IN ('civilian', 'sheriff');

        -- ── Шаг 4: вычисляем базовую величину Ci ────────────────────────────
        -- Ci = i * 0.4 / B   при i <= B
        -- Ci = 0.4           при i > B
        IF _first_killed_cnt = 0 THEN
            _ci_base := 0;
        ELSIF _first_killed_cnt <= _threshold_b THEN
            _ci_base := (_first_killed_cnt * 0.4) / _threshold_b;
        ELSE
            _ci_base := 0.4;
        END IF;

        -- ── Шаг 5: обновляем каждый слот игрока на этой стадии ──────────────
        FOR _rec IN
            SELECT gs.id              AS slot_id,
                   gs.is_first_killed,
                   gs.role,
                   gs.extra_points_positive,
                   gs.extra_points_negative,
                   gs.penalty_points,
                   g.winner,
                   g.id               AS game_id
            FROM game_slots gs
                JOIN games g ON g.id = gs.game_id
            WHERE g.tournament_id = target_tournament_id
              AND g.stage::TEXT    = _stage
              AND gs.user_id       = target_user_id
              AND g.status         = 'completed'
        LOOP
            _points_to_add := 0;

            IF _rec.is_first_killed AND _rec.role IN ('civilian', 'sheriff') THEN
                -- Проверяем: оставил ли игрок хотя бы одного чёрного в лучший ход (п.8.6.2)
                SELECT EXISTS (
                    SELECT 1
                    FROM best_moves bm
                        JOIN game_slots candidate_slot
                             ON candidate_slot.game_id   = bm.game_id
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
                        -- п.8.6.3: красные проиграли → полная компенсация Ci
                        _points_to_add := _ci_base;
                    ELSIF _rec.winner = 'red' THEN
                        -- п.8.6.4: красные выиграли → 50% от Ci
                        _points_to_add := _ci_base / 2;
                    ELSE
                        _points_to_add := 0;
                    END IF;
                ELSE
                    -- п.8.6.5: не оставил чёрных в ЛХ → компенсации нет
                    _points_to_add := 0;
                END IF;
            END IF;

            -- Баллы за лучший ход
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
                computed_score = (
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

    END LOOP; -- конец цикла по стадиям
END;
$$ LANGUAGE plpgsql;


-- ─── Пересчёт всех существующих данных ───────────────────────────────────────
-- После применения миграции пересчитываем компенсации для всех игроков
-- во всех активных и завершённых турнирах.

DO $$
DECLARE
    _rec RECORD;
BEGIN
    FOR _rec IN
        SELECT DISTINCT g.tournament_id, gs.user_id
        FROM games g
            JOIN game_slots gs ON gs.game_id = g.id
        WHERE g.status = 'completed'
          AND gs.user_id IS NOT NULL
    LOOP
        PERFORM recalculate_player_ci(_rec.tournament_id, _rec.user_id);
    END LOOP;
END;
$$;
