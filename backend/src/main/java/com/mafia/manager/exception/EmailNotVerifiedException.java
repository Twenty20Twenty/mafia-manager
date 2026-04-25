package com.mafia.manager.exception;

/**
 * Бросается при попытке войти с незаверифицированным email.
 * Содержит userId — фронтенд использует его для редиректа на /verify-email?userId=...
 */
public class EmailNotVerifiedException extends RuntimeException {

    private final Long userId;
    private final String email;

    public EmailNotVerifiedException(Long userId, String email) {
        super("Email не подтверждён");
        this.userId = userId;
        this.email  = email;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
}
