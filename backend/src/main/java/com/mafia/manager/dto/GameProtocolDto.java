package com.mafia.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class GameProtocolDto {
    private Long id;
    private Long tournamentId;
    private Integer round;
    private Integer table;
    private LocalDate date;
    private Long judgeId;
    private String judgeName;
    private String status;
    private String winner;

    private String stage;
    private BigDecimal coefficient;

    @JsonProperty("slots")
    private List<GameSlotDto> slots;

    @JsonProperty("bestMove")
    private BestMoveDto bestMove;
}
