package com.mafia.manager.dto;

import com.mafia.manager.entity.json.TournamentSettings;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TournamentDto {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long clubId;
    private String clubName;
    private Long cityId;
    private String cityName;
    private TournamentSettings settings;
    private Integer participantsCount;
    private Long organizerId;
    private Long headJudgeId;

    private Integer completedGamesCount;

    private String organizerName;
    private String headJudgeName;
    private String organizerAvatar;
    private String headJudgeAvatar;
}
