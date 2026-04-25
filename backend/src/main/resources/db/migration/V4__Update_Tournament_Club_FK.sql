-- Удаляем старое ограничение (ON DELETE CASCADE)
ALTER TABLE tournaments
DROP CONSTRAINT IF EXISTS tournaments_club_id_fkey;

-- Добавляем новое ограничение (ON DELETE SET NULL)
ALTER TABLE tournaments
    ADD CONSTRAINT tournaments_club_id_fkey
        FOREIGN KEY (club_id)
            REFERENCES clubs (id)
            ON DELETE SET NULL;
