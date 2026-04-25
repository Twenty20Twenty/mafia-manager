package com.mafia.manager.dto;

import lombok.Data;

/** Тело запроса POST /api/auth/verify-email */
@Data
public class VerifyEmailRequest {
    private Long   userId;
    private String code;   // 6-значный цифровой код из письма
}
