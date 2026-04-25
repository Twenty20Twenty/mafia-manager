package com.mafia.manager.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String nickname;
    private String role;
    private Long id;
    private Long clubId;
}
