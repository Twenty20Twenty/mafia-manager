package com.mafia.manager.controller;

import com.mafia.manager.dto.*;
import com.mafia.manager.service.EmailVerificationService;
import com.mafia.manager.service.PlayerStatsService;
import com.mafia.manager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Профили игроков, статистика, смена email")
public class UserController {

    private final UserService               userService;
    private final PlayerStatsService        playerStatsService;
    private final EmailVerificationService  emailVerificationService;

    // ── ПРОФИЛЬ ───────────────────────────────────────────────────────────────

    @Operation(summary = "Список пользователей с поиском и пагинацией",
            parameters = @Parameter(name = "search", description = "Поиск по никнейму"))
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(search, pageable));
    }

    @Operation(summary = "Получить профиль пользователя по ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(
            summary = "Статистика игрока по периодам",
            description = "periodYear=null — за всё время, число — за год."
    )
    @GetMapping("/{id}/stats")
    public ResponseEntity<List<PlayerStatsDto>> getUserStats(@PathVariable Long id) {
        return ResponseEntity.ok(playerStatsService.getStats(id));
    }

    @Operation(summary = "Обновить профиль пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Operation(summary = "Удалить пользователя [ADMIN]",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
                    @ApiResponse(responseCode = "403", description = "Только администратор")
            })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── СМЕНА EMAIL ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Запросить смену email",
            description = "Отправляет 6-значный код на НОВЫЙ email. Старый email остаётся до подтверждения.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Код отправлен"),
                    @ApiResponse(responseCode = "400", description = "Email уже занят")
            }
    )
    @PostMapping("/{id}/request-email-change")
    public ResponseEntity<Void> requestEmailChange(
            @PathVariable Long id,
            @RequestBody RequestEmailChangeRequest request
    ) {
        emailVerificationService.requestEmailChange(id, request.getNewEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Подтвердить смену email по коду",
            description = "Принимает 6-значный код из письма. Обновляет users.email атомарно.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Email обновлён"),
                    @ApiResponse(responseCode = "400", description = "Неверный или истёкший код")
            }
    )
    @PostMapping("/{id}/confirm-email-change")
    public ResponseEntity<Void> confirmEmailChange(
            @PathVariable Long id,
            @RequestBody ConfirmEmailChangeRequest request
    ) {
        emailVerificationService.confirmEmailChange(id, request.getCode());
        return ResponseEntity.ok().build();
    }
}
