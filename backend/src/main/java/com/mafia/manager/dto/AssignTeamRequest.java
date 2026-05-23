package com.mafia.manager.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssignTeamRequest {
    /** Список ID участников, которых нужно добавить в команду */
    private List<Long> memberIds;
}
