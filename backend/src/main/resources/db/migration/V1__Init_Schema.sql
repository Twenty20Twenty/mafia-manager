-- =========================================================
-- 1. ENUMS
-- =========================================================
CREATE TYPE user_gender AS ENUM ('male', 'female');
CREATE TYPE user_role AS ENUM ('user', 'admin');
CREATE TYPE tournament_type AS ENUM ('individual', 'team', 'season');
CREATE TYPE tournament_status AS ENUM ('registration', 'active', 'completed', 'archived');
CREATE TYPE game_winner AS ENUM ('red', 'black', 'draw');
CREATE TYPE player_role_in_game AS ENUM ('civilian', 'sheriff', 'mafia', 'don');
CREATE TYPE participant_status AS ENUM ('pending', 'approved', 'rejected', 'kicked');

-- =========================================================
-- 2. ГЕОГРАФИЯ
-- =========================================================
CREATE TABLE countries
(
    id   BIGSERIAL PRIMARY KEY, -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE cities
(
    id         BIGSERIAL PRIMARY KEY,                             -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    name       VARCHAR(100) NOT NULL,
    country_id BIGINT REFERENCES countries (id) ON DELETE CASCADE -- ИСПРАВЛЕНО: INTEGER -> BIGINT
);

-- =========================================================
-- 3. ОСНОВНЫЕ ТАБЛИЦЫ
-- =========================================================

CREATE TABLE clubs
(
    id                     BIGSERIAL PRIMARY KEY,                                  -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    name                   VARCHAR(100) NOT NULL,
    city_id                BIGINT       REFERENCES cities (id) ON DELETE SET NULL, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    logo_url               TEXT,
    description            TEXT,
    social_link            VARCHAR(255),
    is_tournament_operator BOOLEAN                  DEFAULT FALSE,
    president_id           BIGINT,                                                 -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE users
(
    id                          BIGSERIAL PRIMARY KEY,                                 -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    nickname                    VARCHAR(50) NOT NULL UNIQUE,
    email                       VARCHAR(255) UNIQUE,
    password_hash               VARCHAR(255),
    avatar_url                  TEXT,
    city_id                     BIGINT      REFERENCES cities (id) ON DELETE SET NULL, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    gender                      user_gender              DEFAULT 'male',
    social_link                 VARCHAR(255),
    is_phantom                  BOOLEAN                  DEFAULT FALSE,
    phantom_code                VARCHAR(20),
    role                        user_role                DEFAULT 'user',
    club_id                     BIGINT      REFERENCES clubs (id) ON DELETE SET NULL,  -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    can_create_club_tournaments BOOLEAN                  DEFAULT FALSE,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE clubs
    ADD CONSTRAINT fk_club_president FOREIGN KEY (president_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE TABLE judges_profiles
(
    user_id           BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    is_judge          BOOLEAN DEFAULT TRUE,
    can_judge_finals  BOOLEAN DEFAULT FALSE,
    can_be_head_judge BOOLEAN DEFAULT FALSE,
    judge_since       DATE    DEFAULT CURRENT_DATE
);

CREATE TABLE tournaments
(
    id            BIGSERIAL PRIMARY KEY,                                     -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    title         VARCHAR(255)    NOT NULL,
    description   TEXT,
    city_id       BIGINT          REFERENCES cities (id) ON DELETE SET NULL, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    club_id       BIGINT REFERENCES clubs (id) ON DELETE CASCADE,            -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    organizer_id  BIGINT          REFERENCES users (id) ON DELETE SET NULL,  -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    head_judge_id BIGINT          REFERENCES users (id) ON DELETE SET NULL,  -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    type          tournament_type NOT NULL,
    status        tournament_status        DEFAULT 'registration',
    start_date    DATE,
    end_date      DATE,
    settings      JSONB                    DEFAULT '{}'::jsonb,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE tournament_teams
(
    id            BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT REFERENCES tournaments (id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW() -- ИСПРАВЛЕНО: Добавлена колонка
);

CREATE TABLE tournament_participants
(
    id            BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT REFERENCES tournaments (id) ON DELETE CASCADE,
    user_id       BIGINT REFERENCES users (id) ON DELETE CASCADE,
    team_id       BIGINT REFERENCES tournament_teams (id) ON DELETE SET NULL,
    status        participant_status       DEFAULT 'pending',
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- ИСПРАВЛЕНО: Добавлена колонка
    UNIQUE (tournament_id, user_id)
);

CREATE TABLE tournament_seeding_exceptions
(
    id            BIGSERIAL PRIMARY KEY,                                -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    tournament_id BIGINT REFERENCES tournaments (id) ON DELETE CASCADE, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    player1_id    BIGINT REFERENCES users (id) ON DELETE CASCADE,       -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    player2_id    BIGINT REFERENCES users (id) ON DELETE CASCADE        -- ИСПРАВЛЕНО: INTEGER -> BIGINT
);

CREATE TABLE tournament_judges
(
    tournament_id BIGINT REFERENCES tournaments (id) ON DELETE CASCADE, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    user_id       BIGINT REFERENCES users (id) ON DELETE CASCADE,       -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    PRIMARY KEY (tournament_id, user_id)
);

CREATE TABLE games
(
    id            BIGSERIAL PRIMARY KEY,                                -- ИСПРАВЛЕНО: SERIAL -> BIGSERIAL
    tournament_id BIGINT REFERENCES tournaments (id) ON DELETE CASCADE, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    round_number  INTEGER                                                         DEFAULT 0,
    table_number  INTEGER                                                         DEFAULT 0,
    date          DATE   NOT NULL                                                 DEFAULT CURRENT_DATE,
    judge_id      BIGINT REFERENCES users (id) ON DELETE SET NULL,      -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    status        VARCHAR(20) CHECK (status IN ('pending', 'draft', 'completed')) DEFAULT 'pending',
    winner        game_winner,
    created_at    TIMESTAMP WITH TIME ZONE                                        DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE                                        DEFAULT NOW()
);

CREATE TABLE game_slots
(
    id                    BIGSERIAL PRIMARY KEY,
    game_id               BIGINT REFERENCES games (id) ON DELETE CASCADE,  -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    user_id               BIGINT REFERENCES users (id) ON DELETE SET NULL, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    slot_number           INTEGER CHECK (slot_number BETWEEN 1 AND 10),
    role                  player_role_in_game DEFAULT 'civilian',
    is_first_killed       BOOLEAN             DEFAULT FALSE,
    fouls                 INTEGER             DEFAULT 0 CHECK (fouls BETWEEN 0 AND 4),
    extra_points_positive DECIMAL(4, 1)       DEFAULT 0.0,
    extra_points_negative DECIMAL(4, 1)       DEFAULT 0.0,
    penalty_points        DECIMAL(4, 1)       DEFAULT 0.0,
    compensation_points   DECIMAL(4, 3)       DEFAULT 0.000,
    computed_score        DECIMAL(5, 3)       DEFAULT 0.000,
    UNIQUE (game_id, slot_number),
    UNIQUE (game_id, user_id)
);

CREATE TABLE best_moves
(
    id               BIGSERIAL PRIMARY KEY,
    game_id          BIGINT REFERENCES games (id) ON DELETE CASCADE,
    author_slot_id   BIGINT REFERENCES game_slots (id) ON DELETE CASCADE,
    candidate_1_slot INTEGER,
    candidate_2_slot INTEGER,
    candidate_3_slot INTEGER,
    guessed_count    INTEGER       DEFAULT 0,
    points           DECIMAL(3, 2) DEFAULT 0.0,
    UNIQUE (game_id)
);

CREATE INDEX idx_users_nickname ON users (nickname);
CREATE INDEX idx_games_tournament_date ON games (tournament_id, date);
CREATE INDEX idx_game_slots_user ON game_slots (user_id);

-- =========================================================
-- 4. ФУНКЦИИ И ТРИГГЕРЫ
-- =========================================================

CREATE
OR REPLACE FUNCTION recalculate_player_ci(target_tournament_id BIGINT, target_user_id BIGINT) -- ИСПРАВЛЕНО: INTEGER -> BIGINT
RETURNS VOID AS $$
DECLARE
_total_games INTEGER;
    _threshold_b
NUMERIC;
    _first_killed_cnt
INTEGER;
    _ci_base
NUMERIC;
    _rec RECORD;
    _points_to_add
NUMERIC;
    _has_black_in_lh BOOLEAN;
    _lh_points
NUMERIC;
BEGIN
SELECT COUNT(*)
INTO _total_games
FROM tournament_participants tp
         JOIN game_slots gs ON gs.user_id = tp.user_id
         JOIN games g ON g.id = gs.game_id
WHERE tp.tournament_id = target_tournament_id
  AND tp.user_id = target_user_id
  AND g.status = 'completed';

IF
_total_games = 0 OR _total_games IS NULL THEN RETURN;
END IF;

    _threshold_b
:= ROUND(_total_games * 0.4);
    IF
_threshold_b < 4 THEN _threshold_b := 4;
END IF;

SELECT COUNT(*)
INTO _first_killed_cnt
FROM game_slots gs
         JOIN games g ON g.id = gs.game_id
WHERE g.tournament_id = target_tournament_id
  AND gs.user_id = target_user_id
  AND g.status = 'completed'
  AND gs.is_first_killed = TRUE
  AND gs.role IN ('civilian', 'sheriff');

IF
_first_killed_cnt <= _threshold_b THEN
        _ci_base := (_first_killed_cnt * 0.4) / _threshold_b;
ELSE
        _ci_base := 0.4;
END IF;

FOR _rec IN
SELECT gs.id AS slot_id,
       gs.is_first_killed,
       gs.role,
       gs.extra_points_positive,
       gs.extra_points_negative,
       gs.penalty_points,
       g.winner,
       g.id  AS game_id
FROM game_slots gs
         JOIN games g ON g.id = gs.game_id
WHERE g.tournament_id = target_tournament_id
  AND gs.user_id = target_user_id
  AND g.status = 'completed' LOOP
        _points_to_add := 0;

IF
_rec.is_first_killed AND _rec.role IN ('civilian', 'sheriff') THEN
SELECT EXISTS (SELECT 1
               FROM best_moves bm
                        JOIN game_slots candidate_slot ON candidate_slot.game_id = bm.game_id
                   AND candidate_slot.slot_number IN (bm.candidate_1_slot, bm.candidate_2_slot, bm.candidate_3_slot)
               WHERE bm.game_id = _rec.game_id
                 AND bm.author_slot_id = _rec.slot_id
                 AND candidate_slot.role IN ('mafia', 'don'))
INTO _has_black_in_lh;

IF
_has_black_in_lh THEN
                IF _rec.winner = 'black' THEN
                    _points_to_add := _ci_base;
                ELSIF
_rec.winner = 'red' THEN
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
WHERE game_id = _rec.game_id
  AND author_slot_id = _rec.slot_id;
IF
_lh_points IS NULL THEN _lh_points := 0;
END IF;

UPDATE game_slots
SET compensation_points = _points_to_add,
    computed_score      = (
        CASE
            WHEN (_rec.role IN ('civilian', 'sheriff') AND _rec.winner = 'red') THEN 1.0
            WHEN (_rec.role IN ('mafia', 'don') AND _rec.winner = 'black') THEN 1.0
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
$$
LANGUAGE plpgsql;

CREATE
OR REPLACE FUNCTION trigger_recalc_scores() RETURNS TRIGGER AS $$
DECLARE
_player RECORD;
BEGIN
    IF
(NEW.status = 'completed') OR (OLD.status = 'completed') THEN
        FOR _player IN
SELECT user_id
FROM game_slots
WHERE game_id = NEW.id LOOP
            IF _player.user_id IS NOT NULL THEN
                PERFORM recalculate_player_ci(NEW.tournament_id, _player.user_id);
END IF;
END LOOP;
END IF;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_game_update_recalc
    AFTER UPDATE OF status, winner
    ON games
    FOR EACH ROW
    EXECUTE FUNCTION trigger_recalc_scores();

-- =========================================================
-- 5. VIEWS
-- =========================================================

CREATE VIEW view_tournament_leaderboard AS
SELECT tp.tournament_id,
       tp.user_id,
       u.nickname,
       u.avatar_url,
       COALESCE(SUM(gs.computed_score), 0) AS total_score,
       COUNT(CASE
                 WHEN (gs.role IN ('civilian', 'sheriff') AND g.winner = 'red') OR
                      (gs.role IN ('mafia', 'don') AND g.winner = 'black')
                     THEN 1
           END)                            AS total_wins,
       COUNT(gs.id)                        AS games_played
FROM tournament_participants tp
         JOIN users u ON tp.user_id = u.id
         LEFT JOIN games g ON g.tournament_id = tp.tournament_id AND g.status = 'completed'
         LEFT JOIN game_slots gs ON gs.game_id = g.id AND gs.user_id = tp.user_id
GROUP BY tp.tournament_id, tp.user_id, u.nickname, u.avatar_url
ORDER BY total_score DESC, total_wins DESC;

CREATE VIEW view_tournament_nominations AS
WITH PlayerStats AS (SELECT tp.tournament_id,
                            tp.user_id,
                            u.nickname,
                            u.avatar_url,
                            COUNT(gs.id)                                                       AS games_played,
                            (SELECT (settings ->>'rating_threshold') ::int
                             FROM tournaments
                             WHERE id = tp.tournament_id)                                      AS threshold_percent,
                            (SELECT COUNT(*)
                             FROM games
                             WHERE tournament_id = tp.tournament_id
                               AND status = 'completed')                                       AS total_tournament_games,
                            SUM(gs.extra_points_positive + gs.extra_points_negative +
                                COALESCE(bm.points, 0))                                        AS mvp_score,
                            AVG(CASE WHEN gs.role = 'civilian' THEN gs.computed_score END)     AS avg_score_civilian,
                            AVG(CASE WHEN gs.role = 'sheriff' THEN gs.computed_score END)      AS avg_score_sheriff,
                            AVG(CASE WHEN gs.role = 'mafia' THEN gs.extra_points_positive END) AS avg_extra_mafia,
                            AVG(CASE WHEN gs.role = 'don' THEN gs.extra_points_positive END)   AS avg_extra_don
                     FROM tournament_participants tp
                              JOIN users u ON tp.user_id = u.id
                              LEFT JOIN games g ON g.tournament_id = tp.tournament_id AND g.status = 'completed'
                              LEFT JOIN game_slots gs ON gs.game_id = g.id AND gs.user_id = tp.user_id
                              LEFT JOIN best_moves bm ON bm.game_id = g.id AND bm.author_slot_id = gs.id
                     GROUP BY tp.tournament_id, tp.user_id, u.nickname, u.avatar_url)
SELECT *
FROM PlayerStats ps
WHERE (ps.threshold_percent IS NULL OR ps.threshold_percent = 0 OR
       (ps.games_played::float / NULLIF(ps.total_tournament_games, 0)) * 100 >= ps.threshold_percent);
