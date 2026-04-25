package com.mafia.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Запрос на замену игрока в слоте сгенерированной игры.
 *
 * <p>Позволяет заменить {@code oldUserId} на {@code newUserId} во всех слотах
 * указанной игры, либо заменить игрока в конкретном слоте {@code slotNumber}.</p>
 */
@Data
@Schema(description = "Запрос на замену состава стола")
public class SwapSlotRequest {

    @Schema(description = "ID игрока которого убираем", example = "42")
    private Long oldUserId;

    @Schema(description = "ID игрока которого ставим (null = освободить слот)", example = "77")
    private Long newUserId;

    @Schema(description = "Номер слота для замены (1–10). Если задан, заменяем конкретный слот. " +
                          "Если null — ищем слот по oldUserId.", example = "5")
    private Integer slotNumber;
}
