package com.mafia.manager.dto;
import lombok.Data;

@Data
public class AuthRequest {
    private String nickname;
    private String password;
}
