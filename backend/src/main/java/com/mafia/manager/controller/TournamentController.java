package com.mafia.manager.controller;

import com.mafia.manager.dto.*;
import com.mafia.manager.service.SeedingService;
import com.mafia.manager.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournaments", description = "Управление турнирами, участниками, рассадкой и лидербордами")
public class TournamentController {

    private final TournamentService tournamentService;
    private final SeedingService    seedingService;

    // ── LIST / GET ───────────────────────────────────────────────────────────

    @Operation(summary = "Список турниров с фильтрацией",
            parameters = {
                    @Parameter(name = "type",   description = "Тип турнира: personal | team | rating"),
                    @Parameter(name = "status", description = "Статус: registration | active | completed | archived"),
                    @Parameter(name = "clubId", description = "ID клуба-организатора"),
                    @Parameter(name = "search", description = "Поиск по названию"),
            })
    @GetMapping
    public List<TournamentDto> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long   clubId,
            @RequestParam(required = false) String search
    ) {
        return tournamentService.getAll(type, status, clubId, search);
    }

    @Operation(summary = "Получить турнир по ID")
    @GetMapping("/{id}")
    public TournamentDto getById(@PathVariable Long id) {
        return tournamentService.getById(id);
    }

    // ── CREATE / UPDATE ──────────────────────────────────────────────────────

    @Operation(summary = "Создать турнир",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public TournamentDto create(@RequestBody CreateTournamentRequest request) {
        return tournamentService.create(request);
    }

    @Operation(summary = "Обновить турнир [Организатор / ГС / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public TournamentDto update(@PathVariable Long id, @RequestBody CreateTournamentRequest request) {
        return tournamentService.update(id, request);
    }

    // ── PARTICIPANTS ─────────────────────────────────────────────────────────

    @Operation(summary = "Список участников турнира")
    @GetMapping("/{id}/participants")
    public List<UserDto> getParticipants(@PathVariable Long id) {
        return tournamentService.getParticipants(id);
    }

    @Operation(summary = "Подать заявку на участие в турнире",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/apply")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> apply(@PathVariable Long id) {
        tournamentService.apply(id);
        return ResponseEntity.ok("Заявка подана");
    }

    @Operation(summary = "Добавить участника вручную [Организатор / ГС / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/participants")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> addParticipant(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        tournamentService.addParticipant(id, userId);
        return ResponseEntity.ok("Участник добавлен");
    }

    @Operation(summary = "Удалить участника из турнира [Организатор / ГС / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/participants/{uid}")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> removeParticipant(@PathVariable Long id, @PathVariable Long uid) {
        tournamentService.removeParticipant(id, uid);
        return ResponseEntity.ok("Участник удален");
    }

    @Operation(summary = "Изменить статус участника (принять / отклонить заявку)",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = @Parameter(name = "status", description = "approved | rejected | pending"))
    @PatchMapping("/{id}/participants/{uid}/status")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> updateParticipantStatus(
            @PathVariable Long id,
            @PathVariable Long uid,
            @RequestParam String status
    ) {
        tournamentService.updateParticipantStatus(id, uid, status);
        return ResponseEntity.ok("Статус участника обновлён");
    }

    // ── SEEDING EXCEPTIONS ───────────────────────────────────────────────────

    @Operation(summary = "Список исключений рассадки (пары, которые нельзя сажать вместе)")
    @GetMapping("/{id}/exceptions")
    public List<SeedingExceptionDto> getExceptions(@PathVariable Long id) {
        return tournamentService.getExceptions(id);
    }

    @Operation(summary = "Добавить исключение рассадки",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/exceptions")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> addException(
            @PathVariable Long id,
            @RequestParam Long p1,
            @RequestParam Long p2
    ) {
        tournamentService.addException(id, p1, p2);
        return ResponseEntity.ok("Исключение добавлено");
    }

    @Operation(summary = "Удалить исключение рассадки",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/exceptions/{exId}")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> deleteException(@PathVariable Long id, @PathVariable Long exId) {
        tournamentService.deleteException(id, exId);
        return ResponseEntity.ok("Исключение удалено");
    }

    // ── SEEDING GENERATION ───────────────────────────────────────────────────

    @Operation(summary = "Сгенерировать рассадку на следующий тур",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/generate-seeding")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> generateSeeding(@PathVariable Long id) {
        try {
            seedingService.generateNextRound(id);
            return ResponseEntity.ok("Рассадка успешно сгенерирована");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Зафиксировать топ-10 отборочных участников",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/fix-qualifiers")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> fixQualifiers(@PathVariable Long id) {
        try {
            tournamentService.fixQualifiers(id);
            return ResponseEntity.ok("Топ-10 участников зафиксированы");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Сгенерировать финальную рассадку",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/generate-final-seeding")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> generateFinalSeeding(@PathVariable Long id) {
        try {
            seedingService.generateFinalRound(id);
            return ResponseEntity.ok("Финальная рассадка сгенерирована");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── TABLE JUDGES ─────────────────────────────────────────────────────────

    @Operation(summary = "Получить назначения судей по столам (tableNumber → judgeId)")
    @GetMapping("/{id}/table-judges")
    public ResponseEntity<Map<String, Long>> getTableJudges(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getTableJudges(id));
    }

    @Operation(summary = "Обновить назначения судей по столам",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/table-judges")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> updateTableJudges(
            @PathVariable Long id,
            @RequestBody Map<String, Long> tableJudges
    ) {
        tournamentService.updateTableJudges(id, tableJudges);
        return ResponseEntity.ok("Судьи столов обновлены");
    }

    // ── JUDGES ───────────────────────────────────────────────────────────────

    @Operation(summary = "Получить список ID судей турнира")
    @GetMapping("/{id}/judges")
    public ResponseEntity<List<Long>> getTournamentJudges(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentJudges(id));
    }

    @Operation(summary = "Обновить список судей турнира",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/judges")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> updateTournamentJudges(
            @PathVariable Long id,
            @RequestBody List<Long> judgeIds
    ) {
        tournamentService.updateTournamentJudges(id, judgeIds);
        return ResponseEntity.ok("Судьи турнира сохранены");
    }

    // ── LEADERBOARDS ─────────────────────────────────────────────────────────

    @Operation(summary = "Таблица лидеров (личная / рейтинговая)",
            parameters = {
                    @Parameter(name = "includeFinals", description = "Включать финальные игры"),
                    @Parameter(name = "sortBy",        description = "Поле сортировки: total | wins | sheriff_wins | ..."),
            })
    @GetMapping("/{id}/leaderboard")
    public List<LeaderboardEntryDto> getLeaderboard(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeFinals,
            @RequestParam(defaultValue = "total") String  sortBy
    ) {
        return tournamentService.getLeaderboard(id, includeFinals, sortBy);
    }

    @Operation(summary = "Командная таблица лидеров")
    @GetMapping("/{id}/teams-leaderboard")
    public List<TeamLeaderboardEntryDto> getTeamLeaderboard(@PathVariable Long id) {
        return tournamentService.getTeamLeaderboard(id);
    }

    @Operation(summary = "Номинации турнира",
            parameters = @Parameter(name = "calcMode", description = "Режим расчёта: sum | avg"))
    @GetMapping("/{id}/nominations")
    public List<NominationDto> getNominations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "sum") String calcMode
    ) {
        return tournamentService.getNominations(id, calcMode);
    }

    // ── ФИНАЛИСТЫ ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Получить текущий список финалистов",
            description = "Возвращает ID и флаг фиксации из settings.top10PlayerIds"
    )
    @GetMapping("/{id}/finalists")
    public ResponseEntity<FinalistsDto> getFinalists(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getFinalists(id));
    }

    @Operation(
            summary = "Заполнить финалистов автоматически из лидерборда [Организатор / ГС / Админ]",
            description = "Берёт топ-10 по текущему лидерборду. Не фиксирует — можно скорректировать.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/finalists/auto-fill")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> autoFillFinalists(@PathVariable Long id) {
        try {
            tournamentService.autoFillFinalists(id);
            return ResponseEntity.ok("Финалисты заполнены из лидерборда");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Задать финалистов вручную [Организатор / ГС / Админ]",
            description = "Принимает список ID игроков (до 10). lock=true фиксирует список.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}/finalists")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> setFinalists(
            @PathVariable Long id,
            @RequestBody SetFinalistsRequest request
    ) {
        try {
            tournamentService.setFinalists(id, request);
            return ResponseEntity.ok("Финалисты сохранены");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Зафиксировать финалистов (lock) [Организатор / ГС / Админ]",
            description = "Устанавливает areQualifiersFixed=true. " +
                    "После фиксации изменение списка требует явного разблокирования.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/finalists/lock")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> lockFinalists(@PathVariable Long id) {
        try {
            tournamentService.setFinalists(id, buildLockRequest());
            return ResponseEntity.ok("Финалисты зафиксированы");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Снять фиксацию финалистов [Организатор / ГС / Админ]",
            description = "Устанавливает areQualifiersFixed=false. Позволяет снова редактировать список.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/finalists/unlock")
    @PreAuthorize("@permissionService.canManageTournament(#id)")
    public ResponseEntity<?> unlockFinalists(@PathVariable Long id) {
        try {
            tournamentService.unlockFinalists(id);
            return ResponseEntity.ok("Фиксация снята");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Вспомогательные методы контроллера ───────────────────────────────────

    private SetFinalistsRequest buildLockRequest() {
        SetFinalistsRequest req = new SetFinalistsRequest();
        req.setLock(true);
        return req;
    }

}
