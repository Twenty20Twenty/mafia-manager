package com.mafia.manager.dto;

import lombok.Data;

/** Тело запроса POST /api/users/{id}/confirm-email-change */
@Data
public class ConfirmEmailChangeRequest {
    private String code;   // 6-значный цифровой код из письма
}
