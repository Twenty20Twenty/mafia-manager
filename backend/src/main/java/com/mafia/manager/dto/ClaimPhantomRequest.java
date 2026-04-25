package com.mafia.manager.dto;
import lombok.Data;

@Data
public class ClaimPhantomRequest {
    private String nickname;
    private String phantomCode;
    private String newPassword;
    private String newEmail;
}
