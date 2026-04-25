package com.mafia.manager.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityDto {
    private Long id;
    private String name;
    private Long countryId;
    private String countryName;
}
