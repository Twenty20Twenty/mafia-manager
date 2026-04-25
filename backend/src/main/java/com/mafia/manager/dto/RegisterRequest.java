package com.mafia.manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Никнейм обязателен")
    @Size(min = 2, max = 30, message = "Никнейм от 2 до 30 символов")
    private String nickname;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль минимум 6 символов")
    private String password;

    private String city;    // Название города текстом (в сервисе найдем ID)
    private String gender;  // "male" или "female"
}
