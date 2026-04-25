package com.mafia.manager.dto;

import java.math.BigDecimal;

public interface NominationDto {
    Long getTournamentId();

    Long getUserId();

    String getNickname();

    String getAvatarUrl();

    Integer getGamesPlayed();

    BigDecimal getMvpScore();

    BigDecimal getNominationScoreCivilian();

    BigDecimal getNominationScoreSheriff();

    BigDecimal getNominationScoreMafia();

    BigDecimal getNominationScoreDon();
}
