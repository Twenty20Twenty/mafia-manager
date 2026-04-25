CREATE TABLE IF NOT EXISTS tournament_judges (
                                   tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
                                   user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   PRIMARY KEY (tournament_id, user_id)
);

CREATE INDEX idx_tournament_judges_tournament ON tournament_judges(tournament_id);