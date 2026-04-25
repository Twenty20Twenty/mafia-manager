// java/com/mafia/manager/dto/LeaderboardEntryDto.java
package com.mafia.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface LeaderboardEntryDto {
    Long   getUserId();
    String getNickname();
    String getAvatarUrl();
    Double getTotalScore();
    Long   getGamesCount();
    Long   getTotalWins();
    Long   getSheriffWins();
    Long   getDonWins();
    Long   getFirstKilledCount();        // ← было getFirstKilled()
    Double getExtraPointsPositive();     // ← было getExtraPoints()
    Double getExtraPointsNegative();     // ← новое
    Double getPenaltyPoints();
    Double getCompensationPoints();
    Double getBestMovePoints();

    static LeaderboardEntryDto withHiddenScores(LeaderboardEntryDto original) {
        return new LeaderboardEntryDto() {
            public Long   getUserId()              { return original.getUserId(); }
            public String getNickname()             { return original.getNickname(); }
            public String getAvatarUrl()            { return original.getAvatarUrl(); }
            public Long   getGamesCount()           { return original.getGamesCount(); }
            public Double getTotalScore()           { return null; }
            public Long   getTotalWins()            { return null; }
            public Long   getSheriffWins()          { return null; }
            public Long   getDonWins()              { return null; }
            public Long   getFirstKilledCount()     { return null; }
            public Double getExtraPointsPositive()  { return null; }
            public Double getExtraPointsNegative()  { return null; }
            public Double getPenaltyPoints()        { return null; }
            public Double getCompensationPoints()   { return null; }
            public Double getBestMovePoints()       { return null; }
        };
    }
}