package com.mafia.manager.controller;

import com.mafia.manager.dto.DeleteGamesRequest;
import com.mafia.manager.dto.GameProtocolDto;
import com.mafia.manager.dto.SwapSlotRequest;
import com.mafia.manager.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Games", description = "Протоколы игр, рассадка, удаление")
public class GameController {

    private final GameService gameService;

    // ── СОЗДАНИЕ ─────────────────────────────────────────────────────────────

    @Operation(summary = "Создать игру в турнире [Судья / Организатор / ГС / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/api/tournaments/{tournamentId}/games")
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<GameProtocolDto> createGame(
            @PathVariable Long tournamentId,
            @RequestBody CreateGameRequest request
    ) {
        return ResponseEntity.ok(gameService.createGame(tournamentId, request.getDate(), request.getJudgeId()));
    }

    // ── ЧТЕНИЕ ───────────────────────────────────────────────────────────────

    @Operation(summary = "Получить протокол игры")
    @GetMapping("/api/games/{id}")
    public ResponseEntity<GameProtocolDto> getGame(
            @Parameter(description = "ID игры") @PathVariable Long id
    ) {
        return ResponseEntity.ok(gameService.getProtocol(id));
    }

    @Operation(summary = "Список игр турнира")
    @GetMapping("/api/tournaments/{tournamentId}/games")
    public ResponseEntity<List<GameProtocolDto>> getGames(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(gameService.getGamesByTournament(tournamentId));
    }

    // ── СОХРАНЕНИЕ ПРОТОКОЛА ─────────────────────────────────────────────────

    @Operation(summary = "Сохранить протокол игры [Судья / Организатор / ГС / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/api/games/{id}/protocol")
    @PreAuthorize("@permissionService.canEditGame(#id)")
    public ResponseEntity<?> updateProtocol(
            @Parameter(description = "ID игры") @PathVariable Long id,
            @RequestBody GameProtocolDto dto
    ) {
        gameService.saveProtocol(id, dto);
        return ResponseEntity.ok("Протокол сохранен");
    }

    // ── УДАЛЕНИЕ ОДНОЙ ИГРЫ ──────────────────────────────────────────────────

    @Operation(
            summary = "Удалить одну игру [Организатор / ГС / Админ]",
            description = "Судья стола не может удалить игру — только создавший или вышестоящий.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Игра удалена"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    @DeleteMapping("/api/games/{id}")
    @PreAuthorize("@permissionService.canDeleteGame(#id)")
    public ResponseEntity<?> deleteGame(
            @Parameter(description = "ID игры") @PathVariable Long id
    ) {
        gameService.deleteGame(id);
        return ResponseEntity.ok("Игра удалена");
    }

    // ── МАССОВОЕ УДАЛЕНИЕ ИГР ────────────────────────────────────────────────

    @Operation(
            summary = "Массовое удаление игр турнира [Организатор / ГС / Админ]",
            description = """
                    Режимы удаления (поле mode):
                    - ALL   — удалить все игры (сбрасывает флаг рассадки)
                    - ROUND — удалить один тур (fromRound)
                    - RANGE — удалить туры от fromRound до toRound включительно
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/tournaments/{tournamentId}/games")
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<?> deleteGames(
            @PathVariable Long tournamentId,
            @RequestBody DeleteGamesRequest request
    ) {
        try {
            gameService.deleteGames(tournamentId, request);
            return ResponseEntity.ok("Игры удалены");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── ЗАМЕНА ИГРОКА В СЛОТЕ ────────────────────────────────────────────────

    @Operation(
            summary = "Заменить игрока в слоте игры [Организатор / ГС / Судья стола]",
            description = """
                    Позволяет изменить состав стола в уже созданной (но не завершённой) игре.
                    Можно указать конкретный слот (slotNumber) или найти слот по oldUserId.
                    newUserId = null — освободить слот.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/api/games/{id}/slots")
    @PreAuthorize("@permissionService.canEditGame(#id)")
    public ResponseEntity<?> swapSlot(
            @Parameter(description = "ID игры") @PathVariable Long id,
            @RequestBody SwapSlotRequest request
    ) {
        try {
            GameProtocolDto updated = gameService.swapSlot(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── ВНУТРЕННИЕ DTO ───────────────────────────────────────────────────────

    @lombok.Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "Запрос на создание игры")
    public static class CreateGameRequest {

        @io.swagger.v3.oas.annotations.media.Schema(description = "Дата проведения (null = сегодня)", example = "2025-06-15")
        private LocalDate date;

        @io.swagger.v3.oas.annotations.media.Schema(description = "ID судьи (null = текущий пользователь)", example = "42")
        private Long judgeId;
    }
}
