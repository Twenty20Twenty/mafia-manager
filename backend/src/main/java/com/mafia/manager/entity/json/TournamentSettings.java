// java/com/mafia/manager/entity/json/TournamentSettings.java
package com.mafia.manager.entity.json;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class TournamentSettings implements Serializable {

    // --- Основные параметры ---
    private Integer maxParticipants;
    private Integer roundsCount;
    private Integer finalRoundsCount;
    private Double  finalCoefficient;
    private Integer ratingThreshold;
    private Integer teamSize;

    // --- Флаги ---
    private Boolean isSeedingGenerated;
    private Boolean areResultsHidden;
    private Boolean areQualifiersFixed;

    private Long finalJudgeId;

    // --- Ссылки ---
    private String socialLink;

    // --- Швейцарская система ---
    private Boolean      isSwissSystem;
    private Integer      swissRoundsStart;
    private List<Integer> swissTiers;

    private Map<String, Long> staticTableJudges;

    private List<Long> Top10PlayerIds;
}
