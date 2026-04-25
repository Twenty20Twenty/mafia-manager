package com.mafia.manager.exception;

/**
 * Тело ответа 403 при незаверифицированном email.
 * userId нужен фронтенду для редиректа на /verify-email?userId=...
 */
public record EmailNotVerifiedResponse(String message, Long userId, String email) {}
