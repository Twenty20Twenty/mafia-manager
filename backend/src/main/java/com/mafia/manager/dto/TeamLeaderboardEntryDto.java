// java/com/mafia/manager/dto/TeamLeaderboardEntryDto.java
package com.mafia.manager.dto;

/**
 * Проекция для командной таблицы лидеров.
 */
public interface TeamLeaderboardEntryDto {

    Long   getTeamId();
    String getTeamName();
    Double getTotalScore();
    Long   getTotalWins();
    Long   getMembersCount();

    static TeamLeaderboardEntryDto withHiddenScores(TeamLeaderboardEntryDto original) {
        return new TeamLeaderboardEntryDto() {
            public Long   getTeamId()       { return original.getTeamId(); }
            public String getTeamName()     { return original.getTeamName(); }
            public Double getTotalScore()   { return null; }
            public Long   getTotalWins()    { return null; }
            public Long   getMembersCount() { return original.getMembersCount(); }
        };
    }
}
