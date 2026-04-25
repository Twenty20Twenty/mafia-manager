package com.mafia.manager.controller;

import com.mafia.manager.service.AvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Эндпоинты загрузки аватаров.
 *
 * <p>Отдельный контроллер (не встроен в UserController / ClubController),
 * чтобы не засорять основные контроллеры multipart-логикой.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Avatars", description = "Загрузка и удаление аватаров пользователей и клубов")
public class AvatarController {

    private final AvatarService avatarService;

    // ── Пользователи ─────────────────────────────────────────────────────────

    @Operation(
            summary = "Загрузить аватар пользователя",
            description = "Принимает multipart/form-data с полем 'file'. " +
                          "Допустимые форматы: JPEG, PNG, WebP, GIF. Максимальный размер: 5 МБ.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Аватар загружен, возвращает { avatarUrl }"),
                    @ApiResponse(responseCode = "400", description = "Невалидный файл"),
                    @ApiResponse(responseCode = "403", description = "Нет прав")
            }
    )
    @PostMapping(
            value = "/api/users/{userId}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadUserAvatar(
            @Parameter(description = "ID пользователя") @PathVariable Long userId,
            @Parameter(description = "Файл изображения") @RequestParam("file") MultipartFile file
    ) {
        String avatarUrl = avatarService.uploadUserAvatar(userId, file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @Operation(
            summary = "Удалить аватар пользователя",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/users/{userId}/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteUserAvatar(
            @PathVariable Long userId
    ) {
        avatarService.deleteUserAvatar(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Клубы ────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Загрузить логотип клуба",
            description = "Принимает multipart/form-data с полем 'file'. " +
                          "Допустимые форматы: JPEG, PNG, WebP, GIF. Максимальный размер: 5 МБ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/api/clubs/{clubId}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadClubAvatar(
            @Parameter(description = "ID клуба") @PathVariable Long clubId,
            @Parameter(description = "Файл изображения") @RequestParam("file") MultipartFile file
    ) {
        String avatarUrl = avatarService.uploadClubAvatar(clubId, file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @Operation(
            summary = "Удалить логотип клуба",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/clubs/{clubId}/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteClubAvatar(
            @PathVariable Long clubId
    ) {
        avatarService.deleteClubAvatar(clubId);
        return ResponseEntity.noContent().build();
    }
}
