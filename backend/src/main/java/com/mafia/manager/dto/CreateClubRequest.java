package com.mafia.manager.dto;
import lombok.Data;

@Data
public class CreateClubRequest {
    private String name;
    private String city;
    private String description;
    private String socialLink;
    private String logoUrl;
}
