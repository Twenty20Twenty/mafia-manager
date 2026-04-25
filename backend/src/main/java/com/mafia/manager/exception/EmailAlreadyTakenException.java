package com.mafia.manager.exception;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(String email) {
        super("Email уже используется: " + email);
    }
}
