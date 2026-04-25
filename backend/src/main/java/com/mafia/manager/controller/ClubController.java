package com.mafia.manager.controller;

import com.mafia.manager.dto.ClubDto;
import com.mafia.manager.dto.CreateClubRequest;
import com.mafia.manager.dto.UserDto;
import com.mafia.manager.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Clubs", description = "Управление клубами и членством")
public class ClubController {

    private final ClubService clubService;

    // ── LIST / GET ───────────────────────────────────────────────────────────

    @Operation(summary = "Список всех клубов")
    @GetMapping
    public List<ClubDto> getAll() {
        return clubService.getAllClubs();
    }

    @Operation(summary = "Получить клуб по ID")
    @GetMapping("/{id}")
    public ClubDto getById(@PathVariable Long id) {
        return clubService.getClubById(id);
    }

    // ── CREATE / UPDATE / DELETE ─────────────────────────────────────────────

    @Operation(summary = "Создать клуб",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ClubDto create(@RequestBody CreateClubRequest request) {
        return clubService.createClub(request);
    }

    @Operation(summary = "Обновить клуб [Президент / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ClubDto update(@PathVariable Long id, @RequestBody CreateClubRequest request) {
        return clubService.updateClub(id, request);
    }

    @Operation(summary = "Удалить клуб [Президент / Админ]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        clubService.deleteClub(id);
        return ResponseEntity.ok("Клуб удалён");
    }

    // ── RIGHTS ───────────────────────────────────────────────────────────────

    @Operation(summary = "Выдать члену клуба право создавать турниры",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/members/{uid}/grant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> grantRight(@PathVariable Long id, @PathVariable Long uid) {
        clubService.grantTournamentRight(id, uid);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Отозвать у члена клуба право создавать турниры",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/members/{uid}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> revokeRight(@PathVariable Long id, @PathVariable Long uid) {
        clubService.revokeTournamentRight(id, uid);
        return ResponseEntity.ok().build();
    }

    // ── MEMBERSHIP ───────────────────────────────────────────────────────────

    @Operation(summary = "Подать заявку на вступление в клуб",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> join(@PathVariable Long id) {
        clubService.joinRequest(id);
        return ResponseEntity.ok("Заявка отправлена");
    }

    @Operation(summary = "Список заявок на вступление [Президент]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}/requests")
    @PreAuthorize("isAuthenticated()")
    public List<UserDto> getRequests(@PathVariable Long id) {
        return clubService.getRequests(id);
    }

    @Operation(summary = "Одобрить или отклонить заявку на вступление [Президент]",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/requests/{uid}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @PathVariable Long uid,
            @RequestParam boolean approve
    ) {
        clubService.approveRequest(id, uid, approve);
        return ResponseEntity.ok(approve ? "Принят" : "Отклонён");
    }

    @Operation(summary = "Покинуть клуб",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> leave(@PathVariable Long id) {
        clubService.leaveClub(id);
        return ResponseEntity.ok("Вы покинули клуб");
    }

    @Operation(summary = "Исключить участника из клуба [Президент]",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Участник исключён"),
                    @ApiResponse(responseCode = "403", description = "Только президент может кикнуть")
            })
    @DeleteMapping("/{id}/members/{uid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> kickMember(@PathVariable Long id, @PathVariable Long uid) {
        clubService.kickMember(id, uid);
        return ResponseEntity.ok("Участник исключён из клуба");
    }
}
