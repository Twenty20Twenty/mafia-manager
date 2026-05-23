package com.mafia.manager.controller;

import com.mafia.manager.dto.AssignTeamRequest;
import com.mafia.manager.dto.CreateTeamRequest;
import com.mafia.manager.dto.TeamDto;
import com.mafia.manager.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Управление командами в командном турнире")
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "Список команд турнира")
    @GetMapping
    public List<TeamDto> getTeams(@PathVariable Long tournamentId) {
        return teamService.getTeams(tournamentId);
    }

    @Operation(summary = "Создать команду", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<TeamDto> createTeam(
            @PathVariable Long tournamentId,
            @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.ok(teamService.createTeam(tournamentId, request));
    }

    @Operation(summary = "Переименовать команду", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{teamId}/name")
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<TeamDto> renameTeam(
            @PathVariable Long tournamentId,
            @PathVariable Long teamId,
            @RequestBody RenameRequest request
    ) {
        return ResponseEntity.ok(teamService.renameTeam(tournamentId, teamId, request.getName()));
    }

    @Operation(summary = "Удалить команду", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{teamId}")
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long tournamentId,
            @PathVariable Long teamId
    ) {
        teamService.deleteTeam(tournamentId, teamId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Назначить участников в команду", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{teamId}/members")
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<TeamDto> assignMembers(
            @PathVariable Long tournamentId,
            @PathVariable Long teamId,
            @RequestBody AssignTeamRequest request
    ) {
        return ResponseEntity.ok(teamService.assignMembers(tournamentId, teamId, request));
    }

    @Operation(summary = "Открепить участника от команды", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/members/{userId}")
    @PreAuthorize("@permissionService.canManageTournament(#tournamentId)")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long tournamentId,
            @PathVariable Long userId
    ) {
        teamService.removeMemberFromTeam(tournamentId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Сгенерировать случайные названия команд")
    @GetMapping("/generate-names")
    public List<String> generateNames(
            @PathVariable Long tournamentId,
            @RequestParam(defaultValue = "10") int count
    ) {
        return teamService.generateTeamNames(count);
    }

    // ── Internal DTO ─────────────────────────────────────────────────────────

    @Data
    public static class RenameRequest {
        private String name;
    }
}
