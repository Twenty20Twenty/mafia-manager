package com.mafia.manager.exception;

/**
 * Бросается при попытке войти через /auth/login с фантомным аккаунтом.
 * Фантом активируется только через /auth/phantom/claim.
 */
public class PhantomAccountException extends RuntimeException {
    public PhantomAccountException() {
        super("Это фантомный аккаунт. Войдите через код привязки на странице активации.");
    }
}
