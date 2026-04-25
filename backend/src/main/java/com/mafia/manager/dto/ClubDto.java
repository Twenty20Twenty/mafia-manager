package com.mafia.manager.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ClubDto {
    private Long id;
    private String name;
    private String city;
    private String logoUrl;
    private String description;
    private String socialLink;
    private Boolean isTournamentOperator;
    private Long presidentId;
    private String presidentName;
    private List<UserDto> members; // Упрощенный список
}
