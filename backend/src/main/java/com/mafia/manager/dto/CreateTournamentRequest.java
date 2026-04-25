// java/com/mafia/manager/dto/CreateTournamentRequest.java
package com.mafia.manager.dto;

import com.mafia.manager.entity.json.TournamentSettings;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateTournamentRequest {

    private String title;
    private String description;

    // clubId убран — бэкенд сам определяет клуб из JWT-контекста
    // Оставлен для обратной совместимости (игнорируется сервером)
    // private Long clubId; // УДАЛЕНО — не доверяем клиенту

    private String    type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long      cityId;

    /** ID главного судьи (nullable — можно снять ГС, передав null или 0) */
    private Long headJudgeId;

    /**
     * ID судьи финальной стадии.
     * Сохраняется в settings.finalJudgeId (JSONB).
     * Nullable — нет обязательного финального судьи.
     */
    private Long finalJudgeId;

    /**
     * Статус турнира при обновлении.
     * Допустимые значения строго из TournamentStatus enum:
     *   registration | active | completed | archived
     * При создании игнорируется — всегда ставится registration.
     */
    private String status;

    /** JSONB настройки турнира */
    private TournamentSettings settings;
}
