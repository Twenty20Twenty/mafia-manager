// java/com/mafia/manager/dto/TeamLeaderboardEntryDto.java
package com.mafia.manager.dto;

/**
 * Проекция для командной таблицы лидеров.
 * Содержит те же колонки что и личный лидерборд, но агрегированные по команде.
 */
public interface TeamLeaderboardEntryDto {

    Long   getTeamId();
    String getTeamName();
    Long   getMembersCount();

    Double getTotalScore();
    Long   getGamesCount();
    Long   getTotalWins();
    Long   getSheriffWins();
    Long   getDonWins();
    Long   getFirstKilledCount();
    Double getExtraPointsPositive();
    Double getExtraPointsNegative();
    Double getPenaltyPoints();
    Double getCompensationPoints();
    Double getBestMovePoints();

    static TeamLeaderboardEntryDto withHiddenScores(TeamLeaderboardEntryDto original) {
        return new TeamLeaderboardEntryDto() {
            public Long   getTeamId()              { return original.getTeamId(); }
            public String getTeamName()             { return original.getTeamName(); }
            public Long   getMembersCount()         { return original.getMembersCount(); }
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
