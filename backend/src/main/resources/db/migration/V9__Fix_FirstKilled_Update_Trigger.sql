-- V7__Fix_FirstKilled_Update_Trigger.sql
--
-- FIX #7: При смене is_first_killed у слота (UPDATE game_slots SET is_first_killed)
-- необходимо пересчитать CI для старого и нового ПУ.
--
-- Проблема: триггер trg_game_update_recalc срабатывает только при UPDATE games.status/winner.
-- Но если судья редактирует протокол и меняет ПУ — games.status не меняется (остаётся 'completed'),
-- триггер не срабатывает, CI и lh_points в game_slots остаются для старого ПУ.
--
-- Решение: добавляем триггер на UPDATE game_slots.is_first_killed,
-- который вызывает recalculate_player_ci для затронутых игроков.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Функция-обработчик триггера на game_slots
-- ─────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION trigger_recalc_on_first_killed_change()
RETURNS TRIGGER AS $$
DECLARE
    _tournament_id BIGINT;
BEGIN
    -- Срабатываем только если is_first_killed действительно изменился
    IF OLD.is_first_killed IS NOT DISTINCT FROM NEW.is_first_killed THEN
        RETURN NEW;
    END IF;

    -- Получаем tournament_id через games
    SELECT g.tournament_id INTO _tournament_id
    FROM games g
    WHERE g.id = NEW.game_id;

    IF _tournament_id IS NULL THEN
        RETURN NEW;
    END IF;

    -- Пересчитываем CI для игрока старого ПУ (если был)
    IF OLD.is_first_killed = TRUE AND OLD.user_id IS NOT NULL THEN
        PERFORM recalculate_player_ci(_tournament_id, OLD.user_id);
    END IF;

    -- Пересчитываем CI для игрока нового ПУ (если стал)
    IF NEW.is_first_killed = TRUE AND NEW.user_id IS NOT NULL THEN
        PERFORM recalculate_player_ci(_tournament_id, NEW.user_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Триггер на game_slots — срабатывает при изменении is_first_killed
-- ─────────────────────────────────────────────────────────────────────────────

DROP TRIGGER IF EXISTS trg_first_killed_change_recalc ON game_slots;

CREATE TRIGGER trg_first_killed_change_recalc
    AFTER UPDATE OF is_first_killed
    ON game_slots
    FOR EACH ROW
    EXECUTE FUNCTION trigger_recalc_on_first_killed_change();

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Расширяем существующий триггер trg_game_update_recalc:
--    пересчитываем CI при изменении ЛЮБОГО поля слота (role, user_id),
--    потому что смена игрока в слоте тоже должна обновить компенсацию.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION trigger_recalc_on_slot_user_change()
RETURNS TRIGGER AS $$
DECLARE
    _tournament_id BIGINT;
BEGIN
    -- Срабатываем только если user_id действительно изменился
    IF OLD.user_id IS NOT DISTINCT FROM NEW.user_id THEN
        RETURN NEW;
    END IF;

    SELECT g.tournament_id INTO _tournament_id
    FROM games g
    WHERE g.id = NEW.game_id;

    IF _tournament_id IS NULL THEN
        RETURN NEW;
    END IF;

    -- Пересчитываем CI для старого игрока в слоте
    IF OLD.user_id IS NOT NULL THEN
        PERFORM recalculate_player_ci(_tournament_id, OLD.user_id);
    END IF;

    -- Пересчитываем CI для нового игрока в слоте
    IF NEW.user_id IS NOT NULL THEN
        PERFORM recalculate_player_ci(_tournament_id, NEW.user_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_slot_user_change_recalc ON game_slots;

CREATE TRIGGER trg_slot_user_change_recalc
    AFTER UPDATE OF user_id
    ON game_slots
    FOR EACH ROW
    EXECUTE FUNCTION trigger_recalc_on_slot_user_change();
