-- 1. Создаём тип
CREATE TYPE game_status AS ENUM ('pending', 'draft', 'completed');

-- 2. Дропаем триггер и вьюхи, зависящие от колонки status
DROP TRIGGER IF EXISTS trg_game_update_recalc ON games;
DROP VIEW IF EXISTS view_tournament_nominations;
DROP VIEW IF EXISTS view_tournament_leaderboard;

-- 3. Убираем дефолт
ALTER TABLE games ALTER COLUMN status DROP DEFAULT;

-- 4. Меняем тип через USING
ALTER TABLE games
ALTER COLUMN status TYPE game_status
    USING status::game_status;

-- 5. Возвращаем дефолт
ALTER TABLE games ALTER COLUMN status SET DEFAULT 'pending'::game_status;

-- 6. Пересоздаём триггер
CREATE TRIGGER trg_game_update_recalc
    AFTER UPDATE OF status, winner
    ON games
    FOR EACH ROW
    EXECUTE FUNCTION trigger_recalc_scores();