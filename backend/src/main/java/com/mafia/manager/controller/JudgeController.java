package com.mafia.manager.controller;

import com.mafia.manager.dto.JudgeDto;
import com.mafia.manager.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judges")
@RequiredArgsConstructor
@Tag(name = "Judges", description = "Реестр судей и их права")
public class JudgeController {

    private final JudgeService judgeService;

    @Operation(
            summary = "Список судей с фильтрацией и пагинацией",
            parameters = {
                    @Parameter(name = "canJudgeFinals", description = "Фильтр: может судить финалы"),
                    @Parameter(name = "canBeHead",      description = "Фильтр: может быть главным судьёй"),
                    @Parameter(name = "search",         description = "Поиск по никнейму"),
            }
    )
    @GetMapping
    public ResponseEntity<Page<JudgeDto>> getJudges(
            @RequestParam(required = false) Boolean canJudgeFinals,
            @RequestParam(required = false) Boolean canBeHead,
            @RequestParam(required = false) String  search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(judgeService.getAllJudges(canJudgeFinals, canBeHead, search, pageable));
    }

    @Operation(summary = "Получить данные судьи по ID пользователя")
    @GetMapping("/{userId}")
    public ResponseEntity<JudgeDto> getJudgeByUserId(@PathVariable Long userId) {
        return judgeService.getJudgeByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
