package com.mafia.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Запрос на массовое удаление игр турнира.
 *
 * <p>Поддерживаются три режима:</p>
 * <ul>
 *   <li>{@code ALL}   — удалить всю рассадку (все игры турнира)</li>
 *   <li>{@code ROUND} — удалить конкретный тур</li>
 *   <li>{@code RANGE} — удалить игры за диапазон туров [fromRound, toRound]</li>
 * </ul>
 */
@Data
@Schema(description = "Запрос на массовое удаление игр")
public class DeleteGamesRequest {

    @Schema(description = "Режим удаления: ALL | ROUND | RANGE", example = "RANGE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Mode mode;

    @Schema(description = "Начало диапазона туров (для ROUND и RANGE)", example = "1")
    private Integer fromRound;

    @Schema(description = "Конец диапазона туров (для RANGE)", example = "3")
    private Integer toRound;

    public enum Mode { ALL, ROUND, RANGE }
}
