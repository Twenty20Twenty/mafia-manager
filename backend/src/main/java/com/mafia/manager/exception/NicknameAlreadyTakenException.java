package com.mafia.manager.exception;

public class NicknameAlreadyTakenException extends RuntimeException {
    public NicknameAlreadyTakenException(String nickname) {
        super("Никнейм уже занят: " + nickname);
    }
}
