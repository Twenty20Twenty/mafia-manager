package com.mafia.manager.controller;

import com.mafia.manager.dto.*;
import com.mafia.manager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Аутентификация, регистрация, подтверждение email, сброс пароля")
public class AuthController {

    private final AuthService authService;

    // ── РЕГИСТРАЦИЯ ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создаёт аккаунт и отправляет 6-значный код на email. " +
                          "JWT не выдаётся — нужно подтвердить email через /verify-email.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Аккаунт создан, код отправлен",
                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Никнейм или email уже заняты / невалидные данные")
            }
    )
    @PostMapping("/auth/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Подтверждение email по коду",
            description = "Принимает userId и 6-значный код из письма. При успехе выдаёт JWT.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Email подтверждён, JWT выдан",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный или истёкший код")
            }
    )
    @PostMapping("/auth/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @Operation(
            summary = "Повторная отправка кода подтверждения",
            description = "Инвалидирует предыдущий код и отправляет новый. TTL — 15 минут.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Код отправлен повторно"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @PostMapping("/auth/resend-code")
    public ResponseEntity<Void> resendCode(@RequestBody ResendCodeRequest request) {
        authService.resendRegistrationCode(request);
        return ResponseEntity.ok().build();
    }

    // ── ВХОД ──────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Вход в систему",
            description = "Требует подтверждённого email. Фантомные аккаунты войти не могут.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWT выдан",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неверные данные или email не подтверждён")
            }
    )
    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    // ── СБРОС ПАРОЛЯ ──────────────────────────────────────────────────────────

    @Operation(
            summary = "Запрос сброса пароля",
            description = "Отправляет ссылку на email. Если email не найден — всё равно возвращает 200 " +
                          "(не раскрываем существование аккаунта). Ссылка действительна 1 час.",
            responses = @ApiResponse(responseCode = "200", description = "Запрос принят")
    )
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Установка нового пароля по токену из письма",
            description = "Принимает token из URL-параметра и newPassword. При успехе выдаёт JWT.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пароль изменён, JWT выдан",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Токен недействителен или истёк")
            }
    )
    @PostMapping("/auth/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // ── ФАНТОМНЫЕ АККАУНТЫ ────────────────────────────────────────────────────

    @Operation(
            summary = "Привязка фантомного аккаунта к реальному пользователю",
            description = "Активирует аккаунт, созданный администратором. " +
                          "Отправляет код подтверждения на email. JWT не выдаётся до подтверждения.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Аккаунт активирован, код отправлен на email",
                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный код или email занят")
            }
    )
    @PostMapping("/auth/phantom/claim")
    public ResponseEntity<RegisterResponse> claimPhantom(@RequestBody ClaimPhantomRequest request) {
        return ResponseEntity.ok(authService.claimPhantom(request));
    }

    @Operation(
            summary = "Создание фантомного аккаунта [ADMIN]",
            description = "Создаёт аккаунт-заглушку без email. Возвращает код активации.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Фантом создан",
                            content = @Content(schema = @Schema(implementation = PhantomResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    @PostMapping("/admin/phantom/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhantomResponse> createPhantom(@RequestBody CreatePhantomRequest request) {
        return ResponseEntity.ok(authService.createPhantom(request));
    }
}
