package com.mafia.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO записи о турнире в профиле игрока.
 * Содержит базовую информацию о турнире и позицию игрока (если результаты открыты).
 */
@Data
@Builder
public class PlayerTournamentDto {

    private Long      tournamentId;
    private String    title;
    private String    type;     // individual | team | season
    private String    status;   // registration | active | completed | archived
    private LocalDate startDate;
    private LocalDate endDate;
    private String    clubName;
    private Long      clubId;

    /**
     * Место в личном зачёте. null если турнир не завершён или результаты скрыты
     * или тип season (рейтинг без финальных мест).
     */
    private Integer individualRank;

    /**
     * Место в командном зачёте. Заполняется только для type=team,
     * если турнир завершён и результаты открыты.
     */
    private Integer teamRank;
}
