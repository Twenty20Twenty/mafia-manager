package com.mafia.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Запрос на ручное задание списка финалистов (Top-10).
 *
 * <p>Позволяет организатору вручную указать до 10 игроков,
 * которые выходят в финал, независимо от текущего лидерборда.</p>
 */
@Data
@Schema(description = "Запрос на ручное задание финалистов")
public class SetFinalistsRequest {

    @Schema(description = "Список ID игроков-финалистов (не более 10)",
            example = "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]")
    private List<Long> playerIds;

    @Schema(description = "Зафиксировать список (запрещает дальнейшее изменение без сброса)",
            example = "false")
    private boolean lock;
}
