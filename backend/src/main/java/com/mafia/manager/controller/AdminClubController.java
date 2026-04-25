package com.mafia.manager.controller;

import com.mafia.manager.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/clubs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin / Clubs", description = "Административное управление правами клубов")
public class AdminClubController {

    private final ClubService clubService;

    @Operation(
            summary = "Установить / снять статус «Турнирный Оператор» у клуба",
            description = "Клуб с этим статусом может создавать официальные турниры.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = @Parameter(name = "status", description = "true — выдать, false — отозвать"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Статус изменён"),
                    @ApiResponse(responseCode = "403", description = "Только администратор"),
                    @ApiResponse(responseCode = "404", description = "Клуб не найден")
            }
    )
    @PutMapping("/{id}/operator")
    public ResponseEntity<?> setTournamentOperatorStatus(
            @PathVariable Long id,
            @RequestParam boolean status
    ) {
        clubService.setTournamentOperator(id, status);
        return ResponseEntity.ok("Статус турнирного оператора для клуба " + id + " изменен на " + status);
    }
}
