package com.mafia.manager.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeedingExceptionDto {
    private Long id;
    private Long tournamentId;
    private Long player1Id;
    private String player1Nickname;
    private Long player2Id;
    private String player2Nickname;
}
