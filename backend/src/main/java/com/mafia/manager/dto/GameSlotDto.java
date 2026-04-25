package com.mafia.manager.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GameSlotDto {
    private Long id;
    private Integer slotNumber;
    private Long playerId;
    private String playerNickname;
    private String playerAvatar;
    private String role;
    private Boolean isFirstKilled;
    private Integer fouls;
    private BigDecimal extraPos;
    private BigDecimal extraNeg;
    private BigDecimal penalty;


    private BigDecimal CompensationPoints;
    private BigDecimal ComputedScore;
}
