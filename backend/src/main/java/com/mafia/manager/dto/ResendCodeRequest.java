package com.mafia.manager.dto;

import lombok.Data;

/** Тело запроса POST /api/auth/resend-code */
@Data
public class ResendCodeRequest {
    private Long userId;
}
