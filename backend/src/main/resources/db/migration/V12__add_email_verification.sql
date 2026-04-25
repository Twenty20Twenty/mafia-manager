-- ─── Email verification type ──────────────────────────────────────────────────
CREATE TYPE email_verification_type AS ENUM (
    'REGISTRATION',
    'EMAIL_CHANGE',
    'PASSWORD_RESET'
);

-- ─── Таблица кодов/токенов подтверждения ──────────────────────────────────────
-- Одна таблица на все три сценария: регистрация, смена email, сброс пароля.
-- code: 6-значные цифры для REGISTRATION и EMAIL_CHANGE (удобно вводить),
--       UUID-строка для PASSWORD_RESET (переходят по ссылке).
CREATE TABLE email_verifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT                   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       email_verification_type  NOT NULL,
    code       VARCHAR(255)             NOT NULL,
    new_email  VARCHAR(255),                        -- только для EMAIL_CHANGE
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,            -- NULL = ещё активен
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Гарантируем один активный код на пользователя на тип
CREATE UNIQUE INDEX idx_email_verif_active
    ON email_verifications(user_id, type)
    WHERE used_at IS NULL;

-- ─── Флаг подтверждения email в таблице users ─────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT FALSE;

-- Фантомы не имеют email — считаем их сразу «подтверждёнными»,
-- чтобы не ломать логику входа для уже созданных фантомных аккаунтов
UPDATE users
SET is_email_verified = TRUE
WHERE is_phantom = TRUE;
