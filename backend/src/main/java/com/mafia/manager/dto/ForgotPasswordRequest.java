package com.mafia.manager.dto;

import lombok.Data;

/** Тело запроса POST /api/auth/forgot-password */
@Data
public class ForgotPasswordRequest {
    private String email;
}
