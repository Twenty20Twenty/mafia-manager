CREATE TABLE club_requests
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users (id) ON DELETE CASCADE, -- ИСПРАВЛЕНО: INTEGER -> BIGINT
    club_id    BIGINT REFERENCES clubs (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Игрок может подать только одну заявку в конкретный клуб
    UNIQUE (user_id, club_id)
);
