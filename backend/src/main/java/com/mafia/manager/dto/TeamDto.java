package com.mafia.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TeamDto {
    private Long id;
    private Long tournamentId;
    private String name;
    private List<Long> memberIds;
    private List<String> memberNicknames;
}
