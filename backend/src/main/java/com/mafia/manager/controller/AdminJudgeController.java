package com.mafia.manager.controller;

import com.mafia.manager.dto.JudgeDto;
import com.mafia.manager.dto.UpdateJudgeRightsRequest;
import com.mafia.manager.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/judges")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin / Judges", description = "Административное управление правами судей")
public class AdminJudgeController {

    private final JudgeService judgeService;

    @Operation(
            summary = "Обновить права судьи [ADMIN]",
            description = "Управляет флагами: isJudge, canJudgeFinals, canBeHeadJudge.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Права обновлены"),
                    @ApiResponse(responseCode = "403", description = "Только администратор"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @PutMapping("/{userId}")
    public ResponseEntity<JudgeDto> updateRights(
            @PathVariable Long userId,
            @RequestBody UpdateJudgeRightsRequest request
    ) {
        return ResponseEntity.ok(judgeService.updateRights(userId, request));
    }
}
