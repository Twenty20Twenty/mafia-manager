package com.mafia.manager.dto;

import lombok.Data;

/** Тело запроса POST /api/auth/reset-password */
@Data
public class ResetPasswordRequest {
    private String token;       // UUID из ссылки в письме
    private String newPassword; // новый пароль в открытом виде
}
