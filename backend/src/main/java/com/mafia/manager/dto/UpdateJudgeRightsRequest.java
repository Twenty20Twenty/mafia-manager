package com.mafia.manager.dto;

import lombok.Data;

@Data
public class UpdateJudgeRightsRequest {
    private Boolean isJudge;
    private Boolean canJudgeFinals;
    private Boolean canBeHeadJudge;
}
