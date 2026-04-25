package com.mafia.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Ответ на POST /api/auth/register.
 * JWT не выдаётся — только данные для следующего шага (ввод кода).
 */
@Data
@AllArgsConstructor
public class RegisterResponse {
    private Long   userId;
    private String nickname;
    private String email;    // показываем на экране "код отправлен на ****@gmail.com"
}
