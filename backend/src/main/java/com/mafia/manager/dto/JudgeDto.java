package com.mafia.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class JudgeDto {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isJudge;
    private Boolean canJudgeFinals;
    private Boolean canBeHeadJudge;
    private LocalDate judgeSince;
    private String clubName;
}
